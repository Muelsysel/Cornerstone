package com.cornerstone.demo.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.cornerstone.common.security.UserContext;
import com.cornerstone.common.security.UserContextHolder;
import java.time.LocalDateTime;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

/** 审计字段自动填充：创建/更新人取当前用户上下文，匿名回退 {@code system}； 创建/更新时间由数据库统一取系统时间。 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        String operator = currentOperator();
        strictInsertFill(metaObject, "createBy", String.class, operator);
        strictInsertFill(metaObject, "updateBy", String.class, operator);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        strictUpdateFill(metaObject, "updateBy", String.class, currentOperator());
    }

    /** 取当前用户，匿名回退 system */
    private String currentOperator() {
        UserContext context = UserContextHolder.get();
        if (context != null && context.getUsername() != null && !context.getUsername().isBlank()) {
            return context.getUsername();
        }
        return "system";
    }
}
