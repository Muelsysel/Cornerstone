package com.cornerstone.system.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cornerstone.common.exception.BusinessException;
import com.cornerstone.system.domain.entity.SysDept;
import com.cornerstone.system.domain.entity.SysUser;
import com.cornerstone.system.domain.mapper.SysDeptMapper;
import com.cornerstone.system.domain.mapper.SysUserMapper;
import com.cornerstone.system.domain.mapper.SysUserRoleMapper;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

/** 用户服务单测：密码编码、内置用户保护、角色分配委托（mock 隔离 Mapper）。 */
class SysUserServiceImplTest {

    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final SysUserMapper userMapper = mock(SysUserMapper.class);
    private final SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
    private final SysDeptMapper deptMapper = mock(SysDeptMapper.class);

    private SysUserServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        SysUserServiceImpl impl =
                new SysUserServiceImpl(passwordEncoder, userRoleMapper, deptMapper);
        // 注入 MyBatis-Plus ServiceImpl 的 baseMapper（protected 字段，3.5.9+ 声明于 CrudRepository）
        Field field =
                com.baomidou.mybatisplus.spring.repository.CrudRepository.class.getDeclaredField(
                        "baseMapper");
        field.setAccessible(true);
        field.set(impl, userMapper);
        service = Mockito.spy(impl);
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
    void addRejectsInvalidStatus() {
        // 回归：非法账号状态（char(1)）此前可入库；现保存时即报 400
        when(userMapper.selectCount(any())).thenReturn(0L);
        SysUser u = user(null, "bad-status");
        u.setStatus("9");

        assertThatThrownBy(() -> service.add(u)).isInstanceOf(BusinessException.class);
    }

    @Test
    void deleteRejectsBuiltinAdmin() {
        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(BusinessException.class);
        verify(userRoleMapper, never()).deleteUserRoleByUserId(any());
    }

    @Test
    void deleteCleansUserRoleAssociations() {
        doReturn(true).when(service).removeById(5L);

        service.delete(5L);

        verify(service).removeById(5L);
        verify(userRoleMapper).deleteUserRoleByUserId(5L);
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

    @Test
    void pagePassesParamsAndBuildsWrapper() {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SysUser> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(2, 10);
        doReturn(page).when(service).page(any(), any());

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SysUser> result =
                service.page(2, 10, "adm", "0");

        assertThat(result).isSameAs(page);
        verify(service).page(any(), any());
    }

    @Test
    void pageEnrichesDeptNames() {
        // 列表展示部门名：按 deptId 批量回填 deptName（非表字段），避免 N+1
        SysUser u = user(3L, "bob");
        u.setDeptId(100L);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SysUser> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10);
        page.setRecords(List.of(u));
        doReturn(page).when(service).page(any(), any());
        SysDept dept = new SysDept();
        dept.setId(100L);
        dept.setDeptName("总公司");
        when(deptMapper.selectBatchIds(any())).thenReturn(List.of(dept));

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SysUser> result =
                service.page(1, 10, null, null);

        assertThat(result.getRecords().get(0).getDeptName()).isEqualTo("总公司");
        verify(deptMapper).selectBatchIds(List.of(100L));
    }

    @Test
    void updateRejectsMissingUser() {
        doReturn(null).when(service).getById(99L);

        assertThatThrownBy(() -> service.update(user(99L, "ghost")))
                .isInstanceOf(BusinessException.class);
        verify(service, never()).updateById(any());
    }

    @Test
    void updateWithPasswordEncodesIt() {
        SysUser exist = user(4L, "alice");
        SysUser patch = user(4L, "alice");
        patch.setPassword("new-pass");
        doReturn(exist).when(service).getById(4L);
        doReturn(true).when(service).updateById(any());
        doReturn(exist).when(service).getById(4L);

        service.update(patch);

        verify(passwordEncoder).encode("new-pass");
        verify(service).updateById(argThat(u -> "enc:new-pass".equals(u.getPassword())));
    }

    @Test
    void updateWithoutPasswordKeepsExistingHash() {
        SysUser exist = user(4L, "alice");
        exist.setPassword("keep-hash");
        SysUser patch = user(4L, "alice"); // 未传密码
        doReturn(exist).when(service).getById(4L);
        doReturn(true).when(service).updateById(any());
        doReturn(exist).when(service).getById(4L);

        service.update(patch);

        // 密码字段只在显式传入时更新：null 不清空、不重复编码
        verify(passwordEncoder, never()).encode(any());
        verify(service).updateById(argThat(u -> u.getPassword() == null));
    }

