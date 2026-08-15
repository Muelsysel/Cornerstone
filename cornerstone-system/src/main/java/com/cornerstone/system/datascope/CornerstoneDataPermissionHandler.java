package com.cornerstone.system.datascope;

import com.baomidou.mybatisplus.extension.plugins.handler.DataPermissionHandler;
import com.cornerstone.common.security.UserContext;
import com.cornerstone.common.security.UserContextHolder;
import java.util.List;
import java.util.stream.Collectors;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import org.springframework.stereotype.Component;

/**
 * 数据权限处理器（MyBatis-Plus DataPermissionInterceptor 的 SQL 层拦截）。
 *
 * <p>按 mappedStatementId 判定目标：仅拦截 {@code SysUserMapper} 的查询（用户分页等），其他 Mapper
 * 不拦截（避免递归——处理器内部查角色/部门表的 SQL 不会被二次拦截）。范围语义见 {@link DataScopeService}：1全部 2自定义 3本部门及以下 4本部门
 * 5仅本人。匿名请求不追加条件。
 */
@Component
public class CornerstoneDataPermissionHandler implements DataPermissionHandler {

    /** 受数据权限管控的 Mapper 全限定名片段（单表查询，条件列无表名前缀） */
    private static final String MANAGED_MAPPER = "SysUserMapper";

    private final DataScopeService dataScopeService;

    public CornerstoneDataPermissionHandler(DataScopeService dataScopeService) {
        this.dataScopeService = dataScopeService;
    }

    @Override
    public Expression getSqlSegment(Expression where, String mappedStatementId) {
        if (mappedStatementId == null || !mappedStatementId.contains(MANAGED_MAPPER)) {
            return null;
        }
        UserContext ctx = UserContextHolder.get();
        if (ctx == null || ctx.getUserId() == null) {
            // 匿名或服务调用：不限制
            return null;
        }
        String scope = dataScopeService.resolveDataScope(ctx);
        try {
            Expression condition = buildCondition(scope, ctx);
            if (condition == null) {
                return null;
            }
            // 数据权限条件与原有 WHERE 合并（AND），不可替换——原条件（如 id=?）的参数必须保留
            return where == null ? condition : new AndExpression(where, condition);
        } catch (Exception e) {
            // 数据权限解析失败时保守拒绝：加不可能成立的条件，防止越权
            return new EqualsTo(new Column("id"), new LongValue(-1));
        }
    }

    private Expression buildCondition(String scope, UserContext ctx) throws Exception {
        switch (scope) {
            case "1": // 全部
                return null;
            case "2":
                { // 自定义：角色关联部门
                    List<Long> deptIds = dataScopeService.customDeptIds(ctx);
                    if (deptIds.isEmpty()) {
                        return new EqualsTo(new Column("dept_id"), new LongValue(-1));
                    }
                    return CCJSqlParserUtil.parseCondExpression(
                            "dept_id IN (" + join(deptIds) + ")");
                }
            case "3":
                { // 本部门及以下
                    Long deptId = ctx.getDeptId();
                    if (deptId == null) {
                        // fail-closed：无部门归属时不可见任何数据（与 scope 2 空部门集合同语义），
                        // 避免用户无部门却看到全部数据（越权）
                        return new EqualsTo(new Column("dept_id"), new LongValue(-1));
                    }
                    return CCJSqlParserUtil.parseCondExpression(
                            "dept_id IN (SELECT id FROM sys_dept WHERE id = "
                                    + deptId
                                    + " OR FIND_IN_SET("
                                    + deptId
                                    + ", ancestors) > 0)");
                }
            case "4":
                { // 本部门
                    Long deptId = ctx.getDeptId();
                    if (deptId == null) {
                        return new EqualsTo(new Column("dept_id"), new LongValue(-1));
                    }
                    return CCJSqlParserUtil.parseCondExpression("dept_id = " + deptId);
                }
            case "5": // 仅本人
            default:
                return CCJSqlParserUtil.parseCondExpression("id = " + ctx.getUserId());
        }
    }

    private static String join(List<Long> ids) {
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }
}
