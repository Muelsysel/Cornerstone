package com.cornerstone.system.controller;

import com.cornerstone.api.client.AuthUserClient;
import com.cornerstone.api.dto.UserAuthDTO;
import com.cornerstone.common.core.Result;
import com.cornerstone.system.service.AuthUserSupportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证支持接口：为认证中心登录提供用户认证信息。
 *
 * <p>实现 {@link AuthUserClient} 契约（GET /system/auth/user/{username}），响应含密码哈希，仅供服务间内部调用。 服务间内部接口，v1
 * 简化匿名访问（网关白名单不含 /system/**，已隔离外部）；生产环境需服务间认证。
 */
@RestController
@RequestMapping("/system/auth")
public class SysAuthUserController {

    private final AuthUserSupportService supportService;

    public SysAuthUserController(AuthUserSupportService supportService) {
        this.supportService = supportService;
    }

    /** 按用户名查询认证所需用户信息（含 BCrypt 密码哈希与角色权限）。 */
    @GetMapping("/user/{username}")
    public Result<UserAuthDTO> findByUsername(@PathVariable("username") String username) {
        return Result.success(supportService.findByUsername(username));
    }
}
