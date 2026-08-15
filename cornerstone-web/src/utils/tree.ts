import type { TreeNode } from '@/types'

// 通用树工具：把任意带 children 的节点列表摊平为数组（用于下拉选择“上级”等场景）。
export function flattenTree<T extends TreeNode>(nodes: T[] | undefined): T[] {
  const result: T[] = []
  if (!nodes) return result
  for (const node of nodes) {
    result.push(node)
    if (node.children && node.children.length) {
      result.push(...flattenTree(node.children as T[]))
    }
  }
  return result
}
