package com.cornerstone.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cornerstone.system.domain.entity.SysLoginLog;

/** 登录日志服务接口。 */
public interface SysLoginLogService {

    /** 记录登录日志（成功/失败）。v1 无登录流程，由 v2 登录服务调用。 */
    void record(String username, String ipaddr, boolean success, String msg);

    /** 分页查询登录日志 */
    Page<SysLoginLog> page(long current, long size, String username, String status);

    /** 删除单条登录日志 */
    void delete(Long infoId);

    /** 清空登录日志 */
    void clean();
}
