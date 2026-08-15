package com.cornerstone.system.controller;

import com.cornerstone.api.client.AuthUserClient;
import com.cornerstone.api.client.LoginLogClient;
import com.cornerstone.api.dto.LoginLogDTO;
import com.cornerstone.api.dto.UserAuthDTO;
import com.cornerstone.common.core.Result;
import com.cornerstone.system.service.AuthUserSupportService;
import com.cornerstone.system.service.SysLoginLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证支持接口：为认证中心登录提供服务。
 *
 * <p>实现 {@link AuthUserClient} 契约（GET /system/auth/user/{username}），响应含密码哈希，仅供服务间内部调用； 并实现 {@link
 * LoginLogClient} 契约（POST /system/auth/login-log）接收登录日志落库。 服务间内部接口，v1 简化匿名访问（网关白名单不含
 * /system/**，已隔离外部）；生产环境需服务间认证。
 */
@RestController
@RequestMapping("/system/auth")
public class SysAuthUserController {

    private final AuthUserSupportService supportService;
    private final SysLoginLogService loginLogService;

    public SysAuthUserController(
            AuthUserSupportService supportService, SysLoginLogService loginLogService) {
        this.supportService = supportService;
        this.loginLogService = loginLogService;
    }

    /** 按用户名查询认证所需用户信息（含 BCrypt 密码哈希与角色权限）。 */
    @GetMapping("/user/{username}")
    public Result<UserAuthDTO> findByUsername(@PathVariable("username") String username) {
        return Result.success(supportService.findByUsername(username));
    }

    /** 记录登录日志（认证中心登录成功后/失败时投递）。status：0 成功 / 1 失败。 */
    @PostMapping("/login-log")
    public Result<Void> record(@RequestBody LoginLogDTO dto) {
        loginLogService.record(
                dto.getUsername(), dto.getIpaddr(), "0".equals(dto.getStatus()), dto.getMsg());
        return Result.success();
    }
}
