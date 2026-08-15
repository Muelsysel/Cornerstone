package com.cornerstone.api.client;

import com.cornerstone.api.ServiceConstants;
import com.cornerstone.api.dto.UserAuthDTO;
import com.cornerstone.common.core.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 认证支持契约：auth 登录时经本契约从 system 获取用户认证信息。
 *
 * <p>契约先行：认证中心不得直连 system 的 HTTP 接口或数据库。本接口为服务间内部契约， system 侧实现须注意不把密码哈希暴露给网关外部（网关白名单不含
 * /system/**，已隔离）。
 */
@FeignClient(
        name = ServiceConstants.SYSTEM_SERVICE,
        contextId = "authUserClient",
        path = "/system/auth")
public interface AuthUserClient {

    /** 按用户名查询认证所需用户信息（含 BCrypt 密码哈希与角色权限） */
    @GetMapping("/user/{username}")
    Result<UserAuthDTO> findByUsername(@PathVariable("username") String username);
}
