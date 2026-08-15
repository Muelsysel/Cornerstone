import { request } from '@/api/request'
import type { LoginForm, LoginResponse } from '@/types/auth'

// 认证接口：登录端点约定为 POST /auth/login，经网关 8080 转发到 auth 服务。
export function login(data: LoginForm): Promise<LoginResponse> {
  return request<LoginResponse>({ url: '/auth/login', method: 'post', data })
}

// 退出登录：优先调用后端使令牌失效；后端未就绪时失败不影响前端本地清理。
export function logout(): Promise<unknown> {
  return request<unknown>({ url: '/auth/logout', method: 'post' })
}
