<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
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
            <el-card shadow="never" header="已确权 · 三权分置结构">
              <div ref="pieRef" style="height: 320px"></div>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card shadow="never" header="各子公司 确权覆盖率 / 授权率">
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
import { initChart } from '@/lib/chartBase'
import { C, CHART_COLORS } from '@/lib/chartPalette'
import { getOverview, getLedgerStatistics } from '@/api/ledger'
import PropertyTree from './PropertyTree.vue'

const tab = ref('overview')
const data = reactive({ totalAssets: 0, confirmedAssets: 0, unconfirmedAssets: 0, confirmRate: 0 })
const pieRef = ref()
const barRef = ref()

function toPairs(map) {
  return Object.entries(map || {}).map(([name, value]) => ({ name, value }))
}

async function load() {
  // 概览(确权率卡片+产权类型)与台账统计(各子公司覆盖率)并行;率口径而非资产库存数
  const [res, st] = await Promise.all([getOverview(), getLedgerStatistics()])
  Object.assign(data, res)
  await nextTick()
  initChart(pieRef.value, {
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{ name: '产权类型', type: 'pie', radius: ['40%', '70%'], data: toPairs(res.rightTypeDistribution) }]
  })
  // 各子公司 确权覆盖率/授权率(资产卡片由资产平台维护本模块不增删,故用率而非资产库存数)
  const cov = st.coverageBySubsidiary || []
  initChart(barRef.value, {
    color: CHART_COLORS,
    tooltip: { trigger: 'axis', valueFormatter: (v) => (v == null ? '—' : v + '%') },
    legend: { bottom: 0, data: ['确权覆盖率', '授权率'] },
    grid: { left: 48, right: 24, top: 20, bottom: 56 },
    xAxis: { type: 'category', data: cov.map((s) => s.name), axisLabel: { interval: 0, rotate: 20 } },
    yAxis: { type: 'value', name: '%', max: 100, axisLabel: { formatter: '{value}%' } },
    series: [
      { name: '确权覆盖率', type: 'bar', data: cov.map((s) => s.confirmRate), barMaxWidth: 28, itemStyle: { color: C.blue } },
      { name: '授权率', type: 'bar', data: cov.map((s) => s.authRate), barMaxWidth: 28, itemStyle: { color: C.green } }
    ]
  })
}

onMounted(load)
</script>

<style scoped>
.ov-card { text-align: center; padding: 8px 0; }
.ov-num { font-size: 30px; font-weight: 700; }
.ov-label { color: var(--prm-color-text-secondary); margin-top: 4px; }
.ov-green { color: #36b21d; }
.ov-orange { color: #ffc417; }
.ov-blue { color: #1e87f0; }
</style>
