import { describe, expect, it } from 'vitest'
import { pageNumAfterDelete } from '../pagination'

describe('pageNumAfterDelete', () => {
  it('当前页仅剩 1 条且非首页时回退一页', () => {
    expect(pageNumAfterDelete(3, 1)).toBe(2)
    expect(pageNumAfterDelete(2, 1)).toBe(1)
  })

  it('第一页即使清空也不回退（保持第 1 页）', () => {
    expect(pageNumAfterDelete(1, 1)).toBe(1)
  })

  it('当前页还有多条时不回退', () => {
    expect(pageNumAfterDelete(3, 10)).toBe(3)
    expect(pageNumAfterDelete(2, 2)).toBe(2)
  })
})
