package com.cornerstone.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/** 登录日志实体。 */
@Data
@TableName("sys_login_log")
public class SysLoginLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 访问ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户账号 */
    private String username;

    /** 登录IP地址 */
    private String ipaddr;

    /** 登录状态:0成功,1失败 */
    private String status;

    /** 提示消息 */
    private String msg;

    /** 访问时间 */
    private LocalDateTime loginTime;
}
