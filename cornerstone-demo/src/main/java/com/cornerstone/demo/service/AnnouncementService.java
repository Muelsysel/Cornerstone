package com.cornerstone.demo.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cornerstone.demo.domain.Announcement;

/** 公告服务：公告 CRUD、分页过滤、状态流转，定义业务规则并抛出 {@link com.cornerstone.common.exception.BusinessException}。 */
public interface AnnouncementService {

    /** 分页查询公告：标题模糊匹配 + 状态过滤，两个条件均可为空 */
    Page<Announcement> page(int pageNum, int pageSize, String title, Integer status);

    /** 详情：不存在抛业务异常 */
    Announcement getById(Long id);

    /** 新增：草稿态创建 */
    Announcement create(Announcement announcement);

    /** 编辑：仅草稿可编辑，发布/下线不允许改内容 */
    void update(Announcement announcement);

    /** 发布：草稿→已发布，并填充发布时间 */
    void publish(Long id);

    /** 下线：已发布→已下线 */
    void offline(Long id);

    /** 删除：逻辑删除（散掉删除后不可再流转） */
    void delete(Long id);
}
