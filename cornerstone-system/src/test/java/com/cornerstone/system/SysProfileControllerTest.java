package com.cornerstone.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cornerstone.common.core.ErrorCode;
import com.cornerstone.common.exception.BusinessException;
import com.cornerstone.common.security.UserContext;
import com.cornerstone.common.security.UserContextHolder;
import com.cornerstone.system.controller.SysProfileController;
import com.cornerstone.system.domain.entity.SysUser;
import com.cornerstone.system.exception.SystemErrorCode;
import com.cornerstone.system.service.SysUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

/** 个人中心控制器单测：无上下文 401、旧密码校验、改密只更新密码字段。 */
class SysProfileControllerTest {

    private final SysUserService userService = mock(SysUserService.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private SysProfileController controller;

    @BeforeEach
    void setUp() {
        controller = new SysProfileController(userService, passwordEncoder);
    }

    @AfterEach
    void clearContext() {
        UserContextHolder.clear();
    }

    private void loginAs(Long userId) {
        UserContext context = new UserContext();
        context.setUserId(userId);
        context.setUsername("admin");
        UserContextHolder.set(context);
    }

    @Test
    void profileWithoutContextThrowsUnauthorized() {
        UserContextHolder.clear();

        assertThatThrownBy(() -> controller.profile())
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED.getCode());
    }

    @Test
    void profileReturnsCurrentUser() {
        loginAs(1L);
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("admin");
        when(userService.getById(1L)).thenReturn(user);

        SysUser result = controller.profile().getData();

        assertThat(result.getUsername()).isEqualTo("admin");
        verify(userService).getById(1L);
    }

    @Test
    void updatePasswordRejectsWrongOldPassword() {
        loginAs(1L);
        SysUser user = new SysUser();
        user.setId(1L);
        user.setPassword("hash");
        when(userService.getById(1L)).thenReturn(user);
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThatThrownBy(
                        () ->
                                controller.updatePassword(
                                        new SysProfileController.PasswordUpdateRequest(
                                                "wrong", "new-pass")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(SystemErrorCode.OLD_PASSWORD_ERROR.getCode());
        verify(userService, never()).update(any());
    }

    @Test
    void updatePasswordRejectsMissingUser() {
        loginAs(99L);
        when(userService.getById(99L)).thenReturn(null);

        assertThatThrownBy(
                        () ->
                                controller.updatePassword(
                                        new SysProfileController.PasswordUpdateRequest(
                                                "old", "new-pass")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(SystemErrorCode.OLD_PASSWORD_ERROR.getCode());
    }

    @Test
    void updatePasswordPassesPlainNewPasswordToService() {
        loginAs(1L);
        SysUser user = new SysUser();
        user.setId(1L);
        user.setPassword("hash");
        when(userService.getById(1L)).thenReturn(user);
        when(passwordEncoder.matches("old-pass", "hash")).thenReturn(true);
        when(userService.update(any())).thenAnswer(inv -> inv.getArgument(0));

        controller.updatePassword(
                new SysProfileController.PasswordUpdateRequest("old-pass", "new-pass"));

        // 传明文新密码，由 SysUserServiceImpl.update 统一 BCrypt 编码（避免双重编码）
        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(userService).update(captor.capture());
        SysUser update = captor.getValue();
        assertThat(update.getId()).isEqualTo(1L);
        assertThat(update.getPassword()).isEqualTo("new-pass");
    }
}
