import { describe, expect, it } from 'vitest'
import { businessTypeText } from '@/utils/operlog'

describe('businessTypeText', () => {
  it('maps known business types to Chinese text', () => {
    expect(businessTypeText(0)).toBe('其他')
    expect(businessTypeText(1)).toBe('新增')
    expect(businessTypeText(2)).toBe('修改')
    expect(businessTypeText(3)).toBe('删除')
    expect(businessTypeText(8)).toBe('清空')
  })

  it('falls back to 其他 for unknown or missing types', () => {
    expect(businessTypeText(undefined)).toBe('其他')
    expect(businessTypeText(99)).toBe('其他')
  })
})
