// 系统管理领域实体类型 —— 字段与 cornerstone-system 的 DTO 对齐。
// 后端仍在迭代，字段以实际接口为准；字段缺失时页面宽容处理。

export interface User {
  userId: number
  username: string
  nickname?: string
  status?: string // 0 正常 / 1 停用（与角色/部门/字典一致）
  deptId?: number
  /** 部门名称（列表展示用，后端按 deptId 批量回填） */
  deptName?: string
  remark?: string
  createTime?: string
}

export interface UserQuery {
  pageNum: number
  pageSize: number
  username?: string
  status?: string
  /** 部门过滤（可选） */
  deptId?: number
}

export interface Role {
  roleId: number
  roleName: string
  roleKey: string
  sort?: number
  status?: string
  remark?: string
  /** 数据范围：1全部 2自定义 3本部门及以下 4本部门 5仅本人 */
  dataScope?: string
  /** 数据范围=自定义(2)时的部门 ID 集合 */
  deptIds?: number[]
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
  /** 是否默认：Y 是 / N 否 */
  isDefault?: string
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
  /** 系统内置：Y 是 / N 否 */
  configType?: string
  remark?: string
  createTime?: string
}

export interface ConfigQuery {
  pageNum: number
  pageSize: number
  configKey?: string
  configName?: string
  /** 系统内置：Y 是 / N 否 */
  configType?: string
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
  /** 操作状态：0 成功 / 1 失败 */
  status?: number
  /** 操作时间区间（yyyy-MM-dd HH:mm:ss） */
  beginTime?: string
  endTime?: string
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
  /** 登录时间区间（yyyy-MM-dd HH:mm:ss） */
  beginTime?: string
  endTime?: string
}
