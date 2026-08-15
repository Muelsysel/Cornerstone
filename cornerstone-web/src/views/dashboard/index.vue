<template>
  <div>
    <el-card>
      <el-result icon="success" title="登录成功" sub-title="欢迎使用 Cornerstone 管理后台">
        <template #extra>
          <p class="desc">
            当前用户：{{ userStore.user?.username }}（{{ rolesText }}）
          </p>
          <p class="desc">
            从左侧菜单进入各管理模块。系统管理接口需后端授权（角色权限点），
            权限不足时页面正常渲染、操作会返回权限错误提示。
          </p>
        </template>
      </el-result>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const rolesText = computed(() => {
  const roles = userStore.user?.roles || []
  return roles.length ? roles.join(' / ') : '无角色信息'
})
</script>

<style scoped>
.desc {
  color: #606266;
  font-size: 14px;
  line-height: 1.8;
}
</style>
