<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
<template>
  <div class="prm-page">
    <div class="prm-query-bar">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="使用场景"><el-input v-model="q.scenario" placeholder="授权使用场景" clearable style="width:160px" /></el-form-item>
        <el-form-item label="业务部门">
          <el-select v-model="q.deptName" placeholder="组织/业务域(真实组织树)" clearable filterable allow-create default-first-option style="width:200px">
            <el-option v-for="o in orgOptions" :key="o.id" :label="o.bizOrgName" :value="o.bizOrgName" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间周期">
          <el-date-picker v-model="range" type="daterange" value-format="YYYY-MM-DD" range-separator="至" start-placeholder="开始" end-placeholder="结束" style="width:230px" />
        </el-form-item>
        <el-form-item><el-button type="primary" @click="load">查询</el-button><el-button @click="onReset">重置</el-button></el-form-item>
      </el-form>
    </div>

    <el-row :gutter="16">
      <el-col :span="4"><el-card shadow="hover"><div class="st"><b>{{ d.totalApply }}</b><span>授权申请总量</span></div></el-card></el-col>
      <el-col :span="4"><el-card shadow="hover"><div class="st"><b class="green">{{ d.effective }}</b><span>已生效</span></div></el-card></el-col>
      <el-col :span="4"><el-card shadow="hover"><div class="st"><b class="orange">{{ d.inReview }}</b><span>审核中</span></div></el-card></el-col>
      <el-col :span="4"><el-card shadow="hover"><div class="st"><b class="red">{{ d.rejected }}</b><span>已驳回</span></div></el-card></el-col>
      <el-col :span="4"><el-card shadow="hover"><div class="st"><b class="blue">{{ d.effectiveRate }}%</b><span>授权生效率</span></div></el-card></el-col>
      <el-col :span="4"><el-card shadow="hover"><div class="st"><b>{{ d.certCount }}</b><span>授权证书数</span></div></el-card></el-col>
    </el-row>

    <el-card style="margin-top:16px" header="授权风险预警">
      <el-alert v-for="(a,i) in (d.riskAlerts||[])" :key="i" :type="a.includes('正常')?'success':'warning'" :closable="false" :title="a" style="margin-bottom:6px" />
    </el-card>

    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="6"><el-card header="授权模式分布"><div ref="modeRef" style="height:300px"></div></el-card></el-col>
      <el-col :span="6"><el-card header="授权权益类型分布"><div ref="rightRef" style="height:300px"></div></el-card></el-col>
      <el-col :span="6"><el-card header="合规检查结果(红/黄/绿)"><div ref="compRef" style="height:300px"></div></el-card></el-col>
      <el-col :span="6"><el-card header="使用场景授权频次"><div ref="scenarioRef" style="height:300px"></div></el-card></el-col>
    </el-row>
    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="24"><el-card header="授权趋势（月度申请量 + 生效率）"><div ref="trendRef" style="height:320px"></div></el-card></el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, nextTick } from 'vue'
import { initChart } from '@/lib/chartBase'
import { CHART_COLORS, C, RISK_RAMP } from '@/lib/chartPalette'
import { getAuthDashboard } from '@/api/authorize'
import { listOrg } from '@/api/org'

const d = reactive({ totalApply: 0, effective: 0, inReview: 0, rejected: 0, effectiveRate: 0, certCount: 0, riskAlerts: [] })
const q = reactive({ scenario: '', deptName: '' })
const orgOptions = ref([])
const range = ref([])
const modeRef = ref(); const rightRef = ref(); const compRef = ref(); const scenarioRef = ref(); const trendRef = ref()
const pairs = (m) => Object.entries(m || {}).map(([name, value]) => ({ name, value }))
// 合规分布按规范色板的暖→冷风险梯度取色(高=橙/中=金/低=绿),不再用非规范红
const COMP_COLOR = { '红': RISK_RAMP['高'], '高': RISK_RAMP['高'], '黄': RISK_RAMP['中'], '中': RISK_RAMP['中'], '绿': RISK_RAMP['低'], '低': RISK_RAMP['低'] }

function onReset() { q.scenario = ''; q.deptName = ''; range.value = []; load() }

async function load() {
  const res = await getAuthDashboard({
    scenario: q.scenario || undefined,
    deptName: q.deptName || undefined,
    startTime: range.value?.[0] || undefined,
    endTime: range.value?.[1] ? range.value[1] + ' 23:59:59' : undefined
  })
  Object.assign(d, res)
  await nextTick()
  initChart(modeRef.value,{ color: CHART_COLORS, tooltip: { trigger: 'item' }, legend: { bottom: 0 }, series: [{ type: 'pie', radius: ['40%', '70%'], data: pairs(res.modeDistribution) }] })
  const rt = pairs(res.rightTypeDistribution)
  initChart(rightRef.value,{ color: CHART_COLORS, tooltip: { trigger: 'axis' }, grid: { left: 40, right: 16, top: 20, bottom: 30 }, xAxis: { type: 'category', data: rt.map(x => x.name) }, yAxis: { type: 'value' }, series: [{ type: 'bar', data: rt.map(x => x.value), itemStyle: { color: C.blue }, barMaxWidth: 50 }] })
  initChart(compRef.value,{
    color: CHART_COLORS, tooltip: { trigger: 'item' }, legend: { bottom: 0 },
    series: [{ type: 'pie', radius: ['40%', '70%'], data: pairs(res.complianceDist).map(x => ({ ...x, itemStyle: { color: COMP_COLOR[x.name] } })) }]
  })
  const sc = pairs(res.byScenario)
  initChart(scenarioRef.value,{ color: CHART_COLORS, tooltip: { trigger: 'axis' }, grid: { left: 40, right: 16, top: 20, bottom: 45 }, xAxis: { type: 'category', data: sc.map(x => x.name), axisLabel: { interval: 0, rotate: 20 } }, yAxis: { type: 'value', name: '频次' }, series: [{ type: 'bar', data: sc.map(x => x.value), itemStyle: { color: C.cyan }, barMaxWidth: 40 }] })
  const tr = res.trend || []
  initChart(trendRef.value,{
    color: CHART_COLORS, tooltip: { trigger: 'axis' }, legend: { bottom: 0, data: ['申请量', '生效率%'] },
    grid: { left: 48, right: 48, top: 20, bottom: 40 },
    xAxis: { type: 'category', data: tr.map(p => p.month) },
    yAxis: [{ type: 'value', name: '申请量' }, { type: 'value', name: '%', axisLabel: { formatter: '{value}%' } }],
    series: [
      { name: '申请量', type: 'bar', data: tr.map(p => p.applyCount), itemStyle: { color: C.blue } },
      { name: '生效率%', type: 'line', yAxisIndex: 1, smooth: true, data: tr.map(p => p.effectiveRate), itemStyle: { color: C.green } }
    ]
  })
}
async function loadOrgs() {
  try { orgOptions.value = (await listOrg()) || [] } catch { orgOptions.value = [] }
}
onMounted(() => { loadOrgs(); load() })
</script>

<style scoped>
.st { text-align: center; } .st b { display:block; font-size:24px; } .st span { color: var(--prm-color-text-secondary); font-size:12px; }
.green { color:#36b21d; } .orange { color:#ffc417; } .blue { color:#1e87f0; } .red { color:#e21f0c; }
</style>
