// 趋势补月:后端只返回有数据的月份(常仅当月),前端补齐为「最近 N 个月」连续横坐标。
// 缺月用占位 { month } 填充——计数类字段取 0(柱归零),率类字段保持 undefined(折线跳过/connectNulls 连接)。
// 锚点 = 当前月与数据中最大月份的较晚者,保证未来月数据也能纳入。

const ym = (d) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`

export function padMonthly(trend, months = 12) {
  const list = Array.isArray(trend) ? trend : []
  const byMonth = new Map(list.map((p) => [p.month, p]))
  let end = ym(new Date())
  for (const p of list) {
    if (p.month && p.month > end) end = p.month
  }
  const [ey, em] = end.split('-').map(Number)
  const out = []
  for (let i = months - 1; i >= 0; i--) {
    const d = new Date(ey, em - 1 - i, 1)
    const key = ym(d)
    out.push(byMonth.get(key) || { month: key })
  }
  return out
}
