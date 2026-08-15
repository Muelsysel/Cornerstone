package com.cornerstone.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 字典类型实体。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_type")
public class SysDictType extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 字典主键（JSON 契约对齐前端 dictId） */
    @TableId(type = IdType.AUTO)
    @com.fasterxml.jackson.annotation.JsonProperty("dictId")
    private Long id;

    /** 字典名称 */
    private String dictName;

    /** 字典类型 */
    private String dictType;

    /** 状态:0正常,1停用 */
    private String status;

    /** 备注 */
    private String remark;
}
