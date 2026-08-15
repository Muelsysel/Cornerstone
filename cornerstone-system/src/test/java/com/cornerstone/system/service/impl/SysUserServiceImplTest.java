package com.cornerstone.system.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cornerstone.common.exception.BusinessException;
import com.cornerstone.system.domain.entity.SysUser;
import com.cornerstone.system.domain.mapper.SysUserMapper;
import com.cornerstone.system.domain.mapper.SysUserRoleMapper;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

/** 用户服务单测：密码编码、内置用户保护、角色分配委托（mock 隔离 Mapper）。 */
class SysUserServiceImplTest {

    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final SysUserMapper userMapper = mock(SysUserMapper.class);
    private final SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);

    private SysUserServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        service = new SysUserServiceImpl(passwordEncoder, userRoleMapper);
        // 注入 MyBatis-Plus ServiceImpl 的 baseMapper（protected 字段）
        Field field =
                com.baomidou.mybatisplus.extension.service.impl.ServiceImpl.class.getDeclaredField(
                        "baseMapper");
        field.setAccessible(true);
        field.set(service, userMapper);
        when(passwordEncoder.encode(any())).thenAnswer(inv -> "enc:" + inv.getArgument(0));
    }

    private SysUser user(Long id, String username) {
        SysUser u = new SysUser();
        u.setId(id);
        u.setUsername(username);
        return u;
    }

    @Test
    void addEncodesPasswordAndDefaultsTo123456() {
        when(userMapper.selectCount(any())).thenReturn(0L);

        SysUser created = service.add(user(null, "newbie"));

        verify(passwordEncoder).encode("123456");
        assertThat(created.getPassword()).isEqualTo("enc:123456");
    }

    @Test
    void addRejectsDuplicateUsername() {
        when(userMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.add(user(null, "dup")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void deleteRejectsBuiltinAdmin() {
        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(BusinessException.class);
    }

    @Test
    void assignRolesDeletesThenBatchInserts() {
        when(userMapper.selectById(2L)).thenReturn(user(2L, "normal"));

        service.assignRoles(2L, List.of(10L, 20L));

        verify(userRoleMapper).deleteUserRoleByUserId(2L);
        verify(userRoleMapper).batchInsertUserRole(2L, List.of(10L, 20L));
    }

    @Test
    void assignRolesWithEmptyListOnlyDeletes() {
        when(userMapper.selectById(2L)).thenReturn(user(2L, "normal"));

        service.assignRoles(2L, List.of());

        verify(userRoleMapper).deleteUserRoleByUserId(2L);
        verify(userRoleMapper, never()).batchInsertUserRole(eq(2L), any());
    }

    @Test
    void getRoleIdsDelegatesToMapper() {
        when(userRoleMapper.selectRoleIdsByUserId(3L)).thenReturn(List.of(5L, 6L));
        assertThat(service.getRoleIdsByUserId(3L)).containsExactly(5L, 6L);
    }
}
