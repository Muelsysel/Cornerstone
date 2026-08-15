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
        <template v-for="item in menuRoutes" :key="item.path">
          <el-menu-item :index="`/${item.path}`">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </el-menu-item>
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
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <!-- 主内容区 -->
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { routes } from '@/router'
import { hasPermission } from '@/utils/permission'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 从路由表扁平化用于菜单展示的一级路由，并按权限过滤：
// 路由声明了 meta.permission 时，无对应权限点的菜单项不渲染；未声明则默认展示。
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
    }))
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
