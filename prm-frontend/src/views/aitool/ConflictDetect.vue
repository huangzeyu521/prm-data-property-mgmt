<template>
  <div class="prm-page">
    <el-row :gutter="16">
      <el-col :span="10">
        <el-card header="登记权属主张(构建知识图谱)" shadow="hover">
          <el-form :model="claim" label-width="100px">
            <el-form-item label="资产ID">
              <el-select v-model="claim.assetId" filterable remote allow-create default-first-option clearable
                :remote-method="searchAssets" :loading="assetSearching" style="width:100%"
                placeholder="输名称/ID 搜台账,如 用电 / AST-001" @change="onAssetPicked">
                <el-option v-for="a in assetOpts" :key="a.assetId" :value="a.assetId" :label="a.assetId + '　' + a.assetName">
                  <span>{{ a.assetId }}</span><span style="float:right;color:#8c8c8c;font-size:12px">{{ a.assetName }}</span>
                </el-option>
              </el-select>
              <div class="cd-tip">选定后右侧查询与下方图谱自动跟随该资产;
                <el-button link type="primary" style="vertical-align:baseline" :loading="demoRunning" @click="runDemo">一键演示:登记两条对立主张并检测(测试/演示)</el-button>
              </div>
            </el-form-item>
            <el-form-item label="权利主体"><el-input v-model="claim.subject" placeholder="如 广东电网有限责任公司" /></el-form-item>
            <el-form-item label="权利类型">
              <el-select v-model="claim.rightType" style="width:100%">
                <el-option v-for="t in rts" :key="t" :label="t" :value="t" />
              </el-select>
            </el-form-item>
            <el-form-item label="授权范围"><el-input v-model="claim.authScope" placeholder="全字段/约定字段" /></el-form-item>
            <el-form-item label="排他主张"><el-switch v-model="claim.exclusive" /></el-form-item>
            <el-form-item label="来源">
              <el-radio-group v-model="claim.sourceType"><el-radio value="历史确权">历史确权</el-radio><el-radio value="当前申请">当前申请</el-radio><el-radio value="法规政策">法规政策</el-radio><el-radio value="证明材料">证明材料</el-radio></el-radio-group>
            </el-form-item>
            <el-form-item>
              <el-button @click="onAdd">仅登记主张</el-button>
              <el-button type="primary" @click="onDetect">登记并冲突检测</el-button>
            </el-form-item>
          </el-form>
          <el-divider style="margin:6px 0">或 · 条款语义分析自动建主张(#9)</el-divider>
          <div style="display:flex;gap:8px">
            <el-select v-model="semMaterialId" filterable clearable style="flex:1"
              placeholder="选择已解析成功的材料(无需记材料ID)" @focus="loadParsedMaterials">
              <el-option v-for="m in parsedMats" :key="m.materialId" :value="m.materialId" :label="m.fileName">
                <span>{{ m.fileName }}</span><span style="float:right;color:#8c8c8c;font-size:12px">{{ m.materialId.slice(0, 8) }}…</span>
              </el-option>
            </el-select>
            <el-button type="success" :disabled="!semMaterialId" @click="onSemanticClaim">语义建主张</el-button>
          </div>
        </el-card>
      </el-col>
      <el-col :span="14">
        <el-card shadow="hover" id="conflict-report-print">
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span>权属冲突 · {{ assetId || '请检测' }}</span>
              <el-input v-model="qAsset" placeholder="资产ID查询" style="width:200px" @keyup.enter="loadByAsset">
                <template #append><el-button @click="loadByAsset">查询</el-button></template>
              </el-input>
            </div>
          </template>
          <!-- #17 多维筛选 + 报告导出 -->
          <div class="conflict-filter no-print">
            <el-select v-model="filters.conflictType" placeholder="冲突类型" clearable size="small" style="width:130px">
              <el-option v-for="t in conflictTypes" :key="t" :label="t" :value="t" />
            </el-select>
            <el-input v-model="filters.subject" placeholder="主体" clearable size="small" style="width:120px" />
            <el-select v-model="filters.riskLevel" placeholder="风险" clearable size="small" style="width:90px">
              <el-option v-for="l in riskLevels" :key="l" :label="l" :value="l" />
            </el-select>
            <el-date-picker v-model="filters.startTime" type="date" placeholder="开始" value-format="YYYY-MM-DD" size="small" style="width:130px" />
            <el-date-picker v-model="filters.endTime" type="date" placeholder="结束" value-format="YYYY-MM-DD" size="small" style="width:130px" />
            <el-button size="small" type="primary" :disabled="!assetId" @click="refresh(assetId)">筛选</el-button>
            <el-button size="small" type="warning" :disabled="!assetId" @click="onExportWord">导出Word</el-button>
            <el-button size="small" :disabled="!assetId" @click="onPrintPdf">打印/PDF</el-button>
          </div>
          <!-- 打印态报告抬头(仅打印可见) -->
          <div v-if="report" class="print-only print-head">
            <h2>权属冲突分析报告</h2>
            <div>资产:{{ assetId }} ·
              筛选:{{ [filters.conflictType, filters.subject, filters.riskLevel, (filters.startTime||filters.endTime)?(filters.startTime+'~'+filters.endTime):''].filter(Boolean).join(' / ') || '全部' }}</div>
          </div>
          <el-alert v-if="report" :title="(report.decision ? report.decision + ' · ' : '') + report.conclusion"
                    :type="report.highRiskCount>0?'error':(report.total>0?'warning':'success')" :closable="false" show-icon style="margin-bottom:10px" />
          <!-- #15 结构化报告:决策建议 + 来源/影响范围汇总 + 高风险摘要 -->
          <el-descriptions v-if="report && report.total>0" :column="2" size="small" border style="margin-bottom:10px">
            <el-descriptions-item label="决策建议">
              <el-tag :type="report.decision==='建议驳回/暂缓'?'danger':(report.decision==='建议补正后确权'?'warning':'success')">{{ report.decision }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="高风险冲突">{{ report.highRiskCount }} 项</el-descriptions-item>
            <el-descriptions-item label="涉及客体">{{ report.involvedObject }}</el-descriptions-item>
            <el-descriptions-item label="涉及主体">{{ (report.involvedSubjects||[]).join('、') || '—' }}</el-descriptions-item>
            <el-descriptions-item label="按来源汇总" :span="2">
              <el-tag v-for="(v,k) in report.bySource" :key="k" size="small" effect="plain" style="margin-right:6px">{{ k }} · {{ v }}</el-tag>
              <span v-if="!report.bySource || !Object.keys(report.bySource).length">—</span>
            </el-descriptions-item>
            <el-descriptions-item v-if="(report.highRiskSummary||[]).length" label="高风险摘要(优先处置)" :span="2">
              <div v-for="(s,i) in report.highRiskSummary" :key="i" style="color:#d03050;font-size:12px">• {{ s }}</div>
            </el-descriptions-item>
          </el-descriptions>
          <el-table :data="conflicts" border stripe size="small">
            <el-table-column prop="conflictType" label="冲突类型" width="110">
              <template #default="{ row }"><el-tag type="danger" effect="plain">{{ row.conflictType }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="conflictDesc" label="冲突描述" min-width="220" show-overflow-tooltip />
            <el-table-column prop="riskLevel" label="风险" width="70" align="center">
              <template #default="{ row }"><el-tag :type="row.riskLevel==='高'?'danger':'warning'">{{ row.riskLevel }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="suggestion" label="处置建议" min-width="200" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="84" align="center">
              <template #default="{ row }"><el-tag :type="row.status==='已处置'?'success':'info'">{{ row.status }}</el-tag></template>
            </el-table-column>
            <el-table-column label="操作" width="130" fixed="right" class-name="no-print">
              <template #default="{ row }">
                <el-button link type="primary" @click="onAdvice(row)">方案建议</el-button>
                <el-button link type="success" :disabled="row.status==='已处置'" @click="onResolve(row)">处置</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- #16 冲突解决方案建议:规则 + 法规依据 + AI -->
    <el-dialog v-model="adviceDlg" title="冲突解决方案建议" width="560px" align-center>
      <AiThinking v-bind="aiAdvice.state" />
      <template v-if="advice">
        <el-tag type="danger" effect="plain" style="margin-bottom:10px">{{ advice.conflictType }}</el-tag>
        <el-descriptions :column="1" size="small" border>
          <el-descriptions-item label="规则建议">{{ advice.ruleSuggestion }}</el-descriptions-item>
          <el-descriptions-item label="法规依据">{{ advice.regulationBasis }}</el-descriptions-item>
          <el-descriptions-item label="AI 建议">
            <span style="white-space:pre-wrap">{{ advice.aiSuggestion }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </template>
      <template #footer><el-button @click="adviceDlg=false">关闭</el-button></template>
    </el-dialog>

    <!-- #9 知识图谱结构化输出:节点(主体/客体/授权事项/有效期) + 关系(授权/归属/有效期/冲突) -->
    <el-card shadow="hover" style="margin-top:16px">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>电力权属知识图谱 · {{ graphAsset || '请加载' }}</span>
          <div style="display:flex;gap:8px">
            <el-input v-model="graphAsset" placeholder="资产ID" size="small" style="width:170px" />
            <el-button size="small" type="primary" :disabled="!graphAsset" @click="loadGraph">看图谱</el-button>
            <el-button size="small" type="warning" :disabled="!graphAsset" @click="onSyncHistory">同步历史案例</el-button>
          </div>
        </div>
      </template>
      <div ref="graphRef" style="height:340px"></div>
      <el-empty v-if="graphEmpty" :image-size="50" description="该资产暂无权属主张,先登记/语义建主张" />
      <div v-if="claimList.length" style="margin-top:10px;font-weight:600;font-size:13px">节点(权属主张)· 可人工修改/删除</div>
      <el-table v-if="claimList.length" :data="claimList" border size="small" style="margin-top:6px">
        <el-table-column prop="subject" label="主体" min-width="160" show-overflow-tooltip />
        <el-table-column prop="rightType" label="权利类型" width="130" />
        <el-table-column prop="authScope" label="授权范围" width="110" />
        <el-table-column prop="sourceType" label="来源" width="100" align="center">
          <template #default="{ row }"><el-tag size="small" :type="srcTag(row.sourceType)">{{ row.sourceType }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="onEditClaim(row)">编辑</el-button>
            <el-button link type="danger" @click="onDeleteClaim(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="editDlg" title="修改权属主张(节点与关系)" width="460px" align-center>
      <el-form :model="editClaim" label-width="100px">
        <el-form-item label="权利主体"><el-input v-model="editClaim.subject" /></el-form-item>
        <el-form-item label="权利类型"><el-select v-model="editClaim.rightType" style="width:100%"><el-option v-for="t in rts" :key="t" :label="t" :value="t" /></el-select></el-form-item>
        <el-form-item label="授权范围"><el-input v-model="editClaim.authScope" placeholder="全字段/约定字段" /></el-form-item>
        <el-form-item label="排他主张"><el-switch v-model="editClaim.exclusive" /></el-form-item>
        <el-form-item label="来源"><el-select v-model="editClaim.sourceType" style="width:100%"><el-option v-for="s in ['历史确权','当前申请','法规政策','证明材料']" :key="s" :label="s" :value="s" /></el-select></el-form-item>
      </el-form>
      <template #footer><el-button type="primary" @click="onSaveClaim">保存</el-button><el-button @click="editDlg=false">取消</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, nextTick, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import * as echarts from 'echarts'
import { ElMessage, ElMessageBox } from 'element-plus'
import { aitAddClaim, aitDetectConflict, aitConflicts, aitResolveConflict, aitConflictReport, aitConflictReportExportUrl, buildAitClaimFromMaterial, aitKgGraph, aitClaims, updateAitClaim, deleteAitClaim, syncAitHistoryClaims, aitConflictAdvice } from '@/api/aitool'
import AiThinking from '@/components/AiThinking.vue'
import { useAiThinking } from '@/composables/useAiThinking'
import { AI_PHASES } from '@/lib/aiPhases'

const rts = ['数据持有权', '数据加工使用权', '数据产品经营权', '所有权', '使用权']
const claim = reactive({ assetId: 'DA-DEMO-1', subject: '', rightType: '数据持有权', authScope: '全字段', exclusive: false, sourceType: '当前申请' })
const assetId = ref(''); const qAsset = ref(''); const conflicts = ref([]); const report = ref(null)
// #17 多维筛选
const filters = reactive({ conflictType: '', riskLevel: '', startTime: '', endTime: '', subject: '' })
const conflictTypes = ['主体冲突', '范围冲突', '时效冲突', '历史记录冲突']
const riskLevels = ['高', '中', '低']
// #9 语义建主张 + 知识图谱
const semMaterialId = ref(''); const graphAsset = ref('DA-DEMO-1'); const graphRef = ref(); const graphEmpty = ref(false)
const NODE_COLOR = { 主体: '#2f6bff', 客体: '#13c2c2', 授权事项: '#722ed1', 有效期: '#52c41a' }
const claimList = ref([]); const editDlg = ref(false); const editClaim = reactive({ claimId: '', subject: '', rightType: '', authScope: '', exclusive: false, sourceType: '' })
// #16 冲突解决方案建议
const adviceDlg = ref(false); const advice = ref(null)
const aiAdvice = useAiThinking()
async function onAdvice(row) {
  advice.value = null; adviceDlg.value = true
  advice.value = await aiAdvice.run(() => aitConflictAdvice(row.conflictId),
    { phases: AI_PHASES.conflictAdvice, title: '大模型生成处置建议中' })
}
function srcTag(s) { return { 历史确权: 'info', 当前申请: 'primary', 法规政策: 'warning', 证明材料: 'success' }[s] || 'info' }
async function onSyncHistory() {
  const n = await syncAitHistoryClaims(graphAsset.value)
  ElMessage.success(`历史案例自动同步:新增 ${n} 条历史确权主张`)
  loadGraph()
}
function onEditClaim(row) {
  Object.assign(editClaim, { claimId: row.claimId, subject: row.subject, rightType: row.rightType, authScope: row.authScope, exclusive: !!row.exclusive, sourceType: row.sourceType })
  editDlg.value = true
}
async function onSaveClaim() {
  await updateAitClaim({ ...editClaim })
  ElMessage.success('已修改权属主张'); editDlg.value = false; loadGraph()
}
function onDeleteClaim(row) {
  ElMessageBox.confirm(`确认删除主张「${row.subject} · ${row.rightType}」吗`, '提示', { type: 'warning' })
    .then(async () => { await deleteAitClaim(row.claimId); ElMessage.success('已删除'); loadGraph() }).catch(() => {})
}

async function onSemanticClaim() {
  try {
    await buildAitClaimFromMaterial(semMaterialId.value)
    ElMessage.success('已由材料条款语义分析自动建主张')
    semMaterialId.value = ''
  } catch (e) { ElMessage.error('语义建主张失败:' + (e?.response?.data?.message || '材料需先解析')) }
}
async function loadGraph() {
  claimList.value = await aitClaims(graphAsset.value) || []
  const g = await aitKgGraph(graphAsset.value)
  const nodes = (g.nodes || []).map(n => ({
    id: n.id, name: n.label, category: n.type,
    symbolSize: n.type === '客体' ? 60 : (n.type === '主体' ? 48 : 36),
    itemStyle: { color: NODE_COLOR[n.type] || '#909399' }
  }))
  graphEmpty.value = nodes.length <= 1
  const links = (g.edges || []).map(e => ({
    source: e.from, target: e.to,
    label: { show: true, formatter: e.relation, fontSize: 11, color: e.relation === '冲突' ? '#d03050' : '#666' },
    lineStyle: { color: e.relation === '冲突' ? '#d03050' : '#bbb', width: e.relation === '冲突' ? 2.5 : 1.2, type: e.relation === '冲突' ? 'dashed' : 'solid', curveness: 0.1 }
  }))
  const cats = ['主体', '客体', '授权事项', '有效期'].map(t => ({ name: t, itemStyle: { color: NODE_COLOR[t] } }))
  await nextTick()
  const chart = echarts.init(graphRef.value)
  chart.setOption({
    tooltip: {}, legend: [{ data: cats.map(c => c.name), bottom: 0 }],
    series: [{
      type: 'graph', layout: 'force', roam: true, draggable: true,
      categories: cats, data: nodes, links,
      force: { repulsion: 320, edgeLength: 120 },
      label: { show: true, position: 'right', fontSize: 12 },
      edgeSymbol: ['none', 'arrow'], edgeSymbolSize: 7
    }]
  })
}

// 资产台账搜索(三处联动:主张表单/右侧查询/图谱)
import { pageArchive } from '@/api/propertyArchive'
import { pageAitMaterial } from '@/api/aitool'
const assetOpts = ref([])
const assetSearching = ref(false)
async function searchAssets(kw) {
  if (!kw) { assetOpts.value = []; return }
  assetSearching.value = true
  try {
    const r = await pageArchive({ current: 1, size: 10, assetName: kw })
    assetOpts.value = r.records || []
  } finally { assetSearching.value = false }
}
function onAssetPicked(id) {
  if (!id) return
  qAsset.value = id
  graphAsset.value = id
}

// 语义建主张:下拉选已解析成功的材料(替代手填材料ID)
const parsedMats = ref([])
let parsedLoaded = false
async function loadParsedMaterials() {
  if (parsedLoaded) return
  const r = await pageAitMaterial({ current: 1, size: 50 })
  parsedMats.value = (r.records || []).filter(m => m.parseStatus === '成功')
  parsedLoaded = true
}

// 一键演示:同一资产登记两条对立主张(历史·所有权 vs 当前·持有权,排他全字段)→检测→冲突表+图谱即刻有内容
const demoRunning = ref(false)
async function runDemo() {
  demoRunning.value = true
  try {
    const asset = claim.assetId || 'AST-001'
    await aitAddClaim({ assetId: asset, subject: '深圳供电局', rightType: '所有权', authScope: '全字段',
      exclusive: true, sourceType: '历史确权', validDate: '2027-06-11 00:00:00' })
    const found = await aitDetectConflict({ assetId: asset, subject: '广东电网有限责任公司', rightType: '数据持有权',
      authScope: '全字段', exclusive: true, sourceType: '当前申请', validDate: '2028-06-11 00:00:00' })
    assetId.value = asset; qAsset.value = asset; graphAsset.value = asset
    await refresh(asset)
    await loadGraph()
    ElMessage.success(`演示完成:检出 ${found.length} 项冲突,右侧报告与下方图谱已加载(资产 ${asset})`)
  } finally { demoRunning.value = false }
}

async function onAdd() {
  if (!claim.assetId || !claim.subject) { ElMessage.warning('资产ID与权利主体必填'); return }
  await aitAddClaim({ ...claim }); ElMessage.success('已登记权属主张')
}
async function onDetect() {
  if (!claim.assetId || !claim.subject) { ElMessage.warning('资产ID与权利主体必填'); return }
  const found = await aitDetectConflict({ ...claim })
  assetId.value = claim.assetId
  ElMessage[found.length ? 'warning' : 'success'](found.length ? `检出 ${found.length} 项冲突` : '未检出冲突')
  await refresh(claim.assetId)
}
async function loadByAsset() { if (qAsset.value) { assetId.value = qAsset.value; await refresh(qAsset.value) } }

// 被业务流程调用时(?assetId=)预填资产并自动加载冲突报告
const route = useRoute()
onMounted(() => {
  if (route.query.assetId) {
    qAsset.value = String(route.query.assetId)
    claim.assetId = qAsset.value
    loadByAsset()
  }
})
function params(a) { return { assetId: a, conflictType: filters.conflictType, riskLevel: filters.riskLevel, startTime: filters.startTime, endTime: filters.endTime, subject: filters.subject } }
async function refresh(a) { conflicts.value = await aitConflicts(params(a)); report.value = await aitConflictReport(params(a)) }
function onExportWord() {
  if (!assetId.value) { ElMessage.warning('请先检测或查询某资产的冲突'); return }
  window.open(aitConflictReportExportUrl(params(assetId.value)), '_blank')
}
function onPrintPdf() {
  if (!assetId.value) { ElMessage.warning('请先检测或查询某资产的冲突'); return }
  window.print()   // 浏览器打印 -> 另存为 PDF
}
function onResolve(row) {
  ElMessageBox.prompt('请输入处置说明', '处置冲突', { inputType: 'textarea' })
    .then(async ({ value }) => { await aitResolveConflict(row.conflictId, value); ElMessage.success('已处置'); refresh(assetId.value) }).catch(() => {})
}
</script>

<style scoped>
.conflict-filter { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; margin-bottom: 10px; }
.print-only { display: none; }
.print-head h2 { text-align: center; margin: 0 0 6px; font-size: 18px; }
.print-head div { text-align: center; color: #666; font-size: 12px; margin-bottom: 10px; }
.cd-tip { font-size: 12px; color: #8c8c8c; line-height: 1.6; }
</style>

<!-- #17 PDF 打印优化:只输出报告区(报告抬头+汇总+冲突表),隐藏表单/图谱/操作/对话框 -->
<style>
@media print {
  body * { visibility: hidden; }
  #conflict-report-print, #conflict-report-print * { visibility: visible; }
  #conflict-report-print { position: absolute; left: 0; top: 0; width: 100%; box-shadow: none !important; border: none !important; }
  #conflict-report-print .no-print, #conflict-report-print .no-print * { display: none !important; visibility: hidden !important; }
  #conflict-report-print .print-only { display: block !important; visibility: visible !important; }
  .el-card__header { display: none !important; }
}
</style>
