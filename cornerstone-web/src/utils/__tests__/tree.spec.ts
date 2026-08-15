import { describe, expect, it } from 'vitest'
import { flattenTree } from '@/utils/tree'
import type { TreeNode } from '@/types'

describe('flattenTree', () => {
  it('returns empty array for undefined or empty input', () => {
    expect(flattenTree(undefined)).toEqual([])
    expect(flattenTree([])).toEqual([])
  })

  it('flattens nested tree in depth-first order', () => {
    const tree: TreeNode[] = [
      { id: 1, children: [{ id: 2, children: [{ id: 3 }] }, { id: 4 }] },
      { id: 5 },
    ]
    const flat = flattenTree(tree)
    expect(flat.map((n) => n.id)).toEqual([1, 2, 3, 4, 5])
  })

  it('handles leaf nodes without children', () => {
    const flat = flattenTree([{ id: 1 }, { id: 2 }])
    expect(flat.map((n) => n.id)).toEqual([1, 2])
  })
})
