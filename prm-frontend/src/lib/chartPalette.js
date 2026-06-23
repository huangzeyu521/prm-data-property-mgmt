// 图表统一色板 —— 数研院《电脑端用户界面设计规范 V1.0》第 28 章「图表 Chart」p105 图表用色参考。
// 数据看板内所有 echarts 图表统一引用此色板,保证全平台图表配色一致(轻快、明亮为主)。
// 多分类时按序循环取色;饼/环/多系列可直接把 CHART_COLORS 赋给 setOption 顶层 color。

export const CHART_COLORS = [
  '#4A9EF2', // 1 蓝(主)
  '#70D0D9', // 2 青
  '#8FD480', // 3 绿
  '#AB81DE', // 4 紫
  '#F88F57', // 5 橙
  '#FFC417', // 6 金黄
  '#FFD8D4', // 7 浅粉
  '#DEDEDE'  // 8 浅灰
]

// 命名取色(语义中性,供单系列/指定系列按需引用)
export const C = {
  blue: '#4A9EF2',
  cyan: '#70D0D9',
  green: '#8FD480',
  purple: '#AB81DE',
  orange: '#F88F57',
  gold: '#FFC417',
  pink: '#FFD8D4',
  gray: '#DEDEDE'
}

// 风险/状态梯度(色板无纯红;以暖→冷映射 高→低,既达警示又合规):高=橙 中=金 低=绿
export const RISK_RAMP = { 高: C.orange, 中: C.gold, 低: C.green, 红: C.orange, 黄: C.gold, 绿: C.green }
