package com.cornerstone.api.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 登录日志记录 DTO（契约）。
 *
 * <p>认证中心登录流程经 {@link com.cornerstone.api.client.LoginLogClient} 投递，系统服务据此落库。 status 沿用登录日志约定：0 成功
 * / 1 失败。
 */
public class LoginLogDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 登录账号（用户不存在时用请求中的用户名） */
    private String username;

    /** 登录状态：0 成功 / 1 失败 */
    private String status;

    /** 提示消息 */
    private String msg;

    /** 登录 IP 地址 */
    private String ipaddr;

    /** 登录时间 */
    private LocalDateTime loginTime;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getIpaddr() {
        return ipaddr;
    }

    public void setIpaddr(String ipaddr) {
        this.ipaddr = ipaddr;
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(LocalDateTime loginTime) {
        this.loginTime = loginTime;
    }
}
