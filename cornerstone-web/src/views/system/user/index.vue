<template>
  <div>
    <!-- 搜索栏 -->
    <el-card class="toolbar" shadow="never">
      <el-form inline @submit.prevent>
        <el-form-item label="用户名">
          <el-input v-model="query.username" placeholder="请输入用户名" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="启用" value="0" />
            <el-option label="停用" value="1" />
          </el-select>
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
        <el-button v-permission="'system:user:add'" type="primary" :icon="Plus" @click="handleCreate">
          新增用户
        </el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="userId" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" min-width="110" />
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column prop="phone" label="手机号" min-width="130" />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }: { row: any }">
            <el-switch
              :model-value="row.status === '0'"
              :loading="statusLoadingId === row.userId"
              @change="(val: string | number | boolean) => handleStatusChange(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }: { row: any }">
            <el-button v-permission="'system:user:edit'" link type="primary" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button v-permission="'system:user:edit'" link type="primary" @click="handleAssignRoles(row)">
              分配角色
            </el-button>
            <el-button v-permission="'system:user:edit'" link type="warning" @click="handleResetPassword(row)">
              重置密码
            </el-button>
            <el-button v-permission="'system:user:remove'" link type="danger" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
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

    <!-- 分配角色弹窗 -->
    <el-dialog v-model="rolesVisible" title="分配角色" width="420px" destroy-on-close>
      <el-select
        v-model="selectedRoleIds"
        v-loading="rolesLoading"
        multiple
        collapse-tags
        collapse-tags-tooltip
        filterable
        placeholder="请选择角色（可多选，全量覆盖）"
        style="width: 100%"
      >
        <el-option v-for="role in roleList" :key="role.roleId" :label="role.roleName" :value="role.roleId" />
      </el-select>
      <template #footer>
        <el-button @click="rolesVisible = false">取消</el-button>
        <el-button type="primary" :loading="rolesSaving" @click="submitAssignRoles">确定</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码弹窗 -->
    <el-dialog v-model="pwdResetVisible" :title="`重置密码 - ${pwdResetUser || ''}`" width="400px" destroy-on-close>
      <el-form ref="pwdResetFormRef" :model="pwdResetForm" :rules="pwdResetRules" label-width="80px">
        <el-form-item label="新密码" prop="password">
          <el-input v-model="pwdResetForm.password" type="password" show-password placeholder="至少 6 位" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdResetVisible = false">取消</el-button>
        <el-button type="primary" :loading="pwdResetting" @click="submitResetPassword">确定</el-button>
      </template>
    </el-dialog>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑用户' : '新增用户'"
      width="500px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="isEdit" maxlength="30" placeholder="登录账号" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" maxlength="30" placeholder="显示昵称" />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="初始密码" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="手机号" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="邮箱" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="0">启用</el-radio>
            <el-radio value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注" />
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
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  assignUserRoles,
  changeUserStatus,
  createUser,
  deleteUser,
  getRoleList,
  getUserPage,
  getUserRoleIds,
  resetUserPassword,
  updateUser,
} from '@/api/system'
import type { Role, User, UserQuery } from '@/types/system'
import { pageNumAfterDelete } from '@/utils/pagination'

interface UserForm {
  userId?: number
  username: string
  password?: string
  nickname?: string
  phone?: string
  email?: string
  status: string
  remark?: string
}

const loading = ref(false)
const submitting = ref(false)
const statusLoadingId = ref<number | null>(null)
const list = ref<User[]>([])
const total = ref(0)

const query = reactive<UserQuery>({
  pageNum: 1,
  pageSize: 10,
})

/** 每页条数变化时回到第一页（避免停留在越界页码）。 */
function handleSizeChange() {
  query.pageNum = 1
  loadData()
}
async function loadData() {
  loading.value = true
  try {
    const res = await getUserPage(query)
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
  query.username = undefined
  query.status = undefined
  query.pageNum = 1
  loadData()
}

// ---------------- 新增 / 编辑 ----------------
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<UserForm>({
  username: '',
  status: '0',
})

const rules: FormRules<UserForm> = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入初始密码', trigger: 'blur' },
    { min: 6, max: 72, message: '密码长度需在 6-72 位之间', trigger: 'blur' },
  ],
}

