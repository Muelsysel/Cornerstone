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
      // 401 业务码：登录失效，清会话并跳转登录页
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
    if (status === 401) {
      text = '登录已失效，请重新登录'
      handleSessionExpired()
      return Promise.reject(error)
    } else if (status === 403) {
      text = '无权限访问'
    }
    ElMessage.error(text)
    return Promise.reject(error)
  },
)

/** 泛型请求入口：返回后端的 data 字段（已由拦截器剥离）。 */
export const request = <T = unknown>(
  config: Parameters<AxiosInstance['request']>[0],
): Promise<T> => service.request<unknown, T>(config) as Promise<T>

export default service
