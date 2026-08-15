package com.cornerstone.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.cornerstone.common.exception.BusinessException;
import com.cornerstone.common.util.ValidationUtils;
import com.cornerstone.system.domain.entity.SysRole;
import com.cornerstone.system.domain.mapper.SysRoleDeptMapper;
import com.cornerstone.system.domain.mapper.SysRoleMapper;
import com.cornerstone.system.domain.mapper.SysRoleMenuMapper;
import com.cornerstone.system.domain.mapper.SysUserRoleMapper;
import com.cornerstone.system.exception.SystemErrorCode;
import com.cornerstone.system.service.SysRoleService;
import java.util.List;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 角色服务实现。 */
@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole>
        implements SysRoleService {

    private final SysRoleMenuMapper roleMenuMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleDeptMapper roleDeptMapper;

    public SysRoleServiceImpl(
            SysRoleMenuMapper roleMenuMapper,
            SysUserRoleMapper userRoleMapper,
            SysRoleDeptMapper roleDeptMapper) {
        this.roleMenuMapper = roleMenuMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleDeptMapper = roleDeptMapper;
    }

    @Override
    public SysRole getById(Long roleId) {
        return super.getById(roleId);
    }

    @Override
    public Page<SysRole> page(long current, long size, String roleName, String status) {
        LambdaQueryWrapper<SysRole> wrapper =
                new LambdaQueryWrapper<SysRole>()
                        .like(hasText(roleName), SysRole::getRoleName, roleName)
                        .eq(hasText(status), SysRole::getStatus, status)
                        // 确定性排序：sort 可重复，按 id 升序兜底，保证翻页不重不漏
                        .orderByAsc(SysRole::getSort)
                        .orderByAsc(SysRole::getId);
        return this.page(new Page<>(current, size), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysRole add(SysRole role) {
        validateRoleFields(role);
        long exists =
                this.count(
                        new LambdaQueryWrapper<SysRole>()
                                .eq(SysRole::getRoleKey, role.getRoleKey()));
        if (exists > 0) {
            throw new BusinessException(SystemErrorCode.ROLE_KEY_EXISTS);
        }
        try {
            this.save(role);
        } catch (DuplicateKeyException e) {
            // 并发同 roleKey：唯一索引兜底，转为业务错误而非裸 500
            throw new BusinessException(SystemErrorCode.ROLE_KEY_EXISTS);
        }
        saveDataScopeDepts(role);
        return role;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysRole update(SysRole role) {
        SysRole exist = this.getById(role.getId());
        if (exist == null) {
            throw new BusinessException(SystemErrorCode.RESOURCE_NOT_FOUND);
        }
        validateRoleFields(role);
        // 修改 roleKey 时校验唯一性（排除自己；并发撞唯一索引由 DuplicateKeyException 兜底）
        if (hasText(role.getRoleKey()) && !role.getRoleKey().equals(exist.getRoleKey())) {
            long exists =
                    this.count(
                            new LambdaQueryWrapper<SysRole>()
                                    .eq(SysRole::getRoleKey, role.getRoleKey())
                                    .ne(SysRole::getId, role.getId()));
            if (exists > 0) {
                throw new BusinessException(SystemErrorCode.ROLE_KEY_EXISTS);
            }
        }
        try {
            this.updateById(role);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(SystemErrorCode.ROLE_KEY_EXISTS);
        }
        saveDataScopeDepts(role);
        return this.getById(role.getId());
    }

    /**
     * 数据范围=自定义(2)时维护 role_dept 关联；其他范围清空关联。 注意：事务由调用方 add/update 上的 {@link Transactional}
     * 保证（self-invocation 不经过代理）。
     */
    protected void saveDataScopeDepts(SysRole role) {
        if (role.getId() == null) {
            return;
        }
        roleDeptMapper.deleteByRoleId(role.getId());
        if ("2".equals(role.getDataScope())
                && role.getDeptIds() != null
                && !role.getDeptIds().isEmpty()) {
            roleDeptMapper.batchInsert(role.getId(), role.getDeptIds());
        }
    }

    @Override
    public List<Long> getDeptIdsByRoleId(Long roleId) {
        return roleDeptMapper.selectDeptIdsByRoleId(roleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long roleId) {
        if (roleId != null && roleId == 1L) {
            throw new BusinessException(SystemErrorCode.DELETE_BUILTIN_ROLE);
        }
        this.removeById(roleId);
        // 清理角色-菜单/角色-部门/用户-角色关联，避免孤儿记录残留
        roleMenuMapper.deleteRoleMenuByRoleId(roleId);
        roleDeptMapper.deleteByRoleId(roleId);
        userRoleMapper.deleteUserRoleByRoleId(roleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignMenus(Long roleId, List<Long> menuIds) {
        SysRole exist = this.getById(roleId);
        if (exist == null) {
            throw new BusinessException(SystemErrorCode.RESOURCE_NOT_FOUND);
        }
        roleMenuMapper.deleteRoleMenuByRoleId(roleId);
        if (menuIds == null || menuIds.isEmpty()) {
            return;
        }
        roleMenuMapper.batchInsertRoleMenu(roleId, menuIds);
    }

    @Override
    public List<Long> getMenuIdsByRoleId(Long roleId) {
        return roleMenuMapper.selectMenuIdsByRoleId(roleId);
    }

    @Override
    public List<SysRole> listAll() {
        return this.list(
                new LambdaQueryWrapper<SysRole>()
                        .orderByAsc(SysRole::getSort)
                        .orderByAsc(SysRole::getId));
    }

    @Override
    public Set<String> getRoleKeysByUserId(Long userId) {
        return userRoleMapper.selectRoleKeysByUserId(userId);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * 角色名称/标识长度校验（与 DB 列一致：role_name varchar(30)、role_key varchar(50)）： 超长会触发 DataTruncation →
     * 500，业务层先校验返回友好 400。
     */
    private void validateRoleFields(SysRole role) {
        ValidationUtils.maxLength(role.getRoleName(), 30, "角色名称");
        ValidationUtils.maxLength(role.getRoleKey(), 50, "角色标识");
        ValidationUtils.maxLength(role.getRemark(), 500, "备注");
        ValidationUtils.oneOf(role.getStatus(), "角色状态", "0", "1");
        // 数据范围非法值会在数据权限解析时被 fail-closed 视为「仅本人」（静默收缩权限），
        // 显式校验让配置错误在保存时就暴露
        ValidationUtils.oneOf(role.getDataScope(), "数据范围", "1", "2", "3", "4", "5");
    }
}
