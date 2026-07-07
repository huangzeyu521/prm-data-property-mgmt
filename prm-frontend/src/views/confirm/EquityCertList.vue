<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
<template>
  <div class="prm-page">
    <el-tabs v-model="tab">
      <el-tab-pane label="权益证书" name="cert">
        <!-- 概览统计条:总/生效/已注销/待出证(正常卡未出证)-->
        <div class="stat-bar">
          <div v-for="c in statCards" :key="c.key" class="stat-card" :class="[c.cls, { active: activeStat === c.key, clickable: c.click !== false }]"
            @click="c.click !== false && onStatClick(c)">
            <div class="stat-num">{{ certStat[c.field] }}</div>
            <div class="stat-label">{{ c.label }}</div>
          </div>
        </div>
        <div class="prm-table-card">
          <div style="display:flex;flex-wrap:wrap;align-items:center;gap:10px;margin-bottom:10px">
            <el-select v-model="issueCardId" filterable placeholder="选择待出证权益卡片(正常·未出证)" style="width:380px">
              <el-option v-for="c in pendingCards" :key="c.cardId" :label="`${sysName(c)} · ${rightTag(c.rightType).short} · ${c.cardNo}`" :value="c.cardId" />
              <template #empty><div style="padding:8px;color:var(--prm-color-text-weak)">暂无待出证卡片(正常且未出证)</div></template>
            </el-select>
            <el-button type="primary" :disabled="!issueCardId" @click="onIssue">签发证书</el-button>
            <span style="flex:1"></span>
            <el-input v-model="cq.assetName" placeholder="系统名称" clearable style="width:150px" />
            <el-select v-model="cq.rightType" placeholder="权属类型" clearable style="width:120px">
              <el-option label="持有权" value="持有" /><el-option label="使用权" value="使用" /><el-option label="经营权" value="经营" />
            </el-select>
            <el-select v-model="cq.certStatus" placeholder="状态" clearable style="width:110px">
              <el-option label="生效" value="生效" /><el-option label="已注销" value="已注销" />
            </el-select>
          </div>
          <el-table :data="certView" v-loading="loading" border stripe>
            <el-table-column type="index" label="序号" width="56" align="center" />
            <el-table-column prop="certNo" label="证书编号" width="170" show-overflow-tooltip />
            <el-table-column label="系统名称" min-width="120" show-overflow-tooltip>
              <template #default="{ row }">{{ row._card ? sysName(row._card) : '—' }}</template>
            </el-table-column>
            <el-table-column label="库表" min-width="130" show-overflow-tooltip>
              <template #default="{ row }">{{ row._card ? (row._card.tableCode ? row._card.assetName : '全系统') : '—' }}</template>
            </el-table-column>
            <el-table-column label="权属类型" width="96" align="center">
              <template #default="{ row }">
                <el-tooltip v-if="row._card" :content="rightTag(row._card.rightType).full" placement="top">
                  <span :class="'prm-c-' + ((rightTag(row._card.rightType).type) || 'primary')">{{ rightTag(row._card.rightType).short }}</span>
                </el-tooltip>
                <span v-else style="color:var(--prm-color-text-disabled)">—</span>
              </template>
            </el-table-column>
            <el-table-column label="对应权益卡片" width="150" show-overflow-tooltip>
              <template #default="{ row }">{{ row._card ? row._card.cardNo : (row.cardId || '—') }}</template>
            </el-table-column>
            <el-table-column prop="issueUnit" label="签发单位" min-width="150" show-overflow-tooltip />
            <el-table-column prop="issueTime" label="签发时间" width="160">
              <template #default="{ row }">{{ fmtTime(row.issueTime) }}</template>
            </el-table-column>
            <el-table-column prop="certStatus" label="状态" width="90" align="center">
              <template #default="{ row }"><span :class="'prm-c-' + ((row.certStatus==='生效'?'success':'danger') || 'primary')">{{ row.certStatus }}</span></template>
            </el-table-column>
            <el-table-column label="操作" width="160" fixed="right"><template #default="{ row }">
              <el-button link type="primary" @click="onPreviewCert(row)">预览</el-button>
              <el-button link type="danger" :disabled="row.certStatus!=='生效'" @click="onRevoke(row)">注销</el-button>
            </template></el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
      <el-tab-pane label="证书模板" name="tpl">
        <div class="prm-table-card">
          <div style="margin-bottom:10px"><el-button type="primary" @click="onAddTpl">新增模板</el-button></div>
          <el-table :data="tpls" v-loading="loading2" border stripe>
            <el-table-column type="index" label="序号" width="64" align="center" />
            <el-table-column prop="templateName" label="模板名称" min-width="180" show-overflow-tooltip />
            <el-table-column prop="rightType" label="适用权益" width="150" />
            <el-table-column prop="templateVersion" label="版本" width="80" align="center" />
            <el-table-column prop="templateStatus" label="状态" width="90" align="center">
              <template #default="{ row }"><span :class="'prm-c-' + ((row.templateStatus==='生效中'?'success':'warning') || 'primary')">{{ row.templateStatus }}</span></template>
            </el-table-column>
            <el-table-column label="套版文件" min-width="150">
              <template #default="{ row }">
                <el-link v-if="row.fileName" type="primary" @click="onDownloadTpl(row)">{{ row.fileName }}</el-link>
                <span v-else style="color:var(--prm-color-text-disabled)">未上传</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="290" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="onEditTpl(row)">修改</el-button>
                <el-upload :show-file-list="false" :http-request="(o)=>doUploadTpl(row, o.file)" accept=".pdf,.doc,.docx,.png,.jpg,.jpeg" style="display:inline-block;margin:0 8px">
                  <el-button link type="primary">上传</el-button>
                </el-upload>
                <el-button link type="success" :disabled="row.templateStatus==='生效中'" @click="onEnable(row)">启用</el-button>
                <el-button link type="warning" :disabled="row.templateStatus==='停用'" @click="onDisable(row)">停用</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="prm-table-note">注:已用于出证的模板禁物理删除,仅可停用并生成新版本。</div>
        </div>
      </el-tab-pane>
    </el-tabs>
    <el-dialog v-model="dlg" :title="form.templateId ? '修改证书模板（保存自增版本）' : '新增证书模板'" width="560px" align-center>
      <el-form :model="form" label-width="100px">
        <el-form-item label="模板名称"><el-input v-model="form.templateName" /></el-form-item>
        <el-form-item label="适用权益">
          <el-select v-model="form.rightType" style="width:100%">
            <el-option v-for="t in rightTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="模板内容">
          <el-input v-model="form.templateContent" type="textarea" :rows="5" placeholder="证书正文/格式说明（可含占位：数据资产、权益类型、权益所有者、有效期、签发单位等）" />
        </el-form-item>
      </el-form>
      <template #footer><el-button type="primary" @click="onSaveTpl">确定</el-button><el-button @click="dlg=false">取消</el-button></template>
    </el-dialog>

    <!-- 在线预览证书内容 -->
    <el-dialog v-model="certDlg" title="确权权益证书 · 在线预览" width="620px" align-center>
      <div class="cert" id="cert-print">
        <div class="cert-title">数据资产确权权益证书</div>
        <div class="cert-sub">{{ certVO.issueUnit || '中国南方电网有限责任公司' }}</div>
        <div class="cert-no">证书编号（唯一）：{{ certVO.certNo }}</div>
        <table class="cert-tbl">
          <tbody>
          <tr><td class="k">所属系统 / 模式</td><td>{{ certSys }}{{ certVO.schemaName ? ' / ' + certVO.schemaName : '' }}</td></tr>
          <tr><td class="k">库表(数据资产卡片)</td><td>{{ certVO.assetName || '全系统' }}</td></tr>
          <tr><td class="k">确权范围</td><td>{{ certVO.scope || '全系统库表' }}</td></tr>
          <tr><td class="k">权益类型</td><td>{{ rightTag(certVO.rightType).full }}</td></tr>
          <tr><td class="k">权益主体</td><td>{{ certVO.rightOwner }}<span v-if="certVO.consolidatedUnit"> · 归口 {{ certVO.consolidatedUnit }}</span></td></tr>
          <tr><td class="k">权益取得方式</td><td>{{ certVO.acquireMode || '认定' }}{{ certVO.authorizingUnit ? '(授权单位:' + certVO.authorizingUnit + ')' : '' }}</td></tr>
          <tr v-if="certVO.rightsContent"><td class="k">权益内容摘要</td><td>{{ certVO.rightsContent }}</td></tr>
          <tr v-if="certVO.rightsCredential"><td class="k">权益凭证</td><td>{{ certVO.rightsCredential }}</td></tr>
          <tr><td class="k">对应权益卡片</td><td>{{ certVO.cardNo }}</td></tr>
          <tr><td class="k">确权时间 / 有效期至</td><td>{{ fmtDate(certVO.confirmTime) }} ～ {{ fmtDate(certVO.validDate) }}</td></tr>
          <tr><td class="k">适用模板</td><td>{{ certVO.templateName || '标准权益证书' }}</td></tr>
          <tr><td class="k">签发时间</td><td>{{ fmtTime(certVO.issueTime) }}</td></tr>
          </tbody>
        </table>
        <div v-if="certVO.templateContent" class="cert-body">{{ certVO.templateContent }}</div>
        <div class="cert-foot">
          <div class="cert-note">本证书依确权审批结果与配置模板自动生成，与权益信息一一对应，已经区块链 SM3 指纹存证，真伪可溯。</div>
          <div class="cert-seal">确权专用章</div>
        </div>
      </div>
      <template #footer><el-button @click="printCert">打印 / 另存</el-button><el-button @click="certDlg=false">关闭</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup>
