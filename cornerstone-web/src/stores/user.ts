import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { login as apiLogin, logout as apiLogout } from '@/api/auth'
import { getToken, setToken, setUser, clearAuth, getUser } from '@/utils/auth'
import type { UserInfo } from '@/utils/auth'
import type { LoginForm } from '@/types/auth'

// 用户会话 store：管理登录令牌、用户信息与登录/退出动作。
// 令牌与用户信息同时持久化到 localStorage，刷新页面后仍保持登录态。
export const useUserStore = defineStore('user', () => {
  const token = ref<string>(getToken())
  const user = ref<UserInfo | null>(getUser())

  /** 是否已登录。 */
  const isLoggedIn = computed(() => !!token.value)

  async function login(form: LoginForm) {
    const res = await apiLogin(form)
    setToken(res.access_token)
    token.value = res.access_token
    const info: UserInfo = {
      userId: res.userId,
      username: res.username,
      roles: res.roles || [],
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

  return { token, user, isLoggedIn, login, logout }
})
