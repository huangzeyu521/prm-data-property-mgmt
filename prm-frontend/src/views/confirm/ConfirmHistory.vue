<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
<!--
  确权申请查询(进度/历史):按登记类型分 2 个 Tab(初始确权 / 确权变更)。
  两类申请关注重点不同,表头列按类型定制——
    初始:权属类型 / 来源识别 A–F / 信息关联 G–J / 涉第三方(关注识别完整性);
    变更:变更触发 / 版本 vN / 基线引用 / 变更摘要(关注改了什么 + 影响)。
  共用列:系统名称 / 状态 / 流转进度 / 处理时效 / 操作。统计条按当前 Tab 作用域。
-->
<template>
  <div class="prm-page">
    <el-tabs v-model="activeTab" class="reg-tabs" @tab-change="onTabChange">
      <el-tab-pane name="初始确权" :label="`初始确权 (${statGlobal.initialCount || 0})`" />
      <el-tab-pane name="确权变更" :label="`确权变更 (${statGlobal.changeCount || 0})`" />
    </el-tabs>

    <!-- 概览统计条(作用域=当前 Tab 类型,忽略状态聚合):点选状态卡快速下钻 -->
    <div class="stat-bar">
      <div v-for="c in statCards" :key="c.key" class="stat-card" :class="[c.cls, { active: activeStat === c.key, clickable: c.click !== false }]"
        @click="c.click !== false && onStatClick(c)">
        <div class="stat-num">{{ stat[c.field] || 0 }}</div>
        <div class="stat-label">{{ c.label }}</div>
      </div>
    </div>

    <div class="prm-query-bar">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="系统名称"><el-input v-model="q.assetName" placeholder="系统名称" clearable style="width:160px" /></el-form-item>
        <el-form-item label="权属人"><el-input v-model="q.rightHolder" placeholder="人员/单位" clearable style="width:140px" /></el-form-item>
        <el-form-item v-if="activeTab === '确权变更'" label="变更触发">
          <el-select v-model="q.changeTrigger" placeholder="全部触发" clearable style="width:130px">
            <el-option v-for="t in triggerOpts" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
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
      <el-table :data="rows" v-loading="loading" border stripe @selection-change="onSel" row-key="applyId">
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="expand-detail">
              <div class="ed-item"><span class="ed-k">系统名称</span><span class="ed-v">{{ sysName(row) }}</span></div>
              <div class="ed-item"><span class="ed-k">登记类型</span><span class="ed-v">{{ row.registerType || '初始确权' }}</span></div>
              <template v-if="isChange(row)">
                <div class="ed-item"><span class="ed-k">变更触发</span><span class="ed-v">{{ row.changeTrigger || '-' }}</span></div>
                <div class="ed-item"><span class="ed-k">变更版本</span><span class="ed-v">{{ row.changeVersion ? ('v' + row.changeVersion) : '-' }}<span v-if="row.baselineRef" style="color:var(--prm-color-text-weak)"> · 基线 {{ row.baselineRef }}</span></span></div>
                <div class="ed-item ed-wide"><span class="ed-k">变更摘要</span><span class="ed-v">{{ row.changeSummary || '-' }}</span></div>
              </template>
              <div class="ed-item"><span class="ed-k">权属类型</span><span class="ed-v">
                <el-tag v-for="r in rightTags(row.rightType)" :key="r.full" :type="r.type" size="small" effect="plain" style="margin-right:4px">{{ r.short }}</el-tag>
                <span v-if="!rightTags(row.rightType).length">-</span>
              </span></div>
              <div class="ed-item"><span class="ed-k">来源识别</span><span class="ed-v">{{ row.sourceIdentification || '-' }}</span></div>
              <div class="ed-item"><span class="ed-k">信息关联</span><span class="ed-v">{{ row.relationIdentification || '-' }}</span></div>
              <div class="ed-item"><span class="ed-k">涉第三方/敏感</span><span class="ed-v">{{ row.involvesThirdParty ? '是' : '否' }}</span></div>
              <div class="ed-item"><span class="ed-k">申请模式</span><span class="ed-v">{{ row.applyMode || '常规' }}</span></div>
              <div class="ed-item"><span class="ed-k">主体层级</span><span class="ed-v">{{ row.subjectLevel || '-' }}</span></div>
              <div class="ed-item"><span class="ed-k">责任部门</span><span class="ed-v">{{ row.respDept || '-' }}</span></div>
              <div class="ed-item"><span class="ed-k">申请时间</span><span class="ed-v">{{ row.createTime || '-' }}</span></div>
              <div v-if="row.rejectReason" class="ed-item ed-wide"><span class="ed-k">驳回原因</span><span class="ed-v" style="color:var(--prm-color-danger)">{{ row.rejectReason }}</span></div>
              <div v-if="row.recognitionOpinion" class="ed-item ed-wide"><span class="ed-k">认定意见</span><span class="ed-v">{{ row.recognitionOpinion }}</span></div>
            </div>
          </template>
        </el-table-column>
        <el-table-column type="selection" width="46" :selectable="r => r.status === '草稿'" />
        <el-table-column type="index" label="序号" width="52" align="center" />
        <el-table-column prop="applyNo" label="申请编号" width="158" show-overflow-tooltip />
        <el-table-column label="系统名称" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ sysName(row) }}</template>
        </el-table-column>

        <!-- 初始确权:关注识别完整性 -->
        <template v-if="activeTab === '初始确权'">
          <el-table-column label="权属类型(三权)" width="172">
            <template #default="{ row }">
              <el-tooltip v-for="r in rightTags(row.rightType)" :key="r.full" :content="r.full" placement="top">
                <el-tag :type="r.type" size="small" effect="plain" style="margin:1px">{{ r.short }}</el-tag>
              </el-tooltip>
              <span v-if="!rightTags(row.rightType).length" style="color:var(--prm-color-text-disabled)">—</span>
            </template>
          </el-table-column>
          <el-table-column label="来源识别(A–F)" width="120">
            <template #default="{ row }">
              <el-tag v-for="c in codeList(row.sourceIdentification)" :key="c" size="small" type="primary" effect="plain" style="margin:1px">{{ c }}</el-tag>
              <span v-if="!codeList(row.sourceIdentification).length" style="color:var(--prm-color-text-disabled)">—</span>
            </template>
          </el-table-column>
          <el-table-column label="信息关联(G–J)" width="120">
            <template #default="{ row }">
              <el-tag v-for="c in codeList(row.relationIdentification)" :key="c" size="small" :type="c === 'H' ? 'danger' : 'warning'" effect="plain" style="margin:1px">{{ c }}</el-tag>
              <span v-if="!codeList(row.relationIdentification).length" style="color:var(--prm-color-text-disabled)">—</span>
            </template>
          </el-table-column>
          <el-table-column label="涉第三方" width="84" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.involvesThirdParty" type="warning" size="small" effect="plain">涉</el-tag>
              <span v-else style="color:var(--prm-color-text-disabled)">—</span>
            </template>
          </el-table-column>
        </template>

        <!-- 确权变更:关注改了什么 + 影响 -->
        <template v-else>
          <el-table-column label="变更触发" min-width="160">
            <template #default="{ row }">
              <el-tag v-for="t in triggerTags(row)" :key="t" :type="t === '数据新增' ? 'success' : 'warning'" size="small" effect="plain" style="margin:1px">{{ t }}</el-tag>
              <span v-if="!triggerTags(row).length" style="color:var(--prm-color-text-disabled)">—</span>
            </template>
          </el-table-column>
          <el-table-column label="版本" width="72" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.changeVersion" type="info" size="small" effect="plain">v{{ row.changeVersion }}</el-tag>
              <span v-else style="color:var(--prm-color-text-disabled)">—</span>
            </template>
          </el-table-column>
          <el-table-column prop="baselineRef" label="基线引用" width="148" show-overflow-tooltip>
            <template #default="{ row }">{{ row.baselineRef || '—' }}</template>
          </el-table-column>
          <el-table-column prop="changeSummary" label="变更摘要" min-width="220" show-overflow-tooltip>
            <template #default="{ row }">{{ row.changeSummary || '—' }}</template>
          </el-table-column>
        </template>

        <el-table-column prop="status" label="状态" width="104" align="center">
          <template #default="{ row }"><el-tag :type="tag(row.status)">{{ row.status }}</el-tag></template>
        </el-table-column>
        <el-table-column label="流转进度" min-width="300">
          <template #default="{ row }">
            <el-steps :active="stepOf(row)" align-center finish-status="success"
              :process-status="row.status === '已驳回' ? 'error' : 'process'" simple style="margin:0" class="flow-mini">
              <el-step title="提交" /><el-step title="人工预审" /><el-step title="合规" /><el-step title="主管" /><el-step title="终审" /><el-step title="制卡" />
            </el-steps>
          </template>
        </el-table-column>
        <el-table-column label="处理时效" width="124">
          <template #default="{ row }">{{ duration(row) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="158" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="onProgress(row)">进度详情</el-button>
            <el-button v-if="row.status === '草稿'" link type="danger" @click="onDelete(row)">删除</el-button>
            <el-button v-if="IN_REVIEW.includes(row.status)" link type="warning" @click="onWithdraw(row)">撤回</el-button>
            <el-button v-if="row.status === '已撤回'" link type="primary" @click="onReopen(row)">重新编辑提交</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:16px;justify-content:flex-end" background layout="total, sizes, prev, pager, next, jumper" :page-sizes="[10, 20, 50, 100]"
        :total="total" :current-page="q.current" :page-size="q.size" @current-change="p=>{q.current=p;load()}" @size-change="s=>{q.size=s;q.current=1;load()}" />
    </div>

    <el-drawer v-model="drawer" :title="`进度跟踪 — ${curNo}`" size="46%">
      <div v-if="!logs.length" style="color:var(--prm-color-text-weak);padding:12px">暂无流转记录(草稿尚未提交)。</div>
      <el-timeline v-else style="padding:8px 6px">
        <el-timeline-item v-for="l in logs" :key="l.logId" :timestamp="fmt(l.createTime)" placement="top"
          :type="l.toStatus === '已驳回' ? 'danger' : (l.toStatus === '已完成' ? 'success' : 'primary')">
          <div style="font-weight:600">{{ l.nodeName }}{{ l.node ? '（节点' + l.node + '）' : '' }}：{{ l.fromStatus }} → {{ l.toStatus }}</div>
          <div style="font-size:12px;color:var(--prm-color-text-secondary);margin-top:2px">责任人：{{ l.responder || '-' }}</div>
          <div v-if="l.opinion" style="font-size:12px;color:var(--prm-color-text-secondary);margin-top:2px">意见：{{ l.opinion }}</div>
          <div style="font-size:12px;color:#1e87f0;margin-top:4px"><el-icon style="vertical-align:-2px"><Bell /></el-icon> {{ l.pushChannel }}：{{ l.notifyContent }}</div>
        </el-timeline-item>
      </el-timeline>
    </el-drawer>
  </div>
