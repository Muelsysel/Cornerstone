/**
 * 删除当前页最后一条记录后，若该页已无记录则回退一页，避免停留在空页。
 *
 * @param pageNum 删除前的当前页码
 * @param currentPageSize 删除前当前页的记录条数
 * @returns 删除后应使用的页码：当前页原本仅 1 条且不在第一页时回退一页，否则保持不变
 */
export function pageNumAfterDelete(pageNum: number, currentPageSize: number): number {
  return currentPageSize === 1 && pageNum > 1 ? pageNum - 1 : pageNum
}
