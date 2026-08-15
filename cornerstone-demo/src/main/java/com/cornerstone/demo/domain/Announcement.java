package com.cornerstone.demo.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 公告实体。数据表见 {@code db/migration/V1__baseline.sql}， 审计字段由 {@code config/MyMetaObjectHandler} 自动填充。
 */
@Data
@TableName("announcement")
public class Announcement {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** 状态：0草稿/1已发布/2已下线，见 {@link AnnouncementStatus} */
    private Integer status;

    /** 作者（创建时由服务层自动填充当前用户名，展示用） */
    private String author;

    /** 发布时间 */
    private LocalDateTime publishTime;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除，配合 MyBatis-Plus 逻辑删除配置 */
    @TableLogic private Integer deleted;
}
