import { request } from '@/api/request'
import type { PageResult } from '@/types'

// 公告实体类型（cornerstone-demo 活模板业务实体）。
export interface Announcement {
  id: number
  title: string
  content?: string
  /** 发布状态（后端整数）：0 草稿 / 1 已发布 / 2 已下线 */
  status?: number
  author?: string
  publishTime?: string
  createTime?: string
  updateTime?: string
}

export interface AnnouncementQuery {
  pageNum: number
  pageSize: number
  title?: string
  status?: number
}

// 公开查询：demo 模块内白名单放行，无需登录（见 run-demo.md）。
export function getAnnouncementPage(params: AnnouncementQuery): Promise<PageResult<Announcement>> {
  return request({ url: '/demo/announcement/page', method: 'get', params })
}

/** 公告详情（公开接口；游客仅可见已发布内容，非发布态返回不存在） */
export function getAnnouncementDetail(id: number): Promise<Announcement> {
  return request({ url: `/demo/announcement/${id}`, method: 'get' })
}

// 以下管理操作需要登录态（Authorization: Bearer token）。
export function createAnnouncement(data: Partial<Announcement>): Promise<unknown> {
  return request({ url: '/demo/announcement', method: 'post', data })
}

export function updateAnnouncement(data: Partial<Announcement>): Promise<unknown> {
  return request({ url: `/demo/announcement/${data.id}`, method: 'put', data })
}

export function deleteAnnouncement(id: number): Promise<unknown> {
  return request({ url: `/demo/announcement/${id}`, method: 'delete' })
}

/** 发布公告（DRAFT → PUBLISHED） */
export function publishAnnouncement(id: number): Promise<unknown> {
  return request({ url: `/demo/announcement/${id}/publish`, method: 'post' })
}

/** 下线公告（PUBLISHED → OFFLINE） */
export function offlineAnnouncement(id: number): Promise<unknown> {
  return request({ url: `/demo/announcement/${id}/offline`, method: 'post' })
}
