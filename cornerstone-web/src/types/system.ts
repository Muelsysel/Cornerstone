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
