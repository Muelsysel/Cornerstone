package com.cornerstone.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 字典数据实体。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_data")
public class SysDictData extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 字典编码 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 字典类型 */
    private String dictType;

    /** 字典标签 */
    private String dictLabel;

    /** 字典键值 */
    private String dictValue;

    /** 显示顺序 */
    private Integer dictSort;

    /** 状态:0正常,1停用 */
    private String status;

    /** 是否默认:Y是,N否 */
    private String isDefault;

    /** 备注 */
    private String remark;
}
