// 后端统一返回结构 Result<T>：code===200 表示成功，否则为业务错误。
// 字段与 cornerstone-common 中的 Result 保持一致。
export interface Result<T = unknown> {
  code: number
  message: string
  data: T
}

/** 分页返回结构（MyBatis-Plus Page 的简化视图）。 */
export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

/** 树形节点通用结构（id + 子节点）。 */
export interface TreeNode {
  id: number
  parentId?: number | null
  children?: TreeNode[]
}
