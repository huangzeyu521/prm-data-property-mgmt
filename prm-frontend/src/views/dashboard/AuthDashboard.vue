<template>
  <div class="prm-page">
    <div class="prm-query-bar">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="使用场景"><el-input v-model="q.scenario" placeholder="授权使用场景" clearable style="width:160px" /></el-form-item>
        <el-form-item label="业务部门"><el-input v-model="q.deptName" placeholder="组织/业务域" clearable style="width:140px" /></el-form-item>
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
import * as echarts from 'echarts'
import { getAuthDashboard } from '@/api/authorize'

const d = reactive({ totalApply: 0, effective: 0, inReview: 0, rejected: 0, effectiveRate: 0, certCount: 0, riskAlerts: [] })
const q = reactive({ scenario: '', deptName: '' })
const range = ref([])
const modeRef = ref(); const rightRef = ref(); const compRef = ref(); const scenarioRef = ref(); const trendRef = ref()
const pairs = (m) => Object.entries(m || {}).map(([name, value]) => ({ name, value }))
const COMP_COLOR = { '红': '#d03050', '高': '#d03050', '黄': '#f0a020', '中': '#f0a020', '绿': '#18a058', '低': '#18a058' }

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
  echarts.init(modeRef.value).setOption({ tooltip: { trigger: 'item' }, legend: { bottom: 0 }, series: [{ type: 'pie', radius: ['40%', '70%'], data: pairs(res.modeDistribution) }] })
  const rt = pairs(res.rightTypeDistribution)
  echarts.init(rightRef.value).setOption({ tooltip: { trigger: 'axis' }, grid: { left: 40, right: 16, top: 20, bottom: 30 }, xAxis: { type: 'category', data: rt.map(x => x.name) }, yAxis: { type: 'value' }, series: [{ type: 'bar', data: rt.map(x => x.value), itemStyle: { color: '#2f6bff' }, barMaxWidth: 50 }] })
  echarts.init(compRef.value).setOption({
    tooltip: { trigger: 'item' }, legend: { bottom: 0 },
    series: [{ type: 'pie', radius: ['40%', '70%'], data: pairs(res.complianceDist).map(x => ({ ...x, itemStyle: { color: COMP_COLOR[x.name] } })) }]
  })
  const sc = pairs(res.byScenario)
  echarts.init(scenarioRef.value).setOption({ tooltip: { trigger: 'axis' }, grid: { left: 40, right: 16, top: 20, bottom: 45 }, xAxis: { type: 'category', data: sc.map(x => x.name), axisLabel: { interval: 0, rotate: 20 } }, yAxis: { type: 'value', name: '频次' }, series: [{ type: 'bar', data: sc.map(x => x.value), itemStyle: { color: '#13c2c2' }, barMaxWidth: 40 }] })
  const tr = res.trend || []
  echarts.init(trendRef.value).setOption({
    tooltip: { trigger: 'axis' }, legend: { bottom: 0, data: ['申请量', '生效率%'] },
    grid: { left: 48, right: 48, top: 20, bottom: 40 },
    xAxis: { type: 'category', data: tr.map(p => p.month) },
    yAxis: [{ type: 'value', name: '申请量' }, { type: 'value', name: '%', axisLabel: { formatter: '{value}%' } }],
    series: [
      { name: '申请量', type: 'bar', data: tr.map(p => p.applyCount), itemStyle: { color: '#2f6bff' } },
      { name: '生效率%', type: 'line', yAxisIndex: 1, smooth: true, data: tr.map(p => p.effectiveRate), itemStyle: { color: '#18a058' } }
    ]
  })
}
onMounted(load)
</script>

<style scoped>
.st { text-align: center; } .st b { display:block; font-size:24px; } .st span { color: var(--prm-color-text-secondary); font-size:12px; }
.green { color:#18a058; } .orange { color:#f0a020; } .blue { color:#2f6bff; } .red { color:#d03050; }
</style>
