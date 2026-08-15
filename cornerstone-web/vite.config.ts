import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

// Vite 配置：别名 @ -> src，开发代理转发到网关 8080
// 开发时前端访问 /auth、/system、/demo 前缀的请求都会被代理到本地网关，避免浏览器 CORS。
// 生产环境应在前端服务器（Nginx 等）配置同级反向代理，前端代码无需改动。
//
// Element Plus 按需引入（unplugin-auto-import + unplugin-vue-components）：
// 组件与 API 的样式按需打包，产物体积从「全量引入」显著下降，提升首屏性能。
// 自动生成的类型声明（src/auto-imports.d.ts、src/components.d.ts）已纳入 tsconfig，
// 供 vue-tsc 校验，需随源码一起提交。
export default defineConfig({
  plugins: [
    vue(),
    AutoImport({
      resolvers: [ElementPlusResolver()],
      dts: 'src/auto-imports.d.ts',
    }),
    Components({
      resolvers: [ElementPlusResolver()],
      dts: 'src/components.d.ts',
    }),
  ],
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
