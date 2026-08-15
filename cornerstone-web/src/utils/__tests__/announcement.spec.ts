import { describe, expect, it } from 'vitest'
import {
  announcementStatusText,
  announcementStatusTagType,
} from '@/utils/announcement'

describe('announcement status mapping', () => {
  it('maps status codes to text', () => {
    expect(announcementStatusText(0)).toBe('草稿')
    expect(announcementStatusText(1)).toBe('已发布')
    expect(announcementStatusText(2)).toBe('已下线')
    // 未知/缺失状态宽容回退草稿
    expect(announcementStatusText(undefined)).toBe('草稿')
    expect(announcementStatusText(99)).toBe('草稿')
  })

  it('maps status codes to tag types', () => {
    expect(announcementStatusTagType(1)).toBe('success')
    expect(announcementStatusTagType(2)).toBe('warning')
    expect(announcementStatusTagType(0)).toBe('info')
    expect(announcementStatusTagType(undefined)).toBe('info')
  })
})
