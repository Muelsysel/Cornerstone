package com.cornerstone.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cornerstone.common.exception.BusinessException;
import com.cornerstone.system.domain.entity.SysMenu;
import com.cornerstone.system.domain.mapper.SysMenuMapper;
import com.cornerstone.system.exception.SystemErrorCode;
import com.cornerstone.system.service.SysMenuService;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/** 菜单服务实现。 */
@Service
public class SysMenuServiceImpl implements SysMenuService {

    private final SysMenuMapper menuMapper;

    public SysMenuServiceImpl(SysMenuMapper menuMapper) {
        this.menuMapper = menuMapper;
    }

    @Override
    public List<SysMenu> listTree(String menuName, String status) {
        LambdaQueryWrapper<SysMenu> wrapper =
                new LambdaQueryWrapper<SysMenu>()
                        .like(hasText(menuName), SysMenu::getMenuName, menuName)
                        .eq(hasText(status), SysMenu::getStatus, status)
                        .orderByAsc(SysMenu::getSort);
        List<SysMenu> all = menuMapper.selectList(wrapper);
        return buildTree(all);
    }

    @Override
    public SysMenu add(SysMenu menu) {
        if (menu.getParentId() == null) {
            menu.setParentId(0L);
        }
        menuMapper.insert(menu);
        return menu;
    }

    @Override
    public SysMenu update(SysMenu menu) {
        SysMenu exist = menuMapper.selectById(menu.getId());
        if (exist == null) {
            throw new BusinessException(SystemErrorCode.RESOURCE_NOT_FOUND);
        }
        menuMapper.updateById(menu);
        return menuMapper.selectById(menu.getId());
    }

    @Override
    public void delete(Long menuId) {
        long children =
                menuMapper.selectCount(
                        new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, menuId));
        if (children > 0) {
            throw new BusinessException(1010, "存在子菜单,不允许删除");
        }
        menuMapper.deleteById(menuId);
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
}
