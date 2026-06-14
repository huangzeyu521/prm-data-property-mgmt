<template>
  <div class="prm-page">
    <div class="prm-table-note" style="margin:0 0 12px">
      注:一站式确权申请——填申请 → 传材料 → 校验 → 提交，一条流程办完，无需在菜单间来回切换(对齐附录F"资料随申请一并编制提交")。
    </div>

    <el-steps :active="step" finish-status="success" align-center class="wz-steps">
      <el-step title="填写申请" description="表1 + A–J 来源识别" />
      <el-step title="上传材料" description="按 A–J 应交清单" />
      <el-step title="材料校验" description="完整性/合规校验" />
      <el-step title="提交审核" description="进入三级审批" />
    </el-steps>

    <div class="wz-body">
      <!-- 步骤1:填写申请 -->
      <el-card v-show="step === 0" shadow="never">
        <el-alert v-if="!applyId" type="info" :closable="false" style="margin-bottom:12px;max-width:640px">
          <template #title>
            第一次填写?可
            <el-button link type="primary" style="vertical-align:baseline" @click="fillDemo">一键填充示例(AST-001,测试/演示用)</el-button>
            体验完整流程,材料包见 test/确权申请 目录
          </template>
        </el-alert>
        <el-form ref="formRef" :model="form" :rules="rules" label-width="150px" :disabled="!!applyId" style="max-width:640px">
          <el-form-item label="关联资产ID" prop="assetId">
            <div style="display:flex;gap:8px;width:100%">
              <el-select v-model="form.assetId" filterable remote allow-create default-first-option clearable
                :remote-method="searchAssets" :loading="assetSearching" style="flex:1"
                placeholder="输入资产名称/ID 搜索台账,或直接输入,如 AST-001" @change="onAssetPicked">
                <el-option v-for="a in assetOpts" :key="a.assetId" :value="a.assetId"
                  :label="a.assetId + '　' + a.assetName">
                  <span>{{ a.assetId }}</span>
                  <span style="float:right;color:#8c8c8c;font-size:12px">{{ a.assetName }}</span>
                </el-option>
              </el-select>
              <el-button type="primary" plain :loading="autoLoading" @click="onAutofill">元数据自动填充</el-button>
            </div>
            <div class="form-tip">不清楚资产ID?输入名称关键词(如"用电")即可搜索产权台账;也可在"产权台账概览"页查询</div>
            <el-tag v-if="quality !== null" :type="quality < 80 ? 'danger' : 'success'" effect="plain" style="margin-top:6px">
              元数据质量评分 {{ quality }}{{ quality < 80 ? ' · 低于80,提交将被自动驳回(请先治理元数据)' : '' }}
            </el-tag>
          </el-form-item>
          <el-form-item label="资产名称" prop="assetName"><el-input v-model="form.assetName" /></el-form-item>
          <el-form-item label="权属类型" prop="rightTypes">
            <el-select v-model="form.rightTypes" multiple style="width:100%" placeholder="可多选,多种权属类型合并一份申请">
              <el-option v-for="t in rightTypes" :key="t" :label="t" :value="t" />
            </el-select>
            <div class="form-tip">支持多选:多种权属类型合并发起同一份申请(评审优化)</div>
          </el-form-item>
          <el-form-item label="申报权属主体">
            <el-input v-model="form.rightHolder" placeholder="当前申报主体,最终权属以确权审核结果为准" />
            <div class="form-tip">分省上报的数据产权,确权通过后统一归口中国南方电网有限责任公司</div>
          </el-form-item>
          <el-form-item label="责任部门"><el-input v-model="form.respDept" /></el-form-item>
          <el-form-item label="系统负责人"><el-input v-model="form.systemOwner" placeholder="附录F 表1" /></el-form-item>
          <el-form-item label="联系方式"><el-input v-model="form.contactInfo" placeholder="电话 / 邮箱" /></el-form-item>
          <el-form-item label="登记类型">
            <el-radio-group v-model="form.registerType">
              <el-radio value="初始确权">初始确权</el-radio>
              <el-radio value="确权变更">确权变更</el-radio>
              <el-radio value="产权补录">产权补录(存量系统 MDAU 工单)</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="管制属性">
            <el-radio-group v-model="form.regulated">
              <el-radio value="管制业务">管制业务</el-radio>
              <el-radio value="非管制">非管制</el-radio>
            </el-radio-group>
            <div class="form-tip">权益归集判定关键输入:管制单位默认没有限经营权;自行生产且不涉第三方时,管制单位经营权调整为有并归集网公司</div>
          </el-form-item>
          <el-form-item label="申请模式">
            <el-radio-group v-model="form.applyMode">
              <el-radio value="常规">常规</el-radio>
              <el-radio value="一事一议">一事一议(特殊事项单独审议)</el-radio>
            </el-radio-group>
            <div class="form-tip">权属复杂/跨主体/全网统一转让等特殊场景选择"一事一议",由合规管控小组单独组织审议</div>
          </el-form-item>
          <el-form-item label="来源权益识别">
            <el-checkbox-group v-model="form.sourceIdent">
              <el-checkbox v-for="s in sourceOpts" :key="s.v" :value="s.v">{{ s.v }} {{ s.t }}</el-checkbox>
            </el-checkbox-group>
          </el-form-item>
          <el-form-item label="信息关联识别">
            <el-checkbox-group v-model="form.relationIdent">
              <el-checkbox v-for="r in relationOpts" :key="r.v" :value="r.v">{{ r.v }} {{ r.t }}</el-checkbox>
            </el-checkbox-group>
          </el-form-item>
          <template v-if="needTable2">
            <el-divider content-position="left" style="font-size:12px;color:#909399">表2 涉及第三方权益(结构化)</el-divider>
            <el-form-item label="来源主体名称" prop="sourceSubject"><el-input v-model="form.sourceSubject" placeholder="表2:第三方来源主体" /></el-form-item>
            <el-form-item label="来源权益限制摘要"><el-input v-model="form.sourceLimit" type="textarea" :rows="2" placeholder="表2:BCDEF 来源权益限制说明" /></el-form-item>
            <el-form-item label="信息识别关联主体"><el-input v-model="form.relationSubject" placeholder="表2:GHIJ 信息识别关联主体说明" /></el-form-item>
            <el-form-item label="权益风险说明"><el-input v-model="form.equityRisk" type="textarea" :rows="2" placeholder="表2:权益风险说明" /></el-form-item>
          </template>
          <el-form-item label="用途说明"><el-input v-model="form.purpose" type="textarea" /></el-form-item>

          <el-divider content-position="left" style="font-size:12px;color:#909399">表级数据清单(M02 元数据-系统数据表,确权粒度到库表)</el-divider>
          <el-form-item label="批量粘贴">
            <el-input v-model="tableItemText" type="textarea" :rows="4"
              placeholder="每行一张表:实例TNS,schema,表代码,表名称[,密级][,来源判定][,来源主体]&#10;XC_ORA_ZH01,NCLAIMUSER,PRPLNEGOTIATIONS,谈判表,敏感信息,A 自行生产数据,鼎和保险" />
            <div class="form-tip">密级:不涉密/核心商密/普通商密/工作秘密/敏感信息;来源判定:A自行生产/B公开采集/C公共授权/D公共生产/E交易采购/F其他;G-J 识别默认取上方"信息关联识别"勾选</div>
            <el-button size="small" type="primary" plain style="margin-top:6px" @click="parseTableItems">解析为表级清单</el-button>
            <span v-if="tableItems.length" class="form-tip" style="margin-left:8px">已解析 {{ tableItems.length }} 张表,暂存申请时一并保存</span>
          </el-form-item>
          <el-table v-if="tableItems.length" :data="tableItems" border size="small" style="margin-bottom:8px">
            <el-table-column prop="instanceName" label="实例TNS" width="130" />
            <el-table-column prop="schemaName" label="schema" width="120" />
            <el-table-column prop="tableCode" label="表代码" min-width="160" />
            <el-table-column prop="tableName" label="表名称" min-width="140" />
            <el-table-column prop="secretLevel" label="密级" width="90" />
            <el-table-column prop="sourceType" label="来源判定" width="130" />
          </el-table>
        </el-form>
        <el-alert v-if="applyId" type="success" :closable="false" show-icon
          :title="`申请已暂存(${applyNo || applyId})，进入材料上传`" style="max-width:640px;margin-top:8px" />
      </el-card>

      <!-- 步骤2:上传材料(按 A–J 动态清单) -->
      <el-card v-show="step === 1" shadow="never">
        <div class="prm-table-note" style="margin-bottom:10px">
          按所选来源/关联(A–J)应交材料清单。"上传原件"真实上传文件(仅 PDF/Word/JPG/PNG,自动格式验证);或"仅登记"占位。
          <el-button size="small" type="warning" plain style="margin-left:12px" @click="invokeAitool">
            <el-icon><MagicStick /></el-icon> 调用智能确权辅助工具(解析/比对本申请材料)
          </el-button>
        </div>
        <el-table :data="checklist" border>
          <el-table-column type="index" label="序号" width="60" align="center" />
          <el-table-column prop="code" label="标识" width="70" align="center" />
          <el-table-column prop="name" label="应交材料" min-width="240" />
          <el-table-column label="状态" width="110" align="center">
            <template #default="{ row }">
              <el-tag :type="row.done ? 'success' : 'info'" effect="light">{{ row.done ? '已上传' : '待上传' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="220" align="center">
            <template #default="{ row }">
              <el-upload :auto-upload="false" :show-file-list="false" :on-change="(f) => onUploadFile(row, f)" style="display:inline-block">
                <el-button link type="success">上传原件</el-button>
              </el-upload>
              <el-button link type="primary" :disabled="row.done" @click="registerMaterial(row)" style="margin-left:8px">仅登记</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 步骤3:材料校验(规则校验 + AI 智能校验,评审8.4) -->
      <el-card v-show="step === 2" shadow="never">
        <div class="prm-table-note" style="margin-bottom:10px">基于预设规则(应交清单)自动校验完整性与合规性 + AI 智能校验(智能确权辅助工具);全部通过方可推送审核。</div>
        <el-button type="primary" :loading="checking" @click="runCheck" style="margin-bottom:12px">规则校验全部材料</el-button>
        <el-button type="warning" :loading="aiMatChecking" @click="runAiMaterialCheck" style="margin-bottom:12px;margin-left:8px">
          <el-icon><MagicStick /></el-icon> AI 材料校验(qwen3-max)
        </el-button>
        <el-button type="warning" plain :loading="aiChecking" @click="runAiCheck" style="margin-bottom:12px;margin-left:8px">
          <el-icon><MagicStick /></el-icon> AI 决策研判
        </el-button>
        <el-button :disabled="!checkReport" @click="onExportCheck" style="margin-bottom:12px;margin-left:8px">导出校验结果</el-button>
        <AiThinking v-bind="aiThink.state" />
        <!-- AI 材料校验:大模型逐份校验 完整性/合规性/与表单一致性 -->
        <el-alert v-if="aiMatResult" :type="aiMatResult.overall === '通过' ? 'success' : (aiMatResult.overall === '不通过' ? 'error' : 'warning')" :closable="false" style="margin-bottom:12px">
          <div><b>AI 材料校验:{{ aiMatResult.overall }}</b> — {{ aiMatResult.overallDesc }}</div>
          <el-table :data="aiMatResult.items" border size="small" style="margin-top:8px">
            <el-table-column prop="materialName" label="材料" min-width="220" />
            <el-table-column label="校验结论" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.verdict === '通过' ? 'success' : (row.verdict === '不通过' ? 'danger' : 'warning')" size="small">{{ row.verdict }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="issues" label="问题" min-width="200" />
            <el-table-column prop="suggestion" label="建议" min-width="180" />
          </el-table>
        </el-alert>
        <el-alert v-if="aiResult" :type="aiResult.prediction === '建议通过' ? 'success' : 'warning'" :closable="false" style="margin-bottom:12px">
          <div><b>AI 校验结论:{{ aiResult.prediction }}</b>(综合评分 {{ aiResult.score }};AI 预测:{{ aiResult.aiPrediction || '未生成' }})</div>
          <div style="margin-top:4px">需补材料:{{ aiResult.supplementMaterials }}</div>
          <div style="margin-top:4px">待处理冲突:{{ aiResult.pendingConflicts }}</div>
          <div style="margin-top:4px;color:#909399">由智能确权辅助工具基于本申请已解析材料/权属冲突/法规检索生成;点击下方按钮可进入工具补充材料</div>
          <el-button size="small" type="warning" plain style="margin-top:6px" @click="invokeAitool">进入智能确权辅助工具</el-button>
        </el-alert>
        <el-alert v-if="checkReport" :type="checkReport.allPass ? 'success' : 'warning'" :closable="false" style="margin-bottom:12px">
          <div>{{ checkReport.summary }}</div>
          <div v-if="checkReport.missing && checkReport.missing.length" style="margin-top:4px">缺失项:{{ checkReport.missing.join('、') }}</div>
          <div v-if="checkReport.nonCompliant && checkReport.nonCompliant.length" style="margin-top:4px">不合规项:{{ checkReport.nonCompliant.join('、') }}</div>
        </el-alert>
        <el-table :data="materials" border>
          <el-table-column type="index" label="序号" width="60" align="center" />
          <el-table-column prop="materialName" label="材料名称" min-width="240" />
          <el-table-column prop="materialType" label="类型" width="140" />
          <el-table-column label="校验结果" width="120" align="center">
            <template #default="{ row }">
              <el-tag :type="row.checkResult === '通过' ? 'success' : (row.checkResult === '不通过' ? 'danger' : 'info')" effect="light">
                {{ row.checkResult || '待校验' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="原件" min-width="160">
            <template #default="{ row }">
              <el-link v-if="row.fileName" type="primary" @click="previewMaterial(row)">{{ row.fileName }}（预览/下载）</el-link>
              <span v-else style="color:#bbb">占位/无原件</span>
            </template>
          </el-table-column>
        </el-table>

        <!-- 权益归集判定(分子公司共享网公司,《权益内部管理汇总表》说明页规则) -->
        <el-divider content-position="left" style="font-size:12px;color:#909399">权益归集判定(分子公司共享网公司)</el-divider>
        <el-descriptions v-if="consolidation" :column="4" border size="small" class="consol-panel">
          <el-descriptions-item label="命中规则">规则 {{ consolidation.rule }}</el-descriptions-item>
          <el-descriptions-item label="网公司持有权"><el-tag :type="consolidation.holdRight === '有' ? 'success' : 'info'" size="small">{{ consolidation.holdRight }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="网公司使用权"><el-tag :type="consolidation.useRight === '有' ? 'success' : 'info'" size="small">{{ consolidation.useRight }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="网公司经营权"><el-tag :type="consolidation.operateRight === '有' ? 'success' : (consolidation.operateRight === '无' ? 'info' : 'warning')" size="small">{{ consolidation.operateRight }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="共享判定原因" :span="4">{{ consolidation.reason }}</el-descriptions-item>
        </el-descriptions>
        <el-alert v-else type="info" :closable="false" title="暂存申请后自动按管制属性/来源判定/第三方识别给出网公司权益归集判定" style="margin-bottom:8px" />

        <!-- 审批重要节点显式化(评审8.5) -->
        <el-divider content-position="left" style="font-size:12px;color:#909399">提交后审批链(重要节点)</el-divider>
        <el-steps :active="0" align-center class="approve-chain">
          <el-step title="合规管控小组审核" description="生成表3/表4及认定意见" />
          <el-step title="数据管理部门主管复核" description="权属边界与责任复核" />
          <el-step title="经理/高级经理终审" :description="(form.applyMode === '一事一议' ? '一事一议:单独组织审议' : '逐级审批')" />
          <el-step title="制卡归集" description="生成权益卡片并归集" />
        </el-steps>
      </el-card>

      <!-- 步骤4:完成 -->
      <el-card v-show="step === 3" shadow="never">
        <el-result icon="success" title="确权申请已提交" :sub-title="`申请编号 ${applyNo || applyId}，已进入：合规审核 → 主管复核 → 经理终审`">
          <template #extra>
            <el-button type="primary" @click="goProgress">查看进度</el-button>
            <el-button type="success" @click="router.push('/dpr/confirm/review?applyId=' + applyId)">去审核(审核申请提交)</el-button>
            <el-button @click="reset">再发起一笔</el-button>
          </template>
        </el-result>
      </el-card>
    </div>

    <div class="wz-foot">
      <el-button v-if="step > 0 && step < 3" @click="step--">上一步</el-button>
      <el-button v-if="step === 0" type="primary" :loading="saving" @click="next0">下一步:上传材料</el-button>
      <el-button v-if="step === 1" type="primary" @click="next1">下一步:材料校验</el-button>
      <el-button v-if="step === 2" type="primary" :loading="submitting" @click="next2">提交审核</el-button>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { autofillConfirm, saveConfirmDraft, uploadMaterial, uploadMaterialFile, materialFileUrl, listMaterialByApply, checkMaterial, runMaterialCheck, pushMaterialReview, materialExportUrl, submitConfirm, saveTableItems, getConsolidation, aiMaterialCheck } from '@/api/confirm'
import { aitAnalyze } from '@/api/aitool'
import AiThinking from '@/components/AiThinking.vue'
import { useAiThinking } from '@/composables/useAiThinking'
import { AI_PHASES } from '@/lib/aiPhases'
const aiThink = useAiThinking()

const router = useRouter()
const route = useRoute()
const rightTypes = ['数据资源持有权', '数据加工使用权', '数据产品经营权']
const sourceOpts = [
  { v: 'A', t: '自行生产', m: '数据来源设备/系统建设投入情况说明' },
  { v: 'B', t: '公开采集', m: '公共采集情况说明(方式/方法/来源)' },
  { v: 'C', t: '公共数据授权', m: '公共数据授权说明' },
  { v: 'D', t: '共同生产', m: '共享/共同生产情况说明' },
  { v: 'E', t: '交易采购', m: '交易采购情况说明' },
  { v: 'F', t: '其他', m: '其他来源情况说明' }
]
const relationOpts = [
  { v: 'G', t: '行政监管', m: '行政监管要求补充说明' },
  { v: 'H', t: '个人/家庭隐私', m: '个人/家庭隐私授权说明(如用户入网协议)' },
  { v: 'I', t: '第三方商业机密', m: '第三方商业机密授权说明' },
  { v: 'J', t: '其他第三方协议', m: '其他第三方机构协议' }
]

const step = ref(0)
const formRef = ref()
const autoLoading = ref(false)
const saving = ref(false)
const checking = ref(false)
const submitting = ref(false)
const quality = ref(null)
const applyId = ref('')
const applyNo = ref('')

// 调用独立智能确权辅助工具,带本申请上下文(新标签打开)
function invokeAitool() {
  window.open('/aitool/material?applyId=' + encodeURIComponent(applyId.value || ''), '_blank')
}

// AI 材料校验:qwen3-max 逐份校验 完整性/合规性/与表单一致性(stub 回退)
const aiMatChecking = ref(false)
const aiMatResult = ref(null)
async function runAiMaterialCheck() {
  if (!applyId.value) { ElMessage.warning('请先完成步骤1暂存申请'); return }
  aiMatChecking.value = true
  try {
    const raw = await aiThink.run(() => aiMaterialCheck(applyId.value),
      { phases: AI_PHASES.materialCheck, title: '大模型材料校验中' })
    aiMatResult.value = typeof raw === 'string' ? JSON.parse(raw) : raw
    ElMessage.success('AI 材料校验完成')
  } catch (e) {
    ElMessage.warning('AI 材料校验失败:' + (e?.response?.data?.message || e?.message || '请先上传材料'))
  } finally { aiMatChecking.value = false }
}

// AI 决策研判(评审8.4):调用智能确权辅助工具决策分析,取回 预测/需补材料/冲突 结论
const aiChecking = ref(false)
const aiResult = ref(null)
async function runAiCheck() {
  if (!applyId.value) { ElMessage.warning('请先完成步骤1暂存申请'); return }
  aiChecking.value = true
  try {
    aiResult.value = await aiThink.run(() => aitAnalyze(applyId.value),
      { phases: AI_PHASES.analyze, title: '大模型决策研判中' })
    ElMessage.success('AI 智能校验完成')
  } catch (e) {
    ElMessage.warning('AI 校验失败:' + (e?.response?.data?.message || '请先通过"进入智能确权辅助工具"上传并解析本申请材料'))
  } finally { aiChecking.value = false }
}
const checklist = ref([])
const checkReport = ref(null)
const materials = ref([])

// 表级清单(M02)批量粘贴解析
const tableItemText = ref('')
const tableItems = ref([])
function parseTableItems() {
  tableItems.value = tableItemText.value.split('\n').map(l => l.trim()).filter(Boolean).map(line => {
    const [instanceName, schemaName, tableCode, tableName, secretLevel, sourceType, sourceSubject] =
      line.split(/[,，]/).map(s => (s || '').trim())
    return { instanceName, schemaName, tableCode, tableName, tableComment: tableName,
      secretLevel: secretLevel || '不涉密', sourceType: sourceType || 'A 自行生产数据', sourceSubject: sourceSubject || '',
      gFlag: form.relationIdent.includes('G') ? '是' : '否', hFlag: form.relationIdent.includes('H') ? '是' : '否',
      iFlag: form.relationIdent.includes('I') ? '是' : '否', jFlag: form.relationIdent.includes('J') ? '是' : '否' }
  }).filter(it => it.tableCode || it.tableName)
  if (!tableItems.value.length) ElMessage.warning('未解析到有效表级记录,请检查格式')
}

// 权益归集判定结果(分子公司共享网公司)
const consolidation = ref(null)
async function loadConsolidation() {
  if (!applyId.value) return
  try { consolidation.value = await getConsolidation(applyId.value) } catch (e) { consolidation.value = null }
}

const form = reactive({
  assetId: '', assetName: '', rightTypes: [], rightHolder: '', respDept: '',
  systemOwner: '', contactInfo: '', registerType: '初始确权', applyMode: '常规', regulated: '非管制',
  purpose: '', thirdPartyInfo: '', sourceSubject: '', sourceLimit: '', relationSubject: '', equityRisk: '',
  sourceIdent: [], relationIdent: []
})
const rules = {
  assetId: [{ required: true, message: '请输入关联资产ID', trigger: 'blur' }],
  assetName: [{ required: true, message: '请输入资产名称', trigger: 'blur' }],
  rightTypes: [{ required: true, type: 'array', min: 1, message: '请至少选择一种权属类型', trigger: 'change' }]
}

// 基于原单修改重提:从被驳回确权单带入字段(新申请,旧单保留已驳回)
onMounted(() => {
  if (!route.query.reopen) return
  try {
    const o = JSON.parse(sessionStorage.getItem('prm-reopen') || '{}')
    if (o.domain === '确权' && o.raw) {
      const r = o.raw
      Object.assign(form, {
        assetId: r.assetId || '', assetName: r.assetName || '', rightHolder: r.rightHolder || '',
        respDept: r.respDept || '', systemOwner: r.systemOwner || '', contactInfo: r.contactInfo || '',
        registerType: r.registerType || '初始确权', regulated: r.regulated || '非管制',
        purpose: r.purpose || '', sourceSubject: r.sourceSubject || '', sourceLimit: r.sourceLimit || '',
        relationSubject: r.relationSubject || '', equityRisk: r.equityRisk || '',
        rightTypes: r.rightType ? String(r.rightType).split(/[、,，]/).map(s => s.trim()).filter(Boolean) : [],
      })
      ElMessage.warning('已带入被驳回原单内容,请修改后重新提交(将作为新申请)')
    }
  } catch (e) { /* ignore */ }
  sessionStorage.removeItem('prm-reopen')
})
const needTable2 = computed(() =>
  form.sourceIdent.some(c => ['B', 'C', 'D', 'E', 'F'].includes(c)) ||
  form.relationIdent.some(c => ['G', 'H', 'I', 'J'].includes(c)))

// 资产远程搜索(产权台账):降低"不知道填什么ID"的首填门槛
import { pageArchive } from '@/api/propertyArchive'
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
  const hit = assetOpts.value.find(a => a.assetId === id)
  if (hit) form.assetName = hit.assetName
  if (id) onAutofill(true)
}

// 一键填充示例(测试/演示):对齐 test/确权申请 手册 AST-001 全套数据
function fillDemo() {
  Object.assign(form, {
    assetId: 'AST-001', assetName: '客户用电信息表',
    rightTypes: ['数据资源持有权', '数据加工使用权'],
    rightHolder: '广东电网有限责任公司', respDept: '数字化部',
    systemOwner: '张工', contactInfo: '020-88886666',
    registerType: '初始确权', applyMode: '常规', regulated: '管制业务',
    sourceIdent: ['A'], relationIdent: ['G', 'H'],
    sourceSubject: '用电客户', sourceLimit: '涉个人信息字段对外提供须脱敏并经客户授权',
    relationSubject: '国家能源局南方监管局;用电客户', equityRisk: '未经授权对外提供个人信息存在合规风险',
    purpose: '营销域购售电数据确权(示例)'
  })
  tableItemText.value = 'MKT_DB01,MKT,C_CONS_ELEC_INFO,客户用电信息表,敏感信息,A 自行生产数据,广东电网有限责任公司'
  parseTableItems()
  // autofill 取质量评分,但桩会回写申报主体为网公司,完成后恢复示例主体(分省申报口径)
  onAutofill(true).then(() => {
    form.rightHolder = '广东电网有限责任公司'
    form.respDept = '数字化部'
  })
  ElMessage.success('已填充 AST-001 示例,可直接"下一步";材料文件在 test/确权申请 目录')
}

let lastAutofillId = ''
async function onAutofill(silent = false) {
  if (!form.assetId) { if (!silent) ElMessage.warning('请先填写关联资产ID'); return }
  autoLoading.value = true
  try {
    const r = await autofillConfirm(form.assetId)
    form.assetName = r.assetName; form.rightHolder = r.rightHolder
    form.respDept = r.respDept; if (!form.rightTypes.length && r.rightType) form.rightTypes = [r.rightType]
    quality.value = r.qualityScore
    lastAutofillId = form.assetId
    ElMessage.success(silent ? `已实时同步元数据(${form.assetId})` : '已按元数据自动填充')
  } finally { autoLoading.value = false }
}
// 资产ID 失焦即实时同步元数据(变化且非空才同步,避免重复)
function onAutoFillSilent() {
  if (form.assetId && form.assetId !== lastAutofillId) onAutofill(true)
}

// 步骤1 -> 2:暂存草稿,生成 A–J 应交材料清单
async function next0() {
  await formRef.value.validate()
  if (needTable2.value && !form.sourceSubject) { ElMessage.warning('涉第三方/敏感(B–F或G–J),须填表2来源主体名称'); return }
  if (!applyId.value) {
    saving.value = true
    try {
      const payload = {
        ...form,
        // 多权属类型合并一份申请(评审8.1):拼接保存,兼容单类型
        rightType: form.rightTypes.join('、'),
        sourceIdentification: form.sourceIdent.join(','),
        relationIdentification: form.relationIdent.join(','),
        involvesThirdParty: needTable2.value,
        reConfirm: form.registerType === '确权变更',
        thirdPartyInfo: needTable2.value ? `来源主体:${form.sourceSubject}; 限制:${form.sourceLimit}; 关联主体:${form.relationSubject}; 风险:${form.equityRisk}` : ''
      }
      applyId.value = await saveConfirmDraft(payload)
      if (tableItems.value.length) {
        await saveTableItems(applyId.value, tableItems.value)
        ElMessage.success(`已保存 ${tableItems.value.length} 张表级清单(M02)`)
      }
      loadConsolidation()
      buildChecklist()
    } finally { saving.value = false }
  }
  step.value = 1
}

function buildChecklist() {
  const base = [{ code: '表1', name: '《表1 数据确权信息清单(系统级)》', m: '表1' }, { code: '证明', name: '数据确权证明材料(权属/来源凭证)', m: '证明材料' }]
  if (needTable2.value) base.push({ code: '表2', name: '《表2 数据确权信息清单(涉及第三方权益)》', m: '表2' })
  const picked = [
    ...sourceOpts.filter(s => form.sourceIdent.includes(s.v)),
    ...relationOpts.filter(r => form.relationIdent.includes(r.v))
  ].map(o => ({ code: o.v, name: o.m, m: o.t }))
  checklist.value = [...base, ...picked].map((x, i) => ({ ...x, id: 'ck' + i, done: false }))
}

async function registerMaterial(row) {
  await uploadMaterial({
    applyId: applyId.value, materialName: row.name, materialType: row.m,
    fileUrl: `/files/dev/${applyId.value}-${row.code}.pdf`, owner: form.rightHolder
  })
  row.done = true
  ElMessage.success('已登记')
}

async function onUploadFile(row, file) {
  if (!file || !file.raw) return
  const fd = new FormData()
  fd.append('file', file.raw)
  fd.append('applyId', applyId.value)
  fd.append('materialName', row.name)
  fd.append('materialType', row.m)
  fd.append('owner', form.rightHolder || '')
  await uploadMaterialFile(fd) // 后端格式验证不通过会抛错(拦截器toast)
  row.done = true
  ElMessage.success('原件已上传并通过格式验证')
}

function previewMaterial(row) {
  if (row.materialId) window.open(materialFileUrl(row.materialId), '_blank')
}
function onExportCheck() {
  if (applyId.value) window.open(materialExportUrl(applyId.value), '_blank')
}

async function next1() {
  materials.value = await listMaterialByApply(applyId.value) || []
  if (materials.value.length === 0) { ElMessage.warning('请先至少登记一项材料'); return }
  step.value = 2
}

async function runCheck() {
  checking.value = true
  try {
    checkReport.value = await runMaterialCheck(applyId.value) // 后端规则校验:缺失/不合规自动识别
    materials.value = await listMaterialByApply(applyId.value) || []
    if (checkReport.value.allPass) ElMessage.success('材料校验全部通过,可推送审核')
    else ElMessage.warning(`校验未通过:缺失 ${checkReport.value.missing.length} / 不合规 ${checkReport.value.failCount} 项`)
  } finally { checking.value = false }
}

async function next2() {
  submitting.value = true
  try {
    await pushMaterialReview(applyId.value) // 后端门禁:校验全通过才提交审核,否则抛错(拦截器toast缺失/不合规)
    step.value = 3
  } finally { submitting.value = false }
}

function goProgress() { router.push('/dpr/confirm/history') }
function reset() {
  step.value = 0; applyId.value = ''; applyNo.value = ''; quality.value = null
  checklist.value = []; materials.value = []
  Object.assign(form, { assetId: '', assetName: '', rightTypes: [], rightHolder: '', respDept: '', systemOwner: '', contactInfo: '', registerType: '初始确权', applyMode: '常规', purpose: '', thirdPartyInfo: '', sourceSubject: '', sourceLimit: '', relationSubject: '', equityRisk: '', sourceIdent: [], relationIdent: [] })
}
</script>

<style scoped>
.wz-steps { max-width: 900px; margin: 8px auto 20px; }
.wz-body { min-height: 320px; }
.wz-foot { margin-top: 18px; display: flex; gap: 12px; justify-content: center; }
.form-tip { font-size: 12px; color: #909399; line-height: 1.6; }
.approve-chain { max-width: 880px; margin: 8px auto 0; }
</style>
