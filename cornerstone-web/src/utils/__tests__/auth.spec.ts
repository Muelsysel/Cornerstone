import { describe, it, expect } from 'vitest'
import { getScopesFromToken } from '@/utils/auth'

// JWT payload 解码单测：scope 声明解析（权限点来源，见 src/utils/auth.ts）。

function makeToken(payload: Record<string, unknown>): string {
  const b64 = (s: string) =>
    btoa(s)
      .replace(/\+/g, '-')
      .replace(/\//g, '_')
      .replace(/=+$/, '')
  return `${b64(JSON.stringify({ alg: 'RS256' }))}.${b64(JSON.stringify(payload))}.signature`
}

describe('getScopesFromToken', () => {
  it('解析空格分隔的 scope 字符串', () => {
    const token = makeToken({ scope: 'system:user:list system:role:list' })
    expect(getScopesFromToken(token)).toEqual(['system:user:list', 'system:role:list'])
  })

  it('解析数组形式的 scope', () => {
    const token = makeToken({ scope: ['a:read', 'b:write'] })
    expect(getScopesFromToken(token)).toEqual(['a:read', 'b:write'])
  })

  it('无 scope 声明返回空数组', () => {
    const token = makeToken({ sub: '1' })
    expect(getScopesFromToken(token)).toEqual([])
  })

  it('非法令牌返回空数组（不抛异常）', () => {
    expect(getScopesFromToken('not-a-jwt')).toEqual([])
    expect(getScopesFromToken('')).toEqual([])
  })
})
