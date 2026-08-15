// 操作日志业务类型文本映射（对齐后端 BusinessType 枚举：0其他/1新增/2修改/3删除/8清空）。
// 未知值宽容显示为「其他」。

/** 业务类型 → 中文文本 */
export function businessTypeText(type: number | undefined): string {
  const map: Record<number, string> = {
    0: '其他',
    1: '新增',
    2: '修改',
    3: '删除',
    8: '清空',
  }
  return type !== undefined && type in map ? map[type] : '其他'
}
