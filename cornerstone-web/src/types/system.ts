// 系统管理领域实体类型 —— 字段与 cornerstone-system 的 DTO 对齐。
// 后端仍在迭代，字段以实际接口为准；字段缺失时页面宽容处理。

export interface User {
  userId: number
  username: string
  nickname?: string
  phone?: string
  email?: string
  status?: string // ENABLE / DISABLE
  deptId?: number
  remark?: string
  createTime?: string
}

export interface UserQuery {
  pageNum: number
  pageSize: number
  username?: string
  phone?: string
  status?: string
}

export interface Role {
  roleId: number
  roleName: string
  roleKey: string
  sort?: number
  status?: string
  remark?: string
  createTime?: string
}

export interface RoleQuery {
  pageNum: number
  pageSize: number
  roleName?: string
  roleKey?: string
  status?: string
}

// 菜单类型：目录 M / 菜单 C / 按钮 F
export type MenuType = 'M' | 'C' | 'F'

export interface Menu {
  menuId: number
  parentId: number
  menuName: string
  menuType?: MenuType
  path?: string
  component?: string
  perms?: string
  icon?: string
  sort?: number
  status?: string
  visible?: string
  children?: Menu[]
}

export interface Dept {
  deptId: number
  parentId: number
  deptName: string
  sort?: number
  status?: string
  leader?: string
  phone?: string
  children?: Dept[]
}

// ---------------------------------- 字典 ----------------------------------

export interface DictType {
  dictId: number
  dictName: string
  dictType: string
  status?: string
  remark?: string
  createTime?: string
}

export interface DictTypeQuery {
  pageNum: number
  pageSize: number
  dictName?: string
  dictType?: string
  status?: string
}

export interface DictData {
  dictCode: number
  dictType: string
  dictLabel: string
  dictValue: string
  sort?: number
  status?: string
  remark?: string
  createTime?: string
}

export interface DictDataQuery {
  pageNum: number
  pageSize: number
  dictType?: string
  dictLabel?: string
  status?: string
}

// ---------------------------------- 参数 ----------------------------------

export interface Config {
  configId: number
  configName?: string
  configKey: string
  configValue?: string
  remark?: string
  createTime?: string
}

export interface ConfigQuery {
  pageNum: number
  pageSize: number
  configKey?: string
  configName?: string
}

// ---------------------------------- 日志 ----------------------------------

export interface OperLog {
  operId: number
  title?: string
  businessType?: number
  operName?: string
  operIp?: string
  operTime?: string
  status?: number
  errorMsg?: string
  method?: string
  requestMethod?: string
  operUrl?: string
  operParam?: string
  jsonResult?: string
}

export interface OperLogQuery {
  pageNum: number
  pageSize: number
  title?: string
  operName?: string
}

export interface LoginLog {
  infoId: number
  username?: string
  ipaddr?: string
  status?: string
  msg?: string
  loginTime?: string
}

export interface LoginLogQuery {
  pageNum: number
  pageSize: number
  username?: string
  status?: string
}
