package com.cornerstone.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
        log.setUsername(username);
        log.setIpaddr(ipaddr);
        log.setStatus(success ? "0" : "1");
        log.setMsg(msg);
        log.setLoginTime(LocalDateTime.now());
        this.save(log);
    }

    @Override
    public Page<SysLoginLog> page(long current, long size, String username, String status) {
        LambdaQueryWrapper<SysLoginLog> wrapper =
                new LambdaQueryWrapper<SysLoginLog>()
                        .like(hasText(username), SysLoginLog::getUsername, username)
                        .eq(hasText(status), SysLoginLog::getStatus, status)
                        .orderByDesc(SysLoginLog::getLoginTime);
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
