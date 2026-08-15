import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'

// Vitest 单元测试配置：与 vite.config.ts 保持同一 @ 别名。
// 运行：npm test（vitest run，CI 友好）
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  test: {
    // store 初始化会访问 localStorage，需要 DOM 环境
    environment: 'jsdom',
    include: ['src/**/*.spec.ts'],
  },
})
