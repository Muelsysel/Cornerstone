package com.cornerstone.system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cornerstone.api.dto.UserAuthDTO;
import com.cornerstone.system.domain.entity.SysMenu;
import com.cornerstone.system.domain.entity.SysRole;
import com.cornerstone.system.domain.entity.SysUser;
import com.cornerstone.system.domain.mapper.SysMenuMapper;
import com.cornerstone.system.domain.mapper.SysRoleMapper;
import com.cornerstone.system.domain.mapper.SysRoleMenuMapper;
import com.cornerstone.system.domain.mapper.SysUserMapper;
import com.cornerstone.system.domain.mapper.SysUserRoleMapper;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * 认证支持服务单测：登录认证信息的组装（用户 → 角色 → 权限点，含空角色/菜单过滤分支）。
 *
 * <p>认证核心（auth 登录依赖），用 mock 隔离 Mapper 验证组装逻辑。
 */
class AuthUserSupportServiceTest {

    private final SysUserMapper userMapper = mock(SysUserMapper.class);
    private final SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
    private final SysRoleMapper roleMapper = mock(SysRoleMapper.class);
    private final SysRoleMenuMapper roleMenuMapper = mock(SysRoleMenuMapper.class);
    private final SysMenuMapper menuMapper = mock(SysMenuMapper.class);

    private final AuthUserSupportService service =
            new AuthUserSupportService(
                    userMapper, userRoleMapper, roleMapper, roleMenuMapper, menuMapper);

    private SysUser user(Long id, String username) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername(username);
        user.setPassword("$2a$10$hash");
        user.setDeptId(100L);
        return user;
    }

    @Test
    void userNotFoundReturnsNull() {
        when(userMapper.selectOne(any())).thenReturn(null);
        assertThat(service.findByUsername("ghost")).isNull();
    }

    @Test
    void assemblesRolesAndEnabledPermissions() {
        when(userMapper.selectOne(any())).thenReturn(user(1L, "admin"));
        when(userRoleMapper.selectRoleKeysByUserId(1L)).thenReturn(Set.of("admin"));

        SysRole role = new SysRole();
        role.setId(1L);
        role.setRoleKey("admin");
        role.setStatus("0");
        when(roleMapper.selectList(any())).thenReturn(List.of(role));
        when(roleMenuMapper.selectMenuIdsByRoleId(1L)).thenReturn(List.of(10L, 20L, 30L));

        SysMenu enabled = new SysMenu();
        enabled.setPerms("system:user:list");
        enabled.setStatus("0");
        SysMenu blankPerm = new SysMenu();
        blankPerm.setPerms(" ");
        blankPerm.setStatus("0");
        SysMenu disabled = new SysMenu();
        disabled.setPerms("system:user:add");
        disabled.setStatus("1");
        // 注意：service 实际传 LinkedHashSet（非 List），mock 用 any() 匹配
        when(menuMapper.selectBatchIds(any())).thenReturn(List.of(enabled, blankPerm, disabled));

        UserAuthDTO dto = service.findByUsername("admin");

        assertThat(dto.getUserId()).isEqualTo(1L);
        assertThat(dto.getRoles()).containsExactly("admin");
        // 仅保留 perms 非空且菜单启用的权限点
        assertThat(dto.getPermissions()).containsExactly("system:user:list");
    }

    @Test
    void noRolesYieldsEmptyPermissions() {
        when(userMapper.selectOne(any())).thenReturn(user(2L, "nobody"));
        when(userRoleMapper.selectRoleKeysByUserId(2L)).thenReturn(Set.of());

        UserAuthDTO dto = service.findByUsername("nobody");

        assertThat(dto.getRoles()).isEmpty();
        assertThat(dto.getPermissions()).isEmpty();
    }
}
