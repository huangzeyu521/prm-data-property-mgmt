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
  共用列:系统名称 / 状态 / 处理时效 / 操作(进度详情抽屉看流转时间线)。统计条按当前 Tab 作用域。
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

    <!-- 查询区:筛选项少(初始3/变更4)→ 全部常显单行(数研院规范 §10:字段少用内联单行,「更多/展开」仅为条件溢出);窄屏 flex-wrap 优雅换行;查询/重置右置。若未来筛选项回增到 ≥5,恢复「更多/展开」分层。 -->
    <div class="prm-query-bar">
      <el-form :inline="true" @submit.prevent class="filter-row">
        <el-form-item label="系统名称"><el-input v-model="q.assetName" placeholder="系统名称" clearable style="width:160px" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="q.status" placeholder="全部" clearable style="width:120px" @change="onSearch">
            <el-option v-for="s in statuses" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="activeTab === '确权变更'" label="变更触发">
          <el-select v-model="q.changeTrigger" placeholder="全部触发" clearable style="width:130px" @change="onSearch">
            <el-option v-for="t in triggerOpts" :key="t" :label="t" :value="t" />
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
      <!-- 列自定义 + 列表最大化(高压线:每个列表须支持) -->
      <div style="display:flex;justify-content:flex-end;align-items:center;gap:6px;margin-bottom:8px">
        <el-button size="small" @click="onExport">导出</el-button>
        <ColumnSettings :columns="colDefs" :visible="colVis.visible" @toggle="(p, v) => colVis.setVisible(p, v)" @reset="colVis.reset" />
        <el-button :icon="maximized ? ScaleToOriginal : FullScreen" circle size="small" :title="maximized ? '退出最大化' : '最大化'" @click="toggleMaximize" />
      </div>
      <el-table :data="rows" v-loading="loading" border stripe>
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
                <span v-for="r in rightTags(row.rightType)" :key="r.full" style="margin-right:4px" :class="'prm-c-' + ((r.type) || 'primary')">{{ r.short }}</span>
                <span v-if="!rightTags(row.rightType).length">-</span>
              </span></div>
              <div class="ed-item"><span class="ed-k">来源识别</span><span class="ed-v">{{ identFull(row.sourceIdentification, SOURCE_MAP) }}</span></div>
              <div class="ed-item"><span class="ed-k">信息关联</span><span class="ed-v">{{ identFull(row.relationIdentification, RELATION_MAP) }}</span></div>
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
        <el-table-column type="index" label="序号" width="52" align="center" />
        <el-table-column prop="applyNo" label="申请编号" width="158" show-overflow-tooltip />
        <el-table-column label="系统名称" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ sysName(row) }}</template>
        </el-table-column>

        <!-- 初始确权:关注识别完整性 -->
        <template v-if="activeTab === '初始确权'">
          <el-table-column v-if="colVis.isVisible('rightType')" label="权属类型(三权)" width="172">
            <template #default="{ row }">
              <el-tooltip v-for="r in rightTags(row.rightType)" :key="r.full" :content="r.full" placement="top">
                <span style="margin:1px" :class="'prm-c-' + ((r.type) || 'primary')">{{ r.short }}</span>
              </el-tooltip>
              <span v-if="!rightTags(row.rightType).length" style="color:var(--prm-color-text-disabled)">—</span>
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
              <el-tooltip v-for="c in codeList(row.sourceIdentification)" :key="c" :content="identText(c, SOURCE_MAP)" placement="top">
                <span style="margin:1px;cursor:help" class="prm-c-primary">{{ c }}</span>
              </el-tooltip>
              <span v-if="!codeList(row.sourceIdentification).length" style="color:var(--prm-color-text-disabled)">—</span>
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
              <el-tooltip v-for="c in codeList(row.relationIdentification)" :key="c" :content="identText(c, RELATION_MAP)" placement="top">
                <span style="margin:1px;cursor:help" :class="'prm-c-' + ((c === 'H' ? 'danger' : 'warning') || 'primary')">{{ c }}</span>
              </el-tooltip>
              <span v-if="!codeList(row.relationIdentification).length" style="color:var(--prm-color-text-disabled)">—</span>
            </template>
          </el-table-column>
          <el-table-column v-if="colVis.isVisible('thirdParty')" label="涉第三方" width="84" align="center">
            <template #default="{ row }">
              <span v-if="row.involvesThirdParty" class="prm-c-warning">涉</span>
              <span v-else style="color:var(--prm-color-text-disabled)">—</span>
            </template>
          </el-table-column>
        </template>

        <!-- 确权变更:关注改了什么 + 影响 -->
        <template v-else>
          <el-table-column v-if="colVis.isVisible('changeTrigger')" label="变更触发" min-width="160">
            <template #default="{ row }">
              <span v-for="t in triggerTags(row)" :key="t" style="margin:1px" :class="'prm-c-' + ((t === '数据新增' ? 'success' : 'warning') || 'primary')">{{ t }}</span>
              <span v-if="!triggerTags(row).length" style="color:var(--prm-color-text-disabled)">—</span>
            </template>
          </el-table-column>
          <el-table-column v-if="colVis.isVisible('version')" label="版本" width="72" align="center">
            <template #default="{ row }">
              <span v-if="row.changeVersion" class="prm-c-info">v{{ row.changeVersion }}</span>
              <span v-else style="color:var(--prm-color-text-disabled)">—</span>
            </template>
          </el-table-column>
          <el-table-column v-if="colVis.isVisible('baselineRef')" prop="baselineRef" label="基线引用" width="148" show-overflow-tooltip>
            <template #default="{ row }">{{ row.baselineRef || '—' }}</template>
          </el-table-column>
          <el-table-column v-if="colVis.isVisible('changeSummary')" prop="changeSummary" label="变更摘要" min-width="220" show-overflow-tooltip>
            <template #default="{ row }">{{ row.changeSummary || '—' }}</template>
          </el-table-column>
        </template>

        <el-table-column prop="status" label="状态" width="104" align="center">
          <template #default="{ row }"><span :class="'prm-c-' + ((tag(row.status)) || 'primary')">{{ row.status }}</span></template>
        </el-table-column>
        <el-table-column v-if="colVis.isVisible('duration')" label="处理时效" width="124">
          <template #default="{ row }">{{ duration(row) }}</template>
        </el-table-column>
        <!-- 查询页=纯只读审计视图:草稿的编辑/删除、在审撤回、撤回后重提 均收敛到「我的申请」发件箱,此处只看不改 -->
        <el-table-column label="操作" width="96" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="onProgress(row)">进度详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:20px;justify-content:flex-end" background layout="total, sizes, prev, pager, next, jumper" :page-sizes="[10, 20, 50, 100]"
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
          <div class="prm-c-primary" style="font-size:12px;margin-top:4px"><el-icon style="vertical-align:-2px"><Bell /></el-icon> {{ l.pushChannel }}：{{ l.notifyContent }}</div>
        </el-timeline-item>
      </el-timeline>
    </el-drawer>
  </div>
