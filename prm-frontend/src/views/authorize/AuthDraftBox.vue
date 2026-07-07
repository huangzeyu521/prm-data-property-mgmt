<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
<!--
  申请草稿箱(授权域·本人私有):汇聚「一事一议 / 批量」未提交授权草稿,支持继续填写/删除/清理陈旧。
  第一性:填授权时「材料不全 / 中途中断」→ 系统自动暂存(向导内自动保存引擎),此页是其管理与找回面。
  数据源:① 一事一议 = pageAuthApply(status=草稿·authMode=一事一议)按 formNo 分组 ② 批量 = pageBatchList(status=草案)逐清单
          ③ 本地未同步 'new'(底线未达/离线,尚未落库)。草稿对审核人永不可见;跨域总览见「我的申请」。
-->
<template>
  <div class="prm-page">
    <div class="prm-toolbar">
      <h3 style="margin:0 12px 0 0">申请草稿箱</h3>
      <span class="db-sub">你未提交的<b>授权</b>申请(一事一议 / 批量;系统会在填写时<b>自动暂存</b>)。跨域总览见「我的申请」。</span>
      <el-button v-if="staleCount > 0" type="warning" plain style="margin-left:auto" @click="cleanupStale">清理陈旧草稿（{{ staleCount }}）</el-button>
      <el-button :loading="loading" @click="load" :style="staleCount > 0 ? 'margin-left:12px' : 'margin-left:auto'">刷新</el-button>
    </div>

    <el-alert type="info" :closable="false" show-icon style="margin:0 0 12px"
      :title="`草稿仅本人可见、不进入审核流程。填写「一事一议授权 / 批量授权」时系统自动保存;达落库底线(一事一议已选表 / 批量已填年度)后同步到云端,可换设备继续。超过 ${STALE_DAYS} 天未编辑标「陈旧」,可一键清理(不自动删除)。`" />

    <div class="prm-table-card">
      <el-table :data="rows" v-loading="loading" stripe border style="width:100%">
        <el-table-column label="草稿编号" width="200">
          <template #default="{ row }">
            <span>{{ row.formNo || row.batchListId || row.listNo || '—' }}</span>
            <el-tag v-if="row.__local" size="small" type="warning" effect="plain" style="margin-left:6px">本地未同步</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="标题(被授权方 · 场景/年度)" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">{{ row.title || '(未命名)' }}</template>
        </el-table-column>
        <el-table-column label="授权方式" width="110" align="center">
          <template #default="{ row }">
            <span :class="'prm-c-' + (row.mode === '批量' ? 'warning' : 'primary')">{{ row.mode }}</span>
          </template>
        </el-table-column>
        <el-table-column label="数据表/项" width="90" align="center">
          <template #default="{ row }">{{ row.count || 0 }}</template>
        </el-table-column>
        <el-table-column label="完成度" width="150" align="center">
          <template #default="{ row }">
            <span :class="'prm-c-' + completeness(row).type">{{ completeness(row).text }}</span>
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
            <div class="prm-c-weak" style="margin-bottom:12px">开始一份授权申请,系统会自动为你暂存未完成的内容,随时回来接着填。</div>
            <el-button type="primary" @click="$router.push('/dpr/auth/wizard')">发起一事一议授权</el-button>
            <el-button @click="$router.push('/dpr/auth/batch-wizard')">发起批量授权</el-button>
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
import { pageAuthApply, pageBatchList, deleteAuthApply, deleteBatchList } from '@/api/authorize'
import { currentUser } from '@/api/auth'
import { listLocalDrafts, removeLocalDraft } from '@/composables/useDraftAutosave'
import { notifyDraftChanged } from '@/lib/draftCount'
import { clearDraftSource } from '@/lib/draftSource'

const STALE_DAYS = 90
const router = useRouter()
const loading = ref(false)
const rows = ref([])
const me = () => (currentUser() && currentUser().userId) || ''

function tsOf(s) {
  const t = Date.parse(String(s || '').replace(' ', 'T'))
  return isNaN(t) ? 0 : t
}

function localRows() {
  const sp = listLocalDrafts('auth-special', me())
    .filter((d) => d.id === 'new' && (d.listForm?.granteeOrg || d.pendingAsset || (d.items || []).length))
    .map((d) => ({ __local: true, __key: d.key, mode: '一事一议', title: d.title || d.listForm?.granteeOrg || '(未命名)', count: (d.items || []).length, __ts: d.__ts }))
  const ba = listLocalDrafts('auth-batch', me())
    .filter((d) => d.id === 'new' && (d.listForm?.listYear || d.listForm?.granteeOrg || (d.items || []).length))
    .map((d) => ({ __local: true, __key: d.key, mode: '批量', title: d.title || d.listForm?.granteeOrg || '(未命名)', count: (d.items || []).length, __ts: d.__ts }))
  return [...sp, ...ba]
}

