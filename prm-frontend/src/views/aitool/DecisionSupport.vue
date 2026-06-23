<template>
  <div class="prm-page">
    <div class="prm-query-bar">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="确权申请ID">
          <el-select v-model="applyId" filterable allow-create default-first-option clearable style="width:340px"
            placeholder="下拉选择已有申请,或粘贴申请ID" @focus="loadApplies">
            <el-option v-for="a in applyOpts" :key="a.applyId" :value="a.applyId"
              :label="(a.applyNo || a.applyId.slice(0, 12)) + '　' + (a.assetName || '')">
              <span>{{ a.applyNo || a.applyId.slice(0, 12) }}</span>
              <span style="float:right;color:#8a8a8a;font-size:12px">{{ a.assetName }} · {{ a.status }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" @click="onAnalyze">智能研判</el-button></el-form-item>
        <el-form-item><el-button type="success" :loading="agentRunning" @click="onAgentAudit">Agent 多阶段审核</el-button></el-form-item>
        <el-form-item><el-button @click="onLedger">审核台账</el-button></el-form-item>
        <el-form-item>
          <el-button type="primary" plain :loading="demoRunning" @click="runDemo">一键示例并研判(测试/演示)</el-button>
        </el-form-item>
      </el-form>
      <span class="prm-table-note">综合 材料完整性30% / 权属无冲突40% / 合规15% / 历史匹配15% 加权;RAG 检索历史确权案例与《数据二十条》/南网制度条款,由大模型给出预测结论并与规则预测对照。</span>
      <AiThinking v-bind="aiAnalyze.state" />
    </div>

    <el-row v-if="d" :gutter="16">
      <el-col :span="8">
        <el-card shadow="hover"><div ref="gauge" style="height:200px"></div>
          <div style="text-align:center;margin-top:-6px">
            <el-tag :type="predTag(d.prediction)" effect="dark" size="large">{{ d.prediction }}</el-tag>
          </div>
          <div style="text-align:center;margin-top:8px" class="ai-pred">
            <span class="ai-lbl">AI 预测:</span>
            <el-tag :type="predTag(d.aiPrediction)" effect="plain">{{ d.aiPrediction || '未生成' }}</el-tag>
            <el-tag v-if="d.aiPrediction" :type="d.aiPrediction === d.prediction ? 'success' : 'warning'" size="small" style="margin-left:6px">
              {{ d.aiPrediction === d.prediction ? '与规则一致' : '不一致·建议人工复核' }}
            </el-tag>
            <el-tag v-else type="info" size="small" style="margin-left:6px">建议人工复核</el-tag>
          </div>
        </el-card>
      </el-col>
      <el-col :span="16">
        <el-card header="关键因子分析" shadow="hover">
          <el-table :data="factors" border size="small">
            <el-table-column prop="name" label="因子" width="150" />
            <el-table-column label="权重" width="90" align="center"><template #default="{ row }">{{ (row.weight*100).toFixed(0) }}%</template></el-table-column>
            <el-table-column label="得分" min-width="200">
              <template #default="{ row }">
                <el-tooltip :content="row.reason || '—'" :disabled="!row.reason" placement="top">
                  <el-progress :percentage="row.score" :color="row.score>=85?'#36b21d':(row.score<70?'#e21f0c':'#ffc417')" />
                </el-tooltip>
              </template>
            </el-table-column>
          </el-table>
          <div class="kv">优势因子:<el-tag type="success" effect="plain">{{ d.strengthFactors }}</el-tag></div>
          <div class="kv">短板因子:<el-tag type="danger" effect="plain">{{ d.weakFactors }}</el-tag></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card v-if="d" header="确权决策建议" shadow="hover" style="margin-top:16px">
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="决策理由">{{ d.reason }}</el-descriptions-item>
        <el-descriptions-item label="需补充材料">{{ d.supplementMaterials }}</el-descriptions-item>
        <el-descriptions-item label="待处置冲突">{{ d.pendingConflicts }}</el-descriptions-item>
        <el-descriptions-item label="权益分割方案">
          <div>{{ d.splitPlan }}</div>
          <el-tabs v-if="splitPlans.length" style="margin-top:8px">
            <el-tab-pane v-for="p in splitPlans" :key="p.plan" :label="'方案·' + p.plan">
              <div class="plan-desc">{{ p.desc }}</div>
              <el-table :data="p.items" border size="small">
                <el-table-column prop="subject" label="主体" width="140" />
                <el-table-column prop="rightType" label="权利" width="150" />
                <el-table-column prop="scope" label="权利范围" min-width="220" />
                <el-table-column prop="term" label="使用期限" width="120" />
                <el-table-column prop="duty" label="责任划分" min-width="180" />
              </el-table>
            </el-tab-pane>
          </el-tabs>
        </el-descriptions-item>
        <el-descriptions-item label="RAG 智能建议">{{ d.ragAdvice }}</el-descriptions-item>
        <el-descriptions-item label="法规/知识引用">
          <el-tag v-for="c in citations" :key="c" type="info" effect="plain" size="small" style="margin-right:6px">{{ c }}</el-tag>
          <span v-if="!citations.length">—</span>
        </el-descriptions-item>
        <el-descriptions-item label="证据链(SM3)"><code class="hash">{{ d.evidenceChain }}</code></el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 3.1 智能确权 Agent 多阶段审核 -->
    <el-dialog v-model="agentDlg" title="智能确权 Agent · 多阶段审核与推理决策" width="820px" align-center>
      <template v-if="agent">
        <el-alert :type="agent.channel==='快速通道' ? 'success' : 'warning'" :closable="false" style="margin-bottom:10px"
          :title="`审核通道:${agent.channel} · 授权建议:${agent.authAdvice} · 风险等级:${agent.riskLevel}`"
          :description="agent.reason" show-icon />
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="数据分类">{{ agent.dataClass }}</el-descriptions-item>
          <el-descriptions-item label="数据级别">{{ agent.dataGrade }}</el-descriptions-item>
          <el-descriptions-item label="权利类型">{{ agent.rightType }}</el-descriptions-item>
          <el-descriptions-item label="综合评分">{{ agent.score }}</el-descriptions-item>
          <el-descriptions-item label="限制条件" :span="2">{{ agent.restrictions }}</el-descriptions-item>
          <el-descriptions-item label="补正建议" :span="2">{{ agent.supplement || '—' }}</el-descriptions-item>
          <el-descriptions-item label="命中依据" :span="2">{{ agent.citations || '—' }}</el-descriptions-item>
          <el-descriptions-item label="建议动作" :span="2"><el-tag type="primary">{{ agent.action }}</el-tag></el-descriptions-item>
        </el-descriptions>
        <div style="margin-top:12px;font-weight:600">审核阶段链路(Agent 编排)</div>
        <el-steps direction="vertical" :active="agentStages.length" style="margin-top:8px">
          <el-step v-for="(s,i) in agentStages" :key="i" :title="s.stage" :description="`${s.summary}　[${s.by}]`" status="finish" />
        </el-steps>
        <div style="margin-top:8px;font-weight:600">工具调用链</div>
        <div>
          <el-tag v-for="(t,i) in agentTools" :key="i" size="small" style="margin:2px">{{ t.tool }}<span v-if="t.model!=='-'"> · {{ t.model }}</span></el-tag>
        </div>
        <div style="margin-top:12px">
          <el-button size="small" @click="openUrl(reportUrl(agent.applyId))">审核报告</el-button>
          <el-button size="small" @click="openUrl(registrationUrl(agent.applyId))">确权登记辅助</el-button>
          <el-button size="small" @click="openUrl(legalUrl(agent.applyId))">法律意见辅助</el-button>
          <el-button size="small" type="info" @click="onEvidence(agent.applyId)">查看证据链</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 3.2 审核台账 -->
    <el-dialog v-model="ledgerDlg" title="智能确权审核台账(查询/筛选/汇总/导出)" width="900px" align-center>
      <div style="margin-bottom:10px">
        <el-select v-model="ledgerFilter.riskLevel" placeholder="风险等级" clearable size="small" style="width:110px" @change="loadLedger">
          <el-option label="高" value="高" /><el-option label="中" value="中" /><el-option label="低" value="低" />
        </el-select>
        <el-select v-model="ledgerFilter.channel" placeholder="通道" clearable size="small" style="width:130px;margin-left:8px" @change="loadLedger">
          <el-option label="快速通道" value="快速通道" /><el-option label="深度审核" value="深度审核" />
        </el-select>
        <el-button size="small" type="primary" style="margin-left:8px" @click="openUrl(ledgerExportUrl())">导出Excel</el-button>
        <span v-if="ledgerStats" class="prm-table-note" style="margin-left:10px">
          共 {{ ledgerStats.total }} · 风险 {{ statStr(ledgerStats.byRisk) }} · 通道 {{ statStr(ledgerStats.byChannel) }}
        </span>
      </div>
      <el-table :data="ledgerRows" border size="small" max-height="420">
        <el-table-column prop="assetId" label="资产" width="150" show-overflow-tooltip />
        <el-table-column prop="channel" label="通道" width="90" />
        <el-table-column prop="dataClass" label="数据分类" width="110" />
        <el-table-column prop="dataGrade" label="级别" width="70" />
        <el-table-column prop="authAdvice" label="确权结论" width="100" />
        <el-table-column prop="authLevel" label="授权级别" width="140" show-overflow-tooltip />
        <el-table-column prop="riskLevel" label="风险" width="60" align="center">
          <template #default="{ row }"><el-tag size="small" :type="row.riskLevel==='高'?'danger':(row.riskLevel==='中'?'warning':'success')">{{ row.riskLevel }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="score" label="评分" width="70" align="center" />
      </el-table>
    </el-dialog>

    <!-- 3.2 审核证据链 -->
    <el-dialog v-model="evDlg" title="审核证据链档案(可复核·可留痕·可审计)" width="760px" align-center>
      <template v-if="evidence">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="最终结论">{{ evidence.conclusion }}</el-descriptions-item>
          <el-descriptions-item label="SM3 留痕"><code class="hash">{{ evidence.sm3Hash }}</code></el-descriptions-item>
          <el-descriptions-item label="大模型判断理由">{{ evidence.modelReason }}</el-descriptions-item>
        </el-descriptions>
        <div style="margin-top:10px;font-weight:600">规则命中项</div>
        <pre class="ev-pre">{{ pretty(evidence.ruleHitsJson) }}</pre>
        <div style="font-weight:600">知识库命中片段</div>
        <pre class="ev-pre">{{ pretty(evidence.kbHitsJson) }}</pre>
        <div style="font-weight:600">输入材料片段</div>
        <pre class="ev-pre">{{ pretty(evidence.materialSnippetsJson) }}</pre>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, nextTick, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { aitAnalyze, aitAgentAudit, aitAgentEvidence, aitAgentLedgerPage, aitAgentLedgerStats,
  aitAgentLedgerExportUrl, aitAgentReportUrl, aitAgentRegistrationUrl, aitAgentLegalUrl } from '@/api/aitool'
import AiThinking from '@/components/AiThinking.vue'
import { useAiThinking } from '@/composables/useAiThinking'
import { AI_PHASES } from '@/lib/aiPhases'

const applyId = ref('')
// 3.1 Agent 多阶段审核
const agentDlg = ref(false); const agentRunning = ref(false); const agent = ref(null)
const agentStages = computed(() => { try { return JSON.parse(agent.value?.stageTraceJson || '[]') } catch { return [] } })
const agentTools = computed(() => { try { return JSON.parse(agent.value?.toolTraceJson || '[]') } catch { return [] } })
async function onAgentAudit() {
  if (!applyId.value) { ElMessage.warning('请先选择/输入确权申请ID'); return }
  agentRunning.value = true
  try {
    agent.value = await aitAgentAudit(applyId.value)
    agentDlg.value = true
    ElMessage.success('Agent 审核完成:' + agent.value.channel)
  } catch (e) {
    ElMessage.error('Agent 审核失败:' + (e?.message || ''))
  } finally {
    agentRunning.value = false
  }
}
// 3.2 审核台账 / 报告 / 证据链
function openUrl(u) { window.open(u, '_blank') }
function reportUrl(id) { return aitAgentReportUrl(id) }
function registrationUrl(id) { return aitAgentRegistrationUrl(id) }
function legalUrl(id) { return aitAgentLegalUrl(id) }
function ledgerExportUrl() { return aitAgentLedgerExportUrl() }
function pretty(json) { try { return JSON.stringify(JSON.parse(json || '[]'), null, 2) } catch { return json } }
function statStr(m) { return m ? Object.entries(m).map(([k, v]) => `${k}:${v}`).join(' ') : '' }
const evDlg = ref(false); const evidence = ref(null)
async function onEvidence(id) { evidence.value = await aitAgentEvidence(id); evDlg.value = true }
const ledgerDlg = ref(false); const ledgerRows = ref([]); const ledgerStats = ref(null)
const ledgerFilter = reactive({ riskLevel: '', channel: '' })
async function onLedger() { ledgerDlg.value = true; await loadLedger() }
async function loadLedger() {
  const r = await aitAgentLedgerPage({ current: 1, size: 100, riskLevel: ledgerFilter.riskLevel, channel: ledgerFilter.channel })
  ledgerRows.value = r.records || []
  ledgerStats.value = await aitAgentLedgerStats()
}
const d = ref(null); const factors = ref([]); const gauge = ref()
const aiAnalyze = useAiThinking()
const citations = computed(() => (d.value?.ragCitations || '').split(';').filter(Boolean))
const splitPlans = computed(() => { try { return JSON.parse(d.value?.splitPlansJson || '[]') } catch { return [] } })

// 被业务流程调用时(?applyId=)预填申请并自动研判
const route = useRoute()
onMounted(() => {
  if (route.query.applyId) {
    applyId.value = String(route.query.applyId)
    onAnalyze()
  }
})

function predTag(p) { return { 建议通过: 'success', 建议补充材料: 'warning', 建议驳回: 'danger' }[p] || 'info' }

// 申请下拉:加载现有确权申请(免记ID;H2内存库重启会清空,旧ID失效属预期)
import { pageConfirmApply, saveConfirmDraft } from '@/api/confirm'
import { uploadAitMaterial, parseAitMaterial, aitProgress } from '@/api/aitool'
const applyOpts = ref([])
let appliesLoaded = false
async function loadApplies() {
  if (appliesLoaded) return
  const r = await pageConfirmApply({ current: 1, size: 30 })
  applyOpts.value = r.records || []
  appliesLoaded = true
}

// 一键示例并研判:建申请→工具传含盖章材料→解析→智能研判,全链自给自足
const demoRunning = ref(false)
async function runDemo() {
  demoRunning.value = true
  try {
    const id = await saveConfirmDraft({ assetId: 'AST-001', assetName: '客户用电信息表',
      rightType: '数据资源持有权', rightHolder: '广东电网有限责任公司', regulated: '管制业务',
      purpose: '决策支持示例' })
    const mid = await uploadAitMaterial({ applyId: id, fileName: 'AST-001-确权证明-盖好.pdf',
      content: '兹证明客户用电信息表由广东电网有限责任公司自行生产,权利类型为数据资源持有权,有效期3年,范围全字段,已盖章。' })
    // 解析段绑定真实进度,消灭"一键示例"前置的静默等待
    aiAnalyze.start({ phases: AI_PHASES.materialParse, title: '大模型解析材料中', bound: true })
    await parseAitMaterial(mid)
    for (let i = 0; i < 60; i++) {
      const m = await aitProgress(mid)
      aiAnalyze.setProgress(m.progress || 0)
      if (m.parseStatus === '成功' || m.parseStatus === '失败') break
      await new Promise(r => setTimeout(r, 1000))
    }
    aiAnalyze.stop()
    applyId.value = id
    appliesLoaded = false
    await onAnalyze()
    ElMessage.success('示例完成:已建申请并传含盖章材料,研判结果如下')
  } catch (e) {
    ElMessage.error('示例失败:' + (e?.response?.data?.message || e?.message || ''))
  } finally { aiAnalyze.stop(); demoRunning.value = false }
}

async function onAnalyze() {
  if (!applyId.value) { ElMessage.warning('请输入确权申请ID'); return }
  try {
    d.value = await aiAnalyze.run(() => aitAnalyze(applyId.value),
      { phases: AI_PHASES.analyze, title: '大模型研判中', stepMs: 6000 })
    factors.value = JSON.parse(d.value.factorsJson || '[]')
    await nextTick(); renderGauge(d.value.score)
    ElMessage.success('研判完成')
  } catch (e) { ElMessage.error('研判失败:该申请ID不存在(演示库重启会清空旧ID)。可:①下拉选择现有申请 ②点“一键示例并研判” ③去“确权申请”向导新建') }
}
function renderGauge(score) {
  echarts.init(gauge.value).setOption({
    series: [{ type: 'gauge', min: 0, max: 100, progress: { show: true, width: 14 },
      axisLine: { lineStyle: { width: 14 } }, pointer: { width: 5 },
      detail: { valueAnimation: true, formatter: '{value}', fontSize: 28, offsetCenter: [0, '62%'] },
      data: [{ value: score, name: '确权评分' }], title: { offsetCenter: [0, '88%'], fontSize: 12 } }]
  })
}
</script>

<style scoped>
.kv { margin-top: 8px; font-size: 13px; }
.ai-lbl { font-size: 13px; color: #606266; margin-right: 6px; }
.plan-desc { font-size: 12px; color: #909399; margin-bottom: 6px; }
.hash { font-family: ui-monospace, Consolas, monospace; font-size: 12px; color: #1e87f0; word-break: break-all; }
</style>
