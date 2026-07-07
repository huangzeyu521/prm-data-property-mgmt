<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
<!--
  授权申请历史查询管理:2 个 authMode Tab(批量授权 / 一事一议授权),布局对齐「确权申请查询」与「我的申请」。
  第一性:列表「一行 = 一个申请」,不是「一个库表授权项」。
    批量申请 = 1 份清单(batchListId,可跨 M 系统 × N 库表)→ 按 batchListId 聚成清单级行;
    一事一议 = 1 份申请单(formNo,1 被授权方 × 1 场景 × N 库表)→ 按 formNo 聚成申请单级行;
    展开行(表6/表5 明细)= 该申请下的多系统多库表逐项。计数 = 申请数(去重 batchListId / formNo)。
  两类流程不同(35号文 附录C):一事一议=单位初审→合规→业务→主管→经理→副总→批准(待双签)→生效;批量=合规→主管→经理→副总→领导小组→生效。
  后端无 stats 端点、单页上限 100:拉 pageAuthApply(明细)+ pageBatchList(清单头)后客户端聚合(同「我的申请」/「批量授权清单」)。
-->
<template>
  <div class="prm-page">
    <el-tabs v-model="activeTab" class="reg-tabs" @tab-change="onTabChange">
      <el-tab-pane name="批量授权" :label="`批量授权 (${tabCounts['批量授权'] || 0})`" />
      <el-tab-pane name="一事一议授权" :label="`一事一议授权 (${tabCounts['一事一议授权'] || 0})`" />
    </el-tabs>

    <div class="stat-bar">
      <div v-for="c in statCards" :key="c.key" class="stat-card" :class="[c.cls, { active: activeStat === c.key, clickable: c.click !== false }]"
        @click="c.click !== false && onStatClick(c)">
        <div class="stat-num">{{ stat[c.field] || 0 }}</div>
        <div class="stat-label">{{ c.label }}</div>
      </div>
    </div>

    <div class="prm-query-bar">
      <el-form :inline="true" @submit.prevent class="filter-row">
        <el-form-item label="数据表/系统"><el-input v-model="q.assetName" placeholder="数据表 / 系统" clearable style="width:160px" @keyup.enter="onSearch" /></el-form-item>
        <el-form-item label="被授权方"><el-input v-model="q.granteeOrg" placeholder="被授权方" clearable style="width:150px" @keyup.enter="onSearch" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="q.status" placeholder="全部" clearable style="width:130px" @change="onSearch">
            <el-option v-for="s in statuses" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="申请时间">
          <el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD" range-separator="至"
            start-placeholder="开始" end-placeholder="结束" style="width:230px" @change="onSearch" />
        </el-form-item>
        <el-form-item class="actions">
          <el-button type="primary" @click="onSearch">查询</el-button>
          <el-button @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="prm-table-card" :class="{ 'prm-maximized': maximized }">
      <div style="display:flex;justify-content:flex-end;align-items:center;gap:6px;margin-bottom:8px">
        <el-button size="small" @click="onExport">导出</el-button>
        <ColumnSettings :columns="colDefs" :visible="colVis.visible" @toggle="(p, v) => colVis.setVisible(p, v)" @reset="colVis.reset" />
        <el-button :icon="maximized ? ScaleToOriginal : FullScreen" circle size="small" :title="maximized ? '退出最大化' : '最大化'" @click="toggleMaximize" />
      </div>
      <el-table :data="pageRows" v-loading="loading" border stripe row-key="key">
        <!-- 展开行:该申请下的多系统 × 多库表逐项(表6/表5 明细) -->
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="ah-expand">
              <el-alert v-if="row.crossSystem" type="warning" :closable="false" show-icon style="margin-bottom:8px"
                :title="`本申请为跨系统域授权:覆盖 ${row.systemCount} 个系统(${row.systems.join('、')})、${row.tableCount} 张库表。`" />
              <div v-else class="ah-sub" style="margin-bottom:8px">本申请覆盖 {{ row.systemCount }} 个系统、{{ row.tableCount }} 张库表(单系统)。</div>
              <el-table :data="row.items" border size="small">
                <el-table-column type="index" label="序号" width="52" align="center" />
                <el-table-column label="所属系统" min-width="130" show-overflow-tooltip><template #default="{ row: it }">{{ sysName(it) }}</template></el-table-column>
                <el-table-column prop="assetName" label="数据表" min-width="130" show-overflow-tooltip />
                <el-table-column prop="rightType" label="权益类型" width="110" show-overflow-tooltip><template #default="{ row: it }">{{ it.rightType || '—' }}</template></el-table-column>
                <el-table-column prop="scenario" label="使用场景" min-width="120" show-overflow-tooltip><template #default="{ row: it }">{{ it.scenario || '—' }}</template></el-table-column>
                <el-table-column label="涉第三方" width="84" align="center"><template #default="{ row: it }"><span :class="'prm-c-' + (involvesThird(it) ? 'warning' : 'info')">{{ involvesThird(it) ? '涉' : '否' }}</span></template></el-table-column>
                <el-table-column label="涉隐私/商密" width="104" align="center"><template #default="{ row: it }"><span :class="'prm-c-' + (involvesSensitive(it) ? 'danger' : 'info')">{{ involvesSensitive(it) ? it.sensitiveType : '否' }}</span></template></el-table-column>
                <el-table-column prop="equityCardId" label="生效卡片" width="120" show-overflow-tooltip><template #default="{ row: it }">{{ it.equityCardId || '—' }}</template></el-table-column>
                <el-table-column prop="status" label="项状态" width="104" align="center"><template #default="{ row: it }"><span :class="'prm-c-' + (reviewTag(it.status) || 'primary')">{{ it.status }}</span></template></el-table-column>
              </el-table>
              <el-empty v-if="!row.items.length" :image-size="60" description="该申请暂无明细项" />
            </div>
          </template>
        </el-table-column>
        <el-table-column type="index" label="序号" width="52" align="center" />
        <el-table-column :label="activeTab === '批量授权' ? '清单编号' : '申请单号'" width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.no || '—' }}</template>
        </el-table-column>
        <el-table-column v-if="activeTab === '批量授权'" prop="listYear" label="年度" width="90" align="center">
          <template #default="{ row }">{{ row.listYear || '—' }}</template>
        </el-table-column>
        <el-table-column v-if="colVis.isVisible('granteeOrg')" prop="granteeOrg" label="被授权方" min-width="130" show-overflow-tooltip />
        <el-table-column v-if="activeTab === '一事一议授权' && colVis.isVisible('scenario')" prop="scenario" label="使用场景" min-width="130" show-overflow-tooltip>
          <template #default="{ row }">{{ row.scenario || '—' }}</template>
        </el-table-column>
        <el-table-column label="系统数" width="82" align="center">
          <template #default="{ row }">
            <el-tooltip :disabled="!row.systems.length" :content="row.systems.join('、')" placement="top">
              <span :class="'prm-c-' + (row.crossSystem ? 'warning' : 'primary')">{{ row.systemCount }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="库表数" width="82" align="center"><template #default="{ row }">{{ row.tableCount }}</template></el-table-column>
        <el-table-column v-if="colVis.isVisible('crossSystem')" label="跨系统域" width="90" align="center">
          <template #default="{ row }"><span :class="'prm-c-' + (row.crossSystem ? 'warning' : 'info')">{{ row.crossSystem ? '是' : '否' }}</span></template>
        </el-table-column>
        <el-table-column v-if="colVis.isVisible('review')" label="审核结果" width="92" align="center">
          <template #default="{ row }"><span :class="'prm-c-' + (reviewTag(row.status) || 'primary')">{{ reviewResult(row.status) }}</span></template>
        </el-table-column>
        <el-table-column v-if="colVis.isVisible('authState')" label="授权状态" width="92" align="center">
          <template #default="{ row }"><span :class="'prm-c-' + (authTag(row.status) || 'primary')">{{ authStatus(row.status) }}</span></template>
        </el-table-column>
        <el-table-column label="流转进度" min-width="330">
          <template #default="{ row }">
            <el-steps :active="stepOf(row)" align-center finish-status="success" simple style="margin:0" class="flow-mini">
              <el-step v-for="s in stepsOf(row)" :key="s" :title="s" />
            </el-steps>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" width="155" />
        <el-table-column label="操作" width="96" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="onProgress(row)">进度详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:20px;justify-content:flex-end" background layout="total, sizes, prev, pager, next, jumper" :page-sizes="[10, 20, 50, 100]"
        :total="total" :current-page="page" :page-size="pageSize" @current-change="p => page = p" @size-change="s => { pageSize = s; page = 1 }" />
    </div>

    <el-drawer v-model="drawer" :title="`进度跟踪 — ${curNo}`" size="46%">
      <div v-if="!logs.length" style="color:var(--prm-color-text-weak);padding:12px">暂无流转记录(草稿尚未提交)。</div>
      <el-timeline v-else style="padding:8px 6px">
        <el-timeline-item v-for="(l, i) in logs" :key="l.logId || i" :timestamp="fmt(l.createTime)" placement="top"
          :type="l.toStatus === '已驳回' ? 'danger' : (l.toStatus === '已生效' ? 'success' : 'primary')">
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
import { onMounted, reactive, ref, computed, watch } from 'vue'
import { FullScreen, ScaleToOriginal } from '@element-plus/icons-vue'
import ColumnSettings from '@/components/ColumnSettings.vue'
import { useColumnVisibility } from '@/composables/useColumnVisibility'
import { useMaximize } from '@/composables/useMaximize'
import { pageAuthApply, pageBatchList, getAuthFlowLog } from '@/api/authorize'

const statuses = ['草稿', '单位初审中', '合规审核中', '业务审核中', '主管审核中', '经理审核中', '副总审批中', '领导小组审批中', '批准', '已生效', '已驳回', '已撤回']

const colDefs = [
  { prop: 'granteeOrg', label: '被授权方' }, { prop: 'scenario', label: '使用场景' }, { prop: 'crossSystem', label: '跨系统域' },
  { prop: 'review', label: '审核结果' }, { prop: 'authState', label: '授权状态' }
]
const colVis = useColumnVisibility('auth-history', colDefs)
const { maximized, toggle: toggleMaximize } = useMaximize()

const activeTab = ref('批量授权')
function onTabChange() { q.status = ''; activeStat.value = ''; page.value = 1 }

// ===== 数据:pageAuthApply(明细项)+ pageBatchList(清单头);客户端聚合成「申请级」行 =====
const items = ref([])       // 所有 AuthApply 明细项
const batchHeads = ref([])  // 批量清单头(listNo/listYear/listStatus/itemCount)
const loading = ref(false)
async function load() {
  loading.value = true
  try {
    const [a, b] = await Promise.all([
      pageAuthApply({ current: 1, size: 100 }),
      pageBatchList({ current: 1, size: 100 })
    ])
    items.value = a.records || []
    batchHeads.value = b.records || []
  } finally { loading.value = false }
}

// 申请级状态:批量取清单头 listStatus + 明细最早在审节点;一事一议取明细代表状态。
const STATUS_RANK = { 草稿: 0, 单位初审中: 1, 合规审核中: 2, 业务审核中: 3, 主管审核中: 4, 经理审核中: 5, 副总审批中: 6, 领导小组审批中: 7, 批准: 8, 已生效: 9 }
function groupStatus(its, listStatus, mode) {
  if (mode === '批量') {
    if (listStatus === '草案') return '草稿'
    if (its.length && its.every((i) => i.status === '已生效')) return '已生效'
    if (its.some((i) => i.status === '已驳回')) return '已驳回'
    if (listStatus === '批准') return '批准'
    // 申报稿 → 落到明细最早在审节点(下方通用)
  }
  const sts = its.map((i) => i.status).filter(Boolean)
  if (!sts.length) return '草稿'
  if (sts.every((s) => s === '已生效')) return '已生效'
  if (sts.some((s) => s === '已驳回')) return '已驳回'
  if (sts.every((s) => s === '已撤回')) return '已撤回'
  if (sts.every((s) => s === '草稿')) return '草稿'
  const active = sts.filter((s) => STATUS_RANK[s] != null && !['草稿', '已生效', '已撤回'].includes(s))
  if (active.length) return active.reduce((x, y) => (STATUS_RANK[y] < STATUS_RANK[x] ? y : x))
  return sts[0]
}
function systemsOf(its) { return [...new Set(its.map(sysName).filter((s) => s && s !== '—'))] }

// 按申请单元聚合:批量→batchListId(补清单头),一事一议→formNo
const appRows = computed(() => {
  if (activeTab.value === '批量授权') {
    const byList = {}
    items.value.filter((i) => i.authMode === '批量' && i.batchListId).forEach((i) => { (byList[i.batchListId] = byList[i.batchListId] || []).push(i) })
    return batchHeads.value.map((h) => {
      const its = byList[h.batchListId] || []
      const systems = systemsOf(its)
      return {
        mode: '批量', authMode: '批量', key: h.batchListId, batchListId: h.batchListId,
        no: h.listNo, listYear: h.listYear, listStatus: h.listStatus,
        granteeOrg: (its[0] && its[0].granteeOrg) || '—', scenario: (its[0] && its[0].scenario) || '',
        systems, systemCount: systems.length, tableCount: h.itemCount != null ? h.itemCount : its.length,
        crossSystem: systems.length > 1,
        status: groupStatus(its, h.listStatus, '批量'),
        createTime: fmt(h.createTime || (its[0] && its[0].createTime)),
        items: its, applyId: its[0] && its[0].applyId, remark: h.remark
      }
    })
  }
  const byForm = {}
  items.value.filter((i) => i.authMode !== '批量').forEach((i) => { const k = i.formNo || i.applyId; (byForm[k] = byForm[k] || []).push(i) })
  return Object.entries(byForm).map(([formNo, its]) => {
    const systems = systemsOf(its)
    return {
      mode: '一事一议', authMode: '一事一议', key: formNo, formNo,
      no: formNo, granteeOrg: (its[0] && its[0].granteeOrg) || '—', scenario: (its[0] && its[0].scenario) || '',
      systems, systemCount: systems.length, tableCount: its.length, crossSystem: systems.length > 1,
      status: groupStatus(its, null, '一事一议'),
      createTime: fmt(its[0] && its[0].createTime), items: its, applyId: its[0] && its[0].applyId
    }
  })
})

// ===== 筛选 / 分页(客户端,申请级) =====
const q = reactive({ assetName: '', granteeOrg: '', status: '' })
const dateRange = ref([])
const page = ref(1); const pageSize = ref(10)
const filteredNoStatus = computed(() => appRows.value.filter((r) => {
  if (q.assetName && !(r.systems.some((s) => s.includes(q.assetName)) || r.items.some((it) => (it.assetName || '').includes(q.assetName)))) return false
  if (q.granteeOrg && !(r.granteeOrg || '').includes(q.granteeOrg)) return false
  if (dateRange.value && dateRange.value.length === 2) {
    const t = String(r.createTime || '').slice(0, 10)
    if (t < dateRange.value[0] || t > dateRange.value[1]) return false
  }
  return true
}))
const filtered = computed(() => filteredNoStatus.value.filter((r) => !q.status || r.status === q.status))
const total = computed(() => filtered.value.length)
const pageRows = computed(() => filtered.value.slice((page.value - 1) * pageSize.value, page.value * pageSize.value))
watch(() => filtered.value.length, (len) => { const mp = Math.max(1, Math.ceil(len / pageSize.value)); if (page.value > mp) page.value = mp })

// ===== 统计:Tab 计数 = 申请数(去重 batchListId / formNo)+ 概览条(当前 Tab,忽略状态) =====
const tabCounts = computed(() => ({
  批量授权: batchHeads.value.length,
  一事一议授权: new Set(items.value.filter((i) => i.authMode !== '批量').map((i) => i.formNo || i.applyId)).size
}))
function bucketOf(s) {
  if (s === '草稿') return 'draft'
  if (s === '已生效') return 'done'
  if (s === '已驳回') return 'rejected'
  if (s === '已撤回') return 'withdrawn'
  return 'inReview'
}
const stat = computed(() => {
  const s = { total: 0, draft: 0, inReview: 0, done: 0, rejected: 0, withdrawn: 0 }
  filteredNoStatus.value.forEach((r) => { s.total++; s[bucketOf(r.status)]++ })
  return s
})
const activeStat = ref('')
const statCards = computed(() => [
  { key: 'total', label: activeTab.value + '总数', field: 'total', cls: 'c-total', click: 'clear' },
  { key: 'inReview', label: '在途审批', field: 'inReview', cls: 'c-review', click: false },
  { key: 'done', label: '已生效', field: 'done', cls: 'c-done', click: { status: '已生效' } },
  { key: 'rejected', label: '已驳回', field: 'rejected', cls: 'c-reject', click: { status: '已驳回' } },
  { key: 'draft', label: '草稿', field: 'draft', cls: 'c-init', click: { status: '草稿' } }
])
function onStatClick(c) {
  if (c.click === 'clear') { q.status = ''; activeStat.value = '' }
  else { q.status = c.click.status; activeStat.value = c.key }
  page.value = 1
}
function onSearch() { activeStat.value = ''; page.value = 1 }
function onReset() { q.assetName = ''; q.granteeOrg = ''; q.status = ''; dateRange.value = []; activeStat.value = ''; page.value = 1 }

// 客户端 CSV 导出(申请级,含明细汇总)
function onExport() {
  const cols = [
    [activeTab.value === '批量授权' ? '清单编号' : '申请单号', 'no'], ['授权模式', 'mode'], ['被授权方', 'granteeOrg'],
    ['系统数', 'systemCount'], ['覆盖系统', (r) => r.systems.join('、')], ['库表数', 'tableCount'], ['跨系统域', (r) => (r.crossSystem ? '是' : '否')],
    ['审核结果', (r) => reviewResult(r.status)], ['授权状态', (r) => authStatus(r.status)], ['状态', 'status'], ['申请时间', 'createTime']
  ]
  const esc = (v) => `"${String(v == null ? '' : v).replace(/"/g, '""')}"`
  const head = cols.map((c) => esc(c[0])).join(',')
  const body = filtered.value.map((r) => cols.map((c) => esc(typeof c[1] === 'function' ? c[1](r) : r[c[1]])).join(',')).join('\n')
  const blob = new Blob(['﻿' + head + '\n' + body], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url; a.download = `授权申请历史_${activeTab.value}.csv`; a.click()
  URL.revokeObjectURL(url)
}

// ===== 分类型审批链(申请级流转进度) =====
const STEPS_BY_MODE = {
  一事一议: { labels: ['提交', '单位初审', '合规', '业务', '主管', '经理', '副总', '双签', '生效'], idx: { 草稿: 0, 单位初审中: 1, 合规审核中: 2, 业务审核中: 3, 主管审核中: 4, 经理审核中: 5, 副总审批中: 6, 批准: 7, 已生效: 9, 已驳回: 1, 已撤回: 1 } },
  批量: { labels: ['提交', '合规', '主管', '经理', '副总', '领导小组', '生效'], idx: { 草稿: 0, 合规审核中: 1, 主管审核中: 2, 经理审核中: 3, 副总审批中: 4, 领导小组审批中: 5, 批准: 6, 已生效: 7, 已驳回: 1, 已撤回: 1 } }
}
function chainOf(row) { return STEPS_BY_MODE[row.mode === '批量' ? '批量' : '一事一议'] }
function stepsOf(row) { return chainOf(row).labels }
function stepOf(row) { return chainOf(row).idx[row.status] ?? 0 }

function reviewResult(s) { return s === '草稿' ? '未提交' : (s === '已生效' || s === '批准') ? '通过' : s === '已驳回' ? '驳回' : s === '已撤回' ? '已撤回' : '审核中' }
function reviewTag(s) { return (s === '已生效' || s === '批准') ? 'success' : s === '已驳回' ? 'danger' : (s === '草稿' || s === '已撤回') ? 'info' : 'warning' }
function authStatus(s) { return s === '已生效' ? '已生效' : s === '批准' ? '双签中' : s === '已驳回' ? '未授权' : s === '已撤回' ? '已撤回' : s === '草稿' ? '—' : '待生效' }
function authTag(s) { return s === '已生效' ? 'success' : s === '批准' ? 'warning' : s === '已驳回' ? 'danger' : 'info' }

function sysName(row) {
  if (row.systemName) return row.systemName
  const a = row.assetId || ''
  return a.startsWith('SYS:') ? a.slice(4) : (a || '—')
}
function involvesThird(row) { return !!(row.thirdPartySource && String(row.thirdPartySource).trim()) }
function involvesSensitive(row) { return !!(row.sensitiveType && String(row.sensitiveType).trim() && row.sensitiveType !== '无') }

function fmt(t) { return t ? String(t).replace('T', ' ').slice(0, 19) : '' }
const drawer = ref(false); const logs = ref([]); const curNo = ref('')
async function onProgress(row) {
  curNo.value = row.no || row.key
  // 申请级:取一条明细项的流转时间线为代表(明细随申请单/清单同链推进)
  logs.value = row.applyId ? (await getAuthFlowLog(row.applyId) || []) : []
  drawer.value = true
}

onMounted(load)
</script>

<style scoped>
.reg-tabs { margin-bottom: 4px; }
.reg-tabs :deep(.el-tabs__item) { font-size: 15px; font-weight: 600; }

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

.ah-expand { padding: 10px 18px 12px 56px; background: var(--prm-color-bg); }
.ah-sub { color: var(--prm-color-text-weak); font-size: 13px; }

.filter-row { display: flex; flex-wrap: wrap; align-items: center; }
.filter-row :deep(.el-form-item) { margin-bottom: 8px; }
.filter-row .actions { margin-left: auto; }

.flow-mini :deep(.el-step__title) { font-size: 12px; line-height: 1.2; }
.flow-mini :deep(.el-step__arrow) { display: none; }
</style>
