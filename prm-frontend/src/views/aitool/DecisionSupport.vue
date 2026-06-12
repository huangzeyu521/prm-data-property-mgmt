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
              <span style="float:right;color:#8c8c8c;font-size:12px">{{ a.assetName }} · {{ a.status }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" @click="onAnalyze">智能研判</el-button></el-form-item>
        <el-form-item>
          <el-button type="warning" plain :loading="demoRunning" @click="runDemo">一键示例并研判(测试/演示)</el-button>
        </el-form-item>
      </el-form>
      <span class="prm-table-note">综合 材料完整性30% / 权属无冲突40% / 合规15% / 历史匹配15% 加权;RAG 检索历史确权案例与《数据二十条》/南网制度条款,由大模型给出预测结论并与规则预测对照。</span>
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
                  <el-progress :percentage="row.score" :color="row.score>=85?'#18a058':(row.score<70?'#d03050':'#f0a020')" />
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
  </div>
</template>

<script setup>
import { ref, nextTick, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { aitAnalyze } from '@/api/aitool'

const applyId = ref('')
const d = ref(null); const factors = ref([]); const gauge = ref()
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
    ElMessage.info('示例进行中:材料已上传,正在智能解析(真调大模型约20秒)…')
    await parseAitMaterial(mid)
    for (let i = 0; i < 60; i++) {
      const m = await aitProgress(mid)
      if (m.parseStatus === '成功' || m.parseStatus === '失败') break
      await new Promise(r => setTimeout(r, 1000))
    }
    applyId.value = id
    appliesLoaded = false
    ElMessage.info('解析完成,正在智能研判(真调大模型约30秒)…')
    await onAnalyze()
    ElMessage.success('示例完成:已建申请并传含盖章材料,研判结果如下')
  } catch (e) {
    ElMessage.error('示例失败:' + (e?.response?.data?.message || e?.message || ''))
  } finally { demoRunning.value = false }
}

async function onAnalyze() {
  if (!applyId.value) { ElMessage.warning('请输入确权申请ID'); return }
  try {
    d.value = await aitAnalyze(applyId.value)
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
.hash { font-family: ui-monospace, Consolas, monospace; font-size: 12px; color: #2f6bff; word-break: break-all; }
</style>
