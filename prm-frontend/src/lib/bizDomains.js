// 南网数据资产「业务域」标准集合(对齐确权目录树 网级 子域顺序;表5/表6 所属业务域)。
// 用于统计图恒显:即便某业务域当前无授权,也零填充展示,避免横坐标只剩 1~2 个。
export const BIZ_DOMAINS = [
  '办公域',
  '战略规划域',
  '人力资源域',
  '计划与财务域',
  '企业架构域',
  '科技创新域',
  '政策研究域',
  '输配电域',
  '市场营销域'
]

// 将后端 {业务域: 数量} 映射为「标准域恒显(零填充) + 追加非标准数据项(如自由文本/未分类)」的有序数组。
export function fixedBizDist(map) {
  const m = map || {}
  const out = BIZ_DOMAINS.map((name) => ({ name, value: m[name] || 0 }))
  Object.entries(m).forEach(([name, value]) => {
    if (!BIZ_DOMAINS.includes(name)) out.push({ name, value })
  })
  return out
}
