package com.cornerstone.system.constant;

/** 业务类型常量。 */
public enum BusinessType {
    /** 其它 */
    OTHER(0),
    /** 新增 */
    INSERT(1),
    /** 修改 */
    UPDATE(2),
    /** 删除 */
    DELETE(3),
    /** 清空 */
    CLEAN(8);

    private final int code;

    BusinessType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
