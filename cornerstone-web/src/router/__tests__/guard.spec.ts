import { describe, it, expect, beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import router from '@/router'
import { useUserStore } from '@/stores/user'
import { TOKEN_KEY } from '@/utils/auth'

// 路由守卫单测：未登录跳登录页、已登录访问登录页回首页、无权限跳 403（权限闭环核心）。

describe('路由守卫', () => {
  beforeEach(async () => {
    setActivePinia(createPinia())
    localStorage.clear()
    // 重置到首页（/ → redirect /dashboard）
    await router.replace('/')
    await router.isReady()
  })

  it('未登录访问受保护路由 → 跳转登录页', async () => {
    await router.push('/system/user')
    expect(router.currentRoute.value.path).toBe('/login')
    expect(router.currentRoute.value.query.redirect).toBe('/system/user')
  })

  it('已登录访问登录页 → 回首页', async () => {
    localStorage.setItem(TOKEN_KEY, 'jwt')
    await router.push('/login')
    expect(router.currentRoute.value.path).toBe('/dashboard')
  })

  it('已登录但无页面权限 → 跳 403', async () => {
    localStorage.setItem(TOKEN_KEY, 'jwt')
    const store = useUserStore()
    store.$patch({
      user: { userId: 2, username: 'test', roles: ['test'], permissions: ['system:dept:list'] },
    })
    await router.push('/system/user')
    expect(router.currentRoute.value.path).toBe('/403')
  })

  it('已登录且有权限 → 放行目标页', async () => {
    localStorage.setItem(TOKEN_KEY, 'jwt')
    const store = useUserStore()
    store.$patch({
      user: { userId: 1, username: 'admin', roles: ['admin'], permissions: [] },
    })
    await router.push('/system/user')
    expect(router.currentRoute.value.path).toBe('/system/user')
  })
})
