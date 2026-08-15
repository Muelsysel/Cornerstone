import { request } from '@/api/request'
import type { PageResult } from '@/types'
import type {
  Config,
  ConfigQuery,
  Dept,
  DictData,
  DictDataQuery,
  DictType,
  DictTypeQuery,
  LoginLog,
  LoginLogQuery,
  Menu,
  OperLog,
  OperLogQuery,
  Role,
  RoleQuery,
  User,
  UserQuery,
} from '@/types/system'

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

export function changeUserStatus(userId: number, status: string): Promise<unknown> {
  return request({ url: `/system/user/${userId}/status`, method: 'put', params: { status } })
}

// ---------------------------------- 个人中心 ----------------------------------

export function getProfile(): Promise<User> {
  return request({ url: '/system/user/profile', method: 'get' })
}

export function updatePassword(data: {
  oldPassword: string
  newPassword: string
}): Promise<unknown> {
  return request({ url: '/system/user/profile/password', method: 'put', data })
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

/** 查询角色已分配的菜单 ID（权限回显） */
export function getRoleMenus(roleId: number): Promise<{ menuIds: number[] }> {
  return request({ url: `/system/role/${roleId}/menus`, method: 'get' })
}

/** 分配角色菜单权限 */
export function assignRoleMenus(roleId: number, menuIds: number[]): Promise<unknown> {
  return request({ url: `/system/role/${roleId}/menus`, method: 'put', data: menuIds })
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

// ---------------------------------- 字典类型 ----------------------------------

export function getDictTypePage(params: DictTypeQuery): Promise<PageResult<DictType>> {
  return request({ url: '/system/dict/type/page', method: 'get', params })
}

export function createDictType(data: Partial<DictType>): Promise<unknown> {
  return request({ url: '/system/dict/type', method: 'post', data })
}

export function updateDictType(data: Partial<DictType>): Promise<unknown> {
  return request({ url: '/system/dict/type', method: 'put', data })
}

export function deleteDictType(dictId: number): Promise<unknown> {
  return request({ url: `/system/dict/type/${dictId}`, method: 'delete' })
}

// ---------------------------------- 字典数据 ----------------------------------

export function getDictDataPage(params: DictDataQuery): Promise<PageResult<DictData>> {
  return request({ url: '/system/dict/data/page', method: 'get', params })
}

export function createDictData(data: Partial<DictData>): Promise<unknown> {
  return request({ url: '/system/dict/data', method: 'post', data })
}

export function updateDictData(data: Partial<DictData>): Promise<unknown> {
  return request({ url: '/system/dict/data', method: 'put', data })
}

export function deleteDictData(dictCode: number): Promise<unknown> {
  return request({ url: `/system/dict/data/${dictCode}`, method: 'delete' })
}

// ---------------------------------- 参数 ----------------------------------

export function getConfigPage(params: ConfigQuery): Promise<PageResult<Config>> {
  return request({ url: '/system/config/page', method: 'get', params })
}

export function createConfig(data: Partial<Config>): Promise<unknown> {
  return request({ url: '/system/config', method: 'post', data })
}

export function updateConfig(data: Partial<Config>): Promise<unknown> {
  return request({ url: '/system/config', method: 'put', data })
}

export function deleteConfig(configId: number): Promise<unknown> {
  return request({ url: `/system/config/${configId}`, method: 'delete' })
}

// ---------------------------------- 操作日志（只读） ----------------------------------

export function getOperLogPage(params: OperLogQuery): Promise<PageResult<OperLog>> {
  return request({ url: '/system/operlog/page', method: 'get', params })
}

export function deleteOperLog(operId: number): Promise<unknown> {
  return request({ url: `/system/operlog/${operId}`, method: 'delete' })
}

export function clearOperLog(): Promise<unknown> {
  return request({ url: '/system/operlog/clean', method: 'delete' })
}

// ---------------------------------- 登录日志（只读） ----------------------------------

export function getLoginLogPage(params: LoginLogQuery): Promise<PageResult<LoginLog>> {
  return request({ url: '/system/loginlog/page', method: 'get', params })
}

export function deleteLoginLog(infoId: number): Promise<unknown> {
  return request({ url: `/system/loginlog/${infoId}`, method: 'delete' })
}

export function clearLoginLog(): Promise<unknown> {
  return request({ url: '/system/loginlog/clean', method: 'delete' })
}
