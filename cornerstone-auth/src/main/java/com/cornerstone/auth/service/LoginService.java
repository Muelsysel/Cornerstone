package com.cornerstone.auth.service;

import com.cornerstone.api.client.AuthUserClient;
import com.cornerstone.api.dto.UserAuthDTO;
import com.cornerstone.common.core.ErrorCode;
import com.cornerstone.common.core.Result;
import com.cornerstone.common.exception.BusinessException;
import java.time.Duration;
import java.time.Instant;
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

    /** 签发的 access_token 有效期（12 小时） */
    public static final Duration TOKEN_TTL = Duration.ofHours(12);

    private static final String ISSUER = "http://localhost:8081";
    private static final String TOKEN_TYPE_BEARER = "Bearer";

    private final AuthUserClient authUserClient;
    private final JwtEncoder jwtEncoder;
    private final PasswordEncoder passwordEncoder;

    public LoginService(
            AuthUserClient authUserClient, JwtEncoder jwtEncoder, PasswordEncoder passwordEncoder) {
        this.authUserClient = authUserClient;
        this.jwtEncoder = jwtEncoder;
        this.passwordEncoder = passwordEncoder;
    }

    /** 用户名密码登录；成功返回含 access_token 的响应，失败抛业务异常（401）。 */
    public LoginResponse login(LoginRequest request) {
        UserAuthDTO user = findUser(request.username());
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        Jwt jwt = issueToken(user);
        return new LoginResponse(
                jwt.getTokenValue(),
                TOKEN_TYPE_BEARER,
                TOKEN_TTL.getSeconds(),
                user.getUserId(),
                user.getUsername(),
                user.getRoles());
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
                        .issuer(ISSUER)
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
