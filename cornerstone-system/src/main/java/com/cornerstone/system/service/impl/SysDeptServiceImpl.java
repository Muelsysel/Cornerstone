package com.cornerstone.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cornerstone.common.exception.BusinessException;
import com.cornerstone.system.domain.entity.SysDept;
import com.cornerstone.system.domain.mapper.SysDeptMapper;
import com.cornerstone.system.exception.SystemErrorCode;
import com.cornerstone.system.service.SysDeptService;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/** 部门服务实现。 */
@Service
public class SysDeptServiceImpl implements SysDeptService {

    private final SysDeptMapper deptMapper;

    public SysDeptServiceImpl(SysDeptMapper deptMapper) {
        this.deptMapper = deptMapper;
    }

    @Override
    public List<SysDept> listTree(String deptName, String status) {
        LambdaQueryWrapper<SysDept> wrapper =
                new LambdaQueryWrapper<SysDept>()
                        .like(hasText(deptName), SysDept::getDeptName, deptName)
                        .eq(hasText(status), SysDept::getStatus, status)
                        // 确定性排序：sort 可重复，按 id 升序兜底，保证树形结构稳定
                        .orderByAsc(SysDept::getSort)
                        .orderByAsc(SysDept::getId);
        List<SysDept> all = deptMapper.selectList(wrapper);
        return buildTree(all);
    }

    @Override
    public SysDept add(SysDept dept) {
        validateDeptNameLength(dept.getDeptName());
        if (dept.getParentId() == null) {
            dept.setParentId(0L);
        }
        dept.setAncestors(resolveAncestors(dept.getParentId()));
        deptMapper.insert(dept);
        return dept;
    }

    @Override
    public SysDept update(SysDept dept) {
        SysDept exist = deptMapper.selectById(dept.getId());
        if (exist == null) {
            throw new BusinessException(SystemErrorCode.RESOURCE_NOT_FOUND);
        }
        validateDeptNameLength(dept.getDeptName());
        // 父节点不能选自己或自身子节点（选子节点会形成环：A→B→C→A）
        Long newParent = dept.getParentId();
        if (Objects.equals(newParent, dept.getId())
                || (newParent != null
                        && newParent != 0L
                        && isDescendant(dept.getId(), newParent))) {
            throw new BusinessException(SystemErrorCode.INVALID_PARENT);
        }
        dept.setAncestors(resolveAncestors(dept.getParentId()));
        deptMapper.updateById(dept);
        // 移动部门后级联更新子孙的 ancestors（否则子孙路径指向旧位置，数据不一致）
        cascadeUpdateDescendantAncestors(dept.getId(), dept.getAncestors());
        return deptMapper.selectById(dept.getId());
    }

    /** 递归更新某节点所有子孙的 ancestors：子节点 ancestors = 新父 ancestors + "," + 父 id */
    private void cascadeUpdateDescendantAncestors(Long parentId, String parentAncestors) {
        List<SysDept> children =
                deptMapper.selectList(
                        new LambdaQueryWrapper<SysDept>().eq(SysDept::getParentId, parentId));
        for (SysDept child : children) {
            String childAncestors = parentAncestors + "," + parentId;
            SysDept patch = new SysDept();
            patch.setId(child.getId());
            patch.setAncestors(childAncestors);
            deptMapper.updateById(patch);
            cascadeUpdateDescendantAncestors(child.getId(), childAncestors);
        }
    }

    /** candidateParentId 是否为 nodeId 的子孙节点（全量查询本地遍历，部门数量级小可接受） */
    private boolean isDescendant(Long nodeId, Long candidateParentId) {
        List<SysDept> all = deptMapper.selectList(null);
        java.util.Set<Long> descendants = new java.util.HashSet<>();
        collectDescendants(all, nodeId, descendants);
        return descendants.contains(candidateParentId);
    }

    private void collectDescendants(
            List<SysDept> all, Long parentId, java.util.Set<Long> descendants) {
        for (SysDept dept : all) {
            if (Objects.equals(dept.getParentId(), parentId) && descendants.add(dept.getId())) {
                collectDescendants(all, dept.getId(), descendants);
            }
        }
    }

    @Override
    public void delete(Long deptId) {
        long children =
                deptMapper.selectCount(
                        new LambdaQueryWrapper<SysDept>().eq(SysDept::getParentId, deptId));
        if (children > 0) {
            throw new BusinessException(SystemErrorCode.DELETE_WITH_CHILD_DEPT);
        }
        deptMapper.deleteById(deptId);
    }

    private String resolveAncestors(Long parentId) {
        if (parentId == null || parentId == 0L) {
            return "0";
        }
        SysDept parent = deptMapper.selectById(parentId);
        if (parent == null) {
            throw new BusinessException(SystemErrorCode.INVALID_PARENT);
        }
        return parent.getAncestors() + "," + parentId;
    }

    private List<SysDept> buildTree(List<SysDept> all) {
        Map<Long, List<SysDept>> byParent =
                all.stream()
                        .collect(
                                Collectors.groupingBy(
                                        d -> d.getParentId() == null ? 0L : d.getParentId()));
        return all.stream()
                .filter(d -> d.getParentId() == null || d.getParentId() == 0L)
                .peek(root -> attachChildren(root, byParent))
                .collect(Collectors.toList());
    }

    private void attachChildren(SysDept node, Map<Long, List<SysDept>> byParent) {
        List<SysDept> children = byParent.getOrDefault(node.getId(), List.of());
        node.setChildren(children);
        children.forEach(c -> attachChildren(c, byParent));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /** 部门名长度上限（与 DB varchar(50) 一致）：防 DataTruncation → 500。 */
    private void validateDeptNameLength(String deptName) {
        if (deptName != null && deptName.length() > 50) {
            throw new BusinessException(
                    com.cornerstone.common.core.ErrorCode.BAD_REQUEST, "部门名称不能超过 50 个字符");
        }
    }
}
