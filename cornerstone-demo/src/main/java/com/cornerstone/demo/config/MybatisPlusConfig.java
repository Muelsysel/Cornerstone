package com.cornerstone.demo.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** MyBatis-Plus 配置：分页插件。逻辑删除由 application.yml 全局配置驱动，无需 Java Bean。 */
@Configuration
public class MybatisPlusConfig {

    /** 分页插件：按 MySQL 方言分页；maxLimit 防止超大 pageSize 拖垮数据库 */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        pagination.setMaxLimit(500L);
        // pageNum 超出总页数时自动回退到最后一页，避免越界空页（配合前端删除回退双保险）
        pagination.setOverflow(true);
        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }
}
