<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
<template>
  <div class="prm-page">
    <el-row :gutter="16" style="margin-bottom:16px">
      <el-col :span="8"><el-card shadow="hover"><div class="st"><b>{{ d.totalArchive }}</b><span>产权档案总数(本模块建档登记)</span></div></el-card></el-col>
      <el-col :span="8"><el-card shadow="hover"><div class="st"><b :class="rateClass(d.mom)">{{ fmtRate(d.mom) }}</b><span>本月新增 环比(MoM)</span></div></el-card></el-col>
      <el-col :span="8"><el-card shadow="hover"><div class="st"><b :class="rateClass(d.yoy)">{{ fmtRate(d.yoy) }}</b><span>本月新增 同比(YoY)</span></div></el-card></el-col>
    </el-row>

    <el-card header="新增确权登记趋势 · 同比/环比(本模块建档量)" style="margin-bottom:16px">
      <div ref="trendRef" style="height:320px"></div>
    </el-card>

    <el-row :gutter="16" style="margin-bottom:16px">
      <el-col :span="12"><el-card header="已确权 · 三权分置结构"><div ref="rtRef" style="height:300px"></div></el-card></el-col>
      <el-col :span="12"><el-card header="按确权状态分布"><div ref="csRef" style="height:300px"></div></el-card></el-col>
    </el-row>
    <el-card header="各系统部署单位 确权覆盖率 / 授权率(总部 · 超高压 · 双调 · 广东 · 广西 · 云南 · 贵州 · 海南 · 广州 · 深圳)" style="margin-bottom:16px">
      <div ref="deployRef" style="height:300px"></div>
    </el-card>
    <el-row :gutter="16">
      <el-col :span="12"><el-card header="按授权状态分布"><div ref="authRef" style="height:300px"></div></el-card></el-col>
      <el-col :span="12"><el-card header="确权 · 授权转化漏斗(纳管 → 已确权 → 已授权)"><div ref="funnelRef" style="height:300px"></div></el-card></el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, nextTick } from 'vue'
import { initChart } from '@/lib/chartBase'
import { CHART_COLORS, C } from '@/lib/chartPalette'
import { getLedgerStatistics } from '@/api/ledger'

const d = reactive({ totalArchive: 0, mom: null, yoy: null })
const trendRef = ref(); const rtRef = ref(); const csRef = ref(); const deployRef = ref(); const authRef = ref(); const funnelRef = ref()
const pairs = (m) => Object.entries(m || {}).map(([name, value]) => ({ name, value }))
const fmtRate = (r) => (r == null ? '—' : (r > 0 ? '+' : '') + r + '%')
const rateClass = (r) => (r == null ? '' : r > 0 ? 'up' : r < 0 ? 'down' : '')

function pieOpt(arr, radius, name = '数量') {
  return { color: CHART_COLORS, tooltip: { trigger: 'item' }, legend: { bottom: 0 }, series: [{ name, type: 'pie', radius, data: arr }] }
}
// 覆盖率分组柱:某维度「确权覆盖率 / 授权率」(率口径,非资产库存数;资产卡片由资产平台维护本模块不增删)
function coverageBarOpt(list) {
  return {
    color: CHART_COLORS,
    tooltip: { trigger: 'axis', valueFormatter: (v) => (v == null ? '—' : v + '%') },
    legend: { bottom: 0, data: ['确权覆盖率', '授权率'] },
    grid: { left: 48, right: 24, top: 20, bottom: 56 },
    xAxis: { type: 'category', data: list.map(x => x.name), axisLabel: { interval: 0, rotate: 20 } },
    yAxis: { type: 'value', name: '%', max: 100, axisLabel: { formatter: '{value}%' } },
    series: [
      { name: '确权覆盖率', type: 'bar', data: list.map(x => x.confirmRate), barMaxWidth: 28, itemStyle: { color: C.blue } },
      { name: '授权率', type: 'bar', data: list.map(x => x.authRate), barMaxWidth: 28, itemStyle: { color: C.green } }
    ]
  }
}

async function load() {
  const res = await getLedgerStatistics()
  d.totalArchive = res.totalArchive
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
        return `${p.month}<br/>新增确权登记: <b>${p.count}</b><br/>环比(MoM): ${fmtRate(p.momRate)}<br/>同比(YoY): ${fmtRate(p.yoyRate)}`
      }
    },
    legend: { data: ['新增确权登记数', '环比%'], bottom: 0 },
    grid: { left: 50, right: 50, top: 20, bottom: 50 },
    xAxis: { type: 'category', data: t.map(x => x.month), axisLabel: { rotate: 35 } },
    yAxis: [
      { type: 'value', name: '登记数' },
      { type: 'value', name: '环比%', axisLabel: { formatter: '{value}%' } }
    ],
    series: [
      { name: '新增确权登记数', type: 'bar', data: t.map(x => x.count), itemStyle: { color: C.blue }, barMaxWidth: 36 },
      { name: '环比%', type: 'line', yAxisIndex: 1, data: t.map(x => x.momRate), connectNulls: true, smooth: true, itemStyle: { color: C.gold } }
    ]
  })
  // 系统部署单位:固定 10 桶(后端按打√清单顺序零填充),保持服务端顺序不再二次排序;率口径
  initChart(deployRef.value,coverageBarOpt(res.coverageByDeploymentUnit || []))
  initChart(rtRef.value,pieOpt(pairs(res.byRightType), ['40%', '70%'], '档案数'))
  initChart(csRef.value,pieOpt(pairs(res.byConfirmStatus), '65%', '档案数'))
  initChart(authRef.value,pieOpt(pairs(res.byAuthStatus), '65%', '档案数'))

  // 确权·授权转化漏斗:由各部署单位覆盖率汇总分子分母(纳管资产 → 已确权 → 已授权),反映产权进度链路
  const du = res.coverageByDeploymentUnit || []
  const sum = (k) => du.reduce((s, x) => s + (x[k] || 0), 0)
  const managed = sum('total'); const confirmed = sum('confirmed'); const authorized = sum('authorized')
  const pct = (n) => (managed > 0 ? Math.round(n * 1000 / managed) / 10 : 0)
  initChart(funnelRef.value,{
    color: [C.gray, C.blue, C.green],
    tooltip: { trigger: 'item', formatter: (p) => `${p.name}: ${p.value}(占纳管 ${pct(p.value)}%)` },
    legend: { bottom: 0, data: ['纳管资产', '已确权', '已授权'] },
    series: [{
      name: '确权授权转化', type: 'funnel', left: '8%', right: '8%', top: 16, bottom: 44,
      min: 0, max: managed || 1, minSize: '24%', maxSize: '100%', sort: 'descending', gap: 2,
      label: { show: true, position: 'inside', formatter: (p) => `${p.name} ${p.value}` },
      data: [
        { value: managed, name: '纳管资产' },
        { value: confirmed, name: '已确权' },
        { value: authorized, name: '已授权' }
      ]
    }]
  })
}
onMounted(load)
</script>

<style scoped>
.st { text-align: center; } .st b { display:block; font-size:30px; font-weight:700; } .st span { color: var(--prm-color-text-secondary); }
.st b.up { color:#36b21d; } .st b.down { color:#e21f0c; }
</style>
