package com.cornerstone.system.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cornerstone.common.exception.BusinessException;
import com.cornerstone.system.domain.entity.SysRole;
import com.cornerstone.system.domain.mapper.SysRoleDeptMapper;
import com.cornerstone.system.domain.mapper.SysRoleMapper;
import com.cornerstone.system.domain.mapper.SysRoleMenuMapper;
import com.cornerstone.system.domain.mapper.SysUserRoleMapper;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** 角色服务单测：唯一性校验、数据范围维护、内置角色保护、菜单分配委托。 */
class SysRoleServiceImplTest {

    private final SysRoleMenuMapper roleMenuMapper = mock(SysRoleMenuMapper.class);
    private final SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
    private final SysRoleDeptMapper roleDeptMapper = mock(SysRoleDeptMapper.class);

    private SysRoleServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        SysRoleServiceImpl impl =
                new SysRoleServiceImpl(roleMenuMapper, userRoleMapper, roleDeptMapper);
        // 注入 ServiceImpl 的 baseMapper（3.5.9+ 声明于 CrudRepository）
        Field field =
                com.baomidou.mybatisplus.spring.repository.CrudRepository.class.getDeclaredField(
                        "baseMapper");
        field.setAccessible(true);
        field.set(impl, mock(SysRoleMapper.class));
        // spy：stub 内部方法（count/getById）并验证真实业务逻辑
        service = Mockito.spy(impl);
    }

    private SysRole role(Long id, String key, String scope) {
        SysRole r = new SysRole();
        r.setId(id);
        r.setRoleKey(key);
        r.setDataScope(scope);
        return r;
    }

    @Test
    void addRejectsDuplicateRoleKey() {
        when(service.count(any())).thenReturn(1L);
        assertThatThrownBy(() -> service.add(role(null, "dup", "1")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void addWithCustomScopeInsertsDepts() {
        when(service.count(any())).thenReturn(0L);
        SysRole r = role(5L, "ops", "2");
        r.setDeptIds(List.of(10L, 20L));

        service.add(r);

        verify(roleDeptMapper).deleteByRoleId(5L);
        verify(roleDeptMapper).batchInsert(5L, List.of(10L, 20L));
    }

    @Test
    void addWithGlobalScopeClearsDepts() {
        when(service.count(any())).thenReturn(0L);
        SysRole r = role(6L, "all", "1");
        r.setDeptIds(List.of(10L));

        service.add(r);

        verify(roleDeptMapper).deleteByRoleId(6L);
        verify(roleDeptMapper, never()).batchInsert(eq(6L), any());
    }

    @Test
    void updateRejectsMissingRole() {
        when(service.getById(99L)).thenReturn(null);
        assertThatThrownBy(() -> service.update(role(99L, "x", "1")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void updateRejectsDuplicateRoleKey() {
        when(service.getById(7L)).thenReturn(role(7L, "op", "1"));
        when(service.count(any())).thenReturn(1L);

        SysRole r = role(7L, "taken", "1");

        assertThatThrownBy(() -> service.update(r)).isInstanceOf(BusinessException.class);
        verify(service, never()).updateById(any());
    }

    @Test
    void updateWithCustomScopeRebuildsDepts() {
        when(service.getById(7L)).thenReturn(role(7L, "op", "1"));
        when(service.updateById(any())).thenReturn(true);
        when(service.getById(7L)).thenReturn(role(7L, "op", "2"));
        SysRole r = role(7L, "op", "2");
        r.setDeptIds(List.of(30L, 40L));

        service.update(r);

        verify(roleDeptMapper).deleteByRoleId(7L);
        verify(roleDeptMapper).batchInsert(7L, List.of(30L, 40L));
    }

    @Test
    void updateWithGlobalScopeClearsDepts() {
        when(service.getById(8L)).thenReturn(role(8L, "all", "2"));
        when(service.updateById(any())).thenReturn(true);
        when(service.getById(8L)).thenReturn(role(8L, "all", "1"));
        SysRole r = role(8L, "all", "1");
        r.setDeptIds(List.of(30L));

        service.update(r);

        verify(roleDeptMapper).deleteByRoleId(8L);
        verify(roleDeptMapper, never()).batchInsert(eq(8L), any());
    }

    @Test
    void deleteRejectsBuiltinAdminRole() {
        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(BusinessException.class);
        verify(roleMenuMapper, never()).deleteRoleMenuByRoleId(any());
        verify(roleDeptMapper, never()).deleteByRoleId(any());
        verify(userRoleMapper, never()).deleteUserRoleByRoleId(any());
    }

    @Test
    void deleteCleansAssociations() {
        doReturn(true).when(service).removeById(3L);

        service.delete(3L);

        verify(service).removeById(3L);
        verify(roleMenuMapper).deleteRoleMenuByRoleId(3L);
        verify(roleDeptMapper).deleteByRoleId(3L);
        verify(userRoleMapper).deleteUserRoleByRoleId(3L);
    }

    @Test
    void assignMenusDeletesThenBatchInserts() {
        when(service.getById(2L)).thenReturn(role(2L, "op", "1"));

        service.assignMenus(2L, List.of(10L, 20L));

        verify(roleMenuMapper).deleteRoleMenuByRoleId(2L);
        verify(roleMenuMapper).batchInsertRoleMenu(2L, List.of(10L, 20L));
    }

    @Test
    void assignMenusWithEmptyListOnlyDeletes() {
        when(service.getById(2L)).thenReturn(role(2L, "op", "1"));

        service.assignMenus(2L, List.of());

        verify(roleMenuMapper).deleteRoleMenuByRoleId(2L);
        verify(roleMenuMapper, never()).batchInsertRoleMenu(eq(2L), any());
    }

    @Test
    void getDeptIdsByRoleIdDelegates() {
        when(roleDeptMapper.selectDeptIdsByRoleId(3L)).thenReturn(List.of(100L));
        assertThat(service.getDeptIdsByRoleId(3L)).containsExactly(100L);
    }
}
