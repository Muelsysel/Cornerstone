package com.cornerstone.api.client;

import com.cornerstone.api.ServiceConstants;
import com.cornerstone.api.dto.LoginLogDTO;
import com.cornerstone.common.core.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 登录日志投递契约：认证中心登录流程（成功/失败）经本契约把记录交给 system 落库。
 *
 * <p>契约先行：认证中心不得直连 system 的 HTTP 接口或数据库。 记录失败不允许阻塞登录主流程（auth 侧 try-catch 吞掉并 warn）。 system 侧实现于
 * {@code SysAuthUserController} 的 POST /system/auth/login-log（与 {@link AuthUserClient}
 * 同基路径，匿名白名单内，网关不含 /system/** 已隔离外部）。
 */
@FeignClient(
        name = ServiceConstants.SYSTEM_SERVICE,
        contextId = "loginLogClient",
        path = "/system/auth")
public interface LoginLogClient {

    /** 记录一条登录日志（成功 status=0 / 失败 status=1） */
    @PostMapping("/login-log")
    Result<Void> record(@RequestBody LoginLogDTO dto);
}
