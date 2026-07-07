<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
<!--
  我的申请(申报人"发件箱"):我提交的确权/授权申请进度 + 我自己单子的跟进(驳回重提)。
  定位区别于「统一待办中心」(审核员"收件箱":待我审批别人的单);本页不做审批待办、不做深度流转,
  深查跳对应域的申请查询页。顶部状态总览(待重提/在途/已办结)聚焦"我自己要跟的事"。
-->
<template>
  <div class="prm-page">
    <div class="prm-toolbar">
      <h3 style="margin:0 12px 0 0">我的申请</h3>
      <span class="my-sub">我提交的确权 / 授权申请进度(发件箱)。<b>待我审批别人的单</b>请见「统一待办中心」。</span>
      <el-radio-group v-model="tab" style="margin-left:auto">
        <el-radio-button v-for="t in tabs" :key="t" :value="t" :label="t">{{ t }}</el-radio-button>
      </el-radio-group>
      <el-input v-model="kw" placeholder="按资产名称筛选" clearable style="width:180px;margin-left:12px" />
      <el-checkbox v-model="onlyMine" style="margin-left:12px" @change="load">仅看我提交的</el-checkbox>
      <el-button :loading="loading" @click="load" style="margin-left:8px">刷新</el-button>
    </div>

    <!-- 我的提交·状态总览(点卡片筛选) -->
    <el-row :gutter="12" class="my-stats">
      <el-col :span="8">
        <el-card shadow="never" :class="['my-card', { on: group === '待重提' }]" @click="toggleGroup('待重提')">
          <el-statistic title="待我处理 · 草稿/驳回/撤回" :value="counts.待重提" value-style="color:var(--prm-color-danger)" />
          <div class="my-card-hint">草稿续填、驳回/撤回后修改重提</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never" :class="['my-card', { on: group === '在途' }]" @click="toggleGroup('在途')">
          <el-statistic title="在途 · 审批中" :value="counts.在途" value-style="color:var(--prm-color-warning)" />
          <div class="my-card-hint">已提交,等待逐级审批</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never" :class="['my-card', { on: group === '已办结' }]" @click="toggleGroup('已办结')">
          <el-statistic title="已办结" :value="counts.已办结" value-style="color:var(--prm-color-success)" />
          <div class="my-card-hint">已生效 / 已完成</div>
        </el-card>
      </el-col>
    </el-row>

    <div class="prm-table-card" style="margin-top:12px">
      <el-table :data="view" v-loading="loading" border stripe>
        <el-table-column type="index" label="序号" width="56" align="center" />
        <el-table-column prop="domain" label="类型" width="84" align="center">
          <template #default="{ row }"><span :class="'prm-c-' + ((row.domain === '确权' ? 'primary' : 'success') || 'primary')">{{ row.domain }}{{ row.mode === '批量' ? '·批量' : '' }}</span></template>
        </el-table-column>
        <el-table-column prop="no" label="申请编号" min-width="160" show-overflow-tooltip />
        <el-table-column prop="assetName" label="数据资产" min-width="160" show-overflow-tooltip />
        <el-table-column prop="status" label="当前状态" width="110" align="center">
          <template #default="{ row }"><span :class="'prm-c-' + ((stTag(row.status)) || 'primary')">{{ row.status }}</span></template>
        </el-table-column>
        <!-- 内联流转进度(域/类型感知,与确权/授权申请查询页一致) -->
        <el-table-column label="流转进度" min-width="320">
          <template #default="{ row }">
            <el-steps :active="stepOf(row)" align-center finish-status="success" simple style="margin:0" class="flow-mini">
              <el-step v-for="s in stepsOf(row)" :key="s" :title="s" />
            </el-steps>
          </template>
        </el-table-column>
        <el-table-column prop="rejectReason" label="驳回原因" min-width="130" show-overflow-tooltip>
          <template #default="{ row }">{{ row.status === '已驳回' ? (row.rejectReason || '—') : '—' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="提交时间" width="165" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <!-- 草稿:就地编辑续填 + 删除(确权域;PDD 8.1 草稿生命周期) -->
            <el-button v-if="isConfirmDraft(row)" link type="primary" @click="goEditDraft(row)">编辑</el-button>
            <el-button v-if="isConfirmDraft(row)" link type="danger" @click="onDelete(row)">删除</el-button>
            <el-button v-if="isConfirmInReview(row)" link type="warning" @click="onWithdraw(row)">撤回</el-button>
            <!-- 草稿:就地编辑续填 + 删除(授权域:一事一议按 formNo / 批量按 batchListId) -->
            <el-button v-if="isAuthDraft(row)" link type="primary" @click="goEditAuthDraft(row)">编辑</el-button>
            <el-button v-if="isAuthDraft(row)" link type="danger" @click="onDeleteAuth(row)">删除</el-button>
            <el-button v-if="isAuthInReview(row)" link type="warning" @click="onWithdrawAuth(row)">撤回</el-button>
            <!-- 已驳回/已撤回:修改重提(复制新单,既有行为不变) -->
            <el-button v-if="needReopen(row.status)" link type="warning" @click="goReopen(row)">修改重提</el-button>
            <el-button link type="primary" @click="goProgress(row)">查看进度</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!loading && view.length === 0" style="text-align:center;padding:32px;color:var(--prm-color-text-weak)">
        {{ group ? `「${group}」暂无申请。` : '暂无申请。' }}可前往
        <el-link type="primary" @click="$router.push('/dpr/confirm/wizard')">确权申请</el-link>、
        <el-link type="primary" @click="$router.push('/dpr/auth/wizard')">一事一议授权</el-link> 或
        <el-link type="primary" @click="$router.push('/dpr/auth/batch-wizard')">批量授权</el-link> 发起。
      </div>
    </div>

    <!-- 查看进度:页内抽屉(该申请的流转日志,域感知),不跳转 -->
    <el-drawer v-model="drawer" :title="`进度跟踪 — ${curNo}`" size="46%">
      <el-steps :active="curStep" finish-status="success" align-center class="my-flow" style="margin:4px 4px 20px">
        <el-step v-for="s in curSteps" :key="s" :title="s" />
      </el-steps>
      <div v-if="!logs.length" style="color:var(--prm-color-text-weak);padding:8px 12px">暂无明细流转记录(当前进度见上方步骤条;草稿尚未提交)。</div>
      <el-timeline v-else style="padding:8px 6px">
        <el-timeline-item v-for="(l, i) in logs" :key="l.logId || i" :timestamp="fmt(l.createTime)" placement="top"
          :type="l.toStatus === '已驳回' ? 'danger' : (['已完成', '已生效'].includes(l.toStatus) ? 'success' : 'primary')">
          <div style="font-weight:600">{{ l.nodeName || l.node || '流转' }}：{{ l.fromStatus }} → {{ l.toStatus }}</div>
          <div v-if="l.responder" style="font-size:12px;color:var(--prm-color-text-secondary);margin-top:2px">责任人：{{ l.responder }}</div>
          <div v-if="l.opinion" style="font-size:12px;color:var(--prm-color-text-secondary);margin-top:2px">意见：{{ l.opinion }}</div>
          <div v-if="l.notifyContent" class="prm-c-primary" style="font-size:12px;margin-top:4px">{{ l.pushChannel }}：{{ l.notifyContent }}</div>
        </el-timeline-item>
      </el-timeline>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageConfirmApply, getConfirmFlowLog, deleteConfirmApply, withdrawConfirm } from '@/api/confirm'
