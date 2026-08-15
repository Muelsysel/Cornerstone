package com.cornerstone.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cornerstone.common.exception.BusinessException;
import com.cornerstone.common.util.ValidationUtils;
import com.cornerstone.system.domain.entity.SysMenu;
import com.cornerstone.system.domain.mapper.SysMenuMapper;
import com.cornerstone.system.domain.mapper.SysRoleMenuMapper;
import com.cornerstone.system.exception.SystemErrorCode;
import com.cornerstone.system.service.SysMenuService;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 菜单服务实现。 */
@Service
public class SysMenuServiceImpl implements SysMenuService {

    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    public SysMenuServiceImpl(SysMenuMapper menuMapper, SysRoleMenuMapper roleMenuMapper) {
        this.menuMapper = menuMapper;
        this.roleMenuMapper = roleMenuMapper;
    }

    @Override
    public List<SysMenu> listTree(String menuName, String status) {
        LambdaQueryWrapper<SysMenu> wrapper =
                new LambdaQueryWrapper<SysMenu>()
                        .like(hasText(menuName), SysMenu::getMenuName, menuName)
                        .eq(hasText(status), SysMenu::getStatus, status)
                        // 确定性排序：sort 可重复，按 id 升序兜底，保证树形结构稳定
                        .orderByAsc(SysMenu::getSort)
                        .orderByAsc(SysMenu::getId);
        List<SysMenu> all = menuMapper.selectList(wrapper);
        return buildTree(all);
    }

    @Override
    public SysMenu add(SysMenu menu) {
        validate(menu);
        if (menu.getParentId() == null) {
            menu.setParentId(0L);
        }
        // 父节点必须存在（与部门 add 的 resolveAncestors 同级校验）：否则生成悬空节点，树组装时被丢弃、后台不可见
        if (menu.getParentId() != 0L && menuMapper.selectById(menu.getParentId()) == null) {
            throw new BusinessException(SystemErrorCode.INVALID_PARENT);
        }
        menuMapper.insert(menu);
        return menu;
    }

    @Override
    public SysMenu update(SysMenu menu) {
        validate(menu);
        SysMenu exist = menuMapper.selectById(menu.getId());
        if (exist == null) {
            throw new BusinessException(SystemErrorCode.RESOURCE_NOT_FOUND);
        }
        // 父节点不能选自己或自身子节点（选子节点会形成环，破坏菜单树）；父节点须存在（防悬空节点）
        Long newParent = menu.getParentId();
        if (newParent != null
                && (newParent.equals(menu.getId()) || isDescendant(menu.getId(), newParent))) {
            throw new BusinessException(SystemErrorCode.INVALID_PARENT);
        }
        if (newParent != null && newParent != 0L && menuMapper.selectById(newParent) == null) {
            throw new BusinessException(SystemErrorCode.INVALID_PARENT);
        }
        menuMapper.updateById(menu);
        return menuMapper.selectById(menu.getId());
    }

    /** candidateParentId 是否为 nodeId 的子孙节点（全量查询本地遍历，菜单数量级小可接受） */
    private boolean isDescendant(Long nodeId, Long candidateParentId) {
        List<SysMenu> all = menuMapper.selectList(null);
        java.util.Set<Long> descendants = new java.util.HashSet<>();
        collectDescendants(all, nodeId, descendants);
        return descendants.contains(candidateParentId);
    }

    private void collectDescendants(
            List<SysMenu> all, Long parentId, java.util.Set<Long> descendants) {
        for (SysMenu menu : all) {
            if (java.util.Objects.equals(menu.getParentId(), parentId)
                    && descendants.add(menu.getId())) {
                collectDescendants(all, menu.getId(), descendants);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long menuId) {
        long children =
                menuMapper.selectCount(
                        new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, menuId));
        if (children > 0) {
            throw new BusinessException(SystemErrorCode.DELETE_WITH_CHILD_MENU);
        }
        menuMapper.deleteById(menuId);
        // 清理角色-菜单关联，避免孤儿记录残留
        roleMenuMapper.deleteRoleMenuByMenuId(menuId);
    }

    /** 平铺菜单组装成树 */
    private List<SysMenu> buildTree(List<SysMenu> all) {
        java.util.Map<Long, List<SysMenu>> byParent =
                all.stream()
                        .collect(
                                Collectors.groupingBy(
                                        m -> m.getParentId() == null ? 0L : m.getParentId()));
        return all.stream()
                .filter(m -> m.getParentId() == null || m.getParentId() == 0L)
                .peek(root -> attachChildren(root, byParent))
                .collect(Collectors.toList());
    }

    private void attachChildren(SysMenu node, java.util.Map<Long, List<SysMenu>> byParent) {
        List<SysMenu> children = byParent.getOrDefault(node.getId(), List.of());
        node.setChildren(children);
        children.forEach(c -> attachChildren(c, byParent));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * 菜单字段长度校验（与 DB 列一致：menu_name varchar(50)、path varchar(200)、component varchar(255)、perms/icon
     * varchar(100)）：超长会触发 DataTruncation → 500，业务层先校验返回友好 400。
     */
    private void validate(SysMenu menu) {
        ValidationUtils.maxLength(menu.getMenuName(), 50, "菜单名称");
        ValidationUtils.maxLength(menu.getPath(), 200, "路由地址");
        ValidationUtils.maxLength(menu.getComponent(), 255, "组件路径");
        ValidationUtils.maxLength(menu.getPerms(), 100, "权限标识");
        ValidationUtils.maxLength(menu.getIcon(), 100, "菜单图标");
        ValidationUtils.oneOf(menu.getMenuType(), "菜单类型", "M", "C", "F");
        ValidationUtils.oneOf(menu.getVisible(), "显示状态", "0", "1");
        ValidationUtils.oneOf(menu.getStatus(), "菜单状态", "0", "1");
    }
}
