<template>
  <div class="prm-page">
    <div class="prm-query-bar">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="责任部门"><el-input v-model="q.deptName" placeholder="组织层级/部门" clearable style="width:160px" /></el-form-item>
        <el-form-item label="时间周期">
          <el-date-picker v-model="range" type="daterange" value-format="YYYY-MM-DD" range-separator="至" start-placeholder="开始" end-placeholder="结束" style="width:230px" />
        </el-form-item>
        <el-form-item><el-button type="primary" @click="load">查询</el-button><el-button @click="onReset">重置</el-button></el-form-item>
      </el-form>
    </div>

    <el-row :gutter="16">
      <el-col :span="5"><el-card shadow="hover"><div class="st"><b>{{ d.totalApply }}</b><span>确权申请总量</span></div></el-card></el-col>
      <el-col :span="5"><el-card shadow="hover"><div class="st"><b class="green">{{ d.done }}</b><span>已完成确权</span></div></el-card></el-col>
      <el-col :span="5"><el-card shadow="hover"><div class="st"><b class="orange">{{ d.pending }}</b><span>待处理</span></div></el-card></el-col>
      <el-col :span="5"><el-card shadow="hover"><div class="st"><b class="blue">{{ d.passRate }}%</b><span>确权通过率</span></div></el-card></el-col>
      <el-col :span="4"><el-card shadow="hover"><div class="st"><b>{{ d.cardCount }}</b><span>权益卡片数</span></div></el-card></el-col>
    </el-row>

    <el-card style="margin-top:16px" header="风险趋势预警">
      <div v-if="d.bottleneckNode && d.bottleneckNode!=='无积压'" style="margin-bottom:8px">
        <el-tag type="danger" effect="dark">流程瓶颈：{{ d.bottleneckNode }}</el-tag>
      </div>
      <el-alert v-for="(a,i) in (d.riskAlerts||[])" :key="i" :type="a.includes('正常')?'success':'warning'" :closable="false" :title="a" style="margin-bottom:6px" />
    </el-card>

    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="8"><el-card header="确权状态分布"><div ref="statusRef" style="height:300px"></div></el-card></el-col>
      <el-col :span="8"><el-card header="权益类型构成"><div ref="rightRef" style="height:300px"></div></el-card></el-col>
      <el-col :span="8"><el-card header="流程瓶颈（各审批节点积压）"><div ref="backlogRef" style="height:300px"></div></el-card></el-col>
    </el-row>
    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="24"><el-card header="确权趋势（月度申请量 + 通过率）"><div ref="trendRef" style="height:320px"></div></el-card></el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, nextTick } from 'vue'
import { initChart } from '@/lib/chartBase'
import { CHART_COLORS, C } from '@/lib/chartPalette'
import { getConfirmDashboard } from '@/api/confirm'

const d = reactive({ totalApply: 0, done: 0, pending: 0, rejected: 0, passRate: 0, cardCount: 0, bottleneckNode: '', riskAlerts: [] })
const q = reactive({ deptName: '' })
const range = ref([])
const statusRef = ref(); const rightRef = ref(); const backlogRef = ref(); const trendRef = ref()
const pairs = (m) => Object.entries(m || {}).map(([name, value]) => ({ name, value }))

function onReset() { q.deptName = ''; range.value = []; load() }

async function load() {
  const res = await getConfirmDashboard({
    deptName: q.deptName || undefined,
    startTime: range.value?.[0] || undefined,
    endTime: range.value?.[1] ? range.value[1] + ' 23:59:59' : undefined
  })
  Object.assign(d, res)
  await nextTick()
  initChart(statusRef.value,{ color: CHART_COLORS, tooltip: { trigger: 'item' }, legend: { bottom: 0 }, series: [{ type: 'pie', radius: ['40%', '70%'], data: pairs(res.statusDistribution) }] })
  initChart(rightRef.value,{ color: CHART_COLORS, tooltip: { trigger: 'item' }, legend: { bottom: 0 }, series: [{ type: 'pie', radius: '65%', data: pairs(res.rightTypeDistribution) }] })
  const bk = res.nodeBacklog || {}
  initChart(backlogRef.value,{
    color: CHART_COLORS, tooltip: { trigger: 'axis' }, grid: { left: 40, right: 20, top: 20, bottom: 40 },
    xAxis: { type: 'category', data: Object.keys(bk), axisLabel: { interval: 0, rotate: 15 } },
    yAxis: { type: 'value', name: '积压数' },
    series: [{ type: 'bar', data: Object.values(bk), barWidth: '45%', itemStyle: { color: C.gold } }]
  })
  const tr = res.trend || []
  initChart(trendRef.value,{
    color: CHART_COLORS, tooltip: { trigger: 'axis' }, legend: { bottom: 0, data: ['申请量', '通过率%'] },
    grid: { left: 48, right: 48, top: 20, bottom: 40 },
    xAxis: { type: 'category', data: tr.map(p => p.month) },
    yAxis: [{ type: 'value', name: '申请量' }, { type: 'value', name: '%', axisLabel: { formatter: '{value}%' } }],
    series: [
      { name: '申请量', type: 'bar', data: tr.map(p => p.applyCount), itemStyle: { color: C.blue } },
      { name: '通过率%', type: 'line', yAxisIndex: 1, smooth: true, data: tr.map(p => p.passRate), itemStyle: { color: C.green } }
    ]
  })
}
onMounted(load)
</script>

<style scoped>
.st { text-align: center; } .st b { display:block; font-size:26px; } .st span { color: var(--prm-color-text-secondary); font-size:12px; }
.green { color:#36b21d; } .orange { color:#ffc417; } .blue { color:#1e87f0; }
</style>
