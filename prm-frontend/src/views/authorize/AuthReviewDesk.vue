<template>
  <div class="prm-page">
    <div class="prm-table-card">
      <div class="prm-table-note" style="margin:0 0 12px 0">注:授权审核工作台,支持单条/批量审批(可录审核意见)、查看详情与处理记录;终审通过自动签发授权证书并开通底层权限。</div>
      <div style="margin-bottom:12px">
        <el-button type="success" :disabled="!sel.length" @click="onBatchApprove">批量通过（{{ sel.length }}）</el-button>
        <el-button type="danger" :disabled="!sel.length" @click="onBatchReject">批量驳回（{{ sel.length }}）</el-button>
      </div>
      <el-table :row-class-name="rowHl" :data="reviewing" v-loading="loading" border stripe @selection-change="s => sel = s">
        <el-table-column type="selection" width="46" />
        <el-table-column type="index" label="序号" width="56" align="center" />
        <el-table-column prop="applyNo" label="申请编号" width="150" show-overflow-tooltip />
        <el-table-column prop="authMode" label="模式" width="100" align="center" />
        <el-table-column prop="assetName" label="资产名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="granteeOrg" label="被授权方" min-width="130" show-overflow-tooltip />
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

    <el-drawer v-model="drawer" :title="`审核详情 — ${cur.applyNo || ''}`" size="46%">
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="资产名称" :span="2">{{ cur.assetName }}（{{ cur.assetId }}）</el-descriptions-item>
        <el-descriptions-item label="模式">{{ cur.authMode }}</el-descriptions-item>
        <el-descriptions-item label="权益类型">{{ cur.rightType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="被授权方">{{ cur.granteeOrg || '-' }}</el-descriptions-item>
        <el-descriptions-item label="当前环节">{{ cur.status }}</el-descriptions-item>
        <el-descriptions-item label="使用场景" :span="2">{{ cur.scenario || '-' }}</el-descriptions-item>
        <el-descriptions-item label="授权范围" :span="2">{{ cur.scope || '-' }}</el-descriptions-item>
      </el-descriptions>

      <!-- 大模型校验机制完善(南网):快照验真 + 校验规则可视化 + 校验过程回放 -->
      <div class="rv-h">
        大模型校验（人工审核依据）
        <el-tag v-if="snapVerify" :type="snapVerify.verified ? 'success' : (snapVerify.payloadSm3 ? 'danger' : 'info')" size="small" effect="dark" style="margin-left:8px">
          {{ snapVerify.verified ? '✔ 快照完整·未被篡改' : (snapVerify.payloadSm3 ? '✘ 疑似篡改' : '无防篡改快照') }}
        </el-tag>
        <span v-if="snapVerify && snapVerify.evidenceId" style="font-size:12px;color:#9ca3af;margin-left:8px">存证 {{ snapVerify.evidenceId.slice(0,12) }}… · 留痕 {{ snapVerify.aiRunCount ?? 0 }} 次</span>
      </div>
      <!-- §1 校验规则可视化:逐应交项 校验逻辑 + 规则明细 + AI 判定依据 -->
      <el-collapse v-if="checkLogic && checkLogic.items && checkLogic.items.length" accordion>
        <el-collapse-item v-for="(it, i) in checkLogic.items" :key="i">
          <template #title>
            <span style="font-weight:600">{{ it.materialName }}</span>
            <el-tag size="small" :type="it.materialPresent ? 'success' : 'warning'" effect="light" style="margin-left:8px">{{ it.materialPresent ? '已交' : '待补' }}</el-tag>
            <el-tag v-if="it.aiVerdict" size="small" :type="it.aiVerdict === '通过' ? 'success' : (it.aiVerdict === '不通过' ? 'danger' : 'info')" effect="light" style="margin-left:6px">AI:{{ it.aiVerdict }}</el-tag>
          </template>
          <div style="font-size:13px;line-height:1.8">
            <div><b>校验逻辑(触发规则):</b>{{ it.triggerLabel }}<el-tag size="small" effect="plain" style="margin-left:6px">{{ it.required }}</el-tag></div>
            <div><b>规则明细:</b>{{ it.ruleDetail || '—' }}</div>
            <div><b>判定依据(AI):</b>{{ it.aiIssues || (it.aiVerdict ? ('AI 结论:' + it.aiVerdict) : '该项尚无 AI 判定留痕') }}</div>
          </div>
        </el-collapse-item>
      </el-collapse>
      <div v-if="checkLogic && checkLogic.summary" style="font-size:12px;color:#9ca3af;margin:4px 0">{{ checkLogic.summary }} · 模型 {{ checkLogic.aiModel }}</div>
      <!-- §2 AI 校验过程回放:大模型操作留痕时间线 -->
      <el-timeline v-if="aiRunlog && aiRunlog.length" style="padding:6px">
        <el-timeline-item v-for="(l, i) in aiRunlog" :key="i" :timestamp="fmt(l.createTime)" placement="top" type="primary">
          <div style="font-size:13px">
            <el-tag size="small" effect="dark" style="margin-right:6px">{{ l.capability }}</el-tag>
            <span style="color:#606266">模型 {{ l.model }} · 耗时 {{ l.durationMs }}ms · 触发 {{ l.triggerUser }}</span>
            <div style="font-size:12px;color:#9ca3af">SM3 {{ (l.sm3Hash || '').slice(0,16) }}…(输出防篡改指纹)</div>
          </div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else-if="!checkLogic || !checkLogic.items || !checkLogic.items.length" :image-size="40" description="该申请暂无大模型校验留痕(未经 AI 校验)" />

      <div class="rv-h">处理记录（审批轨迹）</div>
      <el-timeline v-if="logs.length" style="padding:6px">
        <el-timeline-item v-for="l in logs" :key="l.logId" :timestamp="fmt(l.createTime)" placement="top"
          :type="l.toStatus === '已驳回' ? 'danger' : (l.toStatus === '已生效' ? 'success' : 'primary')">
          <div style="font-weight:600">{{ l.fromStatus }} → {{ l.toStatus }}</div>
          <div style="font-size:12px;color:#71717a">责任人：{{ l.responder || '-' }}</div>
          <div v-if="l.opinion" style="font-size:12px;color:#71717a">审核意见：{{ l.opinion }}</div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else :image-size="50" description="暂无处理记录" />

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
import { pageAuthApply, approveAuth, rejectAuth, batchApproveAuth, batchRejectAuth, getAuthFlowLog, getAuthAiCheckLogic, getAuthAiRunlog, verifyAuthAiSnapshot } from '@/api/authorize'

const rows = ref([]); const loading = ref(false); const sel = ref([])
const drawer = ref(false); const cur = ref({}); const logs = ref([])
// 大模型校验机制完善(授权侧):校验规则可视化 / 回放 / 快照防篡改验真
const checkLogic = ref(null); const aiRunlog = ref([]); const snapVerify = ref(null)
const PENDING = ['合规审核中', '业务审核中', '主管审核中', '经理审核中', '副总审批中', '领导小组审批中']
const reviewing = computed(() => rows.value.filter(r => PENDING.includes(r.status)))
function fmt(t) { return t ? String(t).replace('T', ' ').slice(0, 19) : '-' }

async function load() {
  loading.value = true
  try { const r = await pageAuthApply({ current: 1, size: 100 }); rows.value = r.records || [] } finally { loading.value = false }
}
function onApprove(row, fromDrawer) {
  ElMessageBox.prompt('请输入审核意见(可空,默认"同意")', '审批通过', { inputType: 'textarea', inputValue: '' })
    .then(async ({ value }) => {
      const certId = await approveAuth(row.applyId, value || '')
      ElMessage.success(certId ? '终审通过,已签发授权证书' : '本级审批通过,进入下一环节')
      if (fromDrawer) drawer.value = false
      load()
    }).catch(() => {})
}
function onReject(row, fromDrawer) {
  ElMessageBox.prompt('请输入驳回原因', '驳回', {})
    .then(async ({ value }) => { await rejectAuth(row.applyId, value); ElMessage.success('已驳回'); if (fromDrawer) drawer.value = false; load() }).catch(() => {})
}
async function onBatchApprove() {
  const r = await batchApproveAuth(sel.value.map(x => x.applyId))
  ElMessage[r.failed ? 'warning' : 'success'](`批量通过:成功 ${r.success}/${r.total}${r.failed ? '，失败 ' + r.failed : ''}`)
  load()
}
function onBatchReject() {
  ElMessageBox.prompt('请输入统一驳回原因', '批量驳回', {})
    .then(async ({ value }) => {
      const r = await batchRejectAuth(sel.value.map(x => x.applyId), value)
      ElMessage[r.failed ? 'warning' : 'success'](`批量驳回:成功 ${r.success}/${r.total}${r.failed ? '，失败 ' + r.failed : ''}`)
      load()
    }).catch(() => {})
}
async function onDetail(row) {
  cur.value = row
  checkLogic.value = null; aiRunlog.value = []; snapVerify.value = null
  logs.value = await getAuthFlowLog(row.applyId) || []
  drawer.value = true
  // 大模型校验机制(规则可视化 / 回放 / 快照验真):best-effort,任一失败不影响详情
  getAuthAiCheckLogic(row.applyId).then(r => { checkLogic.value = r }).catch(() => {})
  getAuthAiRunlog(row.applyId).then(r => { aiRunlog.value = r || [] }).catch(() => {})
  verifyAuthAiSnapshot(row.applyId).then(r => { snapVerify.value = r }).catch(() => {})
}
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
