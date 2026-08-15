package com.cornerstone.system.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cornerstone.common.exception.BusinessException;
import com.cornerstone.system.domain.entity.SysDept;
import com.cornerstone.system.domain.mapper.SysDeptMapper;
import com.cornerstone.system.exception.SystemErrorCode;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** 部门服务单测：树组装、ancestors 解析、父节点非法校验、子部门删除保护。 */
class SysDeptServiceImplTest {

    private final SysDeptMapper deptMapper = mock(SysDeptMapper.class);
    private SysDeptServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SysDeptServiceImpl(deptMapper);
    }

    private SysDept dept(Long id, Long parentId, String name, String ancestors, Integer sort) {
        SysDept d = new SysDept();
        d.setId(id);
        d.setParentId(parentId);
        d.setDeptName(name);
        d.setAncestors(ancestors);
        d.setSort(sort);
        return d;
    }

    @Test
    void listTreeBuildsNestedTree() {
        SysDept root = dept(1L, 0L, "总公司", "0", 1);
        SysDept child = dept(2L, 1L, "研发部", "0,1", 1);
        SysDept grandchild = dept(3L, 2L, "前端组", "0,1,2", 1);
        when(deptMapper.selectList(any())).thenReturn(List.of(root, grandchild, child));

        List<SysDept> tree = service.listTree(null, null);

        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).getChildren())
                .extracting(SysDept::getDeptName)
                .containsExactly("研发部");
        assertThat(tree.get(0).getChildren().get(0).getChildren())
                .extracting(SysDept::getDeptName)
                .containsExactly("前端组");
    }

    @Test
    void addAtRootSetsAncestorsToZero() {
        SysDept d = dept(null, 0L, "新部门", null, 1);
        when(deptMapper.insert(d)).thenReturn(1);

        service.add(d);

        assertThat(d.getAncestors()).isEqualTo("0");
        verify(deptMapper).insert(d);
    }

    @Test
    void addUnderParentResolvesAncestorsChain() {
        SysDept parent = dept(5L, 1L, "研发部", "0,1", 1);
        SysDept d = dept(null, 5L, "测试组", null, 1);
        when(deptMapper.selectById(5L)).thenReturn(parent);
        when(deptMapper.insert(d)).thenReturn(1);

        service.add(d);

        assertThat(d.getAncestors()).isEqualTo("0,1,5");
    }

    @Test
    void addUnderMissingParentRejects() {
        SysDept d = dept(null, 99L, "孤儿部门", null, 1);
        when(deptMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> service.add(d))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(SystemErrorCode.INVALID_PARENT.getCode());
        verify(deptMapper, never()).insert(any(SysDept.class));
    }

    @Test
    void updateRejectsSelfAsParent() {
        SysDept d = dept(3L, 3L, "把自己挂自己", "0", 1);
        when(deptMapper.selectById(3L)).thenReturn(d);

        assertThatThrownBy(() -> service.update(d))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(SystemErrorCode.INVALID_PARENT.getCode());
        verify(deptMapper, never()).updateById(any(SysDept.class));
    }

    @Test
    void updateRejectsOwnDescendantAsParent() {
        // 树：1(根) → 2 → 3。把 1 的父节点改为 3（自己的子孙）会成环 → 拒绝
        SysDept root = dept(1L, 0L, "总公司", "0", 1);
        SysDept child = dept(2L, 1L, "研发部", "0,1", 1);
        SysDept grandchild = dept(3L, 2L, "前端组", "0,1,2", 1);
        when(deptMapper.selectById(1L)).thenReturn(root);
        when(deptMapper.selectList(null)).thenReturn(List.of(root, child, grandchild));
        SysDept d = dept(1L, 3L, "总公司", "0", 1);

        assertThatThrownBy(() -> service.update(d))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(SystemErrorCode.INVALID_PARENT.getCode());
        verify(deptMapper, never()).updateById(any(SysDept.class));
    }

    @Test
    void updateThrowsWhenDeptMissing() {
        SysDept d = dept(42L, 0L, "不存在", "0", 1);
        when(deptMapper.selectById(42L)).thenReturn(null);

        assertThatThrownBy(() -> service.update(d))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(SystemErrorCode.RESOURCE_NOT_FOUND.getCode());
    }

    @Test
    void updateRefreshesAncestorsAndReturnsNewValue() {
        SysDept exist = dept(2L, 1L, "研发部", "0,1", 1);
        SysDept updated = dept(2L, 5L, "研发部", "0,5", 1);
        SysDept parent = dept(5L, 0L, "新公司", "0", 1);
        when(deptMapper.selectById(2L)).thenReturn(exist);
        when(deptMapper.selectById(5L)).thenReturn(parent);
        when(deptMapper.updateById(updated)).thenReturn(1);
        when(deptMapper.selectById(2L)).thenReturn(exist, updated);
        // 级联更新查询子节点：无子节点
        when(deptMapper.selectList(any())).thenReturn(List.of());

        SysDept result = service.update(updated);

        assertThat(result.getAncestors()).isEqualTo("0,5");
    }

    @Test
    void updateCascadesAncestorsToDescendants() {
        // 树：1(根) → 2 → 3。把 2 从 1 下移到 9 下，3 的 ancestors 应级联更新为 0,9,2
        SysDept exist = dept(2L, 1L, "研发部", "0,1", 1);
        SysDept updated = dept(2L, 9L, "研发部", "0,9", 1);
        SysDept newParent = dept(9L, 0L, "新公司", "0", 1);
        SysDept grandchild = dept(3L, 2L, "前端组", "0,1,2", 1);
        when(deptMapper.selectById(2L)).thenReturn(exist);
        when(deptMapper.selectById(9L)).thenReturn(newParent);
        when(deptMapper.updateById(any(SysDept.class))).thenReturn(1);
        when(deptMapper.selectById(2L)).thenReturn(exist, updated);
        // selectList 调用顺序：isDescendant(2,9)→[3]（9 不在 2 子孙中）；cascade 查 2 子节点→[3]；cascade 查 3 子节点→[]
        when(deptMapper.selectList(any()))
                .thenReturn(List.of(grandchild))
                .thenReturn(List.of(grandchild))
                .thenReturn(List.of());

        service.update(updated);

        // 子节点 3 的 ancestors 被级联为 "0,9,2"
        ArgumentCaptor<SysDept> captor = ArgumentCaptor.forClass(SysDept.class);
        verify(deptMapper, atLeastOnce()).updateById(captor.capture());
        assertThat(captor.getAllValues())
                .anyMatch(
                        p ->
                                p.getId() != null
                                        && p.getId() == 3L
                                        && "0,9,2".equals(p.getAncestors()));
    }

    @Test
    void deleteRejectsWhenChildrenExist() {
        when(deptMapper.selectCount(any())).thenReturn(3L);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(SystemErrorCode.DELETE_WITH_CHILD_DEPT.getCode());
        verify(deptMapper, never()).deleteById(any(Long.class));
    }

    @Test
    void deleteRemovesLeafDept() {
        when(deptMapper.selectCount(any())).thenReturn(0L);

        service.delete(1L);

        verify(deptMapper).deleteById(1L);
    }
}
