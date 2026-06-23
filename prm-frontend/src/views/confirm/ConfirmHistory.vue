<template>
  <div class="prm-page">
    <div class="prm-query-bar">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="数据集"><el-input v-model="q.assetName" placeholder="资产/数据集名称" clearable style="width:160px" /></el-form-item>
        <el-form-item label="权属人"><el-input v-model="q.rightHolder" placeholder="人员/单位" clearable style="width:140px" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="q.status" placeholder="全部" clearable style="width:120px">
            <el-option v-for="s in statuses" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="申请时间">
          <el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD" range-separator="至"
            start-placeholder="开始" end-placeholder="结束" style="width:230px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">查询</el-button>
          <el-button @click="onReset">重置</el-button>
          <el-button @click="onExport">导出</el-button>
          <el-button type="success" plain @click="onExportSummary">导出《数据确权信息汇总表》</el-button>
          <el-button type="success" plain @click="onExportEquity">导出《权益内部管理汇总表》</el-button>
        </el-form-item>
      </el-form>
    </div>
    <div class="prm-table-card">
      <div style="margin-bottom:12px">
        <el-button type="primary" :disabled="!draftSel.length" @click="onBatchSubmit">批量提交审核（{{ draftSel.length }} 份草稿）</el-button>
      </div>
      <el-table :data="rows" v-loading="loading" border stripe @selection-change="onSel">
        <el-table-column type="selection" width="46" :selectable="r => r.status === '草稿'" />
        <el-table-column type="index" label="序号" width="56" align="center" />
        <el-table-column prop="applyNo" label="申请编号" width="160" show-overflow-tooltip />
        <el-table-column prop="assetName" label="资产名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="rightType" label="权属类型" width="130" />
        <el-table-column prop="status" label="状态" width="110" align="center">
          <template #default="{ row }"><el-tag :type="tag(row.status)">{{ row.status }}</el-tag></template>
        </el-table-column>
        <el-table-column label="流转进度" min-width="340">
          <template #default="{ row }">
            <el-steps :active="stepOf(row.status)" align-center finish-status="success" simple style="margin:0" class="flow-mini">
              <el-step title="提交" /><el-step title="人工预审" /><el-step title="合规" /><el-step title="主管" /><el-step title="终审" /><el-step title="制卡" />
            </el-steps>
          </template>
        </el-table-column>
        <el-table-column prop="rejectReason" label="驳回原因" min-width="120" show-overflow-tooltip />
        <el-table-column prop="createTime" label="申请时间" width="160" />
        <el-table-column label="处理时效" width="130">
          <template #default="{ row }">{{ duration(row) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="onProgress(row)">进度详情</el-button>
            <el-button v-if="row.status === '草稿'" link type="danger" @click="onDelete(row)">删除</el-button>
            <el-button v-if="IN_REVIEW.includes(row.status)" link type="warning" @click="onWithdraw(row)">撤回</el-button>
            <el-button v-if="row.status === '已撤回'" link type="primary" @click="onReopen(row)">重新编辑提交</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:16px;justify-content:flex-end" background layout="total, prev, pager, next"
        :total="total" :current-page="q.current" :page-size="q.size" @current-change="p=>{q.current=p;load()}" />
    </div>

    <el-drawer v-model="drawer" :title="`进度跟踪 — ${curNo}`" size="46%">
      <div v-if="!logs.length" style="color:#999;padding:12px">暂无流转记录(草稿尚未提交)。</div>
      <el-timeline v-else style="padding:8px 6px">
        <el-timeline-item v-for="l in logs" :key="l.logId" :timestamp="fmt(l.createTime)" placement="top"
          :type="l.toStatus === '已驳回' ? 'danger' : (l.toStatus === '已完成' ? 'success' : 'primary')">
          <div style="font-weight:600">{{ l.nodeName }}{{ l.node ? '（节点' + l.node + '）' : '' }}：{{ l.fromStatus }} → {{ l.toStatus }}</div>
          <div style="font-size:12px;color:#71717a;margin-top:2px">责任人：{{ l.responder || '-' }}</div>
          <div v-if="l.opinion" style="font-size:12px;color:#71717a;margin-top:2px">意见：{{ l.opinion }}</div>
          <div style="font-size:12px;color:#1e87f0;margin-top:4px"><el-icon style="vertical-align:-2px"><Bell /></el-icon> {{ l.pushChannel }}：{{ l.notifyContent }}</div>
        </el-timeline-item>
      </el-timeline>
    </el-drawer>
  </div>
</template>
<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageConfirmApply, deleteConfirmApply, withdrawConfirm, getConfirmFlowLog, confirmHistoryExportUrl, batchSubmitConfirm, confirmSummaryExportUrl, equityConsolidationExportUrl } from '@/api/confirm'

