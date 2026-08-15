<template>
  <div>
    <el-card shadow="never">
      <div class="table-toolbar">
        <el-button v-permission="'system:dept:add'" type="primary" :icon="Plus" @click="handleCreate(null)">
          新增部门
        </el-button>
        <el-button :icon="Refresh" @click="loadData">刷新</el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="tree"
        row-key="deptId"
        border
        default-expand-all
        :tree-props="{ children: 'children' }"
      >
        <el-table-column prop="deptName" label="部门名称" min-width="200" />
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }: { row: any }">
            <el-tag :type="row.status === '0' ? 'success' : 'info'">
              {{ row.status === '0' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="leader" label="负责人" width="120" />
        <el-table-column prop="phone" label="联系电话" min-width="140" />
        <el-table-column prop="createTime" label="创建时间" min-width="170" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }: { row: any }">
            <el-button v-permission="'system:dept:add'" link type="primary" @click="handleCreate(row)">新增</el-button>
            <el-button v-permission="'system:dept:edit'" link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button v-permission="'system:dept:remove'" link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑部门' : '新增部门'"
      width="500px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级部门">
          <el-tree-select
            v-model="form.parentId"
            :data="treeSelectData"
            :props="{ label: 'deptName', children: 'children' }"
            node-key="deptId"
            check-strictly
            clearable
            default-expand-all
            placeholder="不选则为顶级部门"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="部门名称" prop="deptName">
          <el-input v-model="form.deptName" maxlength="50" placeholder="部门名称" />
        </el-form-item>
        <el-form-item label="显示顺序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="负责人" prop="leader">
          <el-input v-model="form.leader" maxlength="30" placeholder="负责人" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="form.phone" maxlength="30" placeholder="联系电话" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="0">启用</el-radio>
            <el-radio value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { createDept, deleteDept, getDeptTree, updateDept } from '@/api/system'
import type { Dept } from '@/types/system'

interface DeptForm {
  deptId?: number
  parentId: number | undefined
  deptName: string
  sort: number
  status: string
  leader?: string
  phone?: string
}

const loading = ref(false)
const submitting = ref(false)
const tree = ref<Dept[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<DeptForm>({
  parentId: undefined,
  deptName: '',
  sort: 0,
  status: '0',
})

const rules: FormRules<DeptForm> = {
  deptName: [{ required: true, message: '请输入部门名称', trigger: 'blur' }],
}

// 上级部门选择：剔除当前编辑节点自身子树。
const treeSelectData = computed(() => {
  if (!isEdit.value || !form.deptId) return tree.value
  const clone: Dept[] = JSON.parse(JSON.stringify(tree.value || []))
  const filterOut = (nodes: Dept[], id: number): Dept[] =>
    nodes
      .filter((n) => n.deptId !== id)
      .map((n) => ({ ...n, children: n.children ? filterOut(n.children, id) : undefined }))
  return filterOut(clone, form.deptId as number)
})

async function loadData() {
  loading.value = true
  try {
    tree.value = (await getDeptTree()) || []
  } catch {
    tree.value = []
  } finally {
    loading.value = false
  }
}

function handleCreate(parent: Dept | null) {
  isEdit.value = false
  Object.assign(form, {
    deptId: undefined,
    parentId: parent ? parent.deptId : undefined,
    deptName: '',
    sort: 0,
    status: '0',
    leader: '',
    phone: '',
  })
  dialogVisible.value = true
}

function handleEdit(row: Dept) {
  isEdit.value = true
  Object.assign(form, {
    deptId: row.deptId,
    parentId: row.parentId ?? undefined,
    deptName: row.deptName,
    sort: row.sort ?? 0,
    status: row.status || '0',
    leader: row.leader || '',
    phone: row.phone || '',
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateDept(form)
      ElMessage.success('编辑成功')
    } else {
      await createDept(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } catch {
    // 错误提示已由请求拦截器统一处理
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: Dept) {
  await ElMessageBox.confirm(`确认删除部门「${row.deptName}」吗？`, '提示', { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' })
    .catch(() => Promise.reject(new Error('canceled')))
  try {
    await deleteDept(row.deptId)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    if (e instanceof Error && e.message === 'canceled') return
  }
}

onMounted(loadData)
</script>

<style scoped>
.table-toolbar {
  margin-bottom: 12px;
}
</style>
