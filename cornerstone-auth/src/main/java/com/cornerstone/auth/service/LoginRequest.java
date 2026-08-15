package com.cornerstone.auth.service;

import jakarta.validation.constraints.NotBlank;

/** 用户登录请求体（POST /login）。 */
public record LoginRequest(
        @NotBlank(message = "用户名不能为空") String username,
        @NotBlank(message = "密码不能为空") String password) {}