function handleCreate() {
  isEdit.value = false
  Object.assign(form, { userId: undefined, username: '', password: '', nickname: '', phone: '', email: '', status: '0', remark: '' })
  dialogVisible.value = true
}

function handleEdit(row: User) {
  isEdit.value = true
  Object.assign(form, {
    userId: row.userId,
    username: row.username,
    password: '',
    nickname: row.nickname || '',
    phone: row.phone || '',
    email: row.email || '',
    status: row.status || '0',
    remark: row.remark || '',
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
      await updateUser({ ...form, password: undefined })
      ElMessage.success('编辑成功')
    } else {
      await createUser(form)
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

// ---------------- 分配角色 ----------------
const rolesVisible = ref(false)
const rolesLoading = ref(false)
const rolesSaving = ref(false)
const roleList = ref<Role[]>([])
const selectedRoleIds = ref<number[]>([])
let assignUserId = 0

async function handleAssignRoles(row: User) {
  assignUserId = row.userId
  rolesVisible.value = true
  rolesLoading.value = true
  selectedRoleIds.value = []
  try {
    roleList.value = (await getRoleList()) || []
    const { roleIds } = await getUserRoleIds(row.userId)
    selectedRoleIds.value = roleIds || []
  } catch {
    roleList.value = []
  } finally {
    rolesLoading.value = false
  }
}

async function submitAssignRoles() {
  rolesSaving.value = true
  try {
    await assignUserRoles(assignUserId, selectedRoleIds.value)
    ElMessage.success('角色分配成功')
    rolesVisible.value = false
    loadData()
  } catch {
    // 错误提示已由请求拦截器统一处理
  } finally {
    rolesSaving.value = false
  }
}

// ---------------- 重置密码 ----------------
const pwdResetVisible = ref(false)
const pwdResetting = ref(false)
const pwdResetUser = ref('')
const pwdResetUserId = ref(0)
const pwdResetFormRef = ref<FormInstance>()
const pwdResetForm = reactive({ password: '' })
const pwdResetRules: FormRules = {
  password: [
    { required: true, min: 6, max: 72, message: '新密码长度需在 6-72 位之间', trigger: 'blur' },
  ],
}

function handleResetPassword(row: User) {
  pwdResetUser.value = row.username
  pwdResetUserId.value = row.userId
  pwdResetForm.password = ''
  pwdResetVisible.value = true
}

async function submitResetPassword() {
  if (!pwdResetFormRef.value) return
  const valid = await pwdResetFormRef.value.validate().catch(() => false)
  if (!valid) return
  pwdResetting.value = true
  try {
    await resetUserPassword(pwdResetUserId.value, pwdResetForm.password)
    ElMessage.success('密码已重置')
    pwdResetVisible.value = false
  } catch {
    // 错误提示已由请求拦截器统一处理
  } finally {
    pwdResetting.value = false
  }
}

// ---------------- 状态切换 ----------------
async function handleStatusChange(row: User, val: string | number | boolean) {
  const action = val ? '启用' : '停用'
  await ElMessageBox.confirm(`确认${action}用户「${row.username}」吗？`, '提示', { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' })
    .catch(() => Promise.reject(new Error('canceled')))
  statusLoadingId.value = row.userId
  try {
    await changeUserStatus(row.userId, val ? '0' : '1')
    row.status = val ? '0' : '1'
    ElMessage.success(`已${action}`)
  } catch (e) {
    // 失败时还原开关状态；取消则直接忽略
    if (e instanceof Error && e.message !== 'canceled') {
      row.status = val ? '1' : '0'
    }
  } finally {
    statusLoadingId.value = null
  }
}

// ---------------- 删除 ----------------
async function handleDelete(row: User) {
  await ElMessageBox.confirm(`确认删除用户「${row.username}」吗？`, '提示', { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' })
    .catch(() => Promise.reject(new Error('canceled')))
  try {
    await deleteUser(row.userId)
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
