package com.cornerstone.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/** Jackson 时间格式统一测试：LocalDateTime → "yyyy-MM-dd HH:mm:ss"（前后端时间列契约）。 */
class JacksonTimeConfigTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        JacksonTimeConfig config = new JacksonTimeConfig();
        Jackson2ObjectMapperBuilder builder =
                Jackson2ObjectMapperBuilder.json()
                        .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        config.jacksonTimeCustomizer().customize(builder);
        objectMapper = builder.build();
    }

    @Test
    void localDateTimeSerializedWithSpaceSeparatedFormat() throws Exception {
        String json = objectMapper.writeValueAsString(LocalDateTime.of(2026, 8, 15, 10, 30, 45));
        assertThat(json).isEqualTo("\"2026-08-15 10:30:45\"");
    }

    @Test
    void localDateAndTimeSerializedWithConfiguredFormats() throws Exception {
        assertThat(objectMapper.writeValueAsString(LocalDate.of(2026, 8, 15)))
                .isEqualTo("\"2026-08-15\"");
        assertThat(objectMapper.writeValueAsString(LocalTime.of(10, 30, 45)))
                .isEqualTo("\"10:30:45\"");
    }

    @Test
    void localDateTimeDeserializedFromSpaceSeparatedFormat() throws Exception {
        LocalDateTime value =
                objectMapper.readValue("\"2026-08-15 10:30:45\"", LocalDateTime.class);
        assertThat(value).isEqualTo(LocalDateTime.of(2026, 8, 15, 10, 30, 45));
    }
}
