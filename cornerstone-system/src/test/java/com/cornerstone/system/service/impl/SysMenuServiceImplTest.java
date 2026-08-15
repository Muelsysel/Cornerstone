package com.cornerstone.system.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cornerstone.common.exception.BusinessException;
import com.cornerstone.system.domain.entity.SysMenu;
import com.cornerstone.system.domain.mapper.SysMenuMapper;
import com.cornerstone.system.domain.mapper.SysRoleMenuMapper;
import com.cornerstone.system.exception.SystemErrorCode;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** 菜单服务单测：树组装、增删改校验、子菜单删除保护。 */
class SysMenuServiceImplTest {

    private final SysMenuMapper menuMapper = mock(SysMenuMapper.class);
    private final SysRoleMenuMapper roleMenuMapper = mock(SysRoleMenuMapper.class);
    private SysMenuServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SysMenuServiceImpl(menuMapper, roleMenuMapper);
    }

    private SysMenu menu(Long id, Long parentId, String name, Integer sort) {
        SysMenu m = new SysMenu();
        m.setId(id);
        m.setParentId(parentId);
        m.setMenuName(name);
        m.setSort(sort);
        return m;
    }

    @Test
    void listTreeBuildsNestedTreeFromFlatList() {
        SysMenu root = menu(1L, 0L, "系统管理", 1);
        SysMenu child = menu(2L, 1L, "用户管理", 1);
        SysMenu grandchild = menu(3L, 2L, "用户查询", 1);
        when(menuMapper.selectList(any())).thenReturn(List.of(root, grandchild, child));

        List<SysMenu> tree = service.listTree(null, null);

        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).getMenuName()).isEqualTo("系统管理");
        assertThat(tree.get(0).getChildren())
                .extracting(SysMenu::getMenuName)
                .containsExactly("用户管理");
        assertThat(tree.get(0).getChildren().get(0).getChildren())
                .extracting(SysMenu::getMenuName)
                .containsExactly("用户查询");
    }

    @Test
    void listTreeKeepsOnlyRootsAtTopLevel() {
        SysMenu root = menu(1L, 0L, "系统管理", 1);
        SysMenu orphan = menu(9L, 99L, "悬空节点", 2);
        when(menuMapper.selectList(any())).thenReturn(List.of(root, orphan));

        List<SysMenu> tree = service.listTree(null, null);

        assertThat(tree).extracting(SysMenu::getMenuName).containsExactly("系统管理");
    }

    @Test
    void listTreePassesFilterParamsToMapper() {
        when(menuMapper.selectList(any())).thenReturn(List.of());

        service.listTree("用户", "0");

        verify(menuMapper).selectList(any());
    }

    @Test
    void addDefaultsParentIdToZero() {
        SysMenu m = menu(null, null, "菜单", 1);
        when(menuMapper.insert(m)).thenReturn(1);

        service.add(m);

        assertThat(m.getParentId()).isZero();
        verify(menuMapper).insert(m);
    }

    @Test
    void addRejectsOversizedMenuName() {
        // 回归：超长菜单名曾触发 DB varchar(50) DataTruncation → 500；现业务层返回友好 400
        SysMenu m = menu(null, 0L, "n".repeat(51), 1);

        assertThatThrownBy(() -> service.add(m)).isInstanceOf(BusinessException.class);
        verify(menuMapper, never()).insert(any(SysMenu.class));
    }

    @Test
    void addRejectsOversizedComponent() {
        // 回归：超长组件路径曾触发 DB varchar(255) DataTruncation → 500
        SysMenu m = menu(null, 0L, "菜单", 1);
        m.setComponent("c".repeat(256));

        assertThatThrownBy(() -> service.add(m)).isInstanceOf(BusinessException.class);
        verify(menuMapper, never()).insert(any(SysMenu.class));
    }

    @Test
    void addRejectsOversizedPerms() {
        // 回归：超长权限标识曾触发 DB varchar(100) DataTruncation → 500
        SysMenu m = menu(null, 0L, "菜单", 1);
        m.setPerms("p".repeat(101));

        assertThatThrownBy(() -> service.add(m)).isInstanceOf(BusinessException.class);
        verify(menuMapper, never()).insert(any(SysMenu.class));
    }

    @Test
    void addRejectsInvalidMenuType() {
        // 回归：非法菜单类型（char(1)：M/C/F）此前可入库；现保存时即报 400
        SysMenu m = menu(null, 0L, "菜单", 1);
        m.setMenuType("X");

        assertThatThrownBy(() -> service.add(m)).isInstanceOf(BusinessException.class);
        verify(menuMapper, never()).insert(any(SysMenu.class));
    }

    @Test
    void addRejectsInvalidStatus() {
        // 回归：非法菜单状态（char(1)）此前可入库；现保存时即报 400
        SysMenu m = menu(null, 0L, "菜单", 1);
        m.setStatus("9");

        assertThatThrownBy(() -> service.add(m)).isInstanceOf(BusinessException.class);
        verify(menuMapper, never()).insert(any(SysMenu.class));
    }

    @Test
    void addKeepsProvidedParentId() {
        SysMenu m = menu(null, 5L, "子菜单", 1);
        when(menuMapper.insert(m)).thenReturn(1);

        service.add(m);

        assertThat(m.getParentId()).isEqualTo(5L);
    }

    @Test
    void updateThrowsWhenMenuMissing() {
        SysMenu m = menu(42L, 0L, "不存在", 1);
        when(menuMapper.selectById(42L)).thenReturn(null);

        assertThatThrownBy(() -> service.update(m))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(SystemErrorCode.RESOURCE_NOT_FOUND.getCode());
        verify(menuMapper, never()).updateById(any(SysMenu.class));
    }

    @Test
    void updateRejectsOwnDescendantAsParent() {
        // 树：1(根) → 2 → 3。把 1 的父节点改为 3（自己的子孙）会成环 → 拒绝
        SysMenu root = menu(1L, 0L, "系统管理", 1);
        SysMenu child = menu(2L, 1L, "用户管理", 1);
        SysMenu grandchild = menu(3L, 2L, "用户查询", 1);
        when(menuMapper.selectById(1L)).thenReturn(root);
        when(menuMapper.selectList(null)).thenReturn(List.of(root, child, grandchild));
        SysMenu m = menu(1L, 3L, "系统管理", 1);

        assertThatThrownBy(() -> service.update(m))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(SystemErrorCode.INVALID_PARENT.getCode());
        verify(menuMapper, never()).updateById(any(SysMenu.class));
    }

    @Test
    void updateReturnsRefreshedMenu() {
        SysMenu m = menu(1L, 0L, "改名", 1);
        SysMenu refreshed = menu(1L, 0L, "改名后", 1);
        when(menuMapper.selectById(1L)).thenReturn(m);
        when(menuMapper.updateById(m)).thenReturn(1);
        when(menuMapper.selectById(1L)).thenReturn(m, refreshed);

        SysMenu result = service.update(m);

        assertThat(result.getMenuName()).isEqualTo("改名后");
    }

    @Test
    void deleteRejectsWhenChildrenExist() {
        when(menuMapper.selectCount(any())).thenReturn(2L);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(SystemErrorCode.DELETE_WITH_CHILD_MENU.getCode());
        verify(menuMapper, never()).deleteById(any(Long.class));
    }

    @Test
    void deleteRemovesLeafMenuAndCleansRoleMenu() {
        when(menuMapper.selectCount(any())).thenReturn(0L);

        service.delete(1L);

        verify(menuMapper).deleteById(1L);
        verify(roleMenuMapper).deleteRoleMenuByMenuId(1L);
    }
}
