package com.cornerstone.system.datascope;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cornerstone.common.security.UserContext;
import com.cornerstone.system.domain.entity.SysRole;
import com.cornerstone.system.domain.mapper.SysRoleDeptMapper;
import com.cornerstone.system.domain.mapper.SysRoleMapper;
import java.util.List;
import java.util.Objects;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * 数据范围解析：根据当前用户角色计算数据权限。
 *
 * <p>范围取用户所有角色中最严格的（数字越大越严格：仅本人(5) &gt; 本部门(4) &gt; 本部门及以下(3) &gt; 自定义(2) &gt;
 * 全部(1)）。匿名请求（无用户上下文）按无限制处理（调用方自行决定）。
 *
 * <p>Mapper 使用 @Lazy 注入：本服务被 DataPermissionInterceptor 依赖，而 Mapper 依赖
 * sqlSessionFactory（其初始化依赖拦截器），@Lazy 打破该循环。
 */
@Service
public class DataScopeService {

    private final SysRoleMapper roleMapper;
    private final SysRoleDeptMapper roleDeptMapper;

    public DataScopeService(
            @Lazy SysRoleMapper roleMapper, @Lazy SysRoleDeptMapper roleDeptMapper) {
        this.roleMapper = roleMapper;
        this.roleDeptMapper = roleDeptMapper;
    }

    /** 用户所有角色中最严格的 data_scope 值（'1'-'5'），无角色时返回 '4'（保守：本部门） */
    public String resolveDataScope(UserContext ctx) {
        if (ctx == null || ctx.getRoles() == null || ctx.getRoles().isEmpty()) {
            return "4";
        }
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysRole::getRoleKey, ctx.getRoles());
        List<SysRole> roles = roleMapper.selectList(wrapper);
        return roles.stream()
                .map(SysRole::getDataScope)
                .filter(Objects::nonNull)
                .map(s -> Integer.parseInt(s.trim()))
                .max(Integer::compareTo)
                .map(String::valueOf)
                .orElse("4");
    }

    /** 自定义范围(2)：用户角色关联的部门 ID 集合 */
    public List<Long> customDeptIds(UserContext ctx) {
        List<Long> roleIds = roleIdsOf(ctx);
        return roleDeptMapper.selectDeptIdsByRoleIds(roleIds);
    }

    /** 用户角色 ID 集合（由 role_key 反查） */
    public List<Long> roleIdsOf(UserContext ctx) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysRole::getRoleKey, ctx.getRoles());
        return roleMapper.selectList(wrapper).stream().map(SysRole::getId).toList();
    }
}
