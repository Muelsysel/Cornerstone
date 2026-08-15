import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import 'element-plus/dist/index.css'

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
app.use(ElementPlus, { locale: zhCn })

// 注册自定义指令（v-permission 按钮级权限控制）
setupPermissionDirective(app)

app.mount('#app')
