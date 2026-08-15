package com.cornerstone.common.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ResultTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void successCarriesCode200AndData() {
        Result<String> result = Result.success("hello");
        assertTrue(result.isSuccess());
        assertEquals(200, result.getCode());
        assertEquals("hello", result.getData());
    }

    @Test
    void failCarriesErrorCodeAndNullData() {
        Result<Void> result = Result.fail(ErrorCode.BAD_REQUEST);
        assertFalse(result.isSuccess());
        assertEquals(400, result.getCode());
        assertEquals("请求参数错误", result.getMessage());
    }

    @Test
    void serializesToStableJsonShape() throws JsonProcessingException {
        Result<String> result = Result.success("x");
        String json = objectMapper.writeValueAsString(result);
        assertEquals("{\"code\":200,\"message\":\"操作成功\",\"data\":\"x\"}", json);
    }
}
