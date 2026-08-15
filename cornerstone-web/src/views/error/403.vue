<template>
  <div class="error-page">
    <div class="error-code">403</div>
    <div class="error-desc">抱歉，您没有权限访问该页面</div>
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
  background: #f0f2f5;
}
.error-code {
  font-size: 120px;
  font-weight: 700;
  color: #409eff;
  line-height: 1;
}
.error-desc {
  margin: 16px 0 24px;
  color: #606266;
  font-size: 16px;
}
</style>
