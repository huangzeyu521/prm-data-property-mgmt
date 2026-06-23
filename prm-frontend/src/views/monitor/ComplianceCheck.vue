<template>
  <div class="prm-page">
    <div class="prm-query-bar">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="检查结果">
          <el-select v-model="query.checkResult" placeholder="全部" clearable style="width:130px">
            <el-option v-for="r in results" :key="r" :label="r" :value="r" />
          </el-select>
        </el-form-item>
        <el-form-item label="资产ID"><el-input v-model="query.assetId" clearable style="width:160px" /></el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">查询</el-button>
          <el-button @click="onReset">重置</el-button>
          <el-button type="warning" :loading="running" @click="onRun">手工启动合规检查</el-button>
        </el-form-item>
      </el-form>
    </div>
    <div class="prm-table-card">
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column type="index" label="序号" width="56" align="center" />
        <el-table-column prop="assetId" label="资产ID" width="150" show-overflow-tooltip />
        <el-table-column prop="checkDim" label="检查维度" width="100" align="center">
          <template #default="{ row }"><el-tag v-if="row.checkDim" effect="plain" :type="dimTag(row.checkDim)">{{ row.checkDim }}</el-tag><span v-else>-</span></template>
        </el-table-column>
        <el-table-column prop="checkResult" label="检查结果" width="90" align="center">
          <template #default="{ row }"><el-tag :type="tag(row.checkResult)">{{ row.checkResult }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="problemDesc" label="问题描述" min-width="220" show-overflow-tooltip />
        <el-table-column prop="suggestion" label="整改建议" min-width="170" show-overflow-tooltip />
        <el-table-column prop="disposeStatus" label="处置状态" width="90" align="center" />
        <el-table-column prop="checkTime" label="检查时间" width="160" />
        <el-table-column label="报告" width="80" align="center" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.reportUrl" link type="primary" @click="viewReport(row.reportUrl)">查看</el-button>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>
      <div class="prm-table-note">注:合规检查为多维自动巡检——有效期 / 权限范围(越权) / 申请材料 / 协议内容;命中生成检查结果+预警,并产出检查报告。已由定时器周期自动执行。</div>
      <el-pagination style="margin-top:16px;justify-content:flex-end" background layout="total, sizes, prev, pager, next, jumper" :page-sizes="[10, 20, 50, 100]"
        :total="total" :current-page="query.current" :page-size="query.size" @current-change="onPage" @size-change="s=>{query.size=s;query.current=1;load()}" />
    </div>

    <el-dialog v-model="reportDlg" title="数据资产合规性检查报告" width="560px">
      <template v-if="report">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="报告编号">{{ report.reportId }}</el-descriptions-item>
          <el-descriptions-item label="生成时间">{{ fmtTime(report.reportTime) }}</el-descriptions-item>
          <el-descriptions-item label="覆盖档案">{{ report.totalArchives ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="命中问题">{{ report.hitCount }}（警告 {{ report.warnCount }} / 不合规 {{ report.failCount }}）</el-descriptions-item>
          <el-descriptions-item label="在途确权申请">{{ report.inflightConfirm ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="在途授权申请">{{ report.inflightAuth ?? '-' }}</el-descriptions-item>
        </el-descriptions>
        <div style="margin:14px 0 6px;font-weight:600">各维度命中</div>
        <el-table :data="dimRows" border size="small">
          <el-table-column prop="dim" label="检查维度" />
          <el-table-column prop="count" label="命中数" width="100" align="center" />
        </el-table>
        <el-alert :title="report.summary" type="info" :closable="false" style="margin-top:14px" />
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { pageCompliance, runComplianceCheck, getComplianceReport } from '@/api/monitor'

const results = ['合规', '警告', '不合规']
const query = reactive({ current: 1, size: 10, checkResult: '', assetId: '' })
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const running = ref(false)
const reportDlg = ref(false)
const report = ref(null)
const dimRows = computed(() => Object.entries(report.value?.byDimension || {}).map(([dim, count]) => ({ dim, count })))

function tag(r) { return { 合规: 'success', 警告: 'warning', 不合规: 'danger' }[r] || 'info' }
function dimTag(d) { return { 有效期: 'warning', 权限范围: 'danger', 申请材料: 'info', 协议内容: 'danger' }[d] || 'info' }
function fmtTime(t) { return t ? String(t).replace('T', ' ').slice(0, 19) : '-' }

async function load() {
  loading.value = true
  try {
    const res = await pageCompliance({ ...query })
    rows.value = res.records || []
    total.value = res.total || 0
  } finally { loading.value = false }
}
function onSearch() { query.current = 1; load() }
function onReset() { query.checkResult = ''; query.assetId = ''; onSearch() }
function onPage(p) { query.current = p; load() }

async function onRun() {
  running.value = true
  try {
    report.value = await runComplianceCheck()
    reportDlg.value = true
    ElMessage.success(`合规检查完成,新发现 ${report.value.hitCount} 项问题`)
    onSearch()
  } finally { running.value = false }
}
async function viewReport(reportId) {
  report.value = await getComplianceReport(reportId)
  reportDlg.value = true
}
onMounted(load)
</script>
