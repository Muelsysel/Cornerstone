package com.cornerstone.common.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class UserContextHolderTest {

    @Test
    void parsesFullHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put(UserContext.HEADER_USER_ID, "1");
        headers.put(UserContext.HEADER_USERNAME, "admin");
        headers.put(UserContext.HEADER_DEPT_ID, "100");
        headers.put(UserContext.HEADER_ROLES, "admin,common");

        UserContext context = UserContextHolder.parse(headers);

        assertNotNull(context);
        assertEquals(1L, context.getUserId());
        assertEquals("admin", context.getUsername());
        assertEquals(100L, context.getDeptId());
        assertTrue(context.getRoles().contains("admin"));
        assertTrue(context.getRoles().contains("common"));
    }

    @Test
    void returnsNullWhenUserIdMissing() {
        assertNull(UserContextHolder.parse(Map.of(UserContext.HEADER_USERNAME, "admin")));
    }

    @Test
    void returnsNullForEmptyHeaders() {
        assertNull(UserContextHolder.parse(Map.of()));
    }

    @Test
    void parsesInvalidDeptIdAsNull() {
        Map<String, String> headers = new HashMap<>();
        headers.put(UserContext.HEADER_USER_ID, "1");
        headers.put(UserContext.HEADER_DEPT_ID, "not-a-number");

        UserContext context = UserContextHolder.parse(headers);

        // 伪造/异常格式的部门 ID 头应降级为 null，不得抛异常
        assertNotNull(context);
        assertEquals(1L, context.getUserId());
        assertNull(context.getDeptId());
    }
}
