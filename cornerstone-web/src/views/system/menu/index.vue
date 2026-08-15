<template>
  <div>
    <el-card shadow="never">
      <div class="table-toolbar">
        <el-button v-permission="'system:menu:add'" type="primary" :icon="Plus" @click="handleCreate(null)">
          新增菜单
        </el-button>
        <el-button :icon="Refresh" @click="loadData">刷新</el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="tree"
        row-key="menuId"
        border
        default-expand-all
        :tree-props="{ children: 'children' }"
      >
        <el-table-column prop="menuName" label="菜单名称" min-width="180" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }: { row: any }">
            <el-tag :type="typeTag(row.menuType)" size="small">
              {{ typeText(row.menuType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="icon" label="图标" width="90" />
        <el-table-column prop="path" label="路由路径" min-width="150" />
        <el-table-column prop="perms" label="权限标识" min-width="150" />
        <el-table-column prop="sort" label="排序" width="70" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }: { row: any }">
            <el-button v-permission="'system:menu:add'" link type="primary" @click="handleCreate(row)">新增</el-button>
            <el-button v-permission="'system:menu:edit'" link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button v-permission="'system:menu:remove'" link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑菜单' : '新增菜单'"
      width="560px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级菜单">
          <el-tree-select
            v-model="form.parentId"
            :data="treeSelectData"
            :props="{ label: 'menuName', children: 'children' }"
            node-key="menuId"
            check-strictly
            clearable
            default-expand-all
            placeholder="不选则为顶级菜单"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="菜单类型" prop="menuType">
          <el-radio-group v-model="form.menuType">
            <el-radio value="M">目录</el-radio>
            <el-radio value="C">菜单</el-radio>
            <el-radio value="F">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="菜单名称" prop="menuName">
          <el-input v-model="form.menuName" placeholder="菜单显示名称" />
        </el-form-item>
        <el-form-item label="路由路径" prop="path">
          <el-input v-model="form.path" placeholder="如：system/user" />
        </el-form-item>
        <el-form-item label="组件路径" prop="component">
          <el-input v-model="form.component" placeholder="如：system/user/index" />
        </el-form-item>
        <el-form-item label="权限标识" prop="perms">
          <el-input v-model="form.perms" placeholder="如：system:user:list" />
        </el-form-item>
        <el-form-item label="图标" prop="icon">
          <el-select v-model="form.icon" filterable clearable placeholder="选择或输入图标名" style="width: 100%">
            <el-option v-for="name in iconOptions" :key="name" :label="name" :value="name">
              <span class="icon-option">
                <el-icon><component :is="name" /></el-icon>
                <span>{{ name }}</span>
              </span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :max="999" />
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
import { createMenu, deleteMenu, getMenuTree, updateMenu } from '@/api/system'
import type { Menu, MenuType } from '@/types/system'

interface MenuForm {
  menuId?: number
  parentId: number | undefined
  menuType: MenuType
  menuName: string
  path?: string
  component?: string
  perms?: string
  icon?: string
  sort: number
}

const loading = ref(false)
const submitting = ref(false)

// 常用菜单图标（@element-plus/icons-vue 全局注册，可搜索选择避免手填出错）
const iconOptions = [
  'HomeFilled',
  'User',
  'Avatar',
  'Menu',
  'OfficeBuilding',
  'Collection',
  'Setting',
  'Document',
  'Finished',
  'Bell',
  'Key',
  'Lock',
  'Filter',
  'Platform',
  'Folder',
  'FolderOpened',
  'Files',
  'DataAnalysis',
  'Monitor',
  'Cpu',
  'Message',
  'Star',
  'Link',
  'CircleCheck',
  'Warning',
]
const tree = ref<Menu[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<MenuForm>({
  parentId: undefined,
  menuType: 'C',
  menuName: '',
  sort: 0,
})

const rules: FormRules<MenuForm> = {
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
}

// 树选择数据：去掉当前编辑节点自身的子树，避免把节点设为自身子级。
const treeSelectData = computed(() => {
  if (!isEdit.value || !form.menuId) return tree.value
  // 深拷贝后从根移除当前节点
  const clone: Menu[] = JSON.parse(JSON.stringify(tree.value || []))
  const filterOut = (nodes: Menu[], id: number): Menu[] =>
    nodes
      .filter((n) => n.menuId !== id)
      .map((n) => ({ ...n, children: n.children ? filterOut(n.children, id) : undefined }))
  return filterOut(clone, form.menuId as number)
})

async function loadData() {
  loading.value = true
  try {
    tree.value = (await getMenuTree()) || []
  } catch {
    tree.value = []
  } finally {
    loading.value = false
  }
}

function handleCreate(parent: Menu | null) {
  isEdit.value = false
  Object.assign(form, {
    menuId: undefined,
    parentId: parent ? parent.menuId : undefined,
    menuType: 'C',
    menuName: '',
    path: '',
    component: '',
    perms: '',
    icon: '',
    sort: 0,
  })
  dialogVisible.value = true
}

function handleEdit(row: Menu) {
  isEdit.value = true
  Object.assign(form, {
    menuId: row.menuId,
    parentId: row.parentId ?? undefined,
    menuType: row.menuType || 'C',
    menuName: row.menuName,
    path: row.path || '',
    component: row.component || '',
    perms: row.perms || '',
    icon: row.icon || '',
    sort: row.sort ?? 0,
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
      await updateMenu(form)
      ElMessage.success('编辑成功')
    } else {
      await createMenu(form)
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

async function handleDelete(row: Menu) {
  await ElMessageBox.confirm(`确认删除菜单「${row.menuName}」吗？`, '提示', { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' })
    .catch(() => Promise.reject(new Error('canceled')))
  try {
    await deleteMenu(row.menuId)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    if (e instanceof Error && e.message === 'canceled') return
  }
}

function typeText(t?: MenuType): string {
  if (t === 'M') return '目录'
  if (t === 'F') return '按钮'
  return '菜单'
}

function typeTag(t?: MenuType): 'primary' | 'success' | 'info' {
  if (t === 'M') return 'primary'
  if (t === 'F') return 'info'
  return 'success'
}

onMounted(loadData)
</script>

<style scoped>
.table-toolbar {
  margin-bottom: 12px;
}
.icon-option {
  display: flex;
  align-items: center;
  gap: 6px;
}
</style>
