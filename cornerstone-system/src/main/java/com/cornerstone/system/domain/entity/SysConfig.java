package com.cornerstone.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 参数配置实体。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_config")
public class SysConfig extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 参数主键（JSON 契约对齐前端 configId） */
    @TableId(type = IdType.AUTO)
    @com.fasterxml.jackson.annotation.JsonProperty("configId")
    private Long id;

    /** 参数名称 */
    private String configName;

    /** 参数键名 */
    private String configKey;

    /** 参数键值 */
    private String configValue;

    /** 系统内置:Y是,N否 */
    private String configType;

    /** 备注 */
    private String remark;
}
