<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
<!--
  我的申请(申报人"发件箱"):我提交的确权/授权申请进度 + 跟进(草稿续填/驳回撤回重提)。
  4 Tab:初始确权/确权变更(确权域按 registerType 拆) + 批量授权/一事一议授权(授权域按 authMode 拆)。
  版式对齐「确权申请查询」:① 状态统计卡 → ② 筛选行(系统名称/状态/申请时间) → ③ 表格(导出/列设置/最大化 + 分页)。
  数据严格限本人(creatorId);「确权申请查询」「授权申请历史查询管理」两页服务其他角色,不受影响。
-->
<template>
  <div class="prm-page">
    <div class="prm-toolbar">
      <h3 style="margin:0 12px 0 0">我的申请</h3>
      <span class="my-sub">我提交的确权 / 授权申请进度(发件箱)。<b>待我审批别人的单</b>请见「统一待办中心」。</span>
      <el-checkbox v-model="onlyMine" style="margin-left:auto" @change="load">仅看我提交的</el-checkbox>
      <el-button :loading="loading" @click="load" style="margin-left:12px">刷新</el-button>
    </div>

    <el-tabs v-model="activeTab" class="reg-tabs" @tab-change="onTabChange">
      <el-tab-pane v-for="t in TABS" :key="t" :name="t" :label="`${t} (${tabCounts[t] || 0})`" />
    </el-tabs>

    <!-- ① 状态统计卡(作用域=当前 Tab + 系统名称/时间 筛选,忽略状态;点选下钻) -->
    <div class="stat-bar">
      <div v-for="c in statCards" :key="c.key" class="stat-card" :class="[c.cls, { active: activeStat === c.key, clickable: c.click !== false }]"
        @click="c.click !== false && onStatClick(c)">
        <div class="stat-num">{{ stat[c.field] || 0 }}</div>
        <div class="stat-label">{{ c.label }}</div>
      </div>
    </div>

    <!-- ② 筛选行(系统名称 / 状态 / 申请时间;查询·重置右置) -->
    <div class="prm-query-bar">
      <el-form :inline="true" @submit.prevent class="filter-row">
        <el-form-item label="系统名称"><el-input v-model="kw" placeholder="系统名称" clearable style="width:160px" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="status" placeholder="全部" clearable style="width:130px" @change="page = 1">
            <el-option v-for="s in statusOpts" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="申请时间">
          <el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD" range-separator="至"
            start-placeholder="开始" end-placeholder="结束" style="width:230px" @change="page = 1" />
        </el-form-item>
        <el-form-item class="actions">
          <el-button type="primary" @click="page = 1">查询</el-button>
          <el-button @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- ③ 表格卡(导出 / 列设置 / 最大化 + 分页) -->
    <div class="prm-table-card" :class="{ 'prm-maximized': maximized }">
      <div style="display:flex;justify-content:flex-end;align-items:center;gap:6px;margin-bottom:8px">
        <el-button size="small" @click="onExport">导出</el-button>
        <ColumnSettings :columns="colDefs" :visible="colVis.visible" @toggle="(p, v) => colVis.setVisible(p, v)" @reset="colVis.reset" />
        <el-button :icon="maximized ? ScaleToOriginal : FullScreen" circle size="small" :title="maximized ? '退出最大化' : '最大化'" @click="toggleMaximize" />
      </div>
      <el-table :key="activeTab" :data="pageRows" v-loading="loading" border stripe>
        <el-table-column type="index" label="序号" width="56" align="center" :index="(i) => (page - 1) * pageSize + i + 1" />
        <el-table-column prop="no" label="申请编号" min-width="160" show-overflow-tooltip />

        <!-- 初始确权:对齐「确权申请查询」初始 Tab -->
        <template v-if="activeTab === '初始确权'">
          <el-table-column v-if="colVis.isVisible('sysName')" label="系统名称" min-width="150" show-overflow-tooltip>
            <template #default="{ row }">{{ confirmSysName(row.raw) }}</template>
          </el-table-column>
          <el-table-column v-if="colVis.isVisible('rightType')" label="权属类型(三权)" width="172">
            <template #default="{ row }">
              <el-tooltip v-for="r in rightTags(row.raw.rightType)" :key="r.full" :content="r.full" placement="top">
                <span style="margin:1px" :class="'prm-c-' + (r.type || 'primary')">{{ r.short }}</span>
              </el-tooltip>
              <span v-if="!rightTags(row.raw.rightType).length" style="color:var(--prm-color-text-disabled)">—</span>
            </template>
          </el-table-column>
          <el-table-column v-if="colVis.isVisible('sourceIdent')" width="120">
            <template #header>
              <span>来源识别(A–F)</span>
              <el-tooltip placement="top">
                <template #content>
                  <div style="font-weight:600;margin-bottom:2px">数据来源权益识别(对齐表1)</div>
                  <div v-for="o in SOURCE_CODES" :key="o.v" style="line-height:1.7">{{ o.v }} {{ o.label }}</div>
                </template>
                <el-icon class="col-help"><QuestionFilled /></el-icon>
              </el-tooltip>
            </template>
            <template #default="{ row }">
              <el-tooltip v-for="c in codeList(row.raw.sourceIdentification)" :key="c" :content="identText(c, SOURCE_MAP)" placement="top">
                <span style="margin:1px;cursor:help" class="prm-c-primary">{{ c }}</span>
              </el-tooltip>
              <span v-if="!codeList(row.raw.sourceIdentification).length" style="color:var(--prm-color-text-disabled)">—</span>
            </template>
          </el-table-column>
          <el-table-column v-if="colVis.isVisible('relationIdent')" width="120">
            <template #header>
              <span>信息关联(G–J)</span>
              <el-tooltip placement="top">
                <template #content>
                  <div style="font-weight:600;margin-bottom:2px">信息关联权益识别(对齐表1)</div>
                  <div v-for="o in RELATION_CODES" :key="o.v" style="line-height:1.7">{{ o.v }} {{ o.label }}</div>
                </template>
                <el-icon class="col-help"><QuestionFilled /></el-icon>
              </el-tooltip>
            </template>
            <template #default="{ row }">
              <el-tooltip v-for="c in codeList(row.raw.relationIdentification)" :key="c" :content="identText(c, RELATION_MAP)" placement="top">
                <span style="margin:1px;cursor:help" :class="'prm-c-' + (c === 'H' ? 'danger' : 'warning')">{{ c }}</span>
              </el-tooltip>
              <span v-if="!codeList(row.raw.relationIdentification).length" style="color:var(--prm-color-text-disabled)">—</span>
            </template>
          </el-table-column>
          <el-table-column v-if="colVis.isVisible('thirdParty')" label="涉第三方" width="84" align="center">
            <template #default="{ row }">
              <span v-if="row.raw.involvesThirdParty" class="prm-c-warning">涉</span>
              <span v-else style="color:var(--prm-color-text-disabled)">—</span>
            </template>
          </el-table-column>
          <el-table-column v-if="colVis.isVisible('status')" label="状态" width="104" align="center">
            <template #default="{ row }"><span :class="'prm-c-' + (stTag(row.status) || 'primary')">{{ row.status }}</span></template>
          </el-table-column>
          <el-table-column v-if="colVis.isVisible('duration')" label="处理时效" width="124"><template #default="{ row }">{{ duration(row.raw) }}</template></el-table-column>
        </template>

        <!-- 确权变更:对齐「确权申请查询」变更 Tab -->
        <template v-else-if="activeTab === '确权变更'">
          <el-table-column v-if="colVis.isVisible('sysName')" label="系统名称" min-width="150" show-overflow-tooltip>
            <template #default="{ row }">{{ confirmSysName(row.raw) }}</template>
          </el-table-column>
          <el-table-column v-if="colVis.isVisible('changeTrigger')" label="变更触发" min-width="150" show-overflow-tooltip>
            <template #default="{ row }">
              <span v-for="t in codeList(row.raw.changeTrigger)" :key="t" style="margin:1px" :class="'prm-c-' + (t === '数据新增' ? 'success' : 'warning')">{{ t }}</span>
              <span v-if="!codeList(row.raw.changeTrigger).length" style="color:var(--prm-color-text-disabled)">—</span>
            </template>
          </el-table-column>
          <el-table-column v-if="colVis.isVisible('version')" label="版本" width="72" align="center">
            <template #default="{ row }">{{ row.raw.changeVersion ? ('v' + row.raw.changeVersion) : '—' }}</template>
          </el-table-column>
          <el-table-column v-if="colVis.isVisible('baselineRef')" label="基线引用" width="148" show-overflow-tooltip>
            <template #default="{ row }">{{ row.raw.baselineRef || '—' }}</template>
          </el-table-column>
          <el-table-column v-if="colVis.isVisible('changeSummary')" label="变更摘要" min-width="220" show-overflow-tooltip>
            <template #default="{ row }">{{ row.raw.changeSummary || '—' }}</template>
          </el-table-column>
          <el-table-column v-if="colVis.isVisible('status')" label="状态" width="104" align="center">
            <template #default="{ row }"><span :class="'prm-c-' + (stTag(row.status) || 'primary')">{{ row.status }}</span></template>
          </el-table-column>
          <el-table-column v-if="colVis.isVisible('duration')" label="处理时效" width="124"><template #default="{ row }">{{ duration(row.raw) }}</template></el-table-column>
        </template>

        <!-- 批量授权 / 一事一议授权:改造自「授权申请历史查询管理」(去申请人/模式列) -->
        <template v-else>
          <el-table-column v-if="colVis.isVisible('sysName')" label="所属系统" min-width="120" show-overflow-tooltip>
            <template #default="{ row }">{{ authSysName(row.raw) }}</template>
          </el-table-column>
          <el-table-column v-if="colVis.isVisible('assetName')" prop="assetName" label="数据表" min-width="130" show-overflow-tooltip />
          <el-table-column v-if="colVis.isVisible('rightType')" label="权益类型" width="120" show-overflow-tooltip>
            <template #default="{ row }">{{ row.raw.rightType || '—' }}</template>
          </el-table-column>
          <el-table-column v-if="colVis.isVisible('granteeOrg')" label="被授权方" min-width="120" show-overflow-tooltip>
            <template #default="{ row }">{{ row.raw.granteeOrg || '—' }}</template>
          </el-table-column>
          <el-table-column v-if="colVis.isVisible('reviewResult')" label="审核结果" width="92" align="center">
            <template #default="{ row }"><span :class="'prm-c-' + (reviewTag(row.status) || 'primary')">{{ reviewResult(row.status) }}</span></template>
          </el-table-column>
          <el-table-column v-if="colVis.isVisible('authStatus')" label="授权状态" width="92" align="center">
            <template #default="{ row }"><span :class="'prm-c-' + (authTag(row.status) || 'primary')">{{ authStatus(row.status) }}</span></template>
          </el-table-column>
          <el-table-column v-if="colVis.isVisible('processOpinion')" label="处理意见" min-width="130" show-overflow-tooltip>
            <template #default="{ row }">{{ row.rejectReason || '—' }}</template>
          </el-table-column>
          <el-table-column v-if="colVis.isVisible('createTime')" prop="createTime" label="申请时间" width="155" />
        </template>

        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <!-- 申报人操作仅对本人单据可见(上帝视角看全部时,他人单只可「查看进度」) -->
            <el-button v-if="isMine(row) && isConfirmDraft(row)" link type="primary" @click="goEditDraft(row)">编辑</el-button>
            <el-button v-if="isMine(row) && isConfirmDraft(row)" link type="danger" @click="onDelete(row)">删除</el-button>
            <el-button v-if="isMine(row) && isConfirmInReview(row)" link type="warning" @click="onWithdraw(row)">撤回</el-button>
            <el-button v-if="isMine(row) && isAuthDraft(row)" link type="primary" @click="goEditAuthDraft(row)">编辑</el-button>
            <el-button v-if="isMine(row) && isAuthDraft(row)" link type="danger" @click="onDeleteAuth(row)">删除</el-button>
            <el-button v-if="isMine(row) && isAuthInReview(row)" link type="warning" @click="onWithdrawAuth(row)">撤回</el-button>
            <el-button v-if="isMine(row) && needReopen(row.status)" link type="warning" @click="goReopen(row)">修改重提</el-button>
            <el-button link type="primary" @click="goProgress(row)">查看进度</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <div style="padding:8px;color:var(--prm-color-text-weak)">
            「{{ activeTab }}」{{ status || (dateRange && dateRange.length) || kw ? '无符合条件的申请。' : '暂无申请。' }}可前往
            <el-link type="primary" @click="$router.push('/dpr/confirm/wizard')">确权申请</el-link>、
            <el-link type="primary" @click="$router.push('/dpr/auth/wizard')">一事一议授权</el-link> 或
            <el-link type="primary" @click="$router.push('/dpr/auth/batch-wizard')">批量授权</el-link> 发起。
          </div>
        </template>
      </el-table>

      <el-pagination v-if="filtered.length" style="margin-top:16px;justify-content:flex-end" background
        layout="total, sizes, prev, pager, next, jumper" :page-sizes="[10, 20, 50, 100]"
        :total="filtered.length" :current-page="page" :page-size="pageSize"
        @current-change="(p) => (page = p)" @size-change="(s) => { pageSize = s; page = 1 }" />
    </div>

    <!-- 查看进度:页内抽屉(域/模式感知步骤条 + 流转时间线) -->
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
import { FullScreen, ScaleToOriginal } from '@element-plus/icons-vue'
import { pageConfirmApply, getConfirmFlowLog, deleteConfirmApply, withdrawConfirm } from '@/api/confirm'
import { pageAuthApply, getAuthFlowLog, deleteAuthApply, withdrawAuth, withdrawBatchList } from '@/api/authorize'
import { currentUser } from '@/api/auth'
import { SOURCE_CODES, RELATION_CODES, SOURCE_MAP, RELATION_MAP, identText } from '@/lib/identCodes'
import ColumnSettings from '@/components/ColumnSettings.vue'
import { useColumnVisibility } from '@/composables/useColumnVisibility'
import { useMaximize } from '@/composables/useMaximize'

