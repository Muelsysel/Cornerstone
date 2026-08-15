<template>
  <div>
    <!-- 搜索栏 -->
    <el-card class="toolbar" shadow="never">
      <el-form inline @submit.prevent>
        <el-form-item label="角色名称">
          <el-input v-model="query.roleName" placeholder="请输入角色名称" clearable />
        </el-form-item>
        <el-form-item label="权限字符">
          <el-input v-model="query.roleKey" placeholder="请输入权限字符" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never">
      <div class="table-toolbar">
        <el-button v-permission="'system:role:add'" type="primary" :icon="Plus" @click="handleCreate">
          新增角色
        </el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="roleId" label="ID" width="70" />
        <el-table-column prop="roleName" label="角色名称" min-width="130" />
        <el-table-column prop="roleKey" label="权限字符" min-width="140" />
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="数据范围" min-width="110">
          <template #default="{ row }: { row: any }">
            {{ dataScopeText(row.dataScope) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }: { row: any }">
            <el-tag :type="row.status === '0' ? 'success' : 'info'">
              {{ row.status === '0' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="170" />
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }: { row: any }">
            <el-button v-permission="'system:role:edit'" link type="primary" @click="handleAssign(row)">
              分配权限
            </el-button>
            <el-button v-permission="'system:role:edit'" link type="primary" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button v-permission="'system:role:remove'" link type="danger" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        class="pagination"
        @size-change="handleSizeChange"
        @current-change="loadData"
      />
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑角色' : '新增角色'"
      width="500px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" maxlength="30" placeholder="如：系统管理员" />
        </el-form-item>
        <el-form-item label="权限字符" prop="roleKey">
          <el-input v-model="form.roleKey" maxlength="50" placeholder="如：admin" />
        </el-form-item>
        <el-form-item label="显示顺序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="0">启用</el-radio>
            <el-radio value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="数据范围">
          <el-radio-group v-model="form.dataScope">
            <el-radio value="1">全部数据</el-radio>
            <el-radio value="2">自定义</el-radio>
            <el-radio value="3">本部门及以下</el-radio>
            <el-radio value="4">本部门</el-radio>
            <el-radio value="5">仅本人</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.dataScope === '2'" label="自定义部门">
          <el-button :icon="FolderOpened" @click="openDeptPicker">
            选择部门（{{ form.deptIds?.length || 0 }} 个）
          </el-button>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配权限弹窗：菜单树勾选 -->
    <el-dialog v-model="assignVisible" title="分配菜单权限" width="480px" destroy-on-close>
      <el-tree
        ref="menuTreeRef"
        v-loading="treeLoading"
        :data="menuTree"
        node-key="menuId"
        show-checkbox
        default-expand-all
        :props="{ label: 'menuName', children: 'children' }"
        class="menu-tree"
      />
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="assigning" @click="submitAssign">确定</el-button>
      </template>
    </el-dialog>

    <!-- 自定义数据范围部门选择弹窗 -->
    <el-dialog v-model="deptPickerVisible" title="选择自定义部门" width="420px" destroy-on-close>
      <el-tree
        ref="deptTreeRef"
        v-loading="deptTreeLoading"
        :data="deptTree"
        node-key="deptId"
        show-checkbox
        default-expand-all
        :props="{ label: 'deptName', children: 'children' }"
      />
      <template #footer>
        <el-button @click="deptPickerVisible = false">取消</el-button>
        <el-button type="primary" @click="submitDeptPicker">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { FolderOpened, Plus, Refresh, Search } from '@element-plus/icons-vue'
import type { ElTree } from 'element-plus'
import {
  assignRoleMenus,
  createRole,
  deleteRole,
  getDeptTree,
  getMenuTree,
  getRoleDepts,
  getRoleMenus,
  getRolePage,
  updateRole,
} from '@/api/system'
import type { Dept, Menu, Role, RoleQuery } from '@/types/system'
import { pageNumAfterDelete } from '@/utils/pagination'

interface RoleForm {
  roleId?: number
  roleName: string
  roleKey: string
  sort: number
  status: string
  remark?: string
  dataScope: string
  deptIds?: number[]
}

const loading = ref(false)
const submitting = ref(false)
const list = ref<Role[]>([])
const total = ref(0)

const query = reactive<RoleQuery>({ pageNum: 1, pageSize: 10 })

/** 每页条数变化时回到第一页（避免停留在越界页码）。 */
function handleSizeChange() {
  query.pageNum = 1
  loadData()
}
async function loadData() {
  loading.value = true
  try {
    const res = await getRolePage(query)
    list.value = res.records || []
    total.value = res.total || 0
  } catch {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  loadData()
}

function handleReset() {
  query.roleName = undefined
  query.roleKey = undefined
  query.pageNum = 1
  loadData()
}

// ---------------- 新增 / 编辑 ----------------
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<RoleForm>({ roleName: '', roleKey: '', sort: 0, status: '0', dataScope: '1' })

/** 数据范围展示映射：1全部 2自定义 3本部门及以下 4本部门 5仅本人 */
const DATA_SCOPE_TEXT: Record<string, string> = {
  '1': '全部数据',
  '2': '自定义',
  '3': '本部门及以下',
  '4': '本部门',
  '5': '仅本人',
}
function dataScopeText(scope: string | undefined): string {
  return scope ? DATA_SCOPE_TEXT[scope] || scope : '—'
}

const rules: FormRules<RoleForm> = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleKey: [{ required: true, message: '请输入权限字符', trigger: 'blur' }],
}

function handleCreate() {
  isEdit.value = false
  Object.assign(form, {
    roleId: undefined,
    roleName: '',
    roleKey: '',
    sort: 0,
    status: '0',
    remark: '',
    dataScope: '1',
    deptIds: [],
  })
  dialogVisible.value = true
}

async function handleEdit(row: Role) {
  isEdit.value = true
  Object.assign(form, {
    roleId: row.roleId,
    roleName: row.roleName,
    roleKey: row.roleKey,
    sort: row.sort ?? 0,
    status: row.status || '0',
    remark: row.remark || '',
    dataScope: row.dataScope || '1',
    deptIds: [],
  })
  dialogVisible.value = true
  // 数据范围=自定义(2) 时回显已选部门
  if (form.dataScope === '2' && row.roleId != null) {
    try {
      form.deptIds = (await getRoleDepts(row.roleId)) || []
    } catch {
      form.deptIds = []
    }
  }
}

// ---------------- 自定义数据范围部门选择 ----------------
const deptPickerVisible = ref(false)
const deptTreeLoading = ref(false)
const deptTree = ref<Dept[]>([])
const deptTreeRef = ref<InstanceType<typeof ElTree>>()

async function openDeptPicker() {
  deptPickerVisible.value = true
  deptTreeLoading.value = true
  try {
    deptTree.value = (await getDeptTree()) || []
    await nextTick(() => {
      deptTreeRef.value?.setCheckedKeys(form.deptIds || [])
    })
  } catch {
    deptTree.value = []
  } finally {
    deptTreeLoading.value = false
  }
}

function submitDeptPicker() {
  const checked = (deptTreeRef.value?.getCheckedKeys(false) as number[]) || []
  const half = (deptTreeRef.value?.getHalfCheckedKeys() as number[]) || []
  // 自定义范围包含父级与子级（半选父级也算），与后端数据权限语义一致
  form.deptIds = [...checked, ...half]
  deptPickerVisible.value = false
}

async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateRole(form)
      ElMessage.success('编辑成功')
    } else {
      await createRole(form)
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

// ---------------- 分配权限 ----------------
const assignVisible = ref(false)
const assigning = ref(false)
const treeLoading = ref(false)
const menuTreeRef = ref<InstanceType<typeof ElTree>>()
const menuTree = ref<Menu[]>([])
let assignRoleId = 0

async function handleAssign(row: Role) {
  assignRoleId = row.roleId
  assignVisible.value = true
  treeLoading.value = true
  try {
    menuTree.value = (await getMenuTree()) || []
    const { menuIds } = await getRoleMenus(row.roleId)
    // 等待树渲染后回显勾选（仅叶子/半选由 setCheckedKeys 处理）
    await nextTick(() => {
      menuTreeRef.value?.setCheckedKeys(menuIds || [])
    })
  } catch {
    menuTree.value = []
  } finally {
    treeLoading.value = false
  }
}

async function submitAssign() {
  const checked = menuTreeRef.value?.getCheckedKeys(false) as number[]
  const half = menuTreeRef.value?.getHalfCheckedKeys() as number[]
  assigning.value = true
  try {
    await assignRoleMenus(assignRoleId, [...checked, ...half])
    ElMessage.success('权限分配成功')
    assignVisible.value = false
  } catch {
    // 错误已由拦截器提示
  } finally {
    assigning.value = false
  }
}

// ---------------- 删除 ----------------
async function handleDelete(row: Role) {
  await ElMessageBox.confirm(`确认删除角色「${row.roleName}」吗？`, '提示', { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' })
    .catch(() => Promise.reject(new Error('canceled')))
  try {
    await deleteRole(row.roleId)
    ElMessage.success('删除成功')
    // 删除当前页最后一条时回退一页，避免停留在空页
    query.pageNum = pageNumAfterDelete(query.pageNum, list.value.length)
    loadData()
  } catch (e) {
    if (e instanceof Error && e.message === 'canceled') return
    // 其它错误已由拦截器提示
  }
}

onMounted(loadData)
</script>

<style scoped>
.toolbar {
  margin-bottom: 16px;
}
.table-toolbar {
  margin-bottom: 12px;
}
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
