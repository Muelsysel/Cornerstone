<template>
  <div class="dashboard">
    <!-- 欢迎卡片 -->
    <el-card shadow="never" class="welcome">
      <div class="welcome-row">
        <div>
          <h2 class="hello">你好，{{ userStore.user?.username || '管理员' }} 👋</h2>
          <p class="desc">
            {{ today }}。当前角色：{{ rolesText }}；权限点数量：{{ permissionsCount }}。
          </p>
        </div>
      </div>
    </el-card>

    <!-- 能力卡片 -->
    <el-row :gutter="16" class="cards">
      <el-col :span="6" v-for="card in cards" :key="card.title">
        <el-card shadow="hover" class="card">
          <el-icon :size="28" class="card-icon"><component :is="card.icon" /></el-icon>
          <div class="card-title">{{ card.title }}</div>
          <div class="card-desc">{{ card.desc }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷入口 -->
    <el-card shadow="never" class="quick">
      <template #header>快捷入口</template>
      <el-space wrap>
        <el-button v-permission="'system:user:list'" @click="go('/system/user')">用户管理</el-button>
        <el-button v-permission="'system:role:list'" @click="go('/system/role')">角色管理</el-button>
        <el-button v-permission="'system:dict:list'" @click="go('/system/dict')">字典管理</el-button>
        <el-button @click="go('/demo/announcement')">公告管理</el-button>
      </el-space>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const rolesText = computed(() => {
  const roles = userStore.user?.roles || []
  return roles.length ? roles.join(' / ') : '无角色信息'
})

const permissionsCount = computed(() => userStore.permissions?.length || 0)

// 当前日期（中文长格式），欢迎语氛围；computed 保证跨夜刷新
const today = computed(() =>
  new Date().toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    weekday: 'long',
  }),
)

// 能力卡片（静态展示项目能力，不依赖统计接口——克制不造新后端接口）
const cards = [
  { title: '统一认证', desc: 'OAuth2 + 用户名密码登录，JWT 携带角色与权限', icon: 'Lock' },
  { title: 'RBAC 权限', desc: '用户/角色/菜单/部门，按钮级 v-permission 控制', icon: 'Key' },
  { title: '数据权限', desc: '角色数据范围（全部/自定义/部门/本人），SQL 层自动过滤', icon: 'Filter' },
  { title: '审计日志', desc: '操作日志 + 登录日志，AOP 自动记录', icon: 'Document' },
]

function go(path: string) {
  router.push(path)
}
</script>

<style scoped>
.welcome {
  margin-bottom: 16px;
}
.hello {
  margin: 0 0 8px;
}
.desc {
  color: var(--cs-text-secondary);
  font-size: 14px;
  margin: 0;
}
.cards {
  margin-bottom: 16px;
}
.card {
  text-align: center;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}
.card:hover {
  transform: translateY(-2px);
}
.card-icon {
  color: var(--cs-primary);
  margin-bottom: 8px;
}
.card-title {
  font-weight: 600;
  margin-bottom: 4px;
}
.card-desc {
  color: var(--cs-text-secondary);
  font-size: 12px;
  line-height: 1.6;
}
</style>
