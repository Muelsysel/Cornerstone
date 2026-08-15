package com.cornerstone.system.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cornerstone.system.domain.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/** 用户 Mapper。 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {}