</template>
<script setup>
import { onMounted, reactive, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageConfirmApply, statsConfirmApply, deleteConfirmApply, withdrawConfirm, getConfirmFlowLog, confirmHistoryExportUrl, batchSubmitConfirm, confirmSummaryExportUrl, equityConsolidationExportUrl } from '@/api/confirm'

const router = useRouter()
const IN_REVIEW = ['人工预审中', '合规审核中', '主管复核中', '经理终审中']
const statuses = ['草稿', '人工预审中', '合规审核中', '主管复核中', '经理终审中', '已完成', '已驳回', '已撤回']
const triggerOpts = ['数据新增', '数据来源变更', '管理要求变更', '权益到期', '其他']

// ===== 登记类型 Tab(2 个:初始确权 / 确权变更)=====
const activeTab = ref('初始确权')
function onTabChange() {
  q.status = ''; q.changeTrigger = ''; activeStat.value = ''; q.current = 1
  load()
}

// 一份确权申请 = 一个系统:系统名称权威来源为 assetId「SYS:<系统名>」;assetName 仅旧单卡兜底
function sysName(row) {
  const id = row.assetId || ''
  return id.startsWith('SYS:') ? id.slice(4) : (row.assetName || '-')
}
function isChange(row) { return row.registerType === '确权变更' || row.reConfirm === true }
function triggerTags(row) { return String(row.changeTrigger || '').split(/[、,，]/).map(s => s.trim()).filter(Boolean) }
function codeList(s) { return String(s || '').split(/[、,，]/).map(x => x.trim()).filter(Boolean) }
// 权属类型 = 三权多选(持有/使用/经营权可并存):rightType 为「、」拼接串,拆成多标签(短名+全名悬浮)
const RIGHT_MAP = {
  数据资源持有权: { short: '持有权', type: 'primary' },
  数据加工使用权: { short: '使用权', type: 'success' },
  数据产品经营权: { short: '经营权', type: 'warning' }
}
function rightTags(rightType) {
  return String(rightType || '').split(/[、,，]/).map(s => s.trim()).filter(Boolean)
    .map(full => RIGHT_MAP[full] ? { ...RIGHT_MAP[full], full } : { short: full, type: 'info', full })
}

