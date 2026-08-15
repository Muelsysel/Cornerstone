<template>
  <!-- el-config-provider：按需引入模式下由它统一注入 zh-cn 语言包（日期、分页等组件文案） -->
  <el-config-provider :locale="zhCn">
    <router-view />
  </el-config-provider>
</template>

<script setup lang="ts">
import zhCn from 'element-plus/es/locale/lang/zh-cn'
</script>

<style>
html,
body,
#app {
  height: 100%;
  margin: 0;
}
</style>
