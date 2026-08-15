<template>
  <el-container class="layout">
    <!-- 侧边菜单：由路由表驱动 -->
    <el-aside width="220px" class="layout-aside">
      <div class="logo">
        <el-icon :size="22"><Platform /></el-icon>
        <span>Cornerstone</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        class="layout-menu"
        background-color="#001529"
        text-color="rgba(255,255,255,0.68)"
        active-text-color="#ffffff"
      >
        <template v-for="group in menuGroups" :key="group.name || group.items[0]?.path">
          <!-- 分组菜单（如"系统管理"） -->
          <el-sub-menu v-if="group.name" :index="group.name">
            <template #title>
              <el-icon><Folder /></el-icon>
              <span>{{ group.name }}</span>
            </template>
            <el-menu-item
              v-for="item in group.items"
              :key="item.path"
              :index="`/${item.path}`"
            >
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.title }}</span>
            </el-menu-item>
          </el-sub-menu>
          <!-- 未分组菜单项 -->
          <template v-else>
            <el-menu-item
              v-for="item in group.items"
              :key="item.path"
              :index="`/${item.path}`"
            >
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.title }}</span>
            </el-menu-item>
          </template>
        </template>
      </el-menu>
    </el-aside>

    <el-container>
      <!-- 顶栏：当前页标题 + 用户下拉 -->
      <el-header class="layout-header">
        <div class="page-title">{{ currentTitle }}</div>
        <el-dropdown trigger="click" @command="onCommand">
          <div class="user-info">
            <el-icon :size="18"><UserFilled /></el-icon>
            <span>{{ userStore.user?.username || '未登录' }}</span>
            <el-icon :size="12"><ArrowDown /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="dashboard">首页</el-dropdown-item>
              <el-dropdown-item command="password">修改密码</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <!-- 修改密码弹窗 -->
      <el-dialog v-model="pwdVisible" title="修改密码" width="420px" destroy-on-close>
        <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="90px">
          <el-form-item label="旧密码" prop="oldPassword">
            <el-input v-model="pwdForm.oldPassword" type="password" show-password />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input v-model="pwdForm.newPassword" type="password" show-password />
          </el-form-item>
          <el-form-item label="确认新密码" prop="confirmPassword">
            <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="pwdVisible = false">取消</el-button>
          <el-button type="primary" :loading="pwdSaving" @click="submitPassword">确定</el-button>
        </template>
      </el-dialog>

      <!-- 主内容区 -->
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { routes } from '@/router'
import { hasPermission } from '@/utils/permission'
import { updatePassword } from '@/api/system'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 从路由表扁平化用于菜单展示并按权限过滤；按 meta.group 分组（有分组归 el-sub-menu，无分组平级展示）。
const menuRoutes = computed(() => {
  const root = routes.find((r) => r.path === '/')
  return (root?.children || [])
    .filter((c) => {
      const required = c.meta?.permission as string | undefined
      return !required || hasPermission(required)
    })
    .map((c) => ({
      path: c.path,
      title: String(c.meta?.title || ''),
      icon: String(c.meta?.icon || 'Document'),
      group: String(c.meta?.group || ''),
    }))
})

const menuGroups = computed(() => {
  const groups: { name: string; items: typeof menuRoutes.value }[] = []
  for (const item of menuRoutes.value) {
    const existing = groups.find((g) => g.name === item.group)
    if (existing) {
      existing.items.push(item)
    } else {
      groups.push({ name: item.group, items: [item] })
    }
  }
  return groups
})

// 当前高亮菜单 = 当前路由路径；标题取路由 meta.title
const activeMenu = computed(() => route.path)
const currentTitle = computed(() => String(route.meta?.title || ''))

async function onCommand(command: string) {
  if (command === 'logout') {
    await userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  } else if (command === 'dashboard') {
    router.push('/dashboard')
  } else if (command === 'password') {
    openPasswordDialog()
  }
}

// ---------------- 修改密码 ----------------
const pwdVisible = ref(false)
const pwdSaving = ref(false)
const pwdFormRef = ref<FormInstance>()
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pwdRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [{ required: true, min: 6, message: '新密码至少 6 位', trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_r, v, cb) =>
        v === pwdForm.newPassword ? cb() : cb(new Error('两次输入的新密码不一致')),
      trigger: 'blur',
    },
  ],
}

function openPasswordDialog() {
  pwdForm.oldPassword = ''
  pwdForm.newPassword = ''
  pwdForm.confirmPassword = ''
  pwdVisible.value = true
}

async function submitPassword() {
  await pwdFormRef.value?.validate()
  pwdSaving.value = true
  try {
    await updatePassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
    ElMessage.success('密码修改成功，请重新登录')
    pwdVisible.value = false
    await userStore.logout()
    router.push('/login')
  } finally {
    pwdSaving.value = false
  }
}
</script>

<style scoped>
.layout {
  height: 100%;
}
.layout-aside {
  background-color: #001529;
  display: flex;
  flex-direction: column;
}
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #fff;
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 1px;
}
.layout-menu {
  border-right: none;
  flex: 1;
}
.layout-header {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
}
.page-title {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}
.user-info {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: #303133;
}
.layout-main {
  background: #f0f2f5;
  padding: 16px;
}
</style>