import { pageAuthApply, getAuthFlowLog, deleteAuthApply, withdrawAuth, withdrawBatchList } from '@/api/authorize'

const router = useRouter()
const route = useRoute()
const tabs = ['全部', '确权', '授权']
// 高压线§13(返回列表页保留查询条件):本页筛选态不走 useTablePage,单独记 sessionStorage,
// 原因同 useTablePage.js 的注释——App.vue 的 :key="$route.fullPath" 使同步进 URL 会被自己触发的重挂载冲掉。
const STORAGE_KEY = 'prm-list-query:' + route.path
const saved = (() => { try { return JSON.parse(sessionStorage.getItem(STORAGE_KEY) || 'null') } catch (e) { return null } })() || {}
const tab = ref(saved.tab || '全部')
const kw = ref(saved.kw || '')
const onlyMine = ref(saved.onlyMine !== undefined ? saved.onlyMine : true)
const loading = ref(false)
const rows = ref([])
const group = ref(saved.group || '') // '' | 待重提 | 在途 | 已办结
watch([tab, kw, onlyMine, group], () => {
  try { sessionStorage.setItem(STORAGE_KEY, JSON.stringify({ tab: tab.value, kw: kw.value, onlyMine: onlyMine.value, group: group.value })) } catch (e) { /* 存储不可用时静默降级 */ }
})