</template>
<script setup>
import { onMounted, reactive, ref, computed } from 'vue'
import { FullScreen, ScaleToOriginal } from '@element-plus/icons-vue'
import { SOURCE_CODES, RELATION_CODES, SOURCE_MAP, RELATION_MAP, identText } from '@/lib/identCodes'
import ColumnSettings from '@/components/ColumnSettings.vue'
import { useColumnVisibility } from '@/composables/useColumnVisibility'
import { useMaximize } from '@/composables/useMaximize'
import { useTablePage } from '@/composables/useTablePage'
import { pageConfirmApply, statsConfirmApply, getConfirmFlowLog, confirmHistoryExportUrl } from '@/api/confirm'

const statuses = ['草稿', '人工预审中', '合规审核中', '主管复核中', '经理终审中', '已完成', '已驳回', '已撤回']
const triggerOpts = ['数据新增', '数据来源变更', '管理要求变更', '权益到期']


// 列自定义(高压线):两个 Tab 的可选列不同,colDefs 随 activeTab 切换
const COL_DEFS_INITIAL = [
  { prop: 'rightType', label: '权属类型(三权)' }, { prop: 'sourceIdent', label: '来源识别(A–F)' },
  { prop: 'relationIdent', label: '信息关联(G–J)' }, { prop: 'thirdParty', label: '涉第三方' },
  { prop: 'duration', label: '处理时效' }
]
const COL_DEFS_CHANGE = [
  { prop: 'changeTrigger', label: '变更触发' }, { prop: 'version', label: '版本' },
  { prop: 'baselineRef', label: '基线引用' }, { prop: 'changeSummary', label: '变更摘要' },
  { prop: 'duration', label: '处理时效' }
]
const colDefs = computed(() => activeTab.value === '初始确权' ? COL_DEFS_INITIAL : COL_DEFS_CHANGE)
const colVis = useColumnVisibility('main', [...COL_DEFS_INITIAL, ...COL_DEFS_CHANGE])

