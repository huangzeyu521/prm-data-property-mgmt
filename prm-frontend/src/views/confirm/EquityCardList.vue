<template>
  <div class="prm-page">
    <div class="prm-table-card">
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column type="index" label="序号" width="64" align="center" />
        <el-table-column prop="cardNo" label="权益卡片编码" width="200" show-overflow-tooltip />
        <el-table-column prop="assetName" label="资产名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="rightType" label="权益类型" width="130" />
        <el-table-column prop="rightOwner" label="权益所有者" min-width="160" show-overflow-tooltip />
        <el-table-column prop="validDate" label="有效期至" width="150">
          <template #default="{ row }">{{ row.validDate ? String(row.validDate).replace('T',' ').slice(0,10) : '长期' }}</template>
        </el-table-column>
        <el-table-column prop="cardStatus" label="状态" width="90" align="center">
          <template #default="{ row }"><el-tag :type="statusTag(row.cardStatus)">{{ row.cardStatus }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="360" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="onDetail(row)">详情</el-button>
            <el-button link type="primary" @click="onPreview(row)">预览证书</el-button>
            <el-button link type="warning" :disabled="row.cardStatus !== '正常'" @click="onFreeze(row)">冻结</el-button>
            <el-button link type="success" :disabled="row.cardStatus !== '冻结'" @click="onUnfreeze(row)">解冻</el-button>
            <el-button link type="danger" :disabled="row.cardStatus === '失效'" @click="onRevoke(row)">注销</el-button>
            <el-button link type="primary" @click="onLogs(row)">变更史</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="prm-table-note">注:冻结/失效的权益卡片不可用于下游数据授权(风险熔断);全程变更留痕(权益可追溯 3.2.6)。</div>
      <el-pagination style="margin-top:16px;justify-content:flex-end" background layout="total, prev, pager, next"
        :total="total" :current-page="query.current" :page-size="query.size" @current-change="onPage" />
    </div>
    <el-dialog v-model="logDlg" title="权益卡片变更历史" width="560px" align-center>
      <el-timeline>
        <el-timeline-item v-for="l in logs" :key="l.logId" :timestamp="String(l.createTime).replace('T',' ').slice(0,19)">
          <b>{{ l.action }}</b>　{{ l.fromStatus || '—' }} → {{ l.toStatus }}　<span style="color:#909399">{{ l.reason }}</span>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-if="!logs.length" description="暂无变更记录" />
    </el-dialog>

    <!-- 结构化凭证详情 -->
    <el-dialog v-model="detailDlg" title="权益卡片结构化详情" width="660px" align-center>
      <el-descriptions title="卡片信息" :column="2" border size="small">
        <el-descriptions-item label="卡片编码" :span="2">{{ cur.cardNo }}</el-descriptions-item>
        <el-descriptions-item label="状态"><el-tag :type="statusTag(cur.cardStatus)">{{ cur.cardStatus }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="有效期至">{{ fmtDate(cur.validDate) }}</el-descriptions-item>
      </el-descriptions>
      <el-descriptions title="资产属性" :column="2" border size="small" style="margin-top:12px">
        <el-descriptions-item label="资产名称">{{ cur.assetName }}</el-descriptions-item>
        <el-descriptions-item label="资产ID">{{ cur.assetId }}</el-descriptions-item>
        <el-descriptions-item label="所属系统">{{ asset.systemName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="模式名称">{{ asset.schemaName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="资产类型">{{ asset.assetType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="安全等级">{{ asset.securityLevel || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-descriptions title="权属关系" :column="2" border size="small" style="margin-top:12px">
        <el-descriptions-item label="权属类型">{{ cur.rightType }}</el-descriptions-item>
        <el-descriptions-item label="权属来源">{{ cur.rightSource || '确权认定' }}</el-descriptions-item>
        <el-descriptions-item label="权利主体">{{ cur.rightOwner }}</el-descriptions-item>
        <el-descriptions-item label="权利客体">{{ cur.assetName }}</el-descriptions-item>
        <el-descriptions-item label="责任部门" :span="2">{{ asset.respDept || '-' }}{{ asset.subsidiaryName ? '（' + asset.subsidiaryName + '）' : '' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer><el-button type="primary" @click="onPreview(cur)">预览证书</el-button><el-button @click="detailDlg=false">关闭</el-button></template>
    </el-dialog>

    <!-- 在线预览证书 -->
    <el-dialog v-model="certDlg" title="数据资产确权权属凭证 · 在线预览" width="600px" align-center>
      <div class="cert" id="cert-print">
        <div class="cert-title">数据资产确权权属凭证</div>
        <div class="cert-sub">中国南方电网有限责任公司 · 数据产权管理平台</div>
        <table class="cert-tbl">
          <tr><td class="k">凭证编号</td><td>{{ cur.cardNo }}</td></tr>
          <tr><td class="k">数据资产</td><td>{{ cur.assetName }}（{{ cur.assetId }}）</td></tr>
          <tr><td class="k">所属系统/模式</td><td>{{ asset.systemName || '-' }} / {{ asset.schemaName || '-' }}</td></tr>
          <tr><td class="k">权属类型</td><td>{{ cur.rightType }}</td></tr>
          <tr><td class="k">权益所有者</td><td>{{ cur.rightOwner }}</td></tr>
          <tr><td class="k">责任部门</td><td>{{ asset.respDept || '-' }}</td></tr>
          <tr><td class="k">权属来源</td><td>{{ cur.rightSource || '确权认定' }}</td></tr>
          <tr><td class="k">有效期至</td><td>{{ fmtDate(cur.validDate) }}</td></tr>
        </table>
        <div class="cert-foot">
          <div class="cert-note">本凭证由数据产权管理平台依"三权分置"确权认定生成，已经区块链 SM3 指纹存证，真伪可溯。</div>
          <div class="cert-seal">确权专用章</div>
        </div>
      </div>
      <template #footer><el-button @click="printCert">打印 / 另存</el-button><el-button @click="certDlg=false">关闭</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageEquityCard, freezeEquityCard, unfreezeEquityCard, revokeEquityCard, equityCardLogs } from '@/api/confirm'
import { getAsset } from '@/api/ledger'

const query = reactive({ current: 1, size: 10 })
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const logDlg = ref(false)
const logs = ref([])
const detailDlg = ref(false); const certDlg = ref(false)
const cur = ref({}); const asset = ref({})

function statusTag(s) {
  return { 正常: 'success', 冻结: 'warning', 失效: 'danger' }[s] || 'info'
}
function fmtDate(t) { return t ? String(t).replace('T', ' ').slice(0, 10) : '长期' }
async function loadAsset(assetId) { asset.value = assetId ? (await getAsset(assetId).catch(() => ({}))) || {} : {} }
async function onDetail(row) { cur.value = row; await loadAsset(row.assetId); detailDlg.value = true }
async function onPreview(row) { cur.value = row; if (asset.value.assetId !== row.assetId) await loadAsset(row.assetId); certDlg.value = true }
function printCert() { window.print() }
async function load() {
  loading.value = true
  try {
    const res = await pageEquityCard({ ...query })
    rows.value = res.records || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}
function onPage(p) { query.current = p; load() }
function onFreeze(row) {
  ElMessageBox.confirm(`确认冻结权益卡片"${row.cardNo}"吗,冻结后不可用于授权`, '提示', {
    confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
  }).then(async () => { await freezeEquityCard(row.cardId); ElMessage.success('已冻结'); load() }).catch(() => {})
}
function onUnfreeze(row) {
  ElMessageBox.confirm(`确认解冻"${row.cardNo}"恢复正常吗`, '提示', { type: 'warning' })
    .then(async () => { await unfreezeEquityCard(row.cardId); ElMessage.success('已解冻'); load() }).catch(() => {})
}
function onRevoke(row) {
  ElMessageBox.prompt(`注销不可逆,请输入注销原因`, `注销权益卡片 ${row.cardNo}`, { inputType: 'textarea' })
    .then(async ({ value }) => { await revokeEquityCard(row.cardId, value); ElMessage.success('已注销'); load() }).catch(() => {})
}
async function onLogs(row) { logs.value = await equityCardLogs(row.cardId); logDlg.value = true }

onMounted(load)
</script>

<style scoped>
.cert { border: 2px solid #b8893a; border-radius: 8px; padding: 24px 28px; background: linear-gradient(180deg, #fffdf7, #fdf6e8); }
.cert-title { text-align: center; font-size: 22px; font-weight: 800; letter-spacing: 4px; color: #8a5a1a; }
.cert-sub { text-align: center; font-size: 12px; color: #a07b3a; margin: 6px 0 18px; }
.cert-tbl { width: 100%; border-collapse: collapse; }
.cert-tbl td { border: 1px solid #e3cfa0; padding: 9px 12px; font-size: 13px; }
.cert-tbl td.k { width: 130px; background: #faf3e2; color: #6b5320; font-weight: 600; }
.cert-foot { display: flex; align-items: flex-end; justify-content: space-between; margin-top: 18px; gap: 16px; }
.cert-note { font-size: 11px; color: #9a8048; max-width: 60%; line-height: 1.5; }
.cert-seal {
  width: 96px; height: 96px; border: 2px solid #c0392b; border-radius: 50%;
  color: #c0392b; display: flex; align-items: center; justify-content: center;
  font-size: 14px; font-weight: 700; transform: rotate(-12deg); opacity: 0.85; text-align: center; line-height: 1.2;
}
</style>
