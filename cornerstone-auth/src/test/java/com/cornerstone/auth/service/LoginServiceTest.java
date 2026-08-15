package com.cornerstone.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cornerstone.api.client.AuthUserClient;
import com.cornerstone.api.client.LoginLogClient;
import com.cornerstone.api.dto.LoginLogDTO;
import com.cornerstone.api.dto.UserAuthDTO;
import com.cornerstone.common.core.ErrorCode;
import com.cornerstone.common.core.Result;
import com.cornerstone.common.exception.BusinessException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

/** 登录服务单测：锁定计数、Redis 降级、日志投递失败不阻塞、成功清计数与签发。 */
class LoginServiceTest {

    private final AuthUserClient authUserClient = mock(AuthUserClient.class);
    private final LoginLogClient loginLogClient = mock(LoginLogClient.class);
    private final JwtEncoder jwtEncoder = mock(JwtEncoder.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> redisValues = mock(ValueOperations.class);

    private LoginService service;

    @BeforeEach
    void setUp() throws Exception {
        when(redis.opsForValue()).thenReturn(redisValues);
        service =
                new LoginService(
                        authUserClient, loginLogClient, jwtEncoder, passwordEncoder, redis);
        // @Value 注入的 issuer 在纯单测中为 null，反射设置默认值
        java.lang.reflect.Field issuerField = LoginService.class.getDeclaredField("issuer");
        issuerField.setAccessible(true);
        issuerField.set(service, "http://localhost:8081");
    }

    private UserAuthDTO user(String username) {
        UserAuthDTO dto = new UserAuthDTO();
        dto.setUserId(1L);
        dto.setUsername(username);
        dto.setPassword("hash");
        dto.setRoles(new LinkedHashSet<>(Set.of("admin")));
        dto.setPermissions(new LinkedHashSet<>(Set.of("system:user:list")));
        return dto;
    }

    private Jwt jwt() {
        return new Jwt(
                "signed-token",
                Instant.now(),
                Instant.now().plus(LoginService.TOKEN_TTL),
                java.util.Map.of("alg", "RS256"),
                java.util.Map.of("sub", "1"));
    }

    @Test
    void successfulLoginIssuesTokenAndClearsFails() {
        when(authUserClient.findByUsername("admin")).thenReturn(Result.success(user("admin")));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
        when(redisValues.get("login:fail:admin")).thenReturn(null);
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwt());

        LoginResponse response = service.login(new LoginRequest("admin", "secret"), "127.0.0.1");

        assertThat(response.access_token()).isEqualTo("signed-token");
        assertThat(response.token_type()).isEqualTo("Bearer");
        assertThat(response.expires_in()).isEqualTo(LoginService.TOKEN_TTL.getSeconds());
        assertThat(response.userId()).isEqualTo(1L);
        verify(redis).delete("login:fail:admin");
        verify(loginLogClient, times(1)).record(any(LoginLogDTO.class));
    }

    @Test
    void successfulLoginIncludesDeptIdClaim() {
        // 回归：deptId 此前未写入 JWT → 网关无法透传 → 数据权限「本部门(4)/本部门及以下(3)」失效
        UserAuthDTO dto = user("admin");
        dto.setDeptId(100L);
        when(authUserClient.findByUsername("admin")).thenReturn(Result.success(dto));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
        when(redisValues.get("login:fail:admin")).thenReturn(null);
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwt());

        service.login(new LoginRequest("admin", "secret"), "127.0.0.1");

        org.mockito.ArgumentCaptor<JwtEncoderParameters> captor =
                org.mockito.ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder).encode(captor.capture());
        assertThat(captor.getValue().getClaims().getClaimAsString("deptId")).isEqualTo("100");
    }

    @Test
    void successfulLoginOmitsDeptIdClaimWhenNull() {
        // 无部门归属用户：JWT 不含 deptId（避免 null claim）
        when(authUserClient.findByUsername("admin")).thenReturn(Result.success(user("admin")));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
        when(redisValues.get("login:fail:admin")).thenReturn(null);
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwt());

        service.login(new LoginRequest("admin", "secret"), "127.0.0.1");

        org.mockito.ArgumentCaptor<JwtEncoderParameters> captor =
                org.mockito.ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder).encode(captor.capture());
        assertThat(captor.getValue().getClaims().getClaimAsString("deptId")).isNull();
    }

    @Test
    void wrongPasswordIncrementsFailureCounter() {
        when(authUserClient.findByUsername("admin")).thenReturn(Result.success(user("admin")));
        when(passwordEncoder.matches("bad", "hash")).thenReturn(false);
        when(redisValues.get("login:fail:admin")).thenReturn(null);
        when(redisValues.increment("login:fail:admin")).thenReturn(1L);

        assertThatThrownBy(() -> service.login(new LoginRequest("admin", "bad"), "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED.getCode());

        // 首次失败设置 TTL；锁定窗口内继续计数
        verify(redisValues).increment("login:fail:admin");
        verify(redis).expire(eq("login:fail:admin"), eq(Duration.ofMinutes(5)));
    }

    @Test
    void subsequentFailuresDoNotResetTtl() {
        when(authUserClient.findByUsername("admin")).thenReturn(Result.success(user("admin")));
        when(passwordEncoder.matches("bad", "hash")).thenReturn(false);
        when(redisValues.get("login:fail:admin")).thenReturn(null);
        when(redisValues.increment("login:fail:admin")).thenReturn(4L);

        assertThatThrownBy(() -> service.login(new LoginRequest("admin", "bad"), "127.0.0.1"))
                .isInstanceOf(BusinessException.class);

        verify(redis, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    void redisUnavailableDoesNotBlockLogin() {
        when(redisValues.get(anyString())).thenThrow(new RuntimeException("redis down"));
        when(authUserClient.findByUsername("admin")).thenReturn(Result.success(user("admin")));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwt());

        LoginResponse response = service.login(new LoginRequest("admin", "secret"), "127.0.0.1");

        assertThat(response.access_token()).isEqualTo("signed-token");
    }

    @Test
    void failCounterIncrementExceptionStillRejects() {
        when(authUserClient.findByUsername("admin")).thenReturn(Result.success(user("admin")));
        when(passwordEncoder.matches("bad", "hash")).thenReturn(false);
        when(redisValues.get("login:fail:admin")).thenReturn(null);
        when(redisValues.increment("login:fail:admin"))
                .thenThrow(new RuntimeException("redis down"));

        assertThatThrownBy(() -> service.login(new LoginRequest("admin", "bad"), "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED.getCode());
    }

    @Test
    void logDeliveryFailureDoesNotBlockLogin() {
        when(authUserClient.findByUsername("admin")).thenReturn(Result.success(user("admin")));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
        when(redisValues.get("login:fail:admin")).thenReturn(null);
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwt());
        doThrow(new RuntimeException("feign down")).when(loginLogClient).record(any());

        LoginResponse response = service.login(new LoginRequest("admin", "secret"), "127.0.0.1");

        assertThat(response.access_token()).isEqualTo("signed-token");
    }

    @Test
    void lockedAccountRejectsEvenWithCorrectPassword() {
        when(redisValues.get("login:fail:admin")).thenReturn("5");

        assertThatThrownBy(() -> service.login(new LoginRequest("admin", "secret"), "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED.getCode());
        // 锁定拒绝不再递增计数
        verify(redisValues, never()).increment(anyString());
    }
}
