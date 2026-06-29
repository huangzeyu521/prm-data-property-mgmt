<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
<template>
  <div class="prm-page">
    <div class="prm-query-bar">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="责任部门">
          <el-select v-model="q.deptName" placeholder="组织/部门(真实组织树)" clearable filterable allow-create default-first-option style="width:200px">
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

    <!-- 表5/表6 一站式设计新维度:批量清单(表6) + 合规敏感三字段 -->
    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="6"><el-card shadow="hover"><div class="st"><b class="blue">{{ d.batchListCount }}</b><span>批量授权清单(表6)</span></div></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover"><div class="st"><b class="orange">{{ d.crossRegionCount }}</b><span>跨域/跨系统授权</span></div></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover"><div class="st"><b class="orange">{{ d.thirdPartyCount }}</b><span>涉第三方来源(表5)</span></div></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover"><div class="st"><b class="red">{{ d.sensitiveCount }}</b><span>涉个人隐私·商业秘密(表5)</span></div></el-card></el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="12"><el-card header="授权方式分布(一事一议/批量)"><div ref="modeRef" style="height:300px"></div></el-card></el-col>
      <el-col :span="12"><el-card header="授权权益类型(使用权/经营权)"><div ref="rightRef" style="height:300px"></div></el-card></el-col>
    </el-row>
    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="24"><el-card header="按业务域分布(表5/表6 所属业务域)"><div ref="bizRef" style="height:300px"></div></el-card></el-col>
    </el-row>
    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="24"><el-card header="授权趋势（月度申请量 + 生效率）"><div ref="trendRef" style="height:320px"></div></el-card></el-col>
    </el-row>

    <el-card style="margin-top:16px" header="授权风险预警">
      <el-alert v-for="(a,i) in (d.riskAlerts||[])" :key="i" :type="a.includes('正常')?'success':'warning'" :closable="false" :title="a" style="margin-bottom:6px" />
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, nextTick } from 'vue'
import { initChart } from '@/lib/chartBase'
import { CHART_COLORS, C } from '@/lib/chartPalette'
import { padMonthly } from '@/lib/trend'
import { fixedBizDist } from '@/lib/bizDomains'
import { getAuthDashboard } from '@/api/authorize'
import { listOrg } from '@/api/org'

const d = reactive({ totalApply: 0, effective: 0, inReview: 0, rejected: 0, effectiveRate: 0, certCount: 0, batchListCount: 0, crossRegionCount: 0, thirdPartyCount: 0, sensitiveCount: 0, riskAlerts: [] })
const q = reactive({ deptName: '' })
const orgOptions = ref([])
const range = ref([])
const modeRef = ref(); const rightRef = ref(); const bizRef = ref(); const trendRef = ref()
const pairs = (m) => Object.entries(m || {}).map(([name, value]) => ({ name, value }))

function onReset() { q.deptName = ''; range.value = []; load() }

async function load() {
  const res = await getAuthDashboard({
    deptName: q.deptName || undefined,
    startTime: range.value?.[0] || undefined,
    endTime: range.value?.[1] ? range.value[1] + ' 23:59:59' : undefined
  })
  Object.assign(d, res)
  await nextTick()
  initChart(modeRef.value,{ color: CHART_COLORS, tooltip: { trigger: 'item' }, legend: { bottom: 0 }, series: [{ name: '授权模式', type: 'pie', radius: ['40%', '70%'], data: pairs(res.modeDistribution) }] })
  const rt = pairs(res.rightTypeDistribution)
  initChart(rightRef.value,{ color: CHART_COLORS, tooltip: { trigger: 'axis' }, grid: { left: 40, right: 16, top: 20, bottom: 30 }, xAxis: { type: 'category', data: rt.map(x => x.name) }, yAxis: { type: 'value', name: '授权数' }, series: [{ name: '授权数', type: 'bar', data: rt.map(x => x.value), itemStyle: { color: C.blue }, barMaxWidth: 50 }] })
  const biz = fixedBizDist(res.byBusinessDomain) // 标准业务域恒显(零填充)+ 追加非标准项(未分类等)
  initChart(bizRef.value,{ color: CHART_COLORS, tooltip: { trigger: 'axis' }, grid: { left: 48, right: 16, top: 20, bottom: 60 }, xAxis: { type: 'category', data: biz.map(x => x.name), axisLabel: { interval: 0, rotate: 30 } }, yAxis: { type: 'value', name: '授权数' }, series: [{ name: '授权数', type: 'bar', data: biz.map(x => x.value), itemStyle: { color: C.blue }, barMaxWidth: 40 }] })
  const tr = padMonthly(res.trend, 12) // 补齐近12个月连续横坐标(缺月柱归零、率跳过)
  initChart(trendRef.value,{
    color: CHART_COLORS, tooltip: { trigger: 'axis' }, legend: { bottom: 0, data: ['申请量', '生效率%'] },
    grid: { left: 48, right: 48, top: 20, bottom: 40 },
    xAxis: { type: 'category', data: tr.map(p => p.month), axisLabel: { rotate: 35 } },
    yAxis: [{ type: 'value', name: '申请量' }, { type: 'value', name: '%', axisLabel: { formatter: '{value}%' } }],
    series: [
      { name: '申请量', type: 'bar', data: tr.map(p => p.applyCount || 0), barMaxWidth: 36, itemStyle: { color: C.blue } },
      { name: '生效率%', type: 'line', yAxisIndex: 1, smooth: true, connectNulls: true, data: tr.map(p => p.effectiveRate ?? null), itemStyle: { color: C.green } }
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
