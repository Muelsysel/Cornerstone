package com.cornerstone.system.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.cornerstone.common.security.UserContext;
import com.cornerstone.common.security.UserContextHolder;
import java.time.LocalDateTime;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

/** 元数据填充处理器：自动填充 创建/更新 时间与操作人。 操作人取自 {@link UserContextHolder}（网关透传），无上下文时留空。 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "deleted", Integer.class, 0);
        String operName = currentOperName();
        this.strictInsertFill(metaObject, "createBy", String.class, operName);
        this.strictInsertFill(metaObject, "updateBy", String.class, operName);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        this.strictUpdateFill(metaObject, "updateBy", String.class, currentOperName());
    }

    private String currentOperName() {
        UserContext context = UserContextHolder.get();
        return context != null ? context.getUsername() : "";
    }
}