// ===== 统计 =====
// statGlobal:全量(两类)→ Tab 标签计数;stat:当前 Tab 作用域 → 概览条
const statGlobal = reactive({ initialCount: 0, changeCount: 0 })
const stat = reactive({ total: 0, draft: 0, inReview: 0, done: 0, rejected: 0, withdrawn: 0 })
const activeStat = ref('')
const statCards = computed(() => [
  { key: 'total', label: activeTab.value + '总数', field: 'total', cls: 'c-total', click: 'clear' },
  { key: 'inReview', label: '在途审批', field: 'inReview', cls: 'c-review', click: false },
  { key: 'done', label: '已完成', field: 'done', cls: 'c-done', click: { status: '已完成' } },
  { key: 'rejected', label: '已驳回', field: 'rejected', cls: 'c-reject', click: { status: '已驳回' } },
  { key: 'draft', label: '草稿', field: 'draft', cls: 'c-init', click: { status: '草稿' } }
])
function onStatClick(c) {
  if (c.click === 'clear') { q.status = ''; activeStat.value = '' }
  else { q.status = c.click.status; activeStat.value = c.key }
  q.current = 1
  load()
}

const dateRange = ref([])
function duration(row) {
  if (!row.createTime) return '-'
  const start = new Date(String(row.createTime).replace(' ', 'T'))
  const terminal = row.status === '已完成' || row.status === '已驳回' || row.status === '已撤回'
  const end = (terminal && row.updateTime) ? new Date(String(row.updateTime).replace(' ', 'T')) : new Date()
  const mins = Math.max(0, Math.floor((end - start) / 60000))
  return `${Math.floor(mins / 1440)}天${Math.floor((mins % 1440) / 60)}小时${terminal ? '' : '(在途)'}`
}
// 当前 Tab 的过滤(含 registerType=当前类型);全量过滤(供 Tab 标签计数,不含 registerType)
function tabParams() {
  return {
    assetName: q.assetName, status: q.status, rightHolder: q.rightHolder,
    registerType: activeTab.value, changeTrigger: activeTab.value === '确权变更' ? q.changeTrigger : '',
    createTimeStart: dateRange.value?.[0] || '', createTimeEnd: dateRange.value?.[1] ? dateRange.value[1] + ' 23:59:59' : ''
  }
}
function globalParams() {
  return {
    assetName: q.assetName, rightHolder: q.rightHolder,
    createTimeStart: dateRange.value?.[0] || '', createTimeEnd: dateRange.value?.[1] ? dateRange.value[1] + ' 23:59:59' : ''
  }
}
function onExport() { window.open(confirmHistoryExportUrl(tabParams()), '_blank') }
function onExportSummary() { window.open(confirmSummaryExportUrl(), '_blank') }
function onExportEquity() { window.open(equityConsolidationExportUrl(), '_blank') }

