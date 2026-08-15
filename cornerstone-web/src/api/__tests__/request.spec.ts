import { describe, it, expect, vi, beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

// 隔离副作用：element-plus 消息组件在 jsdom 渲染有噪音，mock 掉
vi.mock('element-plus', () => ({
  ElMessage: { error: vi.fn(), success: vi.fn() },
}))

// mock 路由：捕获跳转调用，避免真实路由实例干扰
const replaceMock = vi.hoisted(() => vi.fn())
vi.mock('@/router', () => ({
  default: {
    currentRoute: { value: { fullPath: '/system/user' } },
    replace: replaceMock,
  },
}))

import service from '@/api/request'
import { useUserStore } from '@/stores/user'
import { TOKEN_KEY } from '@/utils/auth'
import { ElMessage } from 'element-plus'

// 401 登录失效死循环回归测试：
// 响应拦截器收到 401（HTTP 或业务码）时必须清空会话（localStorage + store），
// 否则登录页守卫看到残留 token 会弹回受保护页，形成 401↔/login 死循环。
describe('request 拦截器 401 处理', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    replaceMock.mockClear()
  })

  const handlers = (service.interceptors.response as unknown as {
    handlers: { fulfilled: (r: unknown) => unknown; rejected: (e: unknown) => unknown }[]
  }).handlers

  it('HTTP 401：清空会话并带 redirect 跳转登录页', async () => {
    const store = useUserStore()
    store.$patch({
      user: { userId: 1, username: 'admin', roles: ['admin'], permissions: [] },
    })

    await expect(
      handlers[0].rejected({ response: { status: 401, data: { message: '过期' } } }),
    ).rejects.toBeTruthy()

    expect(store.user).toBeNull()
    expect(replaceMock).toHaveBeenCalledWith(
      expect.objectContaining({ path: '/login', query: { redirect: '/system/user' } }),
    )
  })

  it('业务码 401：同样清会话并跳转登录页', async () => {
    const store = useUserStore()
    store.$patch({
      user: { userId: 1, username: 'admin', roles: ['admin'], permissions: [] },
    })

    await expect(
      handlers[0].fulfilled({ data: { code: 401, message: '登录已失效' } }),
    ).rejects.toBeTruthy()

    expect(store.user).toBeNull()
    expect(replaceMock).toHaveBeenCalledWith(expect.objectContaining({ path: '/login' }))
  })

  it('业务成功（code 200）不清理会话', async () => {
    const store = useUserStore()
    store.$patch({
      user: { userId: 1, username: 'admin', roles: ['admin'], permissions: [] },
    })

    const result = handlers[0].fulfilled({ data: { code: 200, data: { id: 1 } } })

    expect(store.user).not.toBeNull()
    expect(replaceMock).not.toHaveBeenCalled()
    expect(result).toBeDefined()
  })

  it('登录接口 401（密码错误）不清理会话不跳转', async () => {
    const store = useUserStore()
    store.$patch({
      user: { userId: 1, username: 'admin', roles: ['admin'], permissions: [] },
    })

    await expect(
      handlers[0].fulfilled({
        data: { code: 401, message: '用户名或密码错误' },
        config: { url: '/auth/login' },
      }),
    ).rejects.toBeTruthy()

    // 登录失败不是会话失效：不清理用户、不跳登录页
    expect(store.user).not.toBeNull()
    expect(replaceMock).not.toHaveBeenCalled()
  })

  it('请求超时：不清会话不跳转，提示友好中文', async () => {
    const store = useUserStore()
    store.$patch({
      user: { userId: 1, username: 'admin', roles: ['admin'], permissions: [] },
    })
    const messageMock = vi.mocked(ElMessage.error)

    await expect(
      handlers[0].rejected({ code: 'ECONNABORTED', message: 'timeout of 15000ms exceeded' }),
    ).rejects.toBeTruthy()

    // 超时不是会话失效：不清理用户、不跳登录页
    expect(store.user).not.toBeNull()
    expect(replaceMock).not.toHaveBeenCalled()
    // 提示中文友好文案，不暴露 axios 英文原语
    expect(messageMock).toHaveBeenCalledWith('请求超时，请稍后重试')
  })

  it('HTTP 500：提示友好中文，不清会话不跳转', async () => {
    const store = useUserStore()
    store.$patch({
      user: { userId: 1, username: 'admin', roles: ['admin'], permissions: [] },
    })
    const messageMock = vi.mocked(ElMessage.error)

    await expect(
      handlers[0].rejected({
        response: { status: 500, data: {} },
        message: 'Request failed with status code 500',
      }),
    ).rejects.toBeTruthy()

    // 服务端异常不是会话失效
    expect(store.user).not.toBeNull()
    expect(replaceMock).not.toHaveBeenCalled()
    expect(messageMock).toHaveBeenCalledWith('服务器开小差了，请稍后重试')
  })
})

// 请求拦截器：携带 Bearer 令牌
describe('request 拦截器令牌附加', () => {
  beforeEach(() => localStorage.clear())

  const requestHandlers = (
    service.interceptors.request as unknown as {
      handlers: { fulfilled: (config: { headers: Record<string, string> }) => unknown }[]
    }
  ).handlers

  it('本地有令牌时附加 Authorization 头', () => {
    localStorage.setItem(TOKEN_KEY, 'jwt-token')
    const config: { headers: Record<string, string> } = { headers: {} }
    requestHandlers[0].fulfilled(config)
    expect(config.headers.Authorization).toBe('Bearer jwt-token')
  })

  it('无令牌时不附加 Authorization 头', () => {
    const config: { headers: Record<string, string> } = { headers: {} }
    requestHandlers[0].fulfilled(config)
    expect(config.headers.Authorization).toBeUndefined()
  })
})
