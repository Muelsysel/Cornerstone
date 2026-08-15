// 登录接口返回结构，与 run-demo.md 约定的 /auth/login 响应一致。
export interface LoginResponse {
  access_token: string
  token_type: string
  expires_in: number
  userId: number
  username: string
  /** 用户角色标识列表（如 ["admin"]）。 */
  roles: string[]
}

export interface LoginForm {
  username: string
  password: string
}
