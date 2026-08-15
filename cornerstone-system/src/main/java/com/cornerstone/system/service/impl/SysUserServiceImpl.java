package com.cornerstone.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.cornerstone.common.exception.BusinessException;
import com.cornerstone.system.domain.entity.SysUser;
import com.cornerstone.system.domain.mapper.SysUserMapper;
import com.cornerstone.system.domain.mapper.SysUserRoleMapper;
import com.cornerstone.system.exception.SystemErrorCode;
import com.cornerstone.system.service.SysUserService;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 用户服务实现。 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser>
        implements SysUserService {

    private final PasswordEncoder passwordEncoder;
    private final SysUserRoleMapper userRoleMapper;

    public SysUserServiceImpl(PasswordEncoder passwordEncoder, SysUserRoleMapper userRoleMapper) {
        this.passwordEncoder = passwordEncoder;
        this.userRoleMapper = userRoleMapper;
    }

    @Override
    public SysUser getById(Long userId) {
        return super.getById(userId);
    }

    @Override
    public Page<SysUser> page(long current, long size, String username, String status) {
        LambdaQueryWrapper<SysUser> wrapper =
                new LambdaQueryWrapper<SysUser>()
                        .eq(hasText(username), SysUser::getUsername, username)
                        .eq(hasText(status), SysUser::getStatus, status)
                        .orderByAsc(SysUser::getId);
        return this.page(new Page<>(current, size), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysUser add(SysUser user) {
        long exists =
                this.count(
                        new LambdaQueryWrapper<SysUser>()
                                .eq(SysUser::getUsername, user.getUsername()));
        if (exists > 0) {
            throw new BusinessException(SystemErrorCode.USERNAME_EXISTS);
        }
        // 未指定密码时使用默认密码
        String rawPassword = hasText(user.getPassword()) ? user.getPassword() : "123456";
        user.setPassword(passwordEncoder.encode(rawPassword));
        try {
            this.save(user);
        } catch (DuplicateKeyException e) {
            // 并发同用户名：唯一索引兜底，转为业务错误而非裸 500
            throw new BusinessException(SystemErrorCode.USERNAME_EXISTS);
        }
        return user;
    }

    @Override
    public SysUser update(SysUser user) {
        SysUser exist = this.getById(user.getId());
        if (exist == null) {
            throw new BusinessException(SystemErrorCode.USER_NOT_FOUND);
        }
        // 密码字段只在显式传入时更新
        if (!hasText(user.getPassword())) {
            user.setPassword(null);
        } else {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        this.updateById(user);
        return this.getById(user.getId());
    }

    @Override
    public void delete(Long userId) {
        if (userId != null && userId == 1L) {
            throw new BusinessException(SystemErrorCode.DELETE_BUILTIN_USER);
        }
        this.removeById(userId);
    }

    @Override
    public void changeStatus(Long userId, String status) {
        SysUser user = new SysUser();
        user.setId(userId);
        user.setStatus(status);
        this.updateById(user);
    }

    @Override
    public void resetPassword(Long userId, String password) {
        SysUser exist = this.getById(userId);
        if (exist == null) {
            throw new BusinessException(SystemErrorCode.USER_NOT_FOUND);
        }
        SysUser patch = new SysUser();
        patch.setId(userId);
        patch.setPassword(passwordEncoder.encode(password));
        this.updateById(patch);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, List<Long> roleIds) {
        if (this.getById(userId) == null) {
            throw new BusinessException(SystemErrorCode.USER_NOT_FOUND);
        }
        userRoleMapper.deleteUserRoleByUserId(userId);
        if (roleIds != null && !roleIds.isEmpty()) {
            userRoleMapper.batchInsertUserRole(userId, roleIds);
        }
    }

    @Override
    public List<Long> getRoleIdsByUserId(Long userId) {
        return userRoleMapper.selectRoleIdsByUserId(userId);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
