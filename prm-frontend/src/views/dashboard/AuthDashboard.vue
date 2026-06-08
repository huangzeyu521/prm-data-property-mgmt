<template>
  <div class="prm-page">
    <el-row :gutter="16">
      <el-col :span="4"><el-card shadow="hover"><div class="st"><b>{{ d.totalApply }}</b><span>授权申请总量</span></div></el-card></el-col>
      <el-col :span="4"><el-card shadow="hover"><div class="st"><b class="green">{{ d.effective }}</b><span>已生效</span></div></el-card></el-col>
      <el-col :span="4"><el-card shadow="hover"><div class="st"><b class="orange">{{ d.inReview }}</b><span>审核中</span></div></el-card></el-col>
      <el-col :span="4"><el-card shadow="hover"><div class="st"><b class="red">{{ d.rejected }}</b><span>已驳回</span></div></el-card></el-col>
      <el-col :span="4"><el-card shadow="hover"><div class="st"><b class="blue">{{ d.effectiveRate }}%</b><span>授权生效率</span></div></el-card></el-col>
      <el-col :span="4"><el-card shadow="hover"><div class="st"><b>{{ d.certCount }}</b><span>授权证书数</span></div></el-card></el-col>
    </el-row>
    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="12"><el-card header="授权模式分布"><div ref="modeRef" style="height:320px"></div></el-card></el-col>
      <el-col :span="12"><el-card header="授权权益类型分布"><div ref="rightRef" style="height:320px"></div></el-card></el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getAuthDashboard } from '@/api/authorize'

const d = reactive({ totalApply: 0, effective: 0, inReview: 0, rejected: 0, effectiveRate: 0, certCount: 0 })
const modeRef = ref()
const rightRef = ref()
const pairs = (m) => Object.entries(m || {}).map(([name, value]) => ({ name, value }))

async function load() {
  const res = await getAuthDashboard()
  Object.assign(d, res)
  await nextTick()
  echarts.init(modeRef.value).setOption({
    tooltip: { trigger: 'item' }, legend: { bottom: 0 },
    series: [{ type: 'pie', radius: ['40%', '70%'], data: pairs(res.modeDistribution) }]
  })
  const rt = pairs(res.rightTypeDistribution)
  echarts.init(rightRef.value).setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: rt.map((x) => x.name) },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: rt.map((x) => x.value), itemStyle: { color: '#2f6bff' }, barMaxWidth: 60 }]
  })
}
onMounted(load)
</script>

<style scoped>
.st { text-align: center; } .st b { display:block; font-size:24px; } .st span { color: var(--prm-color-text-secondary); font-size:12px; }
.green { color:#18a058; } .orange { color:#f0a020; } .blue { color:#2f6bff; } .red { color:#d03050; }
</style>
