// 登录令牌与用户信息在 localStorage 中的存储键，集中管理避免散落各处。
export const TOKEN_KEY = 'cornerstone_token'
export const USER_KEY = 'cornerstone_user'

/** 持久化的用户信息。 */
export interface UserInfo {
  userId: number
  username: string
  roles: string[]
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