const router = useRouter()
// 审批链活动态(申请人可主动撤回);终态(已完成/已驳回/已撤回)与草稿不可撤回
const IN_REVIEW = ['人工预审中', '合规审核中', '主管复核中', '经理终审中']
async function onWithdraw(row) {
  try {
    const { value } = await ElMessageBox.prompt(`确认撤回确权申请「${row.applyNo}」?撤回后可重新编辑提交`, '撤回申请', { inputType: 'textarea', inputPlaceholder: '撤回原因(可空)' })
    await withdrawConfirm(row.applyId, value || '')
    ElMessage.success('已撤回,可在「已撤回」项重新编辑提交')
    load()
  } catch (e) { /* 取消 */ }
}
// 重新编辑提交:带入原单内容到一站式向导(复用 reopen 机制),作为新申请再次发起
function onReopen(row) {
  sessionStorage.setItem('prm-reopen', JSON.stringify({ domain: '确权', raw: row }))
  router.push({ path: '/dpr/confirm/wizard', query: { reopen: 1 } })
}

// 官方汇总表导出(对齐南网《数据确权信息汇总表》/《权益内部管理汇总表》模板)
function onExportSummary() { window.open(confirmSummaryExportUrl(), '_blank') }
function onExportEquity() { window.open(equityConsolidationExportUrl(), '_blank') }
const draftSel = ref([])
function onSel(sel) { draftSel.value = sel.filter(r => r.status === '草稿') }
async function onBatchSubmit() {
  const ids = draftSel.value.map(r => r.applyId)
  const r = await batchSubmitConfirm(ids)
  ElMessage[r.failed ? 'warning' : 'success'](`批量提交:成功 ${r.success}/${r.total}${r.failed ? '，失败 ' + r.failed : ''}`)
  load()
}
const statuses = ['草稿', '人工预审中', '合规审核中', '主管复核中', '经理终审中', '已完成', '已驳回', '已撤回']
const dateRange = ref([])
function duration(row) {
  if (!row.createTime) return '-'
  const start = new Date(String(row.createTime).replace(' ', 'T'))
  const terminal = row.status === '已完成' || row.status === '已驳回' || row.status === '已撤回'
  const end = (terminal && row.updateTime) ? new Date(String(row.updateTime).replace(' ', 'T')) : new Date()
  const mins = Math.max(0, Math.floor((end - start) / 60000))
  return `${Math.floor(mins / 1440)}天${Math.floor((mins % 1440) / 60)}小时${terminal ? '' : '(在途)'}`
}
function exportParams() {
  return {
    assetName: q.assetName, status: q.status, rightHolder: q.rightHolder,
    createTimeStart: dateRange.value?.[0] || '', createTimeEnd: dateRange.value?.[1] ? dateRange.value[1] + ' 23:59:59' : ''
  }
}
function onExport() { window.open(confirmHistoryExportUrl(exportParams()), '_blank') }
const drawer = ref(false); const logs = ref([]); const curNo = ref('')
function fmt(t) { return t ? String(t).replace('T', ' ').slice(0, 19) : '-' }
async function onProgress(row) {
  curNo.value = row.applyNo || row.applyId
  logs.value = await getConfirmFlowLog(row.applyId) || []
  drawer.value = true
}
const q = reactive({ current: 1, size: 10, assetName: '', status: '', rightHolder: '' })
const rows = ref([]); const total = ref(0); const loading = ref(false)
function tag(s) { return { 已完成: 'success', 已驳回: 'danger', 已撤回: 'info', 草稿: 'info' }[s] || 'warning' }
function stepOf(s) { return { 草稿: 0, 人工预审中: 1, 合规审核中: 2, 主管复核中: 3, 经理终审中: 4, 已完成: 6, 已驳回: 1, 已撤回: 1 }[s] ?? 0 }
async function load() {
  loading.value = true
  const p = exportParams()
  try { const r = await pageConfirmApply({ ...q, ...p }); rows.value = r.records || []; total.value = r.total || 0 } finally { loading.value = false }
}
function onSearch() { q.current = 1; load() }
function onReset() { q.assetName = ''; q.status = ''; q.rightHolder = ''; dateRange.value = []; onSearch() }
function onDelete(row) {
  ElMessageBox.confirm(`确认删除草稿确权申请「${row.applyNo}」?`, '删除草稿', { type: 'warning' })
    .then(async () => { await deleteConfirmApply(row.applyId); ElMessage.success('已删除'); load() }).catch(() => {})
}
onMounted(load)
</script>

<style scoped>
/* 流转进度列:仅缩小步骤标题字号 + 收紧行高,让竖排标签更清爽(最小展示优化,不改结构/逻辑) */
.flow-mini :deep(.el-step__title) {
  font-size: 12px;
  line-height: 1.2;
}
/* 去掉步骤之间的箭头分隔符(simple 模式的 V 形连接符) */
.flow-mini :deep(.el-step__arrow) {
  display: none;
}
</style>
