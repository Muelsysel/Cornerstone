package com.cornerstone.auth.controller;

import com.cornerstone.auth.service.LoginRequest;
import com.cornerstone.auth.service.LoginResponse;
import com.cornerstone.auth.service.LoginService;
import com.cornerstone.common.core.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 用户登录端点：POST /login，成功后返回 access_token 及用户摘要。 */
@RestController
public class LoginController {

    private final LoginService loginService;

    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    /** 用户名密码登录，签发 JWT。客户端 IP 一并透传用于登录日志。 */
    @PostMapping("/login")
    public Result<LoginResponse> login(
            @Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return Result.success(loginService.login(request, clientIp(httpRequest)));
    }

    /** 提取客户端 IP：优先 X-Forwarded-For（经网关透传），缺省回退远端地址。 */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
