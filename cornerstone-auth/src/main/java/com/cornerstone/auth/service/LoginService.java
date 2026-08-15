package com.cornerstone.auth.service;

import com.cornerstone.api.client.AuthUserClient;
import com.cornerstone.api.client.LoginLogClient;
import com.cornerstone.api.dto.LoginLogDTO;
import com.cornerstone.api.dto.UserAuthDTO;
import com.cornerstone.common.core.ErrorCode;
import com.cornerstone.common.core.Result;
import com.cornerstone.common.exception.BusinessException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * 用户登录服务：校验用户名密码，成功后签发自带角色权限的 JWT。
 *
 * <p>认证信息经 {@link AuthUserClient} 从 system 获取（契约先行，禁止直连 system HTTP 接口或数据库）。 JWT
 * claims：subject=userId、 username、roles（角色集合）、scope（权限集合，供下游 {@code @PreAuthorize} 从 scope 读取）。
 */
@Service
public class LoginService {

    private static final Logger log = LoggerFactory.getLogger(LoginService.class);

    /** 签发的 access_token 有效期（12 小时） */
    public static final Duration TOKEN_TTL = Duration.ofHours(12);

    private static final String TOKEN_TYPE_BEARER = "Bearer";

    /** JWT issuer：多环境（生产域名）可经 cornerstone.auth.issuer 配置覆盖 */
    @Value("${cornerstone.auth.issuer:http://localhost:8081}")
    private String issuer;

    private final AuthUserClient authUserClient;
    private final LoginLogClient loginLogClient;
    private final JwtEncoder jwtEncoder;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redis;

    public LoginService(
            AuthUserClient authUserClient,
            LoginLogClient loginLogClient,
            JwtEncoder jwtEncoder,
            PasswordEncoder passwordEncoder,
            StringRedisTemplate redis) {
        this.authUserClient = authUserClient;
        this.loginLogClient = loginLogClient;
        this.jwtEncoder = jwtEncoder;
        this.passwordEncoder = passwordEncoder;
        this.redis = redis;
    }

    /** 登录失败锁定：连续失败上限与锁定窗口 */
    private static final String FAIL_KEY_PREFIX = "login:fail:";

    private static final int MAX_FAILS = 5;
    private static final Duration LOCK_WINDOW = Duration.ofMinutes(5);

    /** 用户名密码登录；成功返回含 access_token 的响应，失败抛业务异常（401）。登录日志经 {@link LoginLogClient} 投递 system。 */
    public LoginResponse login(LoginRequest request, String clientIp) {
        String username = request.username();
        if (isLocked(username)) {
            // 锁定拒绝同样落登录日志（审计完整性）；提示剩余锁定时间（秒）便于用户等待
            recordLog(username, clientIp, "1", "登录失败次数过多，已锁定");
            Long remaining = remainingLockSeconds(username);
            String msg = remaining != null ? "登录失败次数过多，请 " + remaining + " 秒后再试" : "登录失败次数过多，请稍后再试";
            throw new BusinessException(ErrorCode.UNAUTHORIZED, msg);
        }
        UserAuthDTO user = findUser(username);
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            // 用户不存在时用请求中的用户名记录失败日志
            recordFailure(username);
            recordLog(username, clientIp, "1", "用户名或密码错误");
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        clearFails(username);
        Jwt jwt = issueToken(user);
        recordLog(username, clientIp, "0", "登录成功");
        return new LoginResponse(
                jwt.getTokenValue(),
                TOKEN_TYPE_BEARER,
                TOKEN_TTL.getSeconds(),
                user.getUserId(),
                user.getUsername(),
                user.getRoles());
    }

    /** 登录失败计数 +1（Redis，TTL 锁定窗口；Redis 不可用降级不阻塞登录） */
    private void recordFailure(String username) {
        try {
            String key = FAIL_KEY_PREFIX + username;
            Long count = redis.opsForValue().increment(key);
            if (count != null && count == 1) {
                redis.expire(key, LOCK_WINDOW);
            }
        } catch (Exception e) {
            log.warn("登录失败计数失败（Redis 不可用，降级）username={}", username, e);
        }
    }

    /** 是否已触发锁定（连续失败 >= MAX_FAILS；Redis 不可用时放行） */
    private boolean isLocked(String username) {
        try {
            String fails = redis.opsForValue().get(FAIL_KEY_PREFIX + username);
            return fails != null && Integer.parseInt(fails) >= MAX_FAILS;
        } catch (Exception e) {
            return false;
        }
    }

    /** 锁定剩余秒数（Redis key TTL）；Redis 不可用或无 TTL 时返回 null（提示不含时间） */
    private Long remainingLockSeconds(String username) {
        try {
            Long ttl = redis.getExpire(FAIL_KEY_PREFIX + username);
            return (ttl != null && ttl > 0) ? ttl : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** 登录成功清除失败计数 */
    private void clearFails(String username) {
        try {
            redis.delete(FAIL_KEY_PREFIX + username);
        } catch (Exception e) {
            // 忽略：清理失败不阻塞登录
        }
    }

    /** 把一条登录日志投递 system 落库。日志记录失败不阻塞登录主流程。 */
    private void recordLog(String username, String clientIp, String status, String msg) {
        try {
            LoginLogDTO dto = new LoginLogDTO();
            dto.setUsername(username);
            dto.setIpaddr(clientIp);
            dto.setStatus(status);
            dto.setMsg(msg);
            dto.setLoginTime(LocalDateTime.now());
            loginLogClient.record(dto);
        } catch (Exception e) {
            log.warn("登录日志投递失败 username={} status={}", username, status, e);
        }
    }

    /** 经契约从 system 按用户名取用户认证信息；返回 null 表示用户不存在。 */
    private UserAuthDTO findUser(String username) {
        Result<UserAuthDTO> result = authUserClient.findByUsername(username);
        if (result == null || !result.isSuccess()) {
            return null;
        }
        return result.getData();
    }

    /** 用 RSA 密钥签发 RS256 JWT：subject=userId，携带 username/roles/scope(permissions)。 */
    private Jwt issueToken(UserAuthDTO user) {
        Instant now = Instant.now();
        JwtClaimsSet claims =
                JwtClaimsSet.builder()
                        .issuer(issuer)
                        .subject(String.valueOf(user.getUserId()))
                        .issuedAt(now)
                        .expiresAt(now.plus(TOKEN_TTL))
                        .claim("username", user.getUsername())
                        .claim("roles", user.getRoles())
                        .claim("scope", user.getPermissions())
                        .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims));
    }
}
