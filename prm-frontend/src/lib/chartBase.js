// 图表统一基座 —— 数研院《电脑端 UI 设计规范 V1.0》§图表。
// 集中固化规范要求:统一色板 + 数据标签直接标在图元 + 字款字号一致 + 间隔统一,
// 全平台看板图表统一经 initChart()/applyChartBase() 套用,杜绝逐图飘移。

import * as echarts from 'echarts'
import { CHART_COLORS } from './chartPalette'

// 规范"字款字号一致":轴/图例/数据标签统一字号
const FONT = 12
const LABEL_COLOR = '#606266'

// 数据标签按系列类型注入(规范:数值直接标在图元上/旁)
function labelFor(type) {
  if (type === 'pie') return { show: true, formatter: '{b} {c}', fontSize: FONT }
  if (type === 'bar' || type === 'line') return { show: true, position: 'top', fontSize: FONT, color: LABEL_COLOR }
  return undefined // graph/gauge 等特殊图不强加数据标签
}

// 轴字号统一(兼容单轴/双轴数组)
function stampAxis(ax) {
  if (!ax) return ax
  const one = (a) => ({ ...a, axisLabel: { fontSize: FONT, ...(a.axisLabel || {}) }, nameTextStyle: { fontSize: FONT, ...(a.nameTextStyle || {}) } })
  return Array.isArray(ax) ? ax.map(one) : one(ax)
}

/**
 * 套用图表统一基座:色板 + 数据标签 + 字款字号一致 + 间隔统一。
 * 已显式设置的 label/color 予以尊重(不覆盖)。
 */
export function applyChartBase(option) {
  const o = { color: CHART_COLORS, ...option }
  if (o.xAxis) o.xAxis = stampAxis(o.xAxis)
  if (o.yAxis) o.yAxis = stampAxis(o.yAxis)
  if (o.legend) o.legend = { textStyle: { fontSize: FONT }, ...o.legend }
  if (Array.isArray(o.series)) {
    o.series = o.series.map((s) => {
      if (s.label) return s
      const lab = labelFor(s.type)
      return lab ? { ...s, label: lab } : s
    })
  }
  return o
}

/** 初始化图表并套用统一基座(看板图表统一入口)。 */
export function initChart(dom, option) {
  return echarts.init(dom).setOption(applyChartBase(option))
}
