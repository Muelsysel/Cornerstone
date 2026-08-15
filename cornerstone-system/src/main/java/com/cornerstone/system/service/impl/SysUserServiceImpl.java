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
        validateUsernameLength(user.getUsername());
        long exists =
                this.count(
                        new LambdaQueryWrapper<SysUser>()
                                .eq(SysUser::getUsername, user.getUsername()));
        if (exists > 0) {
            throw new BusinessException(SystemErrorCode.USERNAME_EXISTS);
        }
        // 未指定密码时使用默认密码
        String rawPassword = hasText(user.getPassword()) ? user.getPassword() : "123456";
        validatePasswordLength(rawPassword);
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
    @Transactional(rollbackFor = Exception.class)
    public SysUser update(SysUser user) {
        SysUser exist = this.getById(user.getId());
        if (exist == null) {
            throw new BusinessException(SystemErrorCode.USER_NOT_FOUND);
        }
        // 修改用户名时校验长度与唯一性（排除自己；并发撞唯一索引由 DuplicateKeyException 兜底）
        if (hasText(user.getUsername())) {
            validateUsernameLength(user.getUsername());
        }
        if (hasText(user.getUsername()) && !user.getUsername().equals(exist.getUsername())) {
            long exists =
                    this.count(
                            new LambdaQueryWrapper<SysUser>()
                                    .eq(SysUser::getUsername, user.getUsername())
                                    .ne(SysUser::getId, user.getId()));
            if (exists > 0) {
                throw new BusinessException(SystemErrorCode.USERNAME_EXISTS);
            }
        }
        // 密码字段只在显式传入时更新
        if (!hasText(user.getPassword())) {
            user.setPassword(null);
        } else {
            validatePasswordLength(user.getPassword());
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        try {
            this.updateById(user);
        } catch (DuplicateKeyException e) {
            // 并发同名：唯一索引兜底，转为业务错误而非裸 500
            throw new BusinessException(SystemErrorCode.USERNAME_EXISTS);
        }
        return this.getById(user.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId) {
        if (userId != null && userId == 1L) {
            throw new BusinessException(SystemErrorCode.DELETE_BUILTIN_USER);
        }
        this.removeById(userId);
        // 清理用户-角色关联，避免孤儿记录残留
        userRoleMapper.deleteUserRoleByUserId(userId);
    }

    @Override
    public void changeStatus(Long userId, String status) {
        // 与 resetPassword/assignRoles 一致：先校验存在，避免对不存在用户静默成功
        if (this.getById(userId) == null) {
            throw new BusinessException(SystemErrorCode.USER_NOT_FOUND);
        }
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
        validatePasswordLength(password);
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

    /** 用户名长度上限（与 DB varchar(30) 一致）：超长会触发 DataTruncation → 500，业务层先校验返回友好 400。 */
    private void validateUsernameLength(String username) {
        if (username != null && username.length() > 30) {
            throw new BusinessException(
                    com.cornerstone.common.core.ErrorCode.BAD_REQUEST, "用户名长度不能超过 30 个字符");
        }
    }

    /**
     * 密码长度校验（与认证/改密契约一致）：6-72 字符。 下限防弱密码；上限因 BCrypt 仅处理前 72 字节，超长会静默截断—— 若允许创建超长密码，登录时认证侧 @Size(72)
     * 会直接拒绝，用户将永远无法登录。
     */
    private void validatePasswordLength(String password) {
        if (password == null) {
            return;
        }
        if (password.length() < 6) {
            throw new BusinessException(
                    com.cornerstone.common.core.ErrorCode.BAD_REQUEST, "密码长度不能少于 6 个字符");
        }
        if (password.length() > 72) {
            throw new BusinessException(
                    com.cornerstone.common.core.ErrorCode.BAD_REQUEST, "密码长度不能超过 72 个字符");
        }
    }
}