const router = useRouter()
const route = useRoute()
const TABS = ['初始确权', '确权变更', '批量授权', '一事一议授权']
const STORAGE_KEY = 'prm-list-query:' + route.path
const saved = (() => { try { return JSON.parse(sessionStorage.getItem(STORAGE_KEY) || 'null') } catch (e) { return null } })() || {}
const activeTab = ref(TABS.includes(saved.activeTab) ? saved.activeTab : '初始确权')
const kw = ref(saved.kw || '')
const status = ref('')
const dateRange = ref([])
// 上帝视角(超级 all / 管理员 admin)默认"看全部"(总览所有人提交);申报人/业务默认"仅看本人发件箱"。
// 用登录账号真实角色判定(prm-user.role,不受顶栏"角色视图"切换影响);onlyMine 不持久化,始终按登录角色初始化。
const GOD_ROLES = ['all', 'admin']
const isGodView = GOD_ROLES.includes((currentUser() || {}).role || '')
const onlyMine = ref(!isGodView)
const activeStat = ref('')
const page = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const rows = ref([])
watch([activeTab, kw], () => {
  try { sessionStorage.setItem(STORAGE_KEY, JSON.stringify({ activeTab: activeTab.value, kw: kw.value })) } catch (e) { /* 静默降级 */ }
})

