<template>
  <div class="error-page">
    <div class="error-code">404</div>
    <div class="error-desc">抱歉，您访问的页面不存在</div>
    <el-button type="primary" @click="goHome">返回首页</el-button>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

function goHome() {
  router.push('/')
}
</script>

<style scoped>
.error-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: var(--cs-content-bg);
}
.error-code {
  font-size: 120px;
  font-weight: 700;
  color: var(--cs-primary);
  line-height: 1;
}
.error-desc {
  margin: 16px 0 24px;
  color: var(--cs-text-secondary);
  font-size: 16px;
}
</style>
