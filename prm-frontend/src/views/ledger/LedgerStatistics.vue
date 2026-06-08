<template>
  <div class="prm-page">
    <el-row :gutter="16" style="margin-bottom:16px">
      <el-col :span="6"><el-card shadow="hover"><div class="st"><b>{{ d.totalArchive }}</b><span>产权档案总数</span></div></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover"><div class="st"><b :class="rateClass(d.mom)">{{ fmtRate(d.mom) }}</b><span>本月新增 环比(MoM)</span></div></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover"><div class="st"><b :class="rateClass(d.yoy)">{{ fmtRate(d.yoy) }}</b><span>本月新增 同比(YoY)</span></div></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover"><div class="st"><b>{{ d.regions }}</b><span>覆盖地域(区域)数</span></div></el-card></el-col>
    </el-row>

    <el-card header="产权档案新增趋势 · 同比/环比" style="margin-bottom:16px">
      <div ref="trendRef" style="height:320px"></div>
    </el-card>

    <el-row :gutter="16" style="margin-bottom:16px">
      <el-col :span="8"><el-card header="按子公司分布"><div ref="subRef" style="height:300px"></div></el-card></el-col>
      <el-col :span="8"><el-card header="按产权类型分布"><div ref="rtRef" style="height:300px"></div></el-card></el-col>
      <el-col :span="8"><el-card header="按确权状态分布"><div ref="csRef" style="height:300px"></div></el-card></el-col>
    </el-row>
    <el-row :gutter="16">
      <el-col :span="12"><el-card header="按地域(区域)分布"><div ref="regionRef" style="height:300px"></div></el-card></el-col>
      <el-col :span="12"><el-card header="按授权状态分布"><div ref="authRef" style="height:300px"></div></el-card></el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getLedgerStatistics } from '@/api/ledger'

const d = reactive({ totalArchive: 0, mom: null, yoy: null, regions: 0 })
const trendRef = ref(); const subRef = ref(); const rtRef = ref(); const csRef = ref(); const regionRef = ref(); const authRef = ref()
const pairs = (m) => Object.entries(m || {}).map(([name, value]) => ({ name, value }))
const fmtRate = (r) => (r == null ? '—' : (r > 0 ? '+' : '') + r + '%')
const rateClass = (r) => (r == null ? '' : r > 0 ? 'up' : r < 0 ? 'down' : '')

function barOpt(arr, color) {
  return {
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: arr.map(x => x.name), axisLabel: { interval: 0, rotate: 20 } },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: arr.map(x => x.value), itemStyle: { color }, barMaxWidth: 48 }]
  }
}
function pieOpt(arr, radius) {
  return { tooltip: { trigger: 'item' }, legend: { bottom: 0 }, series: [{ type: 'pie', radius, data: arr }] }
}

async function load() {
  const res = await getLedgerStatistics()
  d.totalArchive = res.totalArchive
  d.regions = Object.keys(res.byRegion || {}).length
  const t = res.trend || []
  const latest = t[t.length - 1] || {}
  d.mom = latest.momRate ?? null
  d.yoy = latest.yoyRate ?? null
  await nextTick()

  echarts.init(trendRef.value).setOption({
    tooltip: {
      trigger: 'axis',
      formatter: (ps) => {
        const p = t[ps[0].dataIndex] || {}
        return `${p.month}<br/>新增档案: <b>${p.count}</b><br/>环比(MoM): ${fmtRate(p.momRate)}<br/>同比(YoY): ${fmtRate(p.yoyRate)}`
      }
    },
    legend: { data: ['新增档案数', '环比%'], bottom: 0 },
    grid: { left: 50, right: 50, top: 20, bottom: 50 },
    xAxis: { type: 'category', data: t.map(x => x.month), axisLabel: { rotate: 35 } },
    yAxis: [
      { type: 'value', name: '档案数' },
      { type: 'value', name: '环比%', axisLabel: { formatter: '{value}%' } }
    ],
    series: [
      { name: '新增档案数', type: 'bar', data: t.map(x => x.count), itemStyle: { color: '#2f6bff' }, barMaxWidth: 36 },
      { name: '环比%', type: 'line', yAxisIndex: 1, data: t.map(x => x.momRate), connectNulls: true, smooth: true, itemStyle: { color: '#f0a020' } }
    ]
  })
  echarts.init(subRef.value).setOption(barOpt(pairs(res.bySubsidiary), '#2f6bff'))
  echarts.init(rtRef.value).setOption(pieOpt(pairs(res.byRightType), ['40%', '70%']))
  echarts.init(csRef.value).setOption(pieOpt(pairs(res.byConfirmStatus), '65%'))
  echarts.init(regionRef.value).setOption(pieOpt(pairs(res.byRegion), ['40%', '70%']))
  echarts.init(authRef.value).setOption(pieOpt(pairs(res.byAuthStatus), '65%'))
}
onMounted(load)
</script>

<style scoped>
.st { text-align: center; } .st b { display:block; font-size:30px; font-weight:700; } .st span { color: var(--prm-color-text-secondary); }
.st b.up { color:#18a058; } .st b.down { color:#d03050; }
</style>
