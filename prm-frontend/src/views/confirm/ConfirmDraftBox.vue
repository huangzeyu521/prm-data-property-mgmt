<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
<!--
  申请草稿箱(确权域·本人私有):汇聚「初始确权 / 确权变更」未提交草稿,支持继续填写/删除。
  第一性:用户「材料不全 / 中途中断」→ 系统自动暂存(向导内自动保存引擎),此页是其管理与找回面。
  数据源 = ① 服务端 status='草稿' 且 creatorId=本人(归属隔离) ② 本地未同步 'new' 草稿(底线未达/离线,尚未落库)。
  草稿对审核人永不可见;跨域(含授权)总览见「我的申请」。
-->
<template>
  <div class="prm-page">
    <div class="prm-toolbar">
      <h3 style="margin:0 12px 0 0">申请草稿箱</h3>
      <span class="db-sub">你未提交的<b>确权</b>申请(系统会在填写时<b>自动暂存</b>,中途离开不丢)。跨域总览见「我的申请」。</span>
      <el-button v-if="staleCount > 0" type="warning" plain style="margin-left:auto" @click="cleanupStale">
        清理陈旧草稿（{{ staleCount }}）
      </el-button>
      <el-button :loading="loading" @click="load" :style="staleCount > 0 ? 'margin-left:12px' : 'margin-left:auto'">刷新</el-button>
    </div>

    <el-alert type="info" :closable="false" show-icon style="margin:0 0 12px"
      :title="`草稿仅本人可见、不进入审核流程。填写「初始确权申请 / 确权变更申请」时系统自动保存;达「已选系统+库表」后同步到云端,可换设备继续。超过 ${STALE_DAYS} 天未编辑的草稿标记为「陈旧」,可一键清理(不会自动删除)。`" />

    <div class="prm-table-card">
      <el-table :data="rows" v-loading="loading" stripe border style="width:100%">
        <el-table-column label="草稿编号" prop="applyNo" width="180">
          <template #default="{ row }">
            <span>{{ row.applyNo || row.applyId }}</span>
            <el-tag v-if="row.__local" size="small" type="warning" effect="plain" style="margin-left:6px">本地未同步</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="标题(系统 / 资产)" prop="assetName" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ row.assetName || '(未命名)' }}</template>
        </el-table-column>
        <el-table-column label="登记类型" width="110" align="center">
          <template #default="{ row }">
            <span :class="'prm-c-' + (row.registerType === '确权变更' ? 'warning' : 'primary')">{{ row.registerType || '初始确权' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="完成度 · 材料缺口" width="200" align="center">
          <template #default="{ row }">
            <el-tooltip :disabled="!completeness(row).tip" :content="completeness(row).tip" placement="top">
              <span :class="'prm-c-' + completeness(row).type">{{ completeness(row).text }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="最后编辑" width="200" align="center">
          <template #default="{ row }">
            <span>{{ fmtTime(row) }}</span>
            <el-tag v-if="isStale(row)" size="small" type="danger" effect="plain" style="margin-left:6px">陈旧</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="resume(row)">继续填写</el-button>
            <el-button link type="danger" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="草稿箱是空的">
            <div class="prm-c-weak" style="margin-bottom:12px">开始一份确权申请,系统会自动为你暂存未完成的内容,随时回来接着填。</div>
            <el-button type="primary" @click="$router.push('/dpr/confirm/wizard')">发起初始确权申请</el-button>
            <el-button @click="$router.push('/dpr/confirm/change')">发起确权变更申请</el-button>
          </el-empty>
        </template>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageConfirmApply, deleteConfirmApply, listMaterialByApply, listMaterialRules } from '@/api/confirm'
import { currentUser } from '@/api/auth'
import { listLocalDrafts, removeLocalDraft } from '@/composables/useDraftAutosave'
import { filterRequiredRules } from '@/lib/confirmChecklist'
import { notifyDraftChanged } from '@/lib/draftCount'
import { clearDraftSource } from '@/lib/draftSource'

const STALE_DAYS = 90

const router = useRouter()
const loading = ref(false)
const rows = ref([])
const me = () => (currentUser() && currentUser().userId) || ''

function localRows() {
  // 仅取 'new'(未同步)且有实质内容的本地草稿;applyId 键的本地缓存对应云端草稿,已在服务端列表中体现
  const all = [...listLocalDrafts('confirm-initial', me()), ...listLocalDrafts('confirm-change', me())]
  return all
    .filter((d) => d.id === 'new' && (d.form?.systemName || (d.tableItems || []).length || d.form?.rightHolder))
    .map((d) => ({
      __local: true, __key: d.key,
      applyNo: '', applyId: '',
      assetName: d.title || d.form?.systemName || '(未命名)',
      registerType: d.registerType || '初始确权',
      __ts: d.__ts
    }))
}

async function load() {
  loading.value = true
  try {
    // /page 端点不接受 status 入参(多传字段会 400),状态一律客户端过滤(同 MyApplications)
    const res = await pageConfirmApply({ pageNum: 1, pageSize: 100 })
    const mine = (res.records || []).filter((r) => (!me() || r.creatorId === me()) && r.status === '草稿')
    rows.value = [...localRows(), ...mine]
    annotateGaps() // 异步逐项算材料缺口,不阻塞列表首屏
  } catch (e) {
    rows.value = localRows() // 服务端失败也显示本地草稿,不阻断找回
  } finally { loading.value = false }
}

// ===== 材料缺口逐项(P1):按草稿 A–J 识别码 + 应交规则算"必填未上传"项 =====
let rulesCache = null
async function ensureRules() {
  if (rulesCache) return rulesCache
  try { const r = await listMaterialRules('确权'); rulesCache = Array.isArray(r) ? r : [] } catch { rulesCache = [] }
  return rulesCache
}
function parseCodes(s, allowed) {
  if (!s) return []
  return String(s).split(/[,，、]/).map((t) => (t.trim().match(/^[A-J]/) || [''])[0]).filter((c) => allowed.includes(c))
}
async function computeGap(row) {
  const rules = await ensureRules()
  if (!rules.length) return null
  const sourceIdent = parseCodes(row.sourceIdentification, ['A', 'B', 'C', 'D', 'E', 'F'])
  const relationIdent = parseCodes(row.relationIdentification, ['G', 'H', 'I', 'J'])
  const needTable2 = !!row.involvesThirdParty || sourceIdent.some((c) => ['B', 'C', 'D', 'E', 'F'].includes(c)) || relationIdent.length > 0
  const required = filterRequiredRules(rules, { sourceIdent, relationIdent, needTable2, registerType: row.registerType, changeTrigger: row.changeTrigger })
  let uploaded = []
  try { uploaded = (await listMaterialByApply(row.applyId)) || [] } catch { uploaded = [] }
  const upNames = uploaded.map((m) => (m.materialName || m.name || '').trim()).filter(Boolean)
  const isUploaded = (name) => upNames.some((u) => u === name || u.includes(name) || name.includes(u))
  const missing = required.filter((r) => r.required === '必填' && !isUploaded(r.materialName)).map((r) => r.materialName)
  return { requiredCount: required.length, missing }
}
async function annotateGaps() {
  const serverRows = rows.value.filter((r) => !r.__local && r.applyId)
  await Promise.all(serverRows.map(async (r) => { r.gap = await computeGap(r) }))
  rows.value = [...rows.value] // 触发完成度列刷新
}

// 完成度:本地未同步 / 材料缺口逐项(有则列出) / 材料齐备;缺口未算出时回退字段启发式
function completeness(row) {
  if (row.__local) return { text: '本地待续填', type: 'warning', tip: '尚未同步到云端;继续填写将自动保存并入库' }
  if (row.gap) {
    if (!row.gap.missing.length) return { text: `材料齐备(应交 ${row.gap.requiredCount})`, type: 'success', tip: '必填应交材料已齐,可提交审核' }
    return { text: `缺 ${row.gap.missing.length} 项材料`, type: 'warning', tip: '待补:' + row.gap.missing.join('、') }
  }
  const miss = ['rightHolder', 'subjectLevel', 'systemOwner', 'contactInfo'].filter((k) => !row[k])
  if (miss.length) return { text: `要素待补(缺 ${miss.length} 项)`, type: 'warning', tip: '基本要素未填全' }
  return { text: '要素齐 · 待补材料', type: 'info', tip: '正在核算材料缺口…' }
}

function fmtTime(row) {
  if (row.__local) return row.__ts ? new Date(row.__ts).toLocaleString() : ''
  return (row.updateTime || row.createTime || '').replace('T', ' ').slice(0, 19)
}

// ===== P2:陈旧草稿策略 =====
function rowTs(row) {
  if (row.__local) return row.__ts || 0
  const t = Date.parse(String(row.updateTime || row.createTime || '').replace(' ', 'T'))
  return isNaN(t) ? 0 : t
}
function isStale(row) {
  const ts = rowTs(row)
  return ts > 0 && (Date.now() - ts) > STALE_DAYS * 86400000
}
const staleCount = computed(() => rows.value.filter(isStale).length)
async function cleanupStale() {
  const targets = rows.value.filter(isStale)
  if (!targets.length) return
  try {
    await ElMessageBox.confirm(`将清理 ${targets.length} 份超过 ${STALE_DAYS} 天未编辑的陈旧草稿,删除后不可恢复。确认清理?`, '清理陈旧草稿', { type: 'warning' })
  } catch { return }
  let ok = 0
  for (const r of targets) {
    try {
      if (r.__local) removeLocalDraft(r.__key)
      else { await deleteConfirmApply(r.applyId); clearDraftSource(r.applyId) }
      ok++
    } catch { /* 跳过失败项,继续清理其余 */ }
  }
  notifyDraftChanged()
  ElMessage.success(`已清理 ${ok} 份陈旧草稿`)
  load()
}

function resume(row) {
  const base = row.registerType === '确权变更' ? '/dpr/confirm/change' : '/dpr/confirm/wizard'
  if (row.__local) router.push(base) // 无 applyId:进向导由本地「找回提示」恢复
  else router.push(`${base}?applyId=${row.applyId}`)
}

async function onDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除该草稿「${row.assetName || row.applyNo || row.applyId}」?删除后不可恢复。`, '删除草稿', { type: 'warning' })
  } catch { return }
  try {
    if (row.__local) { removeLocalDraft(row.__key) }
    else { await deleteConfirmApply(row.applyId); clearDraftSource(row.applyId) }
    notifyDraftChanged() // 草稿数徽标刷新
    ElMessage.success('草稿已删除')
    load()
  } catch (e) { /* 拦截器已 toast */ }
}

onMounted(load)
</script>

<style scoped>
.db-sub { color: var(--prm-color-text-weak); font-size: 13px; }
</style>