const me = () => localStorage.getItem('X-User-Id') || ''

// 状态 → 分组(发件箱视角:我要跟的事)。草稿=尚未提交、待我继续,归"待我处理"而非"在途·审批中"
function groupOf(s) {
  if (s === '草稿' || s === '已驳回' || s === '已撤回') return '待重提'
  if (s === '已完成' || s === '已生效') return '已办结'
  return '在途' // 审批中
}
function needReopen(s) { return s === '已驳回' || s === '已撤回' }
// 确权在审(可撤回)状态:审批链活动态,对齐后端 withdraw 门禁(flowEngine.canAdvance)
const CONFIRM_IN_REVIEW = ['人工预审中', '合规审核中', '主管复核中', '经理终审中']
function isConfirmDraft(row) { return row.domain === '确权' && row.status === '草稿' }
function isConfirmInReview(row) { return row.domain === '确权' && CONFIRM_IN_REVIEW.includes(row.status) }
// 草稿「编辑」:就地续填同一张单(?applyId,保留 applyId 走 UPDATE),区别于「修改重提」的复制新单
function goEditDraft(row) {
  router.push({ path: '/dpr/confirm/wizard', query: { applyId: row.raw?.applyId } })
}
// 草稿「删除」:仅草稿可删(后端门禁),二次确认防误删
function onDelete(row) {
  ElMessageBox.confirm(`确认删除草稿「${row.assetName || row.no}」?删除后不可恢复。`, '删除草稿', { type: 'warning' })
    .then(async () => {
      try { await deleteConfirmApply(row.raw?.applyId); ElMessage.success('草稿已删除'); load() }
      catch (e) { /* 拦截器已 toast */ }
    }).catch(() => {})
}
// 已提交/在审「撤回」:拉回本人已提交的单(后端限申请人+审批中态);撤回后归"待我处理",可修改重提
function onWithdraw(row) {
  ElMessageBox.prompt('请输入撤回原因(可空)', '撤回申请', { inputType: 'textarea', inputValue: '' })
    .then(async ({ value }) => {
      try { await withdrawConfirm(row.raw?.applyId, value || ''); ElMessage.success('已撤回,可在「待我处理」修改后重提'); load() }
      catch (e) { /* 拦截器已 toast */ }
    }).catch(() => {})
}

