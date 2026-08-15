package com.cornerstone.auth.service;

import java.util.Set;

/** 用户登录成功响应体（POST /login）。 */
public record LoginResponse(
        String access_token,
        String token_type,
        long expires_in,
        Long userId,
        String username,
        Set<String> roles) {}
