<template>
  <div class="prm-page">
    <div class="prm-table-card">
      <div class="prm-table-note" style="margin:0 0 12px 0">注:审核工作台展示待我审核的确权申请,支持单条/批量审批,可查看详情(申请+材料+进度轨迹)。</div>
      <div style="margin-bottom:12px">
        <el-button type="success" :disabled="!sel.length" @click="onBatchApprove">批量通过（{{ sel.length }}）</el-button>
        <el-button type="danger" :disabled="!sel.length" @click="onBatchReject">批量驳回（{{ sel.length }}）</el-button>
      </div>
      <el-table :row-class-name="rowHl" :data="reviewing" v-loading="loading" border stripe @selection-change="s => sel = s">
        <el-table-column type="selection" width="46" />
        <el-table-column type="index" label="序号" width="56" align="center" />
        <el-table-column prop="applyNo" label="申请编号" width="160" show-overflow-tooltip />
        <el-table-column prop="assetName" label="资产名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="rightType" label="权属类型" width="130" />
        <el-table-column prop="status" label="当前环节" width="130" align="center">
          <template #default="{ row }"><el-tag type="warning">{{ row.status }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="onDetail(row)">详情</el-button>
            <el-button link type="success" @click="onApprove(row)">审批通过</el-button>
            <el-button link type="danger" @click="onReject(row)">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-drawer v-model="drawer" :title="`审核详情 — ${cur.applyNo || ''}`" size="48%">
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="资产名称" :span="2">{{ cur.assetName }}（{{ cur.assetId }}）</el-descriptions-item>
        <el-descriptions-item label="权属类型">{{ cur.rightType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="权属人">{{ cur.rightHolder || '-' }}</el-descriptions-item>
        <el-descriptions-item label="责任部门">{{ cur.respDept || '-' }}</el-descriptions-item>
        <el-descriptions-item label="当前环节">{{ cur.status }}</el-descriptions-item>
        <el-descriptions-item label="用途" :span="2">{{ cur.purpose || '-' }}</el-descriptions-item>
      </el-descriptions>

      <div class="rv-h">申请材料（{{ materials.length }}）</div>
      <el-table :data="materials" border size="small">
        <el-table-column prop="materialName" label="材料名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="checkResult" label="校验" width="80" align="center" />
        <el-table-column label="原件" width="120">
          <template #default="{ row }">
            <el-link v-if="row.fileName" type="primary" @click="preview(row)">查看</el-link>
            <span v-else style="color:#bbb">-</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="rv-h">进度轨迹</div>
      <el-timeline v-if="logs.length" style="padding:6px">
        <el-timeline-item v-for="l in logs" :key="l.logId" :timestamp="fmt(l.createTime)" placement="top"
          :type="l.toStatus === '已驳回' ? 'danger' : (l.toStatus === '已完成' ? 'success' : 'primary')">
          {{ l.nodeName }}：{{ l.fromStatus }} → {{ l.toStatus }} · 责任人 {{ l.responder }}
          <div v-if="l.opinion" style="font-size:12px;color:#666">意见：{{ l.opinion }}</div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else :image-size="50" description="暂无流转记录" />

      <template #footer>
        <el-button type="success" @click="onApprove(cur, true)">审批通过</el-button>
        <el-button type="danger" @click="onReject(cur, true)">驳回</el-button>
        <el-button @click="drawer = false">关闭</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageConfirmApply, approveConfirm, rejectConfirm, batchApproveConfirm, batchRejectConfirm, listMaterialByApply, getConfirmFlowLog, materialFileUrl } from '@/api/confirm'

const rows = ref([]); const loading = ref(false); const sel = ref([])
const drawer = ref(false); const cur = ref({}); const materials = ref([]); const logs = ref([])
const reviewing = computed(() => rows.value.filter(r => ['合规审核中', '主管复核中', '经理终审中'].includes(r.status)))
function fmt(t) { return t ? String(t).replace('T', ' ').slice(0, 19) : '-' }

async function load() {
  loading.value = true
  try { const r = await pageConfirmApply({ current: 1, size: 100 }); rows.value = r.records || [] } finally { loading.value = false }
}
async function onApprove(row, fromDrawer) {
  const cardId = await approveConfirm(row.applyId)
  ElMessage.success(cardId ? '终审通过,已生成权益卡片' : '审批通过,进入下一环节')
  if (fromDrawer) drawer.value = false
  load()
}
function onReject(row, fromDrawer) {
  ElMessageBox.prompt('请输入驳回原因', '驳回', {})
    .then(async ({ value }) => { await rejectConfirm(row.applyId, value); ElMessage.success('已驳回'); if (fromDrawer) drawer.value = false; load() }).catch(() => {})
}
async function onBatchApprove() {
  const r = await batchApproveConfirm(sel.value.map(x => x.applyId))
  ElMessage[r.failed ? 'warning' : 'success'](`批量通过:成功 ${r.success}/${r.total}${r.failed ? '，失败 ' + r.failed : ''}`)
  load()
}
function onBatchReject() {
  ElMessageBox.prompt('请输入统一驳回原因', '批量驳回', {})
    .then(async ({ value }) => {
      const r = await batchRejectConfirm(sel.value.map(x => x.applyId), value)
      ElMessage[r.failed ? 'warning' : 'success'](`批量驳回:成功 ${r.success}/${r.total}${r.failed ? '，失败 ' + r.failed : ''}`)
      load()
    }).catch(() => {})
}
async function onDetail(row) {
  cur.value = row
  const [m, l] = await Promise.all([listMaterialByApply(row.applyId), getConfirmFlowLog(row.applyId)])
  materials.value = m || []; logs.value = l || []
  drawer.value = true
}
function preview(row) { if (row.materialId) window.open(materialFileUrl(row.materialId), '_blank') }
// 从向导"去审核"带 applyId 跳入时高亮目标单
import { useRoute } from 'vue-router'
const route = useRoute()
function rowHl({ row }) { return route.query.applyId && row.applyId === route.query.applyId ? 'hl-row' : '' }

onMounted(load)
</script>

<style scoped>
.rv-h { font-weight: 600; margin: 16px 0 8px; }
:deep(.hl-row) { background: var(--prm-color-selected-bg, #eff7ff) !important; outline: 1px solid var(--prm-color-primary, #126cfd); }
</style>
