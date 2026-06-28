<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
<template>
  <div class="prm-page">
    <el-row :gutter="16" style="margin-bottom:16px">
      <el-col :span="6"><el-card shadow="hover"><div class="st"><b>{{ d.totalArchive }}</b><span>产权档案总数</span></div></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover"><div class="st"><b :class="rateClass(d.mom)">{{ fmtRate(d.mom) }}</b><span>本月新增 环比(MoM)</span></div></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover"><div class="st"><b :class="rateClass(d.yoy)">{{ fmtRate(d.yoy) }}</b><span>本月新增 同比(YoY)</span></div></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover"><div class="st"><b>{{ d.provinces }}</b><span>覆盖省域数</span></div></el-card></el-col>
    </el-row>

    <el-card header="产权档案新增趋势 · 同比/环比" style="margin-bottom:16px">
      <div ref="trendRef" style="height:320px"></div>
    </el-card>

    <el-row :gutter="16" style="margin-bottom:16px">
      <el-col :span="8"><el-card header="按子公司分布"><div ref="subRef" style="height:300px"></div></el-card></el-col>
      <el-col :span="8"><el-card header="按产权类型分布"><div ref="rtRef" style="height:300px"></div></el-card></el-col>
      <el-col :span="8"><el-card header="按确权状态分布"><div ref="csRef" style="height:300px"></div></el-card></el-col>
    </el-row>
    <el-card header="按系统部署单位分布(总部 · 超高压 · 双调 · 五省网 · 广州 · 深圳)" style="margin-bottom:16px">
      <div ref="deployRef" style="height:300px"></div>
    </el-card>
    <el-row :gutter="16" style="margin-bottom:16px">
      <el-col :span="12"><el-card header="按省域分布(点击柱形下钻地市)"><div ref="provinceRef" style="height:300px"></div></el-card></el-col>
      <el-col :span="12"><el-card :header="`按地市分布(${drillProvince || '全部省域'})`"><div ref="bureauRef" style="height:300px"></div></el-card></el-col>
    </el-row>
    <el-row :gutter="16">
      <el-col :span="12"><el-card header="按授权状态分布"><div ref="authRef" style="height:300px"></div></el-card></el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, nextTick } from 'vue'
import { initChart, applyChartBase } from '@/lib/chartBase'
import { CHART_COLORS, C } from '@/lib/chartPalette'
import { getLedgerStatistics } from '@/api/ledger'

const d = reactive({ totalArchive: 0, mom: null, yoy: null, provinces: 0 })
const trendRef = ref(); const subRef = ref(); const rtRef = ref(); const csRef = ref(); const deployRef = ref(); const provinceRef = ref(); const bureauRef = ref(); const authRef = ref()
const drillProvince = ref('')
let bureauChart = null
const pairs = (m) => Object.entries(m || {}).map(([name, value]) => ({ name, value }))
// 省→地市嵌套压平为「全部省域」视图(返回 {name,value} 数组)
const flattenBureau = (bb) => {
  const out = {}
  Object.values(bb || {}).forEach(m => Object.entries(m).forEach(([k, v]) => { out[k] = (out[k] || 0) + v }))
  return pairs(out)
}
const fmtRate = (r) => (r == null ? '—' : (r > 0 ? '+' : '') + r + '%')
const rateClass = (r) => (r == null ? '' : r > 0 ? 'up' : r < 0 ? 'down' : '')

function barOpt(arr, color) {
  return {
    color: CHART_COLORS,
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: arr.map(x => x.name), axisLabel: { interval: 0, rotate: 20 } },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: arr.map(x => x.value), itemStyle: { color }, barMaxWidth: 48 }]
  }
}
function pieOpt(arr, radius) {
  return { color: CHART_COLORS, tooltip: { trigger: 'item' }, legend: { bottom: 0 }, series: [{ type: 'pie', radius, data: arr }] }
}

async function load() {
  const res = await getLedgerStatistics()
  d.totalArchive = res.totalArchive
  d.provinces = Object.keys(res.byProvince || {}).length
  const t = res.trend || []
  const latest = t[t.length - 1] || {}
  d.mom = latest.momRate ?? null
  d.yoy = latest.yoyRate ?? null
  await nextTick()

  initChart(trendRef.value,{
    color: CHART_COLORS,
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
      { name: '新增档案数', type: 'bar', data: t.map(x => x.count), itemStyle: { color: C.blue }, barMaxWidth: 36 },
      { name: '环比%', type: 'line', yAxisIndex: 1, data: t.map(x => x.momRate), connectNulls: true, smooth: true, itemStyle: { color: C.gold } }
    ]
  })
  initChart(subRef.value,barOpt(pairs(res.bySubsidiary), C.blue))
  // 系统部署单位:固定 10 桶(后端按打√清单顺序零填充),保持服务端顺序不再二次排序
  initChart(deployRef.value,barOpt(pairs(res.byDeploymentUnit), C.green))
  initChart(rtRef.value,pieOpt(pairs(res.byRightType), ['40%', '70%']))
  initChart(csRef.value,pieOpt(pairs(res.byConfirmStatus), '65%'))
  const byBureau = res.byBureau || {}
  const provinceChart = initChart(provinceRef.value,barOpt(pairs(res.byProvince), C.blue))
  bureauChart = initChart(bureauRef.value,barOpt(flattenBureau(byBureau), C.green))
  // 真下钻:点省柱→该省地市;再点同省→回到全部省域
  provinceChart.on('click', (p) => {
    drillProvince.value = (drillProvince.value === p.name) ? '' : p.name
    const arr = drillProvince.value ? pairs(byBureau[drillProvince.value] || {}) : flattenBureau(byBureau)
    bureauChart.setOption(applyChartBase(barOpt(arr, C.green)), true)
  })
  initChart(authRef.value,pieOpt(pairs(res.byAuthStatus), '65%'))
}
onMounted(load)
</script>

<style scoped>
.st { text-align: center; } .st b { display:block; font-size:30px; font-weight:700; } .st span { color: var(--prm-color-text-secondary); }
.st b.up { color:#36b21d; } .st b.down { color:#e21f0c; }
</style>
