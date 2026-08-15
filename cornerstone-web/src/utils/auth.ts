// 登录令牌与用户信息在 localStorage 中的存储键，集中管理避免散落各处。
export const TOKEN_KEY = 'cornerstone_token'
export const USER_KEY = 'cornerstone_user'

/** 持久化的用户信息。permissions 为 JWT scope 中解析出的权限点。 */
export interface UserInfo {
  userId: number
  username: string
  roles: string[]
  permissions: string[]
}

export function getToken(): string {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

export function removeToken(): void {
  localStorage.removeItem(TOKEN_KEY)
}

export function getUser(): UserInfo | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as UserInfo
  } catch {
    return null
  }
}

export function setUser(user: unknown): void {
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function removeUser(): void {
  localStorage.removeItem(USER_KEY)
}

/** 退出登录：清空令牌与用户信息。 */
export function clearAuth(): void {
  removeToken()
  removeUser()
}

/**
 * 从 JWT 中解析 payload 中的 scope 声明（空格分隔的权限点列表）。
 * 解码失败或缺失时返回空数组，调用方按权限点判断时自然放行/拦截。
 */
export function getScopesFromToken(token: string): string[] {
  try {
    const parts = token.split('.')
    if (parts.length < 2) return []
    // base64url -> base64：补齐填充并替换字符
    let base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/')
    while (base64.length % 4 !== 0) base64 += '='
    const json = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => `%${c.charCodeAt(0).toString(16).padStart(2, '0')}`)
        .join(''),
    )
    const payload = JSON.parse(json) as { scope?: string | string[] }
    const scope = payload.scope
    if (Array.isArray(scope)) return scope
    if (typeof scope === 'string' && scope.trim()) return scope.split(/\s+/)
    return []
  } catch {
    // 令牌解析失败不作为登录失败处理，权限点为空即可
    return []
  }
}
