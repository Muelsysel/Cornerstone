package com.cornerstone.demo.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.cornerstone.common.security.UserContext;
import com.cornerstone.common.security.UserContextHolder;
import com.cornerstone.demo.domain.Announcement;
import java.time.LocalDateTime;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** 审计字段自动填充单测：登录用户回填 createBy/updateBy，匿名回退 system。 */
class MyMetaObjectHandlerTest {

    private final MyMetaObjectHandler handler = new MyMetaObjectHandler();

    @BeforeAll
    static void initTableInfo() {
        // 让 strictInsertFill/strictUpdateFill 能解析实体字段元数据
        com.baomidou.mybatisplus.core.MybatisConfiguration configuration =
                new com.baomidou.mybatisplus.core.MybatisConfiguration();
        org.apache.ibatis.builder.MapperBuilderAssistant assistant =
                new org.apache.ibatis.builder.MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, Announcement.class);
    }

    @AfterEach
    void clearContext() {
        UserContextHolder.clear();
    }

    private static MetaObject metaOf(Announcement entity) {
        return SystemMetaObject.forObject(entity);
    }

    @Test
    void insertFillWithLoggedInUser() {
        UserContext context = new UserContext();
        context.setUsername("alice");
        UserContextHolder.set(context);
        Announcement entity = new Announcement();

        handler.insertFill(metaOf(entity));

        assertThat(entity.getCreateBy()).isEqualTo("alice");
        assertThat(entity.getUpdateBy()).isEqualTo("alice");
        assertThat(entity.getCreateTime()).isNotNull();
        assertThat(entity.getUpdateTime()).isNotNull();
    }

    @Test
    void insertFillWithoutContextFallsBackToSystem() {
        UserContextHolder.clear();
        Announcement entity = new Announcement();

        handler.insertFill(metaOf(entity));

        assertThat(entity.getCreateBy()).isEqualTo("system");
        assertThat(entity.getUpdateBy()).isEqualTo("system");
        assertThat(entity.getCreateTime()).isNotNull();
    }

    @Test
    void updateFillOnlyTouchesUpdateFields() {
        UserContext context = new UserContext();
        context.setUsername("bob");
        UserContextHolder.set(context);
        Announcement entity = new Announcement();
        entity.setCreateBy("alice");
        LocalDateTime createTime = LocalDateTime.of(2026, 1, 1, 0, 0);
        entity.setCreateTime(createTime);

        handler.updateFill(metaOf(entity));

        assertThat(entity.getCreateBy()).isEqualTo("alice");
        assertThat(entity.getCreateTime()).isEqualTo(createTime);
        assertThat(entity.getUpdateBy()).isEqualTo("bob");
        assertThat(entity.getUpdateTime()).isNotNull();
    }

    @Test
    void updateFillBlankUsernameFallsBackToSystem() {
        UserContext context = new UserContext();
        context.setUsername("  ");
        UserContextHolder.set(context);
        Announcement entity = new Announcement();

        handler.updateFill(metaOf(entity));

        assertThat(entity.getUpdateBy()).isEqualTo("system");
    }
}
