package com.cornerstone.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cornerstone.system.domain.entity.SysUser;

/** 用户服务接口。 */
public interface SysUserService {

    /** 分页查询用户 */
    Page<SysUser> page(long current, long size, String username, String status);

    /** 按 ID 查询用户 */
    SysUser getById(Long userId);

    /** 新增用户：密码 BCrypt 加密，用户名唯一校验 */
    SysUser add(SysUser user);

    /** 编辑用户 */
    SysUser update(SysUser user);

    /** 逻辑删除用户 */
    void delete(Long userId);

    /** 启用/停用用户 */
    void changeStatus(Long userId, String status);

    /** 重置密码 */
    void resetPassword(Long userId, String password);
}
