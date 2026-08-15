import { request } from '@/api/request'
import type { PageResult } from '@/types'
import type { Menu, Dept, Role, RoleQuery, User, UserQuery } from '@/types/system'

// ---------------------------------- 用户 ----------------------------------

/** 用户新增/编辑载荷：在用户实体的基础上允许携带密码（新增时必填）。 */
export type UserPayload = Partial<User> & { password?: string }

export function getUserPage(params: UserQuery): Promise<PageResult<User>> {
  return request({ url: '/system/user/page', method: 'get', params })
}

export function createUser(data: UserPayload): Promise<unknown> {
  return request({ url: '/system/user', method: 'post', data })
}

export function updateUser(data: UserPayload): Promise<unknown> {
  return request({ url: '/system/user', method: 'put', data })
}

export function deleteUser(userId: number): Promise<unknown> {
  return request({ url: `/system/user/${userId}`, method: 'delete' })
}

export function changeUserStatus(data: { userId: number; status: string }): Promise<unknown> {
  return request({ url: '/system/user/status', method: 'put', data })
}

// ---------------------------------- 角色 ----------------------------------

export function getRolePage(params: RoleQuery): Promise<PageResult<Role>> {
  return request({ url: '/system/role/page', method: 'get', params })
}

export function createRole(data: Partial<Role>): Promise<unknown> {
  return request({ url: '/system/role', method: 'post', data })
}

export function updateRole(data: Partial<Role>): Promise<unknown> {
  return request({ url: '/system/role', method: 'put', data })
}

export function deleteRole(roleId: number): Promise<unknown> {
  return request({ url: `/system/role/${roleId}`, method: 'delete' })
}

// ---------------------------------- 菜单 ----------------------------------

export function getMenuTree(): Promise<Menu[]> {
  return request({ url: '/system/menu/tree', method: 'get' })
}

export function createMenu(data: Partial<Menu>): Promise<unknown> {
  return request({ url: '/system/menu', method: 'post', data })
}

export function updateMenu(data: Partial<Menu>): Promise<unknown> {
  return request({ url: '/system/menu', method: 'put', data })
}

export function deleteMenu(menuId: number): Promise<unknown> {
  return request({ url: `/system/menu/${menuId}`, method: 'delete' })
}

// ---------------------------------- 部门 ----------------------------------

export function getDeptTree(): Promise<Dept[]> {
  return request({ url: '/system/dept/tree', method: 'get' })
}

export function createDept(data: Partial<Dept>): Promise<unknown> {
  return request({ url: '/system/dept', method: 'post', data })
}

export function updateDept(data: Partial<Dept>): Promise<unknown> {
  return request({ url: '/system/dept', method: 'put', data })
}

export function deleteDept(deptId: number): Promise<unknown> {
  return request({ url: `/system/dept/${deptId}`, method: 'delete' })
}