    @Test
    void updateRejectsUsernameTakenByAnother() {
        SysUser exist = user(4L, "alice");
        SysUser patch = user(4L, "alice-renamed");
        doReturn(exist).when(service).getById(4L);
        doReturn(1L).when(service).count(any());

        assertThatThrownBy(() -> service.update(patch)).isInstanceOf(BusinessException.class);
        verify(service, never()).updateById(any());
    }

    @Test
    void updateWithSameUsernameSkipsUniquenessCheck() {
        SysUser exist = user(4L, "alice");
        SysUser patch = user(4L, "alice"); // 用户名不变（如只改昵称/状态）
        doReturn(exist).when(service).getById(4L);
        doReturn(true).when(service).updateById(any());
        doReturn(exist).when(service).getById(4L);

        service.update(patch);

        // 用户名未变不触发唯一性 count
        verify(service, never()).count(any());
        verify(service).updateById(any());
    }

    @Test
    void changeStatusPatchesOnlyStatus() {
        doReturn(user(7L, "alice")).when(service).getById(7L);
        service.changeStatus(7L, "1");

        verify(service).updateById(argThat(u -> u.getId() == 7L && "1".equals(u.getStatus())));
    }

    @Test
    void changeStatusRejectsMissingUser() {
        // 不 stub getById：真实实现返回 null → 抛不存在异常，避免对不存在用户静默成功
        when(userMapper.selectById(9L)).thenReturn(null);
        assertThatThrownBy(() -> service.changeStatus(9L, "1"))
                .isInstanceOf(BusinessException.class);
        verify(service, never()).updateById(any());
    }

    @Test
    void changeStatusRejectsInvalidStatusValue() {
        // 状态仅允许 0/1：非法值拒绝（防污染 DB char(1) 列）
        doReturn(user(7L, "alice")).when(service).getById(7L);

        assertThatThrownBy(() -> service.changeStatus(7L, "abc"))
                .isInstanceOf(BusinessException.class);
        verify(service, never()).updateById(any());
    }

    @Test
    void resetPasswordEncodesAndUpdates() {
        SysUser exist = user(8L, "bob");
        doReturn(exist).when(service).getById(8L);
        doReturn(true).when(service).updateById(any());

        service.resetPassword(8L, "temp-123");

        verify(passwordEncoder).encode("temp-123");
        verify(service).updateById(argThat(u -> u.getId() == 8L && u.getPassword() != null));
    }

    @Test
    void addRejectsOversizedPassword() {
        // 与认证登录契约一致（@Size 72）：超长密码会静默截断，登录必被拒 → 创建时即拒绝
        when(userMapper.selectCount(any())).thenReturn(0L);
        SysUser u = user(null, "longpw");
        u.setPassword("p".repeat(73));

        assertThatThrownBy(() -> service.add(u)).isInstanceOf(BusinessException.class);
        verify(passwordEncoder, never()).encode(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void addRejectsTooShortPassword() {
        // 弱密码防护：显式传入不足 6 位时拒绝（默认密码 123456 不受影响）
        when(userMapper.selectCount(any())).thenReturn(0L);
        SysUser u = user(null, "shortpw");
        u.setPassword("12345");

        assertThatThrownBy(() -> service.add(u)).isInstanceOf(BusinessException.class);
        verify(passwordEncoder, never()).encode(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void addRejectsOversizedUsername() {
        // 回归：超长用户名曾触发 DB varchar(30) DataTruncation → 500；现业务层返回友好 400
        when(userMapper.selectCount(any())).thenReturn(0L);
        SysUser u = user(null, "u".repeat(31));
        u.setPassword("Passw0rd!");

        assertThatThrownBy(() -> service.add(u)).isInstanceOf(BusinessException.class);
        verify(passwordEncoder, never()).encode(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void resetPasswordRejectsOversizedPassword() {
        SysUser exist = user(8L, "bob");
        doReturn(exist).when(service).getById(8L);

        assertThatThrownBy(() -> service.resetPassword(8L, "p".repeat(73)))
                .isInstanceOf(BusinessException.class);
        verify(passwordEncoder, never()).encode(org.mockito.ArgumentMatchers.anyString());
    }
}
