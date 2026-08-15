package com.cornerstone.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cornerstone.demo.domain.Announcement;
import org.apache.ibatis.annotations.Mapper;

/** 公告 Mapper。继承 {@link BaseMapper} 获得基础 CRUD，结合 MyBatis-Plus 条件构造器完成分页/过滤查询。 */
@Mapper
public interface AnnouncementMapper extends BaseMapper<Announcement> {}
