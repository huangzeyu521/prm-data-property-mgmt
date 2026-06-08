<template>
  <div class="prm-page">
    <el-tabs v-model="tab" class="ov-tabs">
      <el-tab-pane label="总体概览" name="overview">
        <el-row :gutter="16">
          <el-col :span="6"><el-card shadow="hover"><div class="ov-card"><div class="ov-num">{{ data.totalAssets }}</div><div class="ov-label">资产总数</div></div></el-card></el-col>
          <el-col :span="6"><el-card shadow="hover"><div class="ov-card"><div class="ov-num ov-green">{{ data.confirmedAssets }}</div><div class="ov-label">已确权资产数</div></div></el-card></el-col>
          <el-col :span="6"><el-card shadow="hover"><div class="ov-card"><div class="ov-num ov-orange">{{ data.unconfirmedAssets }}</div><div class="ov-label">未确权资产数</div></div></el-card></el-col>
          <el-col :span="6"><el-card shadow="hover"><div class="ov-card"><div class="ov-num ov-blue">{{ data.confirmRate }}%</div><div class="ov-label">确权覆盖率</div></div></el-card></el-col>
        </el-row>
        <el-row :gutter="16" style="margin-top: 16px">
          <el-col :span="12">
            <el-card shadow="never" header="产权类型构成">
              <div ref="pieRef" style="height: 320px"></div>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card shadow="never" header="组织部门资产分布">
              <div ref="barRef" style="height: 320px"></div>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>
      <el-tab-pane label="产权树(子公司—系统—模式—数据集)" name="tree" lazy>
        <property-tree />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getOverview } from '@/api/ledger'
import PropertyTree from './PropertyTree.vue'

const tab = ref('overview')
const data = reactive({ totalAssets: 0, confirmedAssets: 0, unconfirmedAssets: 0, confirmRate: 0 })
const pieRef = ref()
const barRef = ref()

function toPairs(map) {
  return Object.entries(map || {}).map(([name, value]) => ({ name, value }))
}

async function load() {
  const res = await getOverview()
  Object.assign(data, res)
  await nextTick()
  const pie = echarts.init(pieRef.value)
  pie.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{ type: 'pie', radius: ['40%', '70%'], data: toPairs(res.rightTypeDistribution) }]
  })
  const bar = echarts.init(barRef.value)
  const subs = toPairs(res.subsidiaryDistribution)
  bar.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: subs.map((s) => s.name), axisLabel: { interval: 0, rotate: 20 } },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: subs.map((s) => s.value), itemStyle: { color: '#2f6bff' }, barMaxWidth: 48 }]
  })
}

onMounted(load)
</script>

<style scoped>
.ov-card { text-align: center; padding: 8px 0; }
.ov-num { font-size: 30px; font-weight: 700; }
.ov-label { color: var(--prm-color-text-secondary); margin-top: 4px; }
.ov-green { color: #18a058; }
.ov-orange { color: #f0a020; }
.ov-blue { color: #2f6bff; }
</style>
