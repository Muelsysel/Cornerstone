package com.cornerstone.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cornerstone.api.dto.UserAuthDTO;
import com.cornerstone.system.domain.entity.SysMenu;
import com.cornerstone.system.domain.entity.SysRole;
import com.cornerstone.system.domain.entity.SysUser;
import com.cornerstone.system.domain.mapper.SysMenuMapper;
import com.cornerstone.system.domain.mapper.SysRoleMapper;
import com.cornerstone.system.domain.mapper.SysRoleMenuMapper;
import com.cornerstone.system.domain.mapper.SysUserMapper;
import com.cornerstone.system.domain.mapper.SysUserRoleMapper;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * 认证支持服务：为认证中心登录提供用户认证信息（含密码哈希与角色权限）。
 *
 * <p>实现 {@code AuthUserClient} 契约（Feign 路径 /system/auth），由本服务供 auth 内部调用。 独立服务类，不复用/修改
 * SysUserService/SysRoleService/SysMenuService 等现有管理服务。
 *
 * <p>注意：返回的 {@link UserAuthDTO} 含密码哈希，仅限服务间内部传递；网关白名单不含 /system/**，外部访问不到本接口。
 */
@Service
public class AuthUserSupportService {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysMenuMapper menuMapper;

    public AuthUserSupportService(
            SysUserMapper userMapper,
            SysUserRoleMapper userRoleMapper,
            SysRoleMapper roleMapper,
            SysRoleMenuMapper roleMenuMapper,
            SysMenuMapper menuMapper) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.menuMapper = menuMapper;
    }

    /** 按用户名查询认证所需用户信息；用户不存在或已停用（status=1）返回 null。 */
    public UserAuthDTO findByUsername(String username) {
        SysUser user =
                userMapper.selectOne(
                        new LambdaQueryWrapper<SysUser>()
                                .eq(SysUser::getUsername, username)
                                // 停用账号视为不存在：认证中心统一报「用户名或密码错误」，
                                // 与「游客访问非已发布公告按不存在处理」同款 fail-closed 策略，避免账号状态枚举
                                .eq(SysUser::getStatus, "0"));
        if (user == null) {
            return null;
        }
        UserAuthDTO dto = new UserAuthDTO();
        dto.setUserId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setPassword(user.getPassword());
        dto.setDeptId(user.getDeptId());
        dto.setRoles(findRoleKeys(user.getId()));
        dto.setPermissions(findPermissions(user.getId()));
        return dto;
    }

    /** 用户 → 角色标识集合（sys_user_role → sys_role，只取启用角色 role_key）。 */
    private Set<String> findRoleKeys(Long userId) {
        return userRoleMapper.selectRoleKeysByUserId(userId);
    }

    /** 用户 → 权限点集合（sys_role_menu → sys_menu，只取非空且启用的 perms）。 */
    private Set<String> findPermissions(Long userId) {
        Set<String> roleKeys = findRoleKeys(userId);
        if (roleKeys == null || roleKeys.isEmpty()) {
            return new LinkedHashSet<>();
        }
        List<SysRole> roles =
                roleMapper.selectList(
                        new LambdaQueryWrapper<SysRole>()
                                .in(SysRole::getRoleKey, roleKeys)
                                .eq(SysRole::getStatus, "0"));
        // 分角色收集菜单，取 perms 非空且菜单启用的权限点
        Set<Long> menuIds = new LinkedHashSet<>();
        for (SysRole role : roles) {
            List<Long> ids = roleMenuMapper.selectMenuIdsByRoleId(role.getId());
            if (ids != null) {
                menuIds.addAll(ids);
            }
        }
        if (menuIds.isEmpty()) {
            return new LinkedHashSet<>();
        }
        List<SysMenu> menus = menuMapper.selectBatchIds(menuIds);
        Set<String> permissions = new LinkedHashSet<>();
        for (SysMenu menu : menus) {
            if (isEnabledPerm(menu)) {
                permissions.add(menu.getPerms());
            }
        }
        return permissions;
    }

    private boolean isEnabledPerm(SysMenu menu) {
        return menu != null
                && menu.getPerms() != null
                && !menu.getPerms().isBlank()
                && "0".equals(menu.getStatus());
    }
}