// ===== 授权域草稿生命周期(一事一议=表级独立流转 / 批量=清单级) =====
const AUTH_IN_REVIEW = ['单位初审中', '合规审核中', '业务审核中', '主管审核中', '经理审核中', '副总审批中', '领导小组审批中']
function isAuthDraft(row) { return row.domain === '授权' && row.status === '草稿' }
function isAuthInReview(row) { return row.domain === '授权' && AUTH_IN_REVIEW.includes(row.status) }
// 编辑:就地续填整份申请(一事一议按 formNo / 批量按 batchListId),保留原单不复制
function goEditAuthDraft(row) {
  const r = row.raw || {}
  if (row.mode === '批量') router.push({ path: '/dpr/auth/batch-wizard', query: { batchListId: r.batchListId } })
  else router.push({ path: '/dpr/auth/wizard', query: { formNo: r.formNo } })
}
// 删除草稿:一事一议删该数据表行 / 批量删该授权项(均 deleteAuthApply,仅草稿)
function onDeleteAuth(row) {
  const label = row.mode === '批量' ? '批量授权项' : '数据表'
  ElMessageBox.confirm(`确认删除草稿${label}「${row.assetName || row.no}」?删除后不可恢复。`, '删除草稿', { type: 'warning' })
    .then(async () => {
      try { await deleteAuthApply(row.raw?.applyId); ElMessage.success('草稿已删除'); load() }
      catch (e) { /* 拦截器已 toast */ }
    }).catch(() => {})
}
// 撤回:一事一议按表撤回(该表退回可重提);批量按清单撤回(申报稿→草案+明细回草稿,整批可再编辑)
function onWithdrawAuth(row) {
  const isBatch = row.mode === '批量'
  const msg = isBatch
    ? '撤回将把整份批量清单退回草案、其下明细退回草稿,可继续编辑后重新提交。确认撤回?'
    : '撤回该数据表授权申请(退回后可修改重提)。确认撤回?'
  ElMessageBox.confirm(msg, '撤回申请', { type: 'warning' })
    .then(async () => {
      try {
        if (isBatch) await withdrawBatchList(row.raw?.batchListId)
        else await withdrawAuth(row.raw?.applyId, '')
        ElMessage.success('已撤回,可在「待我处理」修改后重提'); load()
      } catch (e) { /* 拦截器已 toast */ }
    }).catch(() => {})
}

async function load() {
  loading.value = true
  try {
    const [c, a] = await Promise.all([
      pageConfirmApply({ current: 1, size: 100 }),
      pageAuthApply({ current: 1, size: 100 })
    ])
    const cf = (c.records || []).map((r) => ({ domain: '确权', mode: '', no: r.applyNo, assetName: r.assetName, status: r.status, rejectReason: r.rejectReason, createTime: r.createTime, creatorId: r.creatorId, raw: r }))
    const af = (a.records || []).map((r) => ({ domain: '授权', mode: r.authMode, no: r.applyNo, assetName: r.assetName, status: r.status, rejectReason: r.rejectReason, createTime: r.createTime, creatorId: r.creatorId, raw: r }))
    let all = [...cf, ...af]
    // "仅看我的"=严格按创建人过滤(不再"无我的记录就回退显示全部"——该回退会把他人草稿连同编辑/删除入口暴露给新用户)。
    // 后端已对草稿做 creatorId 隔离(他人草稿根本不下发);此处再严格过滤,确保"我的申请"是纯发件箱。
    // 演示态无登录用户(me() 为空)时不过滤,保留全量演示视图;取消勾选"仅看我的"亦可查看全部。
    if (onlyMine.value && me()) {
      all = all.filter((r) => r.creatorId === me())
    }
    // 待重提置顶 → 在途 → 已办结,组内按时间倒序
    const order = { 待重提: 0, 在途: 1, 已办结: 2 }
    all.sort((x, y) => (order[groupOf(x.status)] - order[groupOf(y.status)]) || String(y.createTime || '').localeCompare(String(x.createTime || '')))
    rows.value = all
  } finally { loading.value = false }
}

// 类型/资产筛选后的集合(供总览计数,与卡片分组联动)
const scoped = computed(() => rows.value.filter((r) =>
  (tab.value === '全部' || r.domain === tab.value) &&
  (!kw.value || (r.assetName || '').includes(kw.value))))
const counts = computed(() => {
  const c = { 待重提: 0, 在途: 0, 已办结: 0 }
  scoped.value.forEach((r) => { c[groupOf(r.status)]++ })
  return c
})
const view = computed(() => scoped.value.filter((r) => !group.value || groupOf(r.status) === group.value))
function toggleGroup(g) { group.value = group.value === g ? '' : g }

