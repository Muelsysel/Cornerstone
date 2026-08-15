import { describe, it, expect, beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useUserStore } from '@/stores/user'
import { hasPermission, hasRole } from '@/utils/permission'

// hasPermission / hasRole 单测：Pinia store 注入后验证角色与权限点判断逻辑。
// 权限点来源：JWT scope（admin 角色放行全部，见 src/utils/permission.ts）。

function setUser(roles: string[], permissions: string[]) {
  const store = useUserStore()
  store.$patch({ user: { userId: 1, username: 'tester', roles, permissions } })
}

describe('hasPermission', () => {
  beforeEach(() => setActivePinia(createPinia()))

  it('admin 角色放行全部权限点', () => {
    setUser(['admin'], [])
    expect(hasPermission('system:user:add')).toBe(true)
  })

  it('普通用户按权限点精确判断', () => {
    setUser(['user'], ['system:user:list'])
    expect(hasPermission('system:user:list')).toBe(true)
    expect(hasPermission('system:user:add')).toBe(false)
  })

  it('未登录用户无任何权限', () => {
    expect(hasPermission('system:user:list')).toBe(false)
  })
})

describe('hasRole', () => {
  beforeEach(() => setActivePinia(createPinia()))

  it('单角色命中', () => {
    setUser(['operator'], [])
    expect(hasRole('operator')).toBe(true)
    expect(hasRole('admin')).toBe(false)
  })

  it('多角色任一命中即通过', () => {
    setUser(['guest'], [])
    expect(hasRole(['admin', 'guest'])).toBe(true)
    expect(hasRole(['admin', 'root'])).toBe(false)
  })
})
