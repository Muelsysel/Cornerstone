package com.cornerstone.system;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cornerstone.system.domain.entity.SysMenu;
import com.cornerstone.system.domain.entity.SysRole;
import com.cornerstone.system.domain.entity.SysUser;
import com.cornerstone.system.domain.mapper.SysMenuMapper;
import com.cornerstone.system.domain.mapper.SysRoleMapper;
import com.cornerstone.system.domain.mapper.SysRoleMenuMapper;
import com.cornerstone.system.domain.mapper.SysUserMapper;
import com.cornerstone.system.domain.mapper.SysUserRoleMapper;
import com.cornerstone.system.service.SysLoginLogService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 认证支持接口测试：GET /system/auth/user/{username} 返回 UserAuthDTO（roles/permissions 正确）； POST
 * /system/auth/login-log 接收登录日志并调用 SysLoginLogService.record 落库。 Mapper/Service 层 @MockBean 隔离，不依赖
 * MySQL/Redis；本接口在白名单匿名访问，无需 JWT。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SysAuthUserControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private SysUserMapper userMapper;
    @MockBean private SysUserRoleMapper userRoleMapper;
    @MockBean private SysRoleMapper roleMapper;
    @MockBean private SysRoleMenuMapper roleMenuMapper;
    @MockBean private SysMenuMapper menuMapper;
    @MockBean private SysLoginLogService loginLogService;

    @Test
    void findByUsername_shouldReturnRolesAndPermissions() throws Exception {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("admin");
        user.setPassword(
                "$2a$10$xJApkFn6HOZ3FKqZoMP8Be87fUF2FOEdMf12RJ6.ykP8/RXTM272S"); // bcrypt('admin123')
        user.setDeptId(100L);
        when(userMapper.selectOne(any())).thenReturn(user);

        when(userRoleMapper.selectRoleKeysByUserId(1L)).thenReturn(Set.of("admin"));

        SysRole adminRole = new SysRole();
        adminRole.setId(1L);
        adminRole.setRoleKey("admin");
        adminRole.setRoleName("超级管理员");
        adminRole.setStatus("0");
        when(roleMapper.selectList(any())).thenReturn(List.of(adminRole));

        when(roleMenuMapper.selectMenuIdsByRoleId(1L)).thenReturn(List.of(10L, 20L, 30L));

        SysMenu userList = menu("system:user:list", "0");
        SysMenu roleList = menu("system:role:list", "0");
        SysMenu stopped = menu("system:stopped:list", "1"); // 停用菜单 perms 应被过滤
        SysMenu blankPerm = menu("", "0"); // 空 perms 应被过滤
        when(menuMapper.selectBatchIds(any()))
                .thenReturn(List.of(userList, roleList, stopped, blankPerm));

        mockMvc.perform(get("/system/auth/user/admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(
                        jsonPath("$.data.password")
                                .value(
                                        "$2a$10$xJApkFn6HOZ3FKqZoMP8Be87fUF2FOEdMf12RJ6.ykP8/RXTM272S"))
                .andExpect(jsonPath("$.data.deptId").value(100))
                .andExpect(jsonPath("$.data.roles[0]").value("admin"))
                .andExpect(jsonPath("$.data.permissions[0]").value("system:user:list"))
                .andExpect(jsonPath("$.data.permissions[1]").value("system:role:list"))
                .andExpect(jsonPath("$.data.permissions.length()").value(2));
    }

    @Test
    void findByUnknownUser_shouldReturnNullData() throws Exception {
        when(userMapper.selectOne(any())).thenReturn(null);

        mockMvc.perform(get("/system/auth/user/nobody"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void recordLoginLog_shouldInvokeSysLoginLogService() throws Exception {
        mockMvc.perform(
                        post("/system/auth/login-log")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"username\":\"admin\",\"status\":\"0\",\"msg\":\"登录成功\","
                                                + "\"ipaddr\":\"127.0.0.1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 契约实现应调用 SysLoginLogService.record 落库（status "0"→success=true）
        verify(loginLogService).record(eq("admin"), eq("127.0.0.1"), anyBoolean(), eq("登录成功"));
    }

    private SysMenu menu(String perms, String status) {
        SysMenu menu = new SysMenu();
        menu.setPerms(perms);
        menu.setStatus(status);
        return menu;
    }
}
