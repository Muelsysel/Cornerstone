package com.cornerstone.system.datascope;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cornerstone.common.security.UserContext;
import com.cornerstone.system.domain.entity.SysRole;
import com.cornerstone.system.domain.mapper.SysRoleDeptMapper;
import com.cornerstone.system.domain.mapper.SysRoleMapper;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * 数据范围解析单测：最严格范围选取、匿名/无角色回退、自定义部门 ID 委托。
 *
 * <p>数据权限是安全核心（SQL 层自动过滤），用 mock 隔离 Mapper 验证纯逻辑。
 */
class DataScopeServiceTest {

    @BeforeAll
    static void initTableInfo() {
        // 纯单测无 Spring 上下文：LambdaQueryWrapper 需要显式注册实体元数据（MP 3.5.9+）
        com.baomidou.mybatisplus.core.MybatisConfiguration configuration =
                new com.baomidou.mybatisplus.core.MybatisConfiguration();
        org.apache.ibatis.builder.MapperBuilderAssistant assistant =
                new org.apache.ibatis.builder.MapperBuilderAssistant(configuration, "");
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
                assistant, SysRole.class);
    }

    private final SysRoleMapper roleMapper = mock(SysRoleMapper.class);
    private final SysRoleDeptMapper roleDeptMapper = mock(SysRoleDeptMapper.class);
    private final DataScopeService service = new DataScopeService(roleMapper, roleDeptMapper);

    private SysRole role(Long id, String key, String scope) {
        SysRole r = new SysRole();
        r.setId(id);
        r.setRoleKey(key);
        r.setDataScope(scope);
        return r;
    }

    private UserContext ctx(String... roleKeys) {
        UserContext ctx = new UserContext();
        ctx.setRoles(Set.of(roleKeys));
        return ctx;
    }

    @Test
    void anonymousResolvesToDeptScope() {
        assertThat(service.resolveDataScope(null)).isEqualTo("4");
    }

    @Test
    void noRolesResolvesToDeptScope() {
        assertThat(service.resolveDataScope(ctx())).isEqualTo("4");
    }

    @Test
    void picksStrictestScopeAcrossRoles() {
        when(roleMapper.selectList(any()))
                .thenReturn(List.of(role(1L, "r1", "1"), role(2L, "r2", "5"), role(3L, "r3", "3")));
        assertThat(service.resolveDataScope(ctx("r1", "r2", "r3"))).isEqualTo("5");
    }

    @Test
    void unmatchedRolesFallbackToDeptScope() {
        when(roleMapper.selectList(any())).thenReturn(List.of());
        assertThat(service.resolveDataScope(ctx("ghost"))).isEqualTo("4");
    }

    @Test
    void roleIdsOfMapsRoleKeysToIds() {
        when(roleMapper.selectList(any()))
                .thenReturn(List.of(role(1L, "r1", "2"), role(2L, "r2", "3")));
        assertThat(service.roleIdsOf(ctx("r1", "r2"))).containsExactly(1L, 2L);
    }

    @Test
    void customDeptIdsDelegatesToRoleDeptMapper() {
        when(roleMapper.selectList(any())).thenReturn(List.of(role(1L, "r1", "2")));
        when(roleDeptMapper.selectDeptIdsByRoleIds(List.of(1L))).thenReturn(List.of(100L, 101L));
        assertThat(service.customDeptIds(ctx("r1"))).containsExactly(100L, 101L);
    }
}