// 列表最大化(高压线§11)
const { maximized, toggle: toggleMaximize } = useMaximize()

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
// 展开详情:代码串 "A、B" → "A 自行生产数据、B 公开采集数据"(带释义)
function identFull(s, map) { const l = codeList(s); return l.length ? l.map(c => identText(c, map)).join('、') : '-' }
// 权属类型 = 三权多选(持有/使用/经营权可并存):rightType 为「、」拼接串,拆成多标签(短名+全名悬浮)
const RIGHT_MAP = {
  持有权: { short: '持有权', type: 'primary' },
  使用权: { short: '使用权', type: 'success' },
  经营权: { short: '经营权', type: 'warning' }
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
    assetName: q.assetName, status: q.status,
    registerType: activeTab.value, changeTrigger: activeTab.value === '确权变更' ? q.changeTrigger : '',
    createTimeStart: dateRange.value?.[0] || '', createTimeEnd: dateRange.value?.[1] ? dateRange.value[1] + ' 23:59:59' : ''
  }
}
function globalParams() {
  return {
    assetName: q.assetName,
    createTimeStart: dateRange.value?.[0] || '', createTimeEnd: dateRange.value?.[1] ? dateRange.value[1] + ' 23:59:59' : ''
  }
}
function onExport() { window.open(confirmHistoryExportUrl(tabParams()), '_blank') }

const drawer = ref(false); const logs = ref([]); const curNo = ref('')
function fmt(t) { return t ? String(t).replace('T', ' ').slice(0, 19) : '-' }
async function onProgress(row) {
  curNo.value = row.applyNo || row.applyId
  logs.value = await getConfirmFlowLog(row.applyId) || []
  drawer.value = true
}

const { query: q, rows, total, loading, load, search: doSearch, reset: doReset } = useTablePage(
  (p) => pageConfirmApply({ ...p, ...tabParams() }),
  { assetName: '', status: '', changeTrigger: '' },
  {
    onLoaded: async () => {
      const [sg, st] = await Promise.all([statsConfirmApply(globalParams()), statsConfirmApply(tabParams())])
      Object.assign(statGlobal, sg || {}); Object.assign(stat, st || {})
    }
  }
)
function tag(s) { return { 已完成: 'success', 已驳回: 'danger', 已撤回: 'info', 草稿: 'info' }[s] || 'warning' }
function onSearch() { activeStat.value = ''; doSearch() }
function onReset() { dateRange.value = []; activeStat.value = ''; doReset() }
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

/* 列头释义帮助图标(A–F / G–J 代码含义) */
.col-help { margin-left: 3px; vertical-align: -2px; cursor: help; color: var(--prm-color-text-weak); font-size: 13px; }

/* 单行筛选:字段左、查询/重置右;窄屏 flex-wrap 优雅换行(数研院规范 §10) */
.filter-row { display: flex; flex-wrap: wrap; align-items: center; }
.filter-row :deep(.el-form-item) { margin-bottom: 8px; }
.filter-row .actions { margin-left: auto; }
</style>
