package com.cornerstone.api.client;

import com.cornerstone.api.ServiceConstants;
import com.cornerstone.api.dto.UserDTO;
import com.cornerstone.common.core.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** 系统服务用户契约（示例）。 契约先行：任何服务需要用户信息，只能通过本接口调用，禁止直连 cornerstone-system 的 HTTP 接口。 */
@FeignClient(name = ServiceConstants.SYSTEM_SERVICE, path = "/system/user")
public interface SystemUserClient {

    /** 按 ID 查询用户基础信息 */
    @GetMapping("/{userId}")
    Result<UserDTO> getUserById(@PathVariable("userId") Long userId);
}
