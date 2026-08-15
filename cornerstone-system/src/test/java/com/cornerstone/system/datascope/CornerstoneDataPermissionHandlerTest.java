package com.cornerstone.system.datascope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cornerstone.common.security.UserContext;
import com.cornerstone.common.security.UserContextHolder;
import java.util.List;
import java.util.Set;
import net.sf.jsqlparser.expression.Expression;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** 数据权限处理器单元测试：各数据范围 SQL 条件生成。 */
class CornerstoneDataPermissionHandlerTest {

    private static final String USER_PAGE_MS_ID =
            "com.cornerstone.system.domain.mapper.SysUserMapper.page";

    private DataScopeService dataScopeService;
    private CornerstoneDataPermissionHandler handler;

    @BeforeEach
    void setUp() {
        dataScopeService = mock(DataScopeService.class);
        handler = new CornerstoneDataPermissionHandler(dataScopeService);
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    private void user(String scope, Long userId, Long deptId) {
        when(dataScopeService.resolveDataScope(org.mockito.ArgumentMatchers.any()))
                .thenReturn(scope);
        UserContext ctx = new UserContext();
        ctx.setUserId(userId);
        ctx.setDeptId(deptId);
        ctx.setRoles(Set.of("common"));
        UserContextHolder.set(ctx);
    }

    @Test
    void scopeAllAddsNoCondition() {
        user("1", 1L, 100L);
        assertNull(handler.getSqlSegment(null, USER_PAGE_MS_ID));
    }

    @Test
    void scopeDeptAddsDeptEquals() {
        user("4", 1L, 100L);
        Expression expr = handler.getSqlSegment(null, USER_PAGE_MS_ID);
        assertNotNull(expr);
        assertEquals("dept_id = 100", expr.toString());
    }

    @Test
    void scopeSelfAddsUserIdEquals() {
        user("5", 7L, 100L);
        Expression expr = handler.getSqlSegment(null, USER_PAGE_MS_ID);
        assertNotNull(expr);
        assertEquals("id = 7", expr.toString());
    }

    @Test
    void scopeCustomAddsDeptIn() {
        user("2", 1L, 100L);
        when(dataScopeService.customDeptIds(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(200L, 201L));
        Expression expr = handler.getSqlSegment(null, USER_PAGE_MS_ID);
        assertNotNull(expr);
        assertEquals("dept_id IN (200, 201)", expr.toString());
    }

    @Test
    void scopeCustomEmptyDeptsDenies() {
        user("2", 1L, 100L);
        when(dataScopeService.customDeptIds(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of());
        Expression expr = handler.getSqlSegment(null, USER_PAGE_MS_ID);
        assertNotNull(expr);
        assertEquals("dept_id = -1", expr.toString());
    }

    @Test
    void scopeDeptAndChildrenAddsSubquery() {
        user("3", 1L, 100L);
        Expression expr = handler.getSqlSegment(null, USER_PAGE_MS_ID);
        assertNotNull(expr);
        assertEquals(
                "dept_id IN (SELECT id FROM sys_dept WHERE id = 100 OR FIND_IN_SET(100, ancestors) > 0)",
                expr.toString());
    }

    @Test
    void anonymousUserAddsNoCondition() {
        UserContextHolder.clear();
        assertNull(handler.getSqlSegment(null, USER_PAGE_MS_ID));
    }

    @Test
    void nonManagedMapperNotIntercepted() {
        user("5", 1L, 100L);
        assertNull(
                handler.getSqlSegment(
                        null, "com.cornerstone.system.domain.mapper.SysRoleMapper.page"));
    }
}