async function load() {
  loading.value = true
  try {
    // /page 端点不接受 status/listStatus 入参(pageAuthApply 多传字段会 400;pageBatchList 直接忽略)→ 一律客户端过滤
    const [spRes, baRes] = await Promise.all([
      pageAuthApply({ pageNum: 1, pageSize: 100 }).catch(() => ({ records: [] })),
      pageBatchList({ pageNum: 1, pageSize: 100 }).catch(() => ({ records: [] }))
    ])
    // 一事一议:仅草稿态,按 formNo 分组成一份申请单
    const spRows = (spRes.records || []).filter((r) => (!me() || r.creatorId === me()) && r.status === '草稿' && r.authMode === '一事一议' && r.formNo)
    const groups = {}
    for (const r of spRows) {
      const g = groups[r.formNo] || (groups[r.formNo] = { mode: '一事一议', key: r.formNo, formNo: r.formNo, applyIds: [], granteeOrg: '', scenario: '', count: 0, updateTime: '' })
      g.applyIds.push(r.applyId); g.count++
      if (!g.granteeOrg) g.granteeOrg = r.granteeOrg
      if (!g.scenario) g.scenario = r.scenario
      if (tsOf(r.updateTime || r.createTime) > tsOf(g.updateTime)) g.updateTime = r.updateTime || r.createTime
    }
    const special = Object.values(groups).map((g) => ({ ...g, title: [g.granteeOrg, g.scenario].filter(Boolean).join(' · ') || '(未命名)' }))
    // 批量:仅草案态清单(状态字段=listStatus;清单头不含被授权方,标题取年度/清单号)
    const batch = (baRes.records || [])
      .filter((r) => (!me() || r.creatorId === me()) && r.listStatus === '草案')
      .map((r) => ({
        mode: '批量', key: r.batchListId, batchListId: r.batchListId, listNo: r.listNo,
        listYear: r.listYear, count: r.itemCount || 0, updateTime: r.updateTime || r.createTime,
        title: [r.listYear && `${r.listYear} 年度批量授权清单`, r.remark].filter(Boolean).join(' · ') || r.listNo || '(未命名清单)'
      }))
    rows.value = [...localRows(), ...special, ...batch]
  } finally { loading.value = false }
}

// 完成度(授权双场景材料模型复杂 → 用要素启发式;精确材料缺口逐项为确权专属)
function completeness(row) {
  if (row.__local) return { text: '本地待续填', type: 'warning' }
  if (row.mode === '批量') {
    // 批量清单头建单即必填授权年度;有明细即可提交(被授权方在清单级不存,不作判据)
    if (!row.count) return { text: '待加授权项', type: 'info' }
    return { text: '要素齐 · 待提交', type: 'success' }
  }
  if (row.granteeOrg && row.scenario && row.count > 0) return { text: '要素齐 · 待提交', type: 'success' }
  return { text: '要素待补', type: 'warning' }
}

// P2:陈旧草稿策略
function rowTs(row) { return row.__local ? (row.__ts || 0) : tsOf(row.updateTime) }
function isStale(row) { const ts = rowTs(row); return ts > 0 && (Date.now() - ts) > STALE_DAYS * 86400000 }
const staleCount = computed(() => rows.value.filter(isStale).length)

function fmtTime(row) {
  if (row.__local) return row.__ts ? new Date(row.__ts).toLocaleString() : ''
  return String(row.updateTime || '').replace('T', ' ').slice(0, 19)
}

function resume(row) {
  if (row.mode === '批量') router.push(row.__local ? '/dpr/auth/batch-wizard' : `/dpr/auth/batch-wizard?batchListId=${row.batchListId}`)
  else router.push(row.__local ? '/dpr/auth/wizard' : `/dpr/auth/wizard?formNo=${row.formNo}`)
}

async function deleteOne(row) {
  if (row.__local) { removeLocalDraft(row.__key); return }
  if (row.mode === '批量') { await deleteBatchList(row.batchListId); clearDraftSource(row.batchListId) }
  else { for (const id of (row.applyIds || [])) { try { await deleteAuthApply(id) } catch { /* 跳过失败行 */ } } clearDraftSource(row.formNo) }
}

async function onDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除该${row.mode}草稿「${row.title || row.formNo || row.batchListId}」?删除后不可恢复。`, '删除草稿', { type: 'warning' })
  } catch { return }
  try {
    await deleteOne(row)
    notifyDraftChanged()
    ElMessage.success('草稿已删除')
    load()
  } catch (e) { /* 拦截器已 toast */ }
}

async function cleanupStale() {
  const targets = rows.value.filter(isStale)
  if (!targets.length) return
  try {
    await ElMessageBox.confirm(`将清理 ${targets.length} 份超过 ${STALE_DAYS} 天未编辑的陈旧草稿,删除后不可恢复。确认清理?`, '清理陈旧草稿', { type: 'warning' })
  } catch { return }
  let ok = 0
  for (const r of targets) { try { await deleteOne(r); ok++ } catch { /* 跳过失败项 */ } }
  notifyDraftChanged()
  ElMessage.success(`已清理 ${ok} 份陈旧草稿`)
  load()
}

onMounted(load)
</script>

<style scoped>
.db-sub { color: var(--prm-color-text-weak); font-size: 13px; }
</style>
