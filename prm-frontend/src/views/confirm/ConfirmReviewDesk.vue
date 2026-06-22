<template>
  <div class="prm-page">
    <div class="prm-table-card">
      <div class="prm-table-note" style="margin:0 0 12px 0">注:审核工作台展示待我审核的确权申请,支持单条/批量审批,可查看详情(申请+材料+进度轨迹)。</div>
      <div style="margin-bottom:12px">
        <el-button type="success" :disabled="!sel.length" @click="onBatchApprove">批量通过（{{ sel.length }}）</el-button>
        <el-button type="danger" :disabled="!sel.length" @click="onBatchReject">批量驳回（{{ sel.length }}）</el-button>
      </div>
      <el-table :row-class-name="rowHl" :data="reviewing" v-loading="loading" border stripe @selection-change="s => sel = s">
        <el-table-column type="selection" width="46" :selectable="row => canAct(row.status)" />
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
            <template v-if="canAct(row.status)">
              <el-button link type="success" @click="onApprove(row)">审批通过</el-button>
              <el-button link type="danger" @click="onReject(row)">驳回</el-button>
            </template>
            <el-tooltip v-else :content="`本节点须「${needRoleLabel(row.status)}」处理`" placement="top">
              <span style="color:#bbb;font-size:12px;margin-left:6px">非本人审批</span>
            </el-tooltip>
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

      <div class="rv-h">
        AI 校验结果（人工预审依据）
        <el-tag v-if="cur.status === '人工预审中'" type="warning" size="small" effect="plain" style="margin-left:8px">本环节须人工复核 AI 结果</el-tag>
      </div>
      <div v-if="aiSnap">
        <el-alert :type="aiSnap.materialCheck && aiSnap.materialCheck.overall === '通过' ? 'success' : 'warning'" :closable="false" style="margin-bottom:8px">
          <div><b>AI 材料校验：{{ aiSnap.materialCheck && aiSnap.materialCheck.overall || '—' }}</b> {{ aiSnap.materialCheck && aiSnap.materialCheck.overallDesc || '' }}</div>
        </el-alert>
        <el-table v-if="aiSnap.materialCheck && aiSnap.materialCheck.items && aiSnap.materialCheck.items.length" :data="aiSnap.materialCheck.items" border size="small" style="margin-bottom:8px">
          <el-table-column prop="materialName" label="材料" min-width="150" show-overflow-tooltip />
          <el-table-column prop="rightHolder" label="识别权属主体" min-width="110" show-overflow-tooltip />
          <el-table-column prop="rightType" label="识别权类" width="96" />
          <el-table-column label="敏感" width="60" align="center">
            <template #default="{ row }"><el-tag v-if="row.sensitiveHit" type="danger" size="small">敏感</el-tag><span v-else>—</span></template>
          </el-table-column>
          <el-table-column label="AI 结论" min-width="110" show-overflow-tooltip>
            <template #default="{ row }">{{ row.conclusion || row.aiResult || row.suggestion || '—' }}</template>
          </el-table-column>
        </el-table>
        <el-alert v-if="aiSnap.ruleReport" :type="aiSnap.ruleReport.allPass ? 'success' : 'warning'" :closable="false" style="margin-bottom:8px">
          <div>规则完整性：{{ aiSnap.ruleReport.summary || (aiSnap.ruleReport.allPass ? '全部通过' : '存在缺失/不合规') }}</div>
          <div v-if="aiSnap.ruleReport.missing && aiSnap.ruleReport.missing.length" style="font-size:12px">缺失：{{ aiSnap.ruleReport.missing.join('、') }}</div>
        </el-alert>
        <el-descriptions v-if="aiSnap.consolidation" :column="3" border size="small">
          <el-descriptions-item label="命中规则">规则 {{ aiSnap.consolidation.rule }}</el-descriptions-item>
          <el-descriptions-item label="网公司持有权">{{ aiSnap.consolidation.holdRight }}</el-descriptions-item>
          <el-descriptions-item label="网公司经营权">{{ aiSnap.consolidation.operateRight }}</el-descriptions-item>
        </el-descriptions>
        <div style="font-size:12px;color:#9ca3af;margin-top:4px">
          校验时间 {{ fmt(aiSnap.checkedAt) }} · 元数据质量 {{ aiSnap.qualityScore ?? '—' }} · 提交时固化快照,供预审完整复核·可追溯
        </div>
      </div>
      <el-empty v-else :image-size="40" description="该申请无 AI 校验快照(旧数据 / 未经一键校验提交)" />

      <div class="rv-h">申请材料（{{ materials.length }}）</div>
      <el-table :data="materials" border size="small">
        <el-table-column prop="materialName" label="材料名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="checkResult" label="校验" width="80" align="center" />
        <el-table-column label="来源" width="92" align="center">
          <template #default="{ row }">
            <el-tag :type="row.source === '平台同步' ? 'success' : 'info'" effect="light" size="small">{{ row.source || '用户上传' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="原件" width="150">
          <template #default="{ row }">
            <!-- 平台同步材料原件存于数据资产管理平台,本系统无本地原件,显示平台附件名(不可下载) -->
            <span v-if="row.source === '平台同步'" style="color:#67c23a" :title="row.fileName">{{ row.fileName }}（平台原件）</span>
            <el-link v-else-if="row.fileName" type="primary" @click="preview(row)">查看</el-link>
            <span v-else style="color:#bbb">-</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="rv-h">进度轨迹</div>
      <el-timeline v-if="logs.length" style="padding:6px">
        <el-timeline-item v-for="l in logs" :key="l.logId" :timestamp="fmt(l.createTime)" placement="top"
          :type="l.toStatus === '已驳回' ? 'danger' : (l.toStatus === '已完成' ? 'success' : 'primary')">
          {{ l.nodeName }}：{{ l.fromStatus }} → {{ l.toStatus }} · 责任人 {{ l.responder }}
          <div v-if="l.opinion" style="font-size:12px;color:#71717a">意见：{{ l.opinion }}</div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else :image-size="50" description="暂无流转记录" />

      <template #footer>
        <template v-if="canAct(cur.status)">
          <el-button type="success" @click="onApprove(cur, true)">审批通过</el-button>
          <el-button type="danger" @click="onReject(cur, true)">驳回</el-button>
        </template>
        <el-tag v-else type="info" effect="plain" style="margin-right:8px">本节点须「{{ needRoleLabel(cur.status) }}」处理</el-tag>
        <el-button @click="drawer = false">关闭</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageConfirmApply, approveConfirm, rejectConfirm, batchApproveConfirm, batchRejectConfirm, listMaterialByApply, getConfirmFlowLog, materialFileUrl } from '@/api/confirm'
import { openFilePreview } from '@/composables/useFilePreview'
import { currentRole } from '@/lib/roles'

// 逐节点角色门禁(与后端一致):每个节点仅对应角色(及 all/admin)可审批/驳回
const NODE_ROLE = { 人工预审中: 'precheck', 合规审核中: 'review', 主管复核中: 'manager', 经理终审中: 'director' }
const ROLE_LABEL = { precheck: '人工预审员', review: '合规管控小组', manager: '数字化部主管', director: '经理/高级经理' }
function canAct(status) {
  const r = currentRole()
  if (r === 'all' || r === 'admin') return true
  const need = NODE_ROLE[status]
  return !need || need === r
}
function needRoleLabel(status) { return ROLE_LABEL[NODE_ROLE[status]] || '' }

const rows = ref([]); const loading = ref(false); const sel = ref([])
const drawer = ref(false); const cur = ref({}); const materials = ref([]); const logs = ref([])
const reviewing = computed(() => rows.value.filter(r => ['人工预审中', '合规审核中', '主管复核中', '经理终审中'].includes(r.status)))
// 人工预审依据:解析提交时固化的 AI 校验结果快照(JSON)
const aiSnap = computed(() => {
  const s = cur.value && cur.value.aiSnapshot
  if (!s) return null
  try { return typeof s === 'string' ? JSON.parse(s) : s } catch (e) { return null }
})
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
function preview(row) { if (row.materialId) openFilePreview(materialFileUrl(row.materialId), row.fileName) }
// 从向导"去审核"带 applyId 跳入时高亮目标单
import { useRoute } from 'vue-router'
const route = useRoute()
function rowHl({ row }) { return route.query.applyId && row.applyId === route.query.applyId ? 'hl-row' : '' }

onMounted(load)
</script>

<style scoped>
.rv-h { font-weight: 600; margin: 16px 0 8px; }
:deep(.hl-row) { background: var(--prm-color-selected-bg, #eff7ff) !important; outline: 1px solid var(--prm-color-primary, #1886ff); }
</style>