const me = () => localStorage.getItem('X-User-Id') || ''
// 本人单据判定:未登录(演示态)视为本人;登录后仅本人(creatorId 匹配)可编辑/删除/撤回/重提。
function isMine(row) { return !me() || row.creatorId === me() }

// ===== 域 / Tab 归属 =====
const domain = computed(() => (activeTab.value === '初始确权' || activeTab.value === '确权变更') ? '确权' : '授权')
function isChangeRow(r) { return r.domain === '确权' && (r.raw?.registerType === '确权变更' || r.raw?.reConfirm === true) }
function tabOf(r) {
  if (r.domain === '确权') return isChangeRow(r) ? '确权变更' : '初始确权'
  return r.mode === '批量' ? '批量授权' : '一事一议授权'
}

// 状态口径(域相关)
const CONFIRM_STATUS = ['草稿', '人工预审中', '合规审核中', '主管复核中', '经理终审中', '已完成', '已驳回', '已撤回']
const AUTH_STATUS = ['草稿', '单位初审中', '合规审核中', '业务审核中', '主管审核中', '经理审核中', '副总审批中', '领导小组审批中', '批准', '已生效', '已驳回', '已撤回']
const statusOpts = computed(() => domain.value === '确权' ? CONFIRM_STATUS : AUTH_STATUS)
const doneStatus = computed(() => domain.value === '确权' ? '已完成' : '已生效')
function bucketOf(s) {
  if (s === '草稿') return 'draft'
  if (s === '已完成' || s === '已生效') return 'done'
  if (s === '已驳回') return 'rejected'
  if (s === '已撤回') return 'withdrawn'
  return 'inReview' // 审批中(含"批准=待双签")
}

