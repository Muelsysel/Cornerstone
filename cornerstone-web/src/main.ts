import { createApp } from 'vue'
import { createPinia } from 'pinia'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
// Element Plus 按需引入（见 vite.config.ts）：组件样式随组件自动加载；
// 函数式组件（消息/弹窗/通知/加载）在模板中无引用，需手动引入其样式。
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'
import 'element-plus/es/components/notification/style/css'
import 'element-plus/es/components/loading/style/css'

import '@/styles/theme.css'

import App from '@/App.vue'
import router from '@/router'
import { setupPermissionDirective } from '@/directives/permission'

const app = createApp(App)

// 注册 Element Plus 全部图标为全局组件（@element-plus/icons-vue）
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(router)

// 注册自定义指令（v-permission 按钮级权限控制）
setupPermissionDirective(app)

app.mount('#app')
