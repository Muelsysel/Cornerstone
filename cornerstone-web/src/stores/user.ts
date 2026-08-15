import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { login as apiLogin, logout as apiLogout } from '@/api/auth'
import {
  getToken,
  setToken,
  setUser,
  clearAuth,
  getUser,
  getScopesFromToken,
} from '@/utils/auth'
import type { UserInfo } from '@/utils/auth'
import type { LoginForm } from '@/types/auth'

// 用户会话 store：管理登录令牌、用户信息与登录/退出动作。
// 令牌与用户信息同时持久化到 localStorage，刷新页面后仍保持登录态。
export const useUserStore = defineStore('user', () => {
  const token = ref<string>(getToken())
  const user = ref<UserInfo | null>(getUser())

  /** 是否已登录。 */
  const isLoggedIn = computed(() => !!token.value)

  /** 角色列表（如 ["admin"]）。 */
  const roles = computed<string[]>(() => user.value?.roles || [])

  /** 权限点列表（来源：JWT scope；token 无法解析时为登录响应的兜底）。 */
  const permissions = computed<string[]>(() => user.value?.permissions || [])

  async function login(form: LoginForm) {
    const res = await apiLogin(form)
    setToken(res.access_token)
    token.value = res.access_token
    // 权限点优先从 JWT scope 解析；解析不到则回退为空数组
    const scopes = getScopesFromToken(res.access_token)
    const info: UserInfo = {
      userId: res.userId,
      username: res.username,
      roles: res.roles || [],
      permissions: scopes,
    }
    setUser(info)
    user.value = info
  }

  async function logout() {
    try {
      await apiLogout()
    } catch {
      // 后端退出接口失败不阻断本地清理
    } finally {
      clearAuth()
      token.value = ''
      user.value = null
    }
  }

  return { token, user, isLoggedIn, roles, permissions, login, logout }
})