// ===== 数据加载(本人全部提交) =====
async function load() {
  loading.value = true
  try {
    // 发件箱=本人全部提交;后端 page 暂无 creatorId 过滤参数,取满页后客户端严格过滤(本人量通常远小于此)。
    // size 上限=后端 PageRequest 单页最大 100(超 100 后端拒 400「每页大小最大为 100」);本人申请量通常 <100。
    const [c, a] = await Promise.all([
      pageConfirmApply({ current: 1, size: 100 }),
      pageAuthApply({ current: 1, size: 100 })
    ])
    const cf = (c.records || []).map((r) => ({ domain: '确权', mode: '', no: r.applyNo, assetName: r.assetName, status: r.status, rejectReason: r.rejectReason, createTime: r.createTime, creatorId: r.creatorId, raw: r }))
    const af = (a.records || []).map((r) => ({ domain: '授权', mode: r.authMode, no: r.applyNo, assetName: r.assetName, status: r.status, rejectReason: r.rejectReason, createTime: r.createTime, creatorId: r.creatorId, raw: r }))
    let all = [...cf, ...af]
    if (onlyMine.value && me()) all = all.filter((r) => r.creatorId === me())
    all.sort((x, y) => String(y.createTime || '').localeCompare(String(x.createTime || '')))
    rows.value = all
  } finally { loading.value = false }
}