import { onMounted, reactive, ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { pageCert, issueCert, revokeCert, getCertRender, pageEquityCard, pageCertTemplate, createCertTemplate, updateCertTemplate, enableCertTemplate, disableCertTemplate, uploadCertTemplateFile, certTemplateFileUrl } from '@/api/confirm'
import { openFilePreview } from '@/composables/useFilePreview'
const rightTypes = ['持有权', '使用权', '经营权']
const tab = ref('cert')
const issueCardId = ref('')
const certs = ref([]); const cards = ref([]); const loading = ref(false)
const tpls = ref([]); const loading2 = ref(false)
const dlg = ref(false); const form = reactive({ templateId: '', templateName: '', rightType: '', templateContent: '' })
const certDlg = ref(false); const certVO = ref({})
const cq = reactive({ assetName: '', rightType: '', certStatus: '' })
const activeStat = ref('')

// 系统名(SYS: 派生)+ 单权标签(兼容"持有权/持有权")
function sysName(o) { const id = (o && o.assetId) || ''; return id.startsWith('SYS:') ? id.slice(4) : ((o && o.assetName) || '-') }
function rightTag(rt) {
  const s = String(rt || '')
  if (s.includes('持有')) return { short: '持有权', type: 'primary', full: s }
  if (s.includes('使用') || s.includes('加工')) return { short: '使用权', type: 'success', full: s }
  if (s.includes('经营') || s.includes('产品')) return { short: '经营权', type: 'warning', full: s }
  return { short: s || '—', type: 'info', full: s }
}
// cardId → card 映射(供证书富化 + 选择器 + 预览覆盖范围)
const cardMap = computed(() => Object.fromEntries(cards.value.map(c => [c.cardId, c])))
// 证书富化:挂上对应卡片(系统/权属/卡号/覆盖范围)
const certEnriched = computed(() => certs.value.map(c => ({ ...c, _card: cardMap.value[c.cardId] || null })))
// 客户端筛选(系统名/权属/状态)
const certView = computed(() => certEnriched.value.filter(c => {
  const card = c._card || {}
  const okSys = !cq.assetName || sysName(card).includes(cq.assetName)
  const okRt = !cq.rightType || String(card.rightType || '').includes(cq.rightType)
  const okSt = !cq.certStatus || c.certStatus === cq.certStatus
  return okSys && okRt && okSt
}))
// 待出证卡片 = 正常卡 且 无生效证书
const effectiveCardIds = computed(() => new Set(certs.value.filter(c => c.certStatus === '生效').map(c => c.cardId)))
const pendingCards = computed(() => cards.value.filter(c => c.cardStatus === '正常' && !effectiveCardIds.value.has(c.cardId)))

// 概览统计
const certStat = computed(() => ({
  total: certs.value.length,
  effective: certs.value.filter(c => c.certStatus === '生效').length,
  revoked: certs.value.filter(c => c.certStatus === '已注销').length,
  pending: pendingCards.value.length
}))
const statCards = [
  { key: 'total', label: '总证书', field: 'total', cls: 'c-total', click: 'clear' },
  { key: 'effective', label: '生效', field: 'effective', cls: 'c-done', click: { certStatus: '生效' } },
  { key: 'revoked', label: '已注销', field: 'revoked', cls: 'c-reject', click: { certStatus: '已注销' } },
  { key: 'pending', label: '待出证', field: 'pending', cls: 'c-review', click: false }
]
function onStatClick(c) {
  if (c.click === 'clear') { cq.certStatus = ''; activeStat.value = '' }
  else { cq.certStatus = c.click.certStatus; activeStat.value = c.key }
}

// 预览证书:render VO + 合并卡片覆盖范围;系统名洁净显示
const certSys = computed(() => {
  const v = certVO.value || {}
  const id = v.assetId || ''
  return id.startsWith('SYS:') ? id.slice(4) : (v.assetName || '-')
})
function fmtDate(t) { return t ? String(t).replace('T', ' ').slice(0, 10) : '长期' }
function fmtTime(t) { return t ? String(t).replace('T', ' ').slice(0, 19) : '-' }
async function onPreviewCert(row) {
  const v = await getCertRender(row.certId) || {}
  // 合并对应权益卡片的表4 权益要素(render VO 未含 scope/模式/权益内容等)
  const c = row._card
  if (c) {
    v.scope = c.scope || v.scope
    v.schemaName = c.schemaName
    v.rightsContent = c.rightsContent
    v.rightsCredential = c.rightsCredential
    v.acquireMode = c.acquireMode
    v.authorizingUnit = c.authorizingUnit
    v.consolidatedUnit = c.consolidatedUnit
    v.confirmTime = c.confirmTime
  }
  certVO.value = v; certDlg.value = true
}
function printCert() { window.print() }
async function loadCerts() {
  loading.value = true
  try {
    const [rc, rk] = await Promise.all([pageCert({ current: 1, size: 100 }), pageEquityCard({ current: 1, size: 100 })])
    certs.value = rc.records || []
    cards.value = rk.records || []
  } finally { loading.value = false }
}
async function loadTpls() { loading2.value = true; try { const r = await pageCertTemplate({ current: 1, size: 50 }); tpls.value = r.records || [] } finally { loading2.value = false } }
async function onIssue() {
  if (!issueCardId.value) return
  await issueCert({ cardId: issueCardId.value }); ElMessage.success('已签发证书'); issueCardId.value = ''; loadCerts()
}
async function onRevoke(row) { await revokeCert(row.certId); ElMessage.success('已注销'); loadCerts() }
function onAddTpl() { Object.assign(form, { templateId: '', templateName: '', rightType: '', templateContent: '' }); dlg.value = true }
function onEditTpl(row) { Object.assign(form, { templateId: row.templateId, templateName: row.templateName, rightType: row.rightType, templateContent: row.templateContent || '' }); dlg.value = true }
async function onSaveTpl() {
  if (form.templateId) { await updateCertTemplate({ ...form }); ElMessage.success('已修改，版本自增') }
  else { await createCertTemplate({ ...form }); ElMessage.success('已新增') }
  dlg.value = false; loadTpls()
}
async function doUploadTpl(row, file) {
  const fd = new FormData(); fd.append('file', file)
  await uploadCertTemplateFile(row.templateId, fd)
  ElMessage.success('套版文件已上传'); loadTpls()
}
function onDownloadTpl(row) { if (row.fileName) openFilePreview(certTemplateFileUrl(row.templateId), row.fileName) }
async function onEnable(row) { await enableCertTemplate(row.templateId); ElMessage.success('已启用'); loadTpls() }
async function onDisable(row) { await disableCertTemplate(row.templateId); ElMessage.success('已停用'); loadTpls() }
onMounted(() => { loadCerts(); loadTpls() })
</script>

<style scoped>
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

.cert { border: 2px solid #b8893a; border-radius: 8px; padding: 24px 28px; background: linear-gradient(180deg, #fffdf7, #fdf6e8); }
.cert-title { text-align: center; font-size: 22px; font-weight: 800; letter-spacing: 4px; color: #8a5a1a; }
.cert-sub { text-align: center; font-size: 13px; color: #a07b3a; margin: 6px 0 4px; }
.cert-no { text-align: center; font-size: 12px; color: #c0392b; font-weight: 600; margin-bottom: 16px; letter-spacing: 1px; }
.cert-tbl { width: 100%; border-collapse: collapse; }
.cert-tbl td { border: 1px solid #e3cfa0; padding: 9px 12px; font-size: 13px; }
.cert-tbl td.k { width: 120px; background: #faf3e2; color: #6b5320; font-weight: 600; }
.cert-body { margin-top: 12px; font-size: 12px; color: #6b5320; line-height: 1.7; white-space: pre-wrap; }
.cert-foot { display: flex; align-items: flex-end; justify-content: space-between; margin-top: 18px; gap: 16px; }
.cert-note { font-size: 11px; color: #9a8048; max-width: 62%; line-height: 1.5; }
.cert-seal {
  width: 96px; height: 96px; border: 2px solid #c0392b; border-radius: 50%;
  color: #c0392b; display: flex; align-items: center; justify-content: center;
  font-size: 14px; font-weight: 700; transform: rotate(-12deg); opacity: 0.85; text-align: center; line-height: 1.2;
}
</style>
