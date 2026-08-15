// 公告状态展示映射（后端整数契约：0草稿 / 1已发布 / 2已下线）。
// 与 cornerstone-demo 的 AnnouncementStatus 枚举保持一致。

export type AnnouncementStatusType = 'success' | 'warning' | 'info'

/** 状态文本 */
export function announcementStatusText(status: number | undefined): string {
  return status === 1 ? '已发布' : status === 2 ? '已下线' : '草稿'
}

/** 状态标签类型（el-tag type） */
export function announcementStatusTagType(status: number | undefined): AnnouncementStatusType {
  return status === 1 ? 'success' : status === 2 ? 'warning' : 'info'
}