function stTag(s) {
  if (s === '已完成' || s === '已生效') return 'success'
  if (s === '已驳回') return 'danger'
  if (s === '已撤回' || s === '草稿') return 'info'
  return 'warning'
}

// 查看进度:页内抽屉(不跳转)。步骤条按状态推导(域/类型感知,种子无日志也有进度),flow-log 明细有则显示
const drawer = ref(false); const logs = ref([]); const curNo = ref(''); const curSteps = ref([]); const curStep = ref(0)
function fmt(t) { return t ? String(t).replace('T', ' ').slice(0, 19) : '-' }
// 步骤链与各域审批流一致:确权 / 授权·一事一议(有"业务"无"领导小组")/ 授权·批量(反之)
const STEPS = {
  确权: { labels: ['提交', '人工预审', '合规', '主管', '终审', '制卡'], idx: { 草稿: 0, 人工预审中: 1, 合规审核中: 2, 主管复核中: 3, 经理终审中: 4, 已完成: 6, 已驳回: 1, 已撤回: 1 } },
  一事一议: { labels: ['提交', '单位初审', '合规', '业务', '主管', '经理', '副总', '双签', '生效'], idx: { 草稿: 0, 单位初审中: 1, 合规审核中: 2, 业务审核中: 3, 主管审核中: 4, 经理审核中: 5, 副总审批中: 6, 批准: 7, 已生效: 9, 已驳回: 1 } },
  批量: { labels: ['提交', '合规', '主管', '经理', '副总', '领导小组', '生效'], idx: { 草稿: 0, 合规审核中: 1, 主管审核中: 2, 经理审核中: 3, 副总审批中: 4, 领导小组审批中: 5, 已生效: 7, 已驳回: 1 } }
}
function chainOf(row) { return row.domain === '确权' ? STEPS.确权 : (row.mode === '批量' ? STEPS.批量 : STEPS.一事一议) }
function stepsOf(row) { return chainOf(row).labels }
function stepOf(row) { return chainOf(row).idx[row.status] ?? 0 }
async function goProgress(row) {
  curNo.value = row.no || row.raw?.applyId || ''
  const ch = chainOf(row)
  curSteps.value = ch.labels
  curStep.value = ch.idx[row.status] ?? 0
  const api = row.domain === '确权' ? getConfirmFlowLog : getAuthFlowLog
  logs.value = (await api(row.raw?.applyId)) || []
  drawer.value = true
}
// 修改重提:暂存原单,向导预填为新申请(授权按 一事一议/批量 跳对应向导)
function goReopen(row) {
  sessionStorage.setItem('prm-reopen', JSON.stringify({ domain: row.domain, raw: row.raw || {} }))
  let base = '/dpr/confirm/wizard'
  if (row.domain === '授权') base = row.mode === '批量' ? '/dpr/auth/batch-wizard' : '/dpr/auth/wizard'
  router.push({ path: base, query: { reopen: '1' } })
}

onMounted(load)
</script>

<style scoped>
.my-sub { font-size: 12.5px; color: var(--prm-color-text-weak); }
.my-sub b { color: var(--prm-color-text-secondary); }
.my-stats { margin-top: 4px; }
.my-card { cursor: pointer; transition: border-color .15s, box-shadow .15s; }
.my-card:hover { border-color: #c0d4ff; }
.my-card.on { border-color: var(--prm-color-primary); box-shadow: 0 0 0 1px var(--prm-color-primary) inset; }
.my-card :deep(.el-card__body) { padding: 14px 18px; }
.my-card-hint { margin-top: 4px; font-size: 12px; color: var(--prm-color-text-weak); }
/* 内联流转进度:与确权/授权申请查询页一致(缩小标题、去 simple 箭头) */
.flow-mini :deep(.el-step__title) { font-size: 12px; line-height: 1.2; }
.flow-mini :deep(.el-step__arrow) { display: none; }
</style>
