package com.cornerstone.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.cornerstone.system.domain.entity.SysLoginLog;
import com.cornerstone.system.domain.mapper.SysLoginLogMapper;
import com.cornerstone.system.service.SysLoginLogService;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

/** 登录日志服务实现。 */
@Service
public class SysLoginLogServiceImpl extends ServiceImpl<SysLoginLogMapper, SysLoginLog>
        implements SysLoginLogService {

    @Override
    public void record(String username, String ipaddr, boolean success, String msg) {
        SysLoginLog log = new SysLoginLog();
        // 截断到 DB 列上限：登录接口允许 64 字符用户名（Redis key 安全），日志列 varchar(50)——
        // 超长会 DataTruncation → 审计记录丢失（登录不阻塞但审计断档）
        log.setUsername(truncate(username, 50));
        log.setIpaddr(truncate(ipaddr, 128));
        log.setStatus(success ? "0" : "1");
        log.setMsg(truncate(msg, 255));
        log.setLoginTime(LocalDateTime.now());
        this.save(log);
    }

    private String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }

    @Override
    public Page<SysLoginLog> page(
            long current,
            long size,
            String username,
            String status,
            String beginTime,
            String endTime) {
        LambdaQueryWrapper<SysLoginLog> wrapper =
                new LambdaQueryWrapper<SysLoginLog>()
                        .like(hasText(username), SysLoginLog::getUsername, username)
                        .eq(hasText(status), SysLoginLog::getStatus, status)
                        // 登录时间区间过滤（MySQL DATETIME 与字符串比较隐式转换）
                        .ge(hasText(beginTime), SysLoginLog::getLoginTime, beginTime)
                        .le(hasText(endTime), SysLoginLog::getLoginTime, endTime)
                        // 确定性排序：同一秒多条登录时按 id 倒序兜底，保证翻页不重不漏
                        .orderByDesc(SysLoginLog::getLoginTime)
                        .orderByDesc(SysLoginLog::getId);
        return this.page(new Page<>(current, size), wrapper);
    }

    @Override
    public void delete(Long infoId) {
        this.removeById(infoId);
    }

    @Override
    public void clean() {
        // 审计日志不做逻辑删除：全表清空
        this.remove(null);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