const drawer = ref(false); const logs = ref([]); const curNo = ref('')
function fmt(t) { return t ? String(t).replace('T', ' ').slice(0, 19) : '-' }
async function onProgress(row) {
  curNo.value = row.applyNo || row.applyId
  logs.value = await getConfirmFlowLog(row.applyId) || []
  drawer.value = true
}

const draftSel = ref([])
function onSel(sel) { draftSel.value = sel.filter(r => r.status === '草稿') }
async function onBatchSubmit() {
  const ids = draftSel.value.map(r => r.applyId)
  const r = await batchSubmitConfirm(ids)
  ElMessage[r.failed ? 'warning' : 'success'](`批量提交:成功 ${r.success}/${r.total}${r.failed ? '，失败 ' + r.failed : ''}`)
  load()
}

async function onWithdraw(row) {
  try {
    const { value } = await ElMessageBox.prompt(`确认撤回确权申请「${row.applyNo}」?撤回后可重新编辑提交`, '撤回申请', { inputType: 'textarea', inputPlaceholder: '撤回原因(可空)' })
    await withdrawConfirm(row.applyId, value || '')
    ElMessage.success('已撤回,可在「已撤回」项重新编辑提交')
    load()
  } catch (e) { /* 取消 */ }
}
function onReopen(row) {
  sessionStorage.setItem('prm-reopen', JSON.stringify({ domain: '确权', raw: row }))
  const path = isChange(row) ? '/dpr/confirm/change' : '/dpr/confirm/wizard'
  router.push({ path, query: { reopen: 1 } })
}
function onDelete(row) {
  ElMessageBox.confirm(`确认删除草稿确权申请「${row.applyNo}」?`, '删除草稿', { type: 'warning' })
    .then(async () => { await deleteConfirmApply(row.applyId); ElMessage.success('已删除'); load() }).catch(() => {})
}