// ===== 筛选 / 分组 / 分页 =====
function rowSys(r) { return r.domain === '确权' ? confirmSysName(r.raw) : authSysName(r.raw) }
const mineInTab = computed(() => rows.value.filter((r) => tabOf(r) === activeTab.value))
const filteredNoStatus = computed(() => mineInTab.value.filter((r) => {
  if (kw.value && !rowSys(r).includes(kw.value) && !(r.assetName || '').includes(kw.value)) return false
  if (dateRange.value && dateRange.value.length === 2) {
    const t = String(r.createTime || '').slice(0, 10)
    if (t < dateRange.value[0] || t > dateRange.value[1]) return false
  }
  return true
}))
const filtered = computed(() => filteredNoStatus.value.filter((r) => !status.value || r.status === status.value))
const pageRows = computed(() => filtered.value.slice((page.value - 1) * pageSize.value, page.value * pageSize.value))
// 关键字筛选变化回到第 1 页;结果集缩小(筛选/删除/撤回后重载)时把页码夹回有效范围,避免停在空页。
watch(kw, () => { page.value = 1 })
watch(() => filtered.value.length, (len) => {
  const maxPage = Math.max(1, Math.ceil(len / pageSize.value))
  if (page.value > maxPage) page.value = maxPage
})
// Tab 徽标:本人全部(忽略筛选)
const tabCounts = computed(() => {
  const c = { 初始确权: 0, 确权变更: 0, 批量授权: 0, 一事一议授权: 0 }
  rows.value.forEach((r) => { c[tabOf(r)]++ })
  return c
})
// 统计卡:当前 Tab + 系统名称/时间 作用域,忽略状态
const stat = computed(() => {
  const s = { total: 0, draft: 0, inReview: 0, done: 0, rejected: 0, withdrawn: 0 }
  filteredNoStatus.value.forEach((r) => { s.total++; s[bucketOf(r.status)]++ })
  return s
})
const statCards = computed(() => [
  { key: 'total', label: activeTab.value + '总数', field: 'total', cls: 'c-total', click: 'clear' },
  { key: 'inReview', label: '在途审批', field: 'inReview', cls: 'c-review', click: false },
  { key: 'done', label: doneStatus.value, field: 'done', cls: 'c-done', click: 'done' },
  { key: 'rejected', label: '已驳回', field: 'rejected', cls: 'c-reject', click: { status: '已驳回' } },
  { key: 'draft', label: '草稿', field: 'draft', cls: 'c-init', click: { status: '草稿' } }
])
function onStatClick(c) {
  if (c.click === 'clear') { status.value = ''; activeStat.value = '' }
  else if (c.click === 'done') { status.value = doneStatus.value; activeStat.value = 'done' }
  else { status.value = c.click.status; activeStat.value = c.key }
  page.value = 1
}
function onReset() { kw.value = ''; status.value = ''; dateRange.value = []; activeStat.value = ''; page.value = 1 }
function onTabChange() { status.value = ''; activeStat.value = ''; dateRange.value = []; page.value = 1 }
watch(status, (v) => { if (!v) activeStat.value = '' })

