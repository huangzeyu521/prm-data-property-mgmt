<template>
  <div class="prm-page">
    <el-row :gutter="16">
      <el-col :span="5"><el-card shadow="hover"><div class="st"><b>{{ d.totalApply }}</b><span>确权申请总量</span></div></el-card></el-col>
      <el-col :span="5"><el-card shadow="hover"><div class="st"><b class="green">{{ d.done }}</b><span>已完成确权</span></div></el-card></el-col>
      <el-col :span="5"><el-card shadow="hover"><div class="st"><b class="orange">{{ d.pending }}</b><span>待处理</span></div></el-card></el-col>
      <el-col :span="5"><el-card shadow="hover"><div class="st"><b class="blue">{{ d.passRate }}%</b><span>确权通过率</span></div></el-card></el-col>
      <el-col :span="4"><el-card shadow="hover"><div class="st"><b>{{ d.cardCount }}</b><span>权益卡片数</span></div></el-card></el-col>
    </el-row>
    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="12"><el-card header="确权状态分布"><div ref="statusRef" style="height:320px"></div></el-card></el-col>
      <el-col :span="12"><el-card header="权益类型构成"><div ref="rightRef" style="height:320px"></div></el-card></el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getConfirmDashboard } from '@/api/confirm'

const d = reactive({ totalApply: 0, done: 0, pending: 0, rejected: 0, passRate: 0, cardCount: 0 })
const statusRef = ref()
const rightRef = ref()
const pairs = (m) => Object.entries(m || {}).map(([name, value]) => ({ name, value }))

async function load() {
  const res = await getConfirmDashboard()
  Object.assign(d, res)
  await nextTick()
  echarts.init(statusRef.value).setOption({
    tooltip: { trigger: 'item' }, legend: { bottom: 0 },
    series: [{ type: 'pie', radius: ['40%', '70%'], data: pairs(res.statusDistribution) }]
  })
  echarts.init(rightRef.value).setOption({
    tooltip: { trigger: 'item' }, legend: { bottom: 0 },
    series: [{ type: 'pie', radius: '65%', data: pairs(res.rightTypeDistribution) }]
  })
}
onMounted(load)
</script>

<style scoped>
.st { text-align: center; } .st b { display:block; font-size:26px; } .st span { color: var(--prm-color-text-secondary); font-size:12px; }
.green { color:#18a058; } .orange { color:#f0a020; } .blue { color:#2f6bff; }
</style>
