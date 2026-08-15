package com.cornerstone.system.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cornerstone.system.domain.entity.SysOperLog;
import org.apache.ibatis.annotations.Mapper;

/** 操作日志 Mapper。 */
@Mapper
public interface SysOperLogMapper extends BaseMapper<SysOperLog> {}
