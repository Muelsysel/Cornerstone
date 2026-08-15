package com.cornerstone.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/** 操作日志实体。 */
@Data
@TableName("sys_oper_log")
public class SysOperLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 日志主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 模块标题 */
    private String title;

    /** 业务类型:0其它,1新增,2修改,3删除 */
    private Integer businessType;

    /** 方法名称 */
    private String method;

    /** 请求方式 */
    private String requestMethod;

    /** 操作人员 */
    private String operName;

    /** 请求URL */
    private String operUrl;

    /** 主机地址 */
    private String operIp;

    /** 请求参数 */
    private String operParam;

    /** 返回参数 */
    private String jsonResult;

    /** 操作状态:0正常,1异常 */
    private Integer status;

    /** 错误消息 */
    private String errorMsg;

    /** 操作时间 */
    private LocalDateTime operTime;
}
