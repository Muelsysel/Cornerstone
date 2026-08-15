import axios, {
  type AxiosInstance,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { getToken } from '@/utils/auth'
import { useUserStore } from '@/stores/user'
import type { Result } from '@/types'

// 基于 axios 的请求封装：
// - baseURL 为 /，经 vite 代理转发到网关，避免开发期 CORS；
// - 请求拦截器自动附加 Authorization: Bearer <token>；
// - 响应拦截器统一处理后端 Result 结构，剥离 data 并做全局错误提示；
// - HTTP 401 / 业务码 401 视为登录失效：先清空会话（localStorage + store），
//   再跳登录页——否则登录页守卫看到残留 token 会弹回原页，形成 401↔/login 死循环。
const service: AxiosInstance = axios.create({
  baseURL: '/',
  timeout: 15000,
})

/** 登录失效统一处理：清会话 + 带 redirect 跳登录页。 */
function handleSessionExpired() {
  useUserStore().resetSession()
  const redirect = router.currentRoute.value.fullPath
  router.replace({
    path: '/login',
    query: redirect && redirect !== '/login' ? { redirect } : {},
  })
}

// 请求拦截器：携带令牌
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error),
)

// 响应拦截器：统一处理 Result 结构
service.interceptors.response.use(
  (response: AxiosResponse<Result>) => {
    const res = response.data
    // 非 Result 结构（如文件下载）直接返回
    if (typeof res !== 'object' || res === null || res.code === undefined) {
      return response
    }
    if (res.code !== 200) {
      // 登录接口 401 = 密码错误/账号禁用（HTTP 200 + 业务码 401），不是会话失效：
      // 只提示后端 message，不清理会话、不跳登录页（避免首次登录被误报"已失效"）
      if (isLoginUrl(response.config?.url)) {
        ElMessage.error(res.message || '登录失败')
        return Promise.reject(new Error(res.message || '登录失败'))
      }
      // 其他接口 401 业务码：登录失效，清会话并跳转登录页
      if (res.code === 401) {
        handleSessionExpired()
        return Promise.reject(new Error(res.message || '登录已失效'))
      }
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    // 直接返回 data，调用方无需再解包
    return res.data as unknown as AxiosResponse
  },
  (error) => {
    const status: number | undefined = error?.response?.status
    const message: string | undefined = error?.response?.data?.message
    let text = message || error.message || '网络异常，请稍后重试'
    if (error?.code === 'ECONNABORTED' || error?.message?.includes('timeout')) {
      // 请求超时：无响应体可读，给出友好提示（避免暴露 axios 英文原语）
      text = '请求超时，请稍后重试'
    } else if (status === 401) {
      // 登录接口的 HTTP 401 同理不视为会话失效
      if (isLoginUrl(error?.config?.url)) {
        text = message || '用户名或密码错误'
      } else {
        text = '登录已失效，请重新登录'
        handleSessionExpired()
        return Promise.reject(error)
      }
    } else if (status === 403) {
      text = '无权限访问'
    } else if (status === 429) {
      // 网关限流（RequestRateLimiter）拒绝：提示友好，不视为系统错误
      text = '请求过于频繁，请稍后再试'
    } else if (status !== undefined && status >= 500) {
      // 5xx：服务端异常，提示友好（不暴露 axios 英文原语）；网关/后端不可达的典型场景
      text = '服务器开小差了，请稍后重试'
    }
    ElMessage.error(text)
    return Promise.reject(error)
  },
)

/** 是否为登录请求（登录失败不应触发会话失效处理） */
function isLoginUrl(url: string | undefined): boolean {
  return typeof url === 'string' && url.includes('/auth/login')
}

/** 泛型请求入口：返回后端的 data 字段（已由拦截器剥离）。 */
export const request = <T = unknown>(
  config: Parameters<AxiosInstance['request']>[0],
): Promise<T> => service.request<unknown, T>(config) as Promise<T>

export default service
