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
          <el-statistic title="已驳回 · 待我重提" :value="counts.待重提" value-style="color:var(--prm-color-danger)" />
          <div class="my-card-hint">需我修改后重新提交</div>
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
        <el-table-column prop="domain" label="类型" width="84" align="center">
          <template #default="{ row }"><el-tag :type="row.domain === '确权' ? 'primary' : 'success'" effect="plain">{{ row.domain }}{{ row.mode === '批量' ? '·批量' : '' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="no" label="申请编号" min-width="160" show-overflow-tooltip />
        <el-table-column prop="assetName" label="数据资产" min-width="160" show-overflow-tooltip />
        <el-table-column prop="status" label="当前状态" width="110" align="center">
          <template #default="{ row }"><el-tag :type="stTag(row.status)">{{ row.status }}</el-tag></template>
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
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
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
          <div v-if="l.notifyContent" style="font-size:12px;color:#1e87f0;margin-top:4px">{{ l.pushChannel }}：{{ l.notifyContent }}</div>
        </el-timeline-item>
      </el-timeline>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { pageConfirmApply, getConfirmFlowLog } from '@/api/confirm'
import { pageAuthApply, getAuthFlowLog } from '@/api/authorize'

const router = useRouter()
const tabs = ['全部', '确权', '授权']
const tab = ref('全部')
const kw = ref('')
const onlyMine = ref(true)
const loading = ref(false)
const rows = ref([])
const group = ref('') // '' | 待重提 | 在途 | 已办结

const me = () => localStorage.getItem('X-User-Id') || ''

// 状态 → 分组(发件箱视角:我要跟的事)
function groupOf(s) {
  if (s === '已驳回' || s === '已撤回') return '待重提'
  if (s === '已完成' || s === '已生效') return '已办结'
  return '在途' // 草稿 + 审批中
}
function needReopen(s) { return s === '已驳回' || s === '已撤回' }

async function load() {
  loading.value = true
  try {
    const [c, a] = await Promise.all([
      pageConfirmApply({ current: 1, size: 200 }),
      pageAuthApply({ current: 1, size: 200 })
    ])
    const cf = (c.records || []).map((r) => ({ domain: '确权', mode: '', no: r.applyNo, assetName: r.assetName, status: r.status, rejectReason: r.rejectReason, createTime: r.createTime, creatorId: r.creatorId, raw: r }))
    const af = (a.records || []).map((r) => ({ domain: '授权', mode: r.authMode, no: r.applyNo, assetName: r.assetName, status: r.status, rejectReason: r.rejectReason, createTime: r.createTime, creatorId: r.creatorId, raw: r }))
    let all = [...cf, ...af]
    // "仅我的"智能回退:仅当确有归属于我的申请时才过滤;种子/历史数据 creatorId 为空(无创建人)
    // 则回退显示全部,避免"我的申请"空白、与各域查询页不对齐。真实提交(后端填充创建人)则正确只看自己的。
    if (onlyMine.value && me()) {
      const mine = all.filter((r) => r.creatorId === me())
      if (mine.length) all = mine
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
  一事一议: { labels: ['提交', '合规', '业务', '主管', '经理', '副总', '生效'], idx: { 草稿: 0, 合规审核中: 1, 业务审核中: 2, 主管审核中: 3, 经理审核中: 4, 副总审批中: 5, 已生效: 7, 已驳回: 1 } },
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
.my-card.on { border-color: #1e87f0; box-shadow: 0 0 0 1px #1e87f0 inset; }
.my-card :deep(.el-card__body) { padding: 14px 18px; }
.my-card-hint { margin-top: 4px; font-size: 12px; color: var(--prm-color-text-weak); }
/* 内联流转进度:与确权/授权申请查询页一致(缩小标题、去 simple 箭头) */
.flow-mini :deep(.el-step__title) { font-size: 12px; line-height: 1.2; }
.flow-mini :deep(.el-step__arrow) { display: none; }
</style>