// ===== 列自定义 + 最大化 =====
const COL_INITIAL = [
  { prop: 'sysName', label: '系统名称' }, { prop: 'rightType', label: '权属类型(三权)' },
  { prop: 'sourceIdent', label: '来源识别(A–F)' }, { prop: 'relationIdent', label: '信息关联(G–J)' },
  { prop: 'thirdParty', label: '涉第三方' }, { prop: 'status', label: '状态' }, { prop: 'duration', label: '处理时效' }
]
const COL_CHANGE = [
  { prop: 'sysName', label: '系统名称' }, { prop: 'changeTrigger', label: '变更触发' }, { prop: 'version', label: '版本' },
  { prop: 'baselineRef', label: '基线引用' }, { prop: 'changeSummary', label: '变更摘要' }, { prop: 'status', label: '状态' }, { prop: 'duration', label: '处理时效' }
]
const COL_AUTH = [
  { prop: 'sysName', label: '所属系统' }, { prop: 'assetName', label: '数据表' }, { prop: 'rightType', label: '权益类型' },
  { prop: 'granteeOrg', label: '被授权方' }, { prop: 'reviewResult', label: '审核结果' }, { prop: 'authStatus', label: '授权状态' },
  { prop: 'processOpinion', label: '处理意见' }, { prop: 'createTime', label: '申请时间' }
]
const colDefs = computed(() => activeTab.value === '初始确权' ? COL_INITIAL : activeTab.value === '确权变更' ? COL_CHANGE : COL_AUTH)
// 三 Tab 列并集(按 prop 去重)供列可见性存储
const UNION = [...COL_INITIAL, ...COL_CHANGE, ...COL_AUTH].filter((c, i, arr) => arr.findIndex((x) => x.prop === c.prop) === i)
const colVis = useColumnVisibility('my-apply', UNION)
const { maximized, toggle: toggleMaximize } = useMaximize()

// ===== 列渲染 helper =====
function confirmSysName(raw) { const id = raw?.assetId || ''; return id.startsWith('SYS:') ? id.slice(4) : (raw?.assetName || '-') }
function authSysName(raw) { if (raw?.systemName) return raw.systemName; const id = raw?.assetId || ''; return id.startsWith('SYS:') ? id.slice(4) : (id || '-') }
function codeList(s) { return String(s || '').split(/[、,，]/).map((x) => x.trim()).filter(Boolean) }
const RIGHT_MAP = { 持有权: { short: '持有权', type: 'primary' }, 使用权: { short: '使用权', type: 'success' }, 经营权: { short: '经营权', type: 'warning' } }
function rightTags(rightType) { return codeList(rightType).map((full) => RIGHT_MAP[full] ? { ...RIGHT_MAP[full], full } : { short: full, type: 'info', full }) }
function duration(raw) {
  if (!raw?.createTime) return '-'
  const start = new Date(String(raw.createTime).replace(' ', 'T'))
  const terminal = raw.status === '已完成' || raw.status === '已驳回' || raw.status === '已撤回'
  const end = (terminal && raw.updateTime) ? new Date(String(raw.updateTime).replace(' ', 'T')) : new Date()
  const mins = Math.max(0, Math.floor((end - start) / 60000))
  return `${Math.floor(mins / 1440)}天${Math.floor((mins % 1440) / 60)}小时${terminal ? '' : '(在途)'}`
}
function stTag(s) {
  if (s === '已完成' || s === '已生效') return 'success'
  if (s === '已驳回') return 'danger'
  if (s === '已撤回' || s === '草稿') return 'info'
  return 'warning'
}
function reviewResult(s) { return s === '草稿' ? '未提交' : (s === '已生效' || s === '批准') ? '通过' : s === '已驳回' ? '驳回' : s === '已撤回' ? '已撤回' : '审核中' }
function reviewTag(s) { return (s === '已生效' || s === '批准') ? 'success' : s === '已驳回' ? 'danger' : (s === '草稿' || s === '已撤回') ? 'info' : 'warning' }
function authStatus(s) { return s === '已生效' ? '已生效' : s === '批准' ? '双签中' : s === '已驳回' ? '未授权' : s === '已撤回' ? '已撤回' : s === '草稿' ? '—' : '待生效' }
function authTag(s) { return s === '已生效' ? 'success' : s === '批准' ? 'warning' : s === '已驳回' ? 'danger' : 'info' }