const q = reactive({ current: 1, size: 10, assetName: '', status: '', rightHolder: '', changeTrigger: '' })
const rows = ref([]); const total = ref(0); const loading = ref(false)
function tag(s) { return { 已完成: 'success', 已驳回: 'danger', 已撤回: 'info', 草稿: 'info' }[s] || 'warning' }
const NODE_STEP = { 草稿: 0, 人工预审中: 1, 合规审核中: 2, 主管复核中: 3, 经理终审中: 4, 已完成: 6, 已撤回: 1 }
function stepOf(row) {
  if (row.status === '已驳回') { return ({ 40: 1, 50: 2, 60: 3, 70: 4 }[row.currentNode]) || 1 }
  return NODE_STEP[row.status] ?? 0
}

async function load() {
  loading.value = true
  try {
    const [r, sg, st] = await Promise.all([
      pageConfirmApply({ ...q, ...tabParams() }),
      statsConfirmApply(globalParams()),
      statsConfirmApply(tabParams())
    ])
    rows.value = r.records || []; total.value = r.total || 0
    Object.assign(statGlobal, sg || {})
    Object.assign(stat, st || {})
  } finally { loading.value = false }
}
function onSearch() { q.current = 1; activeStat.value = ''; load() }
function onReset() {
  q.assetName = ''; q.status = ''; q.rightHolder = ''; q.changeTrigger = ''
  dateRange.value = []; activeStat.value = ''; q.current = 1; load()
}
onMounted(load)
</script>

<style scoped>
.reg-tabs { margin-bottom: 4px; }
.reg-tabs :deep(.el-tabs__item) { font-size: 15px; font-weight: 600; }

/* 概览统计条:左强调色边 + 数字层级,点选下钻 */
.stat-bar { display: flex; gap: 12px; margin-bottom: 14px; flex-wrap: wrap; }
.stat-card {
  flex: 1; min-width: 132px; background: #fff; border: 1px solid var(--prm-color-bg); border-left: 3px solid var(--prm-color-text-disabled);
  border-radius: 6px; padding: 12px 16px; transition: box-shadow .15s, transform .15s;
}
.stat-card.clickable { cursor: pointer; }
.stat-card.clickable:hover { box-shadow: 0 2px 10px rgba(18, 108, 253, .1); transform: translateY(-1px); }
.stat-card.active { box-shadow: 0 0 0 2px var(--prm-color-primary) inset; }
.stat-num { font-size: 26px; font-weight: 700; line-height: 1.1; color: var(--prm-color-text); }
.stat-label { font-size: 12.5px; color: var(--prm-color-text-weak); margin-top: 4px; }
.c-total { border-left-color: var(--prm-color-primary); }
.c-review { border-left-color: var(--prm-color-warning); }
.c-done { border-left-color: var(--prm-color-success); }
.c-reject { border-left-color: var(--prm-color-danger); }
.c-init { border-left-color: var(--prm-color-primary); }

/* 展开详情:键值网格 */
.expand-detail { display: flex; flex-wrap: wrap; gap: 6px 28px; padding: 10px 18px 12px 56px; background: var(--prm-color-bg); }
.ed-item { display: flex; gap: 8px; min-width: 220px; font-size: 13px; line-height: 1.7; }
.ed-item.ed-wide { flex-basis: 100%; }
.ed-k { color: var(--prm-color-text-weak); min-width: 78px; }
.ed-v { color: var(--prm-color-text-secondary); }

/* 流转进度列:缩小步骤标题字号 + 收紧行高,去 simple 模式 V 形连接符 */
.flow-mini :deep(.el-step__title) { font-size: 12px; line-height: 1.2; }
.flow-mini :deep(.el-step__arrow) { display: none; }
</style>
