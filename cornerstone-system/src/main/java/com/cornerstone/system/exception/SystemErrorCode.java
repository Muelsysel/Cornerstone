package com.cornerstone.system.exception;

import com.cornerstone.common.core.IErrorCode;

/** 系统服务业务错误码。从 1000 起，避免与 common 内置码冲突。 */
public enum SystemErrorCode implements IErrorCode {

    /** 用户名已存在 */
    USERNAME_EXISTS(1001, "用户名已存在"),
    /** 用户不存在 */
    USER_NOT_FOUND(1002, "用户不存在"),
    /** 角色已存在 */
    ROLE_KEY_EXISTS(1003, "角色标识已存在"),
    /** 父节点不能选自己或自身子节点 */
    INVALID_PARENT(1004, "父节点非法"),
    /** 字典类型已存在 */
    DICT_TYPE_EXISTS(1005, "字典类型已存在"),
    /** 参数键名已存在 */
    CONFIG_KEY_EXISTS(1006, "参数键名已存在"),
    /** 资源不存在 */
    RESOURCE_NOT_FOUND(1007, "资源不存在"),
    /** 不允许删除内置管理员用户 */
    DELETE_BUILTIN_USER(1008, "不允许删除内置管理员"),
    /** 不允许删除内置超级管理员角色 */
    DELETE_BUILTIN_ROLE(1009, "不允许删除内置超级管理员角色"),
    /** 存在子菜单，不允许删除 */
    DELETE_WITH_CHILD_MENU(1010, "存在子菜单,不允许删除"),
    /** 存在子部门，不允许删除 */
    DELETE_WITH_CHILD_DEPT(1011, "存在子部门,不允许删除"),
    /** 旧密码错误 */
    OLD_PASSWORD_ERROR(1012, "旧密码错误");

    private final int code;
    private final String message;

    SystemErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
