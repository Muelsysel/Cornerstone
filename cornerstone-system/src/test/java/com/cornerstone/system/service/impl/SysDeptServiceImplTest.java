package com.cornerstone.system.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
        verify(deptMapper, never()).insert(any());
    }

    @Test
    void updateRejectsSelfAsParent() {
        SysDept d = dept(3L, 3L, "把自己挂自己", "0", 1);
        when(deptMapper.selectById(3L)).thenReturn(d);

        assertThatThrownBy(() -> service.update(d))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(SystemErrorCode.INVALID_PARENT.getCode());
        verify(deptMapper, never()).updateById(any());
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

        SysDept result = service.update(updated);

        assertThat(result.getAncestors()).isEqualTo("0,5");
    }

    @Test
    void deleteRejectsWhenChildrenExist() {
        when(deptMapper.selectCount(any())).thenReturn(3L);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(SystemErrorCode.DELETE_WITH_CHILD_DEPT.getCode());
        verify(deptMapper, never()).deleteById(any());
    }

    @Test
    void deleteRemovesLeafDept() {
        when(deptMapper.selectCount(any())).thenReturn(0L);

        service.delete(1L);

        verify(deptMapper).deleteById(1L);
    }
}
