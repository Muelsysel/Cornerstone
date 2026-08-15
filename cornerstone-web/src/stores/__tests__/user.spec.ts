import { describe, it, expect, vi, beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

vi.mock('@/api/auth', () => ({
  login: vi.fn(),
  logout: vi.fn(),
}))

import { useUserStore } from '@/stores/user'
import { login as apiLogin, logout as apiLogout } from '@/api/auth'
import { TOKEN_KEY, USER_KEY } from '@/utils/auth'

// 用户会话 store 单测：登录保存令牌与用户、退出/重置清空本地会话。
// 后端 API 层 mock；localStorage 用 jsdom 真实实现。

function mockLoginResponse(overrides: Partial<Parameters<typeof apiLogin>[0]> = {}) {
  return {
    access_token: 'jwt-token',
    token_type: 'Bearer',
    expires_in: 43200,
    userId: 1,
    username: 'admin',
    roles: ['admin'],
    ...overrides,
  }
}

describe('user store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('login 保存令牌、用户与角色', async () => {
    vi.mocked(apiLogin).mockResolvedValue(mockLoginResponse() as never)
    const store = useUserStore()

    await store.login({ username: 'admin', password: 'admin123' })

    expect(localStorage.getItem(TOKEN_KEY)).toBe('jwt-token')
    expect(store.token).toBe('jwt-token')
    expect(store.isLoggedIn).toBe(true)
    expect(store.user?.username).toBe('admin')
    expect(store.roles).toEqual(['admin'])
  })

  it('resetSession 清空内存与 localStorage 会话', () => {
    const store = useUserStore()
    store.$patch({
      user: { userId: 1, username: 'admin', roles: ['admin'], permissions: [] },
    })
    localStorage.setItem(TOKEN_KEY, 't')
    localStorage.setItem(USER_KEY, '{"username":"admin"}')

    store.resetSession()

    expect(store.token).toBe('')
    expect(store.user).toBeNull()
    expect(store.isLoggedIn).toBe(false)
    expect(localStorage.getItem(TOKEN_KEY)).toBeNull()
    expect(localStorage.getItem(USER_KEY)).toBeNull()
  })

  it('logout 调用后端并清理本地（后端失败不阻断）', async () => {
    vi.mocked(apiLogin).mockResolvedValue(mockLoginResponse() as never)
    vi.mocked(apiLogout).mockRejectedValue(new Error('network'))
    const store = useUserStore()
    await store.login({ username: 'admin', password: 'admin123' })

    await store.logout()

    expect(apiLogout).toHaveBeenCalled()
    expect(store.token).toBe('')
    expect(store.user).toBeNull()
    expect(localStorage.getItem(TOKEN_KEY)).toBeNull()
  })
})