// ===== 导出(客户端 CSV,当前筛选结果) =====
function csvCell(v) { const s = String(v == null ? '' : v).replace(/"/g, '""'); return /[",\n]/.test(s) ? `"${s}"` : s }
function onExport() {
  const isC = domain.value === '确权'
  const cols = isC
    ? (activeTab.value === '初始确权'
      ? [['申请编号', (r) => r.no], ['系统名称', (r) => confirmSysName(r.raw)], ['权属类型', (r) => r.raw.rightType], ['来源识别', (r) => r.raw.sourceIdentification], ['信息关联', (r) => r.raw.relationIdentification], ['涉第三方', (r) => r.raw.involvesThirdParty ? '涉' : '否'], ['状态', (r) => r.status], ['提交时间', (r) => r.createTime]]
      : [['申请编号', (r) => r.no], ['系统名称', (r) => confirmSysName(r.raw)], ['变更触发', (r) => r.raw.changeTrigger], ['版本', (r) => r.raw.changeVersion], ['基线引用', (r) => r.raw.baselineRef], ['变更摘要', (r) => r.raw.changeSummary], ['状态', (r) => r.status], ['提交时间', (r) => r.createTime]])
    : [['申请编号', (r) => r.no], ['所属系统', (r) => authSysName(r.raw)], ['数据表', (r) => r.assetName], ['权益类型', (r) => r.raw.rightType], ['被授权方', (r) => r.raw.granteeOrg], ['审核结果', (r) => reviewResult(r.status)], ['授权状态', (r) => authStatus(r.status)], ['处理意见', (r) => r.rejectReason], ['申请时间', (r) => r.createTime]]
  const lines = [cols.map((c) => c[0]).join(',')]
  filtered.value.forEach((r) => lines.push(cols.map((c) => csvCell(c[1](r))).join(',')))
  const blob = new Blob(['﻿' + lines.join('\n')], { type: 'text/csv;charset=utf-8' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob); a.download = `我的申请-${activeTab.value}.csv`; a.click(); URL.revokeObjectURL(a.href)
}

// ===== 草稿/在审 操作(确权 + 授权) =====
function needReopen(s) { return s === '已驳回' || s === '已撤回' }
const CONFIRM_IN_REVIEW = ['人工预审中', '合规审核中', '主管复核中', '经理终审中']
function isConfirmDraft(row) { return row.domain === '确权' && row.status === '草稿' }
function isConfirmInReview(row) { return row.domain === '确权' && CONFIRM_IN_REVIEW.includes(row.status) }
function goEditDraft(row) { router.push({ path: '/dpr/confirm/wizard', query: { applyId: row.raw?.applyId } }) }
function onDelete(row) {
  ElMessageBox.confirm(`确认删除草稿「${row.assetName || row.no}」?删除后不可恢复。`, '删除草稿', { type: 'warning' })
    .then(async () => { try { await deleteConfirmApply(row.raw?.applyId); ElMessage.success('草稿已删除'); load() } catch (e) { /* 拦截器已 toast */ } }).catch(() => {})
}
function onWithdraw(row) {
  ElMessageBox.prompt('请输入撤回原因(可空)', '撤回申请', { inputType: 'textarea', inputValue: '' })
    .then(async ({ value }) => { try { await withdrawConfirm(row.raw?.applyId, value || ''); ElMessage.success('已撤回,可修改后重提'); load() } catch (e) { /* 拦截器已 toast */ } }).catch(() => {})
}
const AUTH_IN_REVIEW = ['单位初审中', '合规审核中', '业务审核中', '主管审核中', '经理审核中', '副总审批中', '领导小组审批中']
function isAuthDraft(row) { return row.domain === '授权' && row.status === '草稿' }
function isAuthInReview(row) { return row.domain === '授权' && AUTH_IN_REVIEW.includes(row.status) }
function goEditAuthDraft(row) {
  const r = row.raw || {}
  if (row.mode === '批量') router.push({ path: '/dpr/auth/batch-wizard', query: { batchListId: r.batchListId } })
  else router.push({ path: '/dpr/auth/wizard', query: { formNo: r.formNo } })
}
function onDeleteAuth(row) {
  const label = row.mode === '批量' ? '批量授权项' : '数据表'
  ElMessageBox.confirm(`确认删除草稿${label}「${row.assetName || row.no}」?删除后不可恢复。`, '删除草稿', { type: 'warning' })
    .then(async () => { try { await deleteAuthApply(row.raw?.applyId); ElMessage.success('草稿已删除'); load() } catch (e) { /* 拦截器已 toast */ } }).catch(() => {})
}
function onWithdrawAuth(row) {
  const isBatch = row.mode === '批量'
  const msg = isBatch ? '撤回将把整份批量清单退回草案、其下明细退回草稿,可继续编辑后重新提交。确认撤回?' : '撤回该数据表授权申请(退回后可修改重提)。确认撤回?'
  ElMessageBox.confirm(msg, '撤回申请', { type: 'warning' })
    .then(async () => { try { if (isBatch) await withdrawBatchList(row.raw?.batchListId); else await withdrawAuth(row.raw?.applyId, ''); ElMessage.success('已撤回,可修改后重提'); load() } catch (e) { /* 拦截器已 toast */ } }).catch(() => {})
}
function goReopen(row) {
  sessionStorage.setItem('prm-reopen', JSON.stringify({ domain: row.domain, raw: row.raw || {} }))
  let base = '/dpr/confirm/wizard'
  if (row.domain === '授权') base = row.mode === '批量' ? '/dpr/auth/batch-wizard' : '/dpr/auth/wizard'
  router.push({ path: base, query: { reopen: '1' } })
}

// ===== 查看进度抽屉 =====
const drawer = ref(false); const logs = ref([]); const curNo = ref(''); const curSteps = ref([]); const curStep = ref(0)
function fmt(t) { return t ? String(t).replace('T', ' ').slice(0, 19) : '-' }
const STEPS = {
  确权: { labels: ['提交', '人工预审', '合规', '主管', '终审', '制卡'], idx: { 草稿: 0, 人工预审中: 1, 合规审核中: 2, 主管复核中: 3, 经理终审中: 4, 已完成: 6, 已驳回: 1, 已撤回: 1 } },
  一事一议: { labels: ['提交', '单位初审', '合规', '业务', '主管', '经理', '副总', '双签', '生效'], idx: { 草稿: 0, 单位初审中: 1, 合规审核中: 2, 业务审核中: 3, 主管审核中: 4, 经理审核中: 5, 副总审批中: 6, 批准: 7, 已生效: 9, 已驳回: 1 } },
  批量: { labels: ['提交', '合规', '主管', '经理', '副总', '领导小组', '生效'], idx: { 草稿: 0, 合规审核中: 1, 主管审核中: 2, 经理审核中: 3, 副总审批中: 4, 领导小组审批中: 5, 已生效: 7, 已驳回: 1 } }
}
function chainOf(row) { return row.domain === '确权' ? STEPS.确权 : (row.mode === '批量' ? STEPS.批量 : STEPS.一事一议) }
async function goProgress(row) {
  curNo.value = row.no || row.raw?.applyId || ''
  const ch = chainOf(row); curSteps.value = ch.labels; curStep.value = ch.idx[row.status] ?? 0
  const api = row.domain === '确权' ? getConfirmFlowLog : getAuthFlowLog
  logs.value = (await api(row.raw?.applyId)) || []
  drawer.value = true
}

onMounted(load)
</script>

<style scoped>
.my-sub { font-size: 12.5px; color: var(--prm-color-text-weak); }
.my-sub b { color: var(--prm-color-text-secondary); }
.stat-bar { display: flex; gap: 12px; margin: 8px 0 14px; flex-wrap: wrap; }
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
.filter-row { display: flex; flex-wrap: wrap; align-items: center; }
.filter-row :deep(.el-form-item) { margin-bottom: 8px; }
.filter-row .actions { margin-left: auto; }
.col-help { margin-left: 3px; vertical-align: -2px; cursor: help; color: var(--prm-color-text-weak); font-size: 13px; }
</style>
