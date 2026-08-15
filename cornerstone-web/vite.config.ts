import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// Vite 配置：别名 @ -> src，开发代理转发到网关 8080
// 开发时前端访问 /auth、/system、/demo 前缀的请求都会被代理到本地网关，避免浏览器 CORS。
// 生产环境应在前端服务器（Nginx 等）配置同级反向代理，前端代码无需改动。
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    host: '0.0.0.0',
    port: 5173,
    proxy: {
      '/auth': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/system': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/demo': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
