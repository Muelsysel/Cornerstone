package com.cornerstone.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cornerstone.common.exception.BusinessException;
import com.cornerstone.system.domain.entity.SysRole;
import com.cornerstone.system.domain.mapper.SysRoleMapper;
import com.cornerstone.system.domain.mapper.SysRoleMenuMapper;
import com.cornerstone.system.domain.mapper.SysUserRoleMapper;
import com.cornerstone.system.exception.SystemErrorCode;
import com.cornerstone.system.service.SysRoleService;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 角色服务实现。 */
@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole>
        implements SysRoleService {

    private final SysRoleMenuMapper roleMenuMapper;
    private final SysUserRoleMapper userRoleMapper;

    public SysRoleServiceImpl(SysRoleMenuMapper roleMenuMapper, SysUserRoleMapper userRoleMapper) {
        this.roleMenuMapper = roleMenuMapper;
        this.userRoleMapper = userRoleMapper;
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
                        .orderByAsc(SysRole::getSort);
        return this.page(new Page<>(current, size), wrapper);
    }

    @Override
    public SysRole add(SysRole role) {
        long exists =
                this.count(
                        new LambdaQueryWrapper<SysRole>()
                                .eq(SysRole::getRoleKey, role.getRoleKey()));
        if (exists > 0) {
            throw new BusinessException(SystemErrorCode.ROLE_KEY_EXISTS);
        }
        this.save(role);
        return role;
    }

    @Override
    public SysRole update(SysRole role) {
        SysRole exist = this.getById(role.getId());
        if (exist == null) {
            throw new BusinessException(SystemErrorCode.RESOURCE_NOT_FOUND);
        }
        this.updateById(role);
        return this.getById(role.getId());
    }

    @Override
    public void delete(Long roleId) {
        if (roleId != null && roleId == 1L) {
            throw new BusinessException(1009, "不允许删除内置超级管理员角色");
        }
        this.removeById(roleId);
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
        return this.list(new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getSort));
    }

    @Override
    public Set<String> getRoleKeysByUserId(Long userId) {
        return userRoleMapper.selectRoleKeysByUserId(userId);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
