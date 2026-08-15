package com.cornerstone.auth.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 用户登录请求体（POST /login）。
 *
 * <p>长度上限是安全边界：用户名会被拼入 Redis key 与日志（防超大值打爆内存/日志）； 密码限制 ≤72 字节（BCrypt 仅处理前 72
 * 字节，超长会静默截断，两个不同长密码可能判为相同）。
 */
public record LoginRequest(
        @NotBlank(message = "用户名不能为空") @Size(max = 64, message = "用户名长度不能超过 64 个字符")
                String username,
        @NotBlank(message = "密码不能为空") @Size(max = 72, message = "密码长度不能超过 72 个字符")
                String password) {}
