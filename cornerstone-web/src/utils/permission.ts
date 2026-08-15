import { useUserStore } from '@/stores/user'

/**
 * 权限工具：基于 Pinia 用户 store 判断角色与权限点。
 * 权限点来自 JWT 的 scope 声明（登录时解码存入 store.permissions）。
 */

/** 管理员角色标识：拥有全部权限。 */
const ADMIN_ROLE = 'admin'

/** 是否拥有指定权限点（如 'system:user:add'）。无权限点声明的用户视为无任何额外权限。 */
export function hasPermission(perm: string): boolean {
  const store = useUserStore()
  // admin 角色放行全部
  if (store.roles.includes(ADMIN_ROLE)) return true
  return store.permissions.includes(perm)
}

/** 是否拥有指定角色（可传多个，任一命中即通过）。 */
export function hasRole(role: string | string[]): boolean {
  const store = useUserStore()
  const targets = Array.isArray(role) ? role : [role]
  return targets.some((r) => store.roles.includes(r))
}
