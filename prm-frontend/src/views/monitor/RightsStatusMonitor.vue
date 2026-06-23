<template>
  <div class="prm-page">
    <el-row :gutter="16" style="margin-bottom:16px">
      <el-col :span="6"><el-card shadow="hover"><div class="st"><b>{{ stats.total }}</b><span>权益事件总数</span></div></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover"><div class="st"><b class="red">{{ stats.pending }}</b><span>待处理</span></div></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover"><div class="st"><b class="orange">{{ stats.processing }}</b><span>处理中</span></div></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover"><div class="st"><b class="green">{{ stats.closureRate }}%</b><span>闭环率</span></div></el-card></el-col>
    </el-row>
    <el-row :gutter="16">
      <el-col :span="10"><el-card header="风险等级分布"><div ref="levelRef" style="height:300px"></div></el-card></el-col>
      <el-col :span="14">
        <el-card header="权益状态实时清单">
          <el-table :data="rows" v-loading="loading" border stripe height="300">
            <el-table-column type="index" label="序号" width="60" align="center" />
            <el-table-column prop="alertLevel" label="级别" width="80" align="center">
              <template #default="{ row }"><el-tag :type="levelTag(row.alertLevel)">{{ row.alertLevel }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="assetId" label="资产" width="140" show-overflow-tooltip />
            <el-table-column prop="abnormalDesc" label="异常描述" min-width="160" show-overflow-tooltip />
            <el-table-column prop="disposeStatus" label="状态" width="90" align="center" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, nextTick } from 'vue'
import { initChart } from '@/lib/chartBase'
import { getAlertStats, pageAlert } from '@/api/monitor'

const stats = reactive({ total: 0, pending: 0, processing: 0, closed: 0, closureRate: 0 })
const rows = ref([])
const loading = ref(false)
const levelRef = ref()
function levelTag(l) { return { 紧急: 'danger', 重要: 'warning', 普通: 'info' }[l] || 'info' }
const pairs = (m) => Object.entries(m || {}).map(([name, value]) => ({ name, value }))

async function load() {
  loading.value = true
  try {
    const s = await getAlertStats()
    Object.assign(stats, s)
    const res = await pageAlert({ current: 1, size: 50 })
    rows.value = res.records || []
    await nextTick()
    initChart(levelRef.value, {
      tooltip: { trigger: 'item' }, legend: { bottom: 0 },
      series: [{ type: 'pie', radius: ['40%', '70%'], data: pairs(s.levelDistribution) }]
    })
  } finally { loading.value = false }
}
onMounted(load)
</script>

<style scoped>
.st { text-align: center; } .st b { display:block; font-size:26px; } .st span { color: var(--prm-color-text-secondary); font-size:12px; }
.red { color:#e21f0c; } .orange { color:#ffc417; } .green { color:#36b21d; }
</style>
