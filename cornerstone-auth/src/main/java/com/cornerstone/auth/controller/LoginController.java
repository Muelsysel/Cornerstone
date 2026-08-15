package com.cornerstone.auth.controller;

import com.cornerstone.auth.service.LoginRequest;
import com.cornerstone.auth.service.LoginResponse;
import com.cornerstone.auth.service.LoginService;
import com.cornerstone.common.core.Result;
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

    /** 用户名密码登录，签发 JWT。 */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(loginService.login(request));
    }
}
