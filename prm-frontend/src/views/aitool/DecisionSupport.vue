<template>
  <div class="prm-page">
    <div class="prm-query-bar">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="确权申请ID"><el-input v-model="applyId" placeholder="输入确权申请ID" style="width:280px" /></el-form-item>
        <el-form-item><el-button type="primary" @click="onAnalyze">智能研判</el-button></el-form-item>
      </el-form>
      <span class="prm-table-note">综合 材料完整性30% / 权属无冲突40% / 合规15% / 历史匹配15% 加权,RAG 结合《数据二十条》与南网制度给出决策建议。</span>
    </div>

    <el-row v-if="d" :gutter="16">
      <el-col :span="8">
        <el-card shadow="hover"><div ref="gauge" style="height:200px"></div>
          <div style="text-align:center;margin-top:-6px">
            <el-tag :type="predTag(d.prediction)" effect="dark" size="large">{{ d.prediction }}</el-tag>
          </div>
        </el-card>
      </el-col>
      <el-col :span="16">
        <el-card header="关键因子分析" shadow="hover">
          <el-table :data="factors" border size="small">
            <el-table-column prop="name" label="因子" width="150" />
            <el-table-column label="权重" width="90" align="center"><template #default="{ row }">{{ (row.weight*100).toFixed(0) }}%</template></el-table-column>
            <el-table-column label="得分" min-width="200">
              <template #default="{ row }"><el-progress :percentage="row.score" :color="row.score>=85?'#18a058':(row.score<70?'#d03050':'#f0a020')" /></template>
            </el-table-column>
          </el-table>
          <div class="kv">优势因子:<el-tag type="success" effect="plain">{{ d.strengthFactors }}</el-tag></div>
          <div class="kv">短板因子:<el-tag type="danger" effect="plain">{{ d.weakFactors }}</el-tag></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card v-if="d" header="确权决策建议" shadow="hover" style="margin-top:16px">
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="决策理由">{{ d.reason }}</el-descriptions-item>
        <el-descriptions-item label="需补充材料">{{ d.supplementMaterials }}</el-descriptions-item>
        <el-descriptions-item label="待处置冲突">{{ d.pendingConflicts }}</el-descriptions-item>
        <el-descriptions-item label="权益分割方案">{{ d.splitPlan }}</el-descriptions-item>
        <el-descriptions-item label="RAG 智能建议">{{ d.ragAdvice }}</el-descriptions-item>
        <el-descriptions-item label="证据链(SM3)"><code class="hash">{{ d.evidenceChain }}</code></el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { aitAnalyze } from '@/api/aitool'

const applyId = ref('')
const d = ref(null); const factors = ref([]); const gauge = ref()

function predTag(p) { return { 建议通过: 'success', 建议补充材料: 'warning', 建议驳回: 'danger' }[p] || 'info' }

async function onAnalyze() {
  if (!applyId.value) { ElMessage.warning('请输入确权申请ID'); return }
  try {
    d.value = await aitAnalyze(applyId.value)
    factors.value = JSON.parse(d.value.factorsJson || '[]')
    await nextTick(); renderGauge(d.value.score)
    ElMessage.success('研判完成')
  } catch (e) { ElMessage.error('研判失败:确认申请ID存在') }
}
function renderGauge(score) {
  echarts.init(gauge.value).setOption({
    series: [{ type: 'gauge', min: 0, max: 100, progress: { show: true, width: 14 },
      axisLine: { lineStyle: { width: 14 } }, pointer: { width: 5 },
      detail: { valueAnimation: true, formatter: '{value}', fontSize: 28, offsetCenter: [0, '62%'] },
      data: [{ value: score, name: '确权评分' }], title: { offsetCenter: [0, '88%'], fontSize: 12 } }]
  })
}
</script>

<style scoped>
.kv { margin-top: 8px; font-size: 13px; }
.hash { font-family: ui-monospace, Consolas, monospace; font-size: 12px; color: #2f6bff; word-break: break-all; }
</style>
