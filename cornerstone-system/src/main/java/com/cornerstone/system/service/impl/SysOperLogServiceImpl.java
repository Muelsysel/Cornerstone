package com.cornerstone.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cornerstone.system.domain.entity.SysOperLog;
import com.cornerstone.system.domain.mapper.SysOperLogMapper;
import com.cornerstone.system.service.SysOperLogService;
import org.springframework.stereotype.Service;

/** 操作日志服务实现。 */
@Service
public class SysOperLogServiceImpl extends ServiceImpl<SysOperLogMapper, SysOperLog>
        implements SysOperLogService {

    @Override
    public void record(SysOperLog log) {
        this.save(log);
    }

    @Override
    public Page<SysOperLog> page(
            long current, long size, String title, String operName, String status) {
        LambdaQueryWrapper<SysOperLog> wrapper =
                new LambdaQueryWrapper<SysOperLog>()
                        .like(hasText(title), SysOperLog::getTitle, title)
                        .like(hasText(operName), SysOperLog::getOperName, operName)
                        .eq(hasText(status), SysOperLog::getStatus, status)
                        .orderByDesc(SysOperLog::getOperTime);
        return this.page(new Page<>(current, size), wrapper);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
