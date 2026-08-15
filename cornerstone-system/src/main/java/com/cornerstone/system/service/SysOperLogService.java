package com.cornerstone.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cornerstone.system.domain.entity.SysOperLog;

/** 操作日志服务接口。 */
public interface SysOperLogService {

    /** 记录操作日志 */
    void record(SysOperLog log);

    /** 分页查询操作日志 */
    Page<SysOperLog> page(long current, long size, String title, String operName, String status);
}
