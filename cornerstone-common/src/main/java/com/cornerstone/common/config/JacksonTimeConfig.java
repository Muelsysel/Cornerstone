package com.cornerstone.common.config;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import java.time.format.DateTimeFormatter;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 统一 java.time 的 JSON 序列化/反序列化格式：LocalDateTime → "yyyy-MM-dd HH:mm:ss"。
 *
 * <p>前后端契约（见 cornerstone-web 时间列展示）：前端直接展示后端字符串，故用空格分隔的 人类可读格式替代默认
 * ISO-8601（"2026-08-15T10:30:00"）。所有服务依赖 common 即全局生效。
 */
@Configuration
public class JacksonTimeConfig {

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonTimeCustomizer() {
        return builder ->
                builder.serializers(
                                new LocalDateTimeSerializer(DATE_TIME),
                                new LocalDateSerializer(DATE),
                                new LocalTimeSerializer(TIME))
                        .deserializers(
                                new LocalDateTimeDeserializer(DATE_TIME),
                                new LocalDateDeserializer(DATE),
                                new LocalTimeDeserializer(TIME));
    }
}
