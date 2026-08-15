package com.cornerstone.demo.domain;

/** 公告状态。领域术语：草稿→已发布→已下线，单向流转，非法流转由服务层拒绝。 */
public enum AnnouncementStatus {

    /** 草稿 */
    DRAFT(0),
    /** 已发布 */
    PUBLISHED(1),
    /** 已下线 */
    OFFLINE(2);

    private final int code;

    AnnouncementStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    /** 按数据库字面值解析状态；未知值返回 null（按不存在处理） */
    public static AnnouncementStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (AnnouncementStatus s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        return null;
    }
}
