import type { App, Directive, DirectiveBinding } from 'vue'
import { hasPermission } from '@/utils/permission'

/**
 * v-permission 指令：无权限时移除元素，实现按钮级权限控制。
 * 用法：<el-button v-permission="'system:user:add'">新增</el-button>
 * 支持字符串权限点（如 'system:user:add'）或多个权限点组成的数组（任一命中即保留）。
 */
function checkPermission(el: HTMLElement, binding: DirectiveBinding<string | string[]>) {
  const value = binding.value
  if (!value) return
  const required = Array.isArray(value) ? value : [value]
  const authorized = required.some((perm) => hasPermission(perm))
  if (!authorized) {
    el.parentNode?.removeChild(el)
  }
}

const permission: Directive<HTMLElement, string | string[]> = {
  mounted(el, binding) {
    checkPermission(el, binding)
  },
  updated(el, binding) {
    // 权限变化时重新判定（例如切换到其它账号的会话）
    checkPermission(el, binding)
  },
}

/** 注册 v-permission 指令到应用实例。 */
export function setupPermissionDirective(app: App) {
  app.directive('permission', permission)
}

export default permission
