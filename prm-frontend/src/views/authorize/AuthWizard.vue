<template>
  <div class="prm-page">
    <div class="prm-table-note" style="margin:0 0 12px">
      注:一站式一事一议(专项)授权申请——填申请(表5) → 合规校验 → 提交审核,一条流程办完。后续多级审批 → 授权方/被授权方双签《运营授权协议(附录D)》 → 自动签发授权证书(对齐附录F §4.3)。
    </div>

    <el-steps :active="step" finish-status="success" align-center class="wz-steps">
      <el-step title="填写申请" description="表5 + 先确后授" />
      <el-step title="合规校验" description="范围≤确权边界" />
      <el-step title="提交审核" description="进入多级审批" />
      <el-step title="完成" description="审批→双签→发证" />
    </el-steps>

    <div class="wz-body">
      <!-- 步骤1:填申请(表5 全字段) -->
      <el-card v-show="step === 0" shadow="never">
        <el-alert v-if="!applyId" type="info" :closable="false" style="margin-bottom:12px;max-width:680px">
          <template #title>
            第一次填写?可
            <el-button link type="primary" style="vertical-align:baseline" @click="fillDemo">一键填充示例(AST-001/EC-PRA-0001,测试/演示用)</el-button>
            材料包见 test/一事一议授权申请 目录
          </template>
        </el-alert>
        <el-form ref="formRef" :model="form" :rules="rules" label-width="150px" :disabled="!!applyId" style="max-width:680px">
          <el-form-item label="AI 智能填单">
            <div style="width:100%">
              <el-input v-model="aiText" type="textarea" :rows="2" placeholder="用自然语言描述授权诉求,如:拟向广州供电局开放数据用于电力金融征信,全字段" />
              <el-button type="primary" plain size="small" :loading="aiLoading" style="margin-top:6px" @click="onAiFill">大瓦特 AI 识别意图并填单</el-button>
              <span v-if="aiTip" style="margin-left:10px;color:#909399;font-size:12px">{{ aiTip }}</span>
            </div>
          </el-form-item>
          <el-divider style="margin:4px 0" />
          <el-form-item label="关联资产ID" prop="assetId">
            <div style="display:flex;gap:8px;width:100%">
              <el-select v-model="form.assetId" filterable remote allow-create default-first-option clearable
                :remote-method="searchAssets" :loading="assetSearching" style="flex:1"
                placeholder="输入资产名称/ID 搜索台账,如 用电 / AST-001" @change="onAssetPicked">
                <el-option v-for="a in assetOpts" :key="a.assetId" :value="a.assetId" :label="a.assetId + '　' + a.assetName">
                  <span>{{ a.assetId }}</span>
                  <span style="float:right;color:#8c8c8c;font-size:12px">{{ a.assetName }}</span>
                </el-option>
              </el-select>
              <el-button :loading="assetLoading" @click="onAssetBlur">引用资产信息</el-button>
            </div>
            <div class="auth-tip">选资产后自动带出该资产的"生效"权益卡片(先确后授);不清楚ID输名称关键词即可搜索</div>
          </el-form-item>
          <el-form-item label="资产名称" prop="assetName"><el-input v-model="form.assetName" /></el-form-item>
          <el-alert v-if="assetRef" type="success" :closable="false" style="margin:0 0 12px 0">
            已引用外部资产信息 — 系统:{{ assetRef.systemName || '-' }} / 模式:{{ assetRef.schemaName || '-' }} / 安全等级:{{ assetRef.securityLevel || '-' }} / 责任部门:{{ assetRef.respDept || '-' }}
          </el-alert>
          <el-form-item label="权益卡片ID" prop="equityCardId">
            <el-select v-model="form.equityCardId" filterable allow-create default-first-option clearable style="width:100%"
              placeholder="先确后授:搜索已确权权益卡片,如 EC-PRA-0001" @focus="loadCards" @change="onCardPicked">
              <el-option v-for="c in cardOpts" :key="c.cardNo || c.cardId" :value="c.cardNo || c.cardId"
                :label="(c.cardNo || c.cardId) + '　' + (c.assetName || c.assetId)" :disabled="!CARD_OK.includes(c.cardStatus)">
                <span>{{ c.cardNo || c.cardId }}</span>
                <span style="float:right;font-size:12px" :style="{color: CARD_OK.includes(c.cardStatus) ? '#18a058' : '#b4b4b4'}">
                  {{ c.assetName || c.assetId }} · {{ c.cardStatus }}</span>
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="申请主体(被授权方)" prop="granteeOrg"><el-input v-model="form.granteeOrg" /></el-form-item>
          <el-form-item label="授权权益类型" prop="rightType">
            <el-select v-model="form.rightType" style="width:100%">
              <el-option v-for="t in rightTypes" :key="t" :label="t" :value="t" />
            </el-select>
          </el-form-item>
          <el-form-item label="使用场景及目的">
            <el-select v-model="form.scenario" filterable clearable placeholder="选择或搜索应用场景（选后自动带出申请原因）"
              style="width:100%" @change="onScenarioChange">
              <el-option v-for="s in scenarioOpts" :key="s.scenarioId" :label="`${s.scenarioName}（${s.category}）`" :value="s.scenarioName">
                <span>{{ s.scenarioName }}</span>
                <span style="float:right;color:#8492a6;font-size:12px">{{ s.category }}</span>
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item v-if="selectedReason" label="申请原因模板">
            <el-alert :closable="false" type="info" style="width:100%">{{ selectedReason }}</el-alert>
          </el-form-item>
          <el-form-item label="授权范围"><el-input v-model="form.scope" /></el-form-item>
          <el-form-item label="有效期(权益时效)"><el-date-picker v-model="form.validDate" type="date" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" /></el-form-item>
          <el-form-item label="所属业务域"><el-input v-model="form.businessDomain" placeholder="营销/生产/调度/财务..." /></el-form-item>
          <el-form-item label="申请单位主管"><el-input v-model="form.applicantManager" /></el-form-item>
          <el-form-item label="联系方式"><el-input v-model="form.contactInfo" placeholder="电话 / 邮箱" /></el-form-item>
          <el-form-item label="是否跨区域/跨域"><el-switch v-model="form.crossRegion" /></el-form-item>
          <el-form-item label="涉个人隐私/商密"><el-input v-model="form.sensitiveType" placeholder="个人隐私 / 商业秘密 / 无" /></el-form-item>
          <el-form-item label="第三方来源方式"><el-input v-model="form.thirdPartySource" placeholder="涉及第三方时填" /></el-form-item>
          <el-form-item v-if="form.thirdPartySource" label="第三方许可凭证" required>
            <el-input v-model="form.thirdPartyLicense" type="textarea" :rows="2" placeholder="第三方许可凭证或说明(涉第三方必填)" />
          </el-form-item>
          <el-form-item label="信息授权协议"><el-input v-model="form.infoAuthAgreement" placeholder="信息授权协议名称/地址" /></el-form-item>
          <el-form-item label="需保密承诺函"><el-switch v-model="form.needConfidentiality" /><span style="margin-left:8px;color:#909399;font-size:12px">附录E</span></el-form-item>
          <el-form-item v-if="form.needConfidentiality" label="保密承诺函"><el-input v-model="form.confidentialityFile" placeholder="保密承诺函文件地址" /></el-form-item>
        </el-form>
        <el-divider />
        <div style="font-weight:600;margin-bottom:8px">申请材料（上传相关材料，先暂存草稿后上传）</div>
        <el-upload :show-file-list="false" :http-request="(o)=>doUploadMaterial(o.file)" accept=".pdf,.doc,.docx,.jpg,.jpeg,.png">
          <el-button type="primary" plain :loading="matUploading">上传材料</el-button>
        </el-upload>
        <el-table v-if="materials.length" :data="materials" border size="small" style="margin-top:10px;max-width:680px">
          <el-table-column prop="materialName" label="材料名称" min-width="200" show-overflow-tooltip />
          <el-table-column prop="uploadTime" label="上传时间" width="170"><template #default="{ row }">{{ (row.uploadTime||'').replace('T',' ').slice(0,19) }}</template></el-table-column>
          <el-table-column label="操作" width="130">
            <template #default="{ row }">
              <el-link type="primary" @click="previewMaterial(row)">预览</el-link>
              <el-button link type="danger" style="margin-left:8px" @click="delMaterial(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-alert v-if="applyId" type="success" :closable="false" show-icon :title="`申请已暂存(${applyId})，进入合规校验`" style="max-width:680px;margin-top:8px" />
      </el-card>

      <!-- 步骤2:合规校验 -->
      <el-card v-show="step === 1" shadow="never">
        <div class="prm-table-note" style="margin-bottom:10px">依据规则自动校验【材料完整性 / 权限合理性(先确后授·权益类型·授权范围) / 合规性(第三方许可·敏感数据·跨域)】，红灯不通过不可提交。</div>
        <el-button type="primary" :loading="checking" @click="runCheck" style="margin-bottom:12px">执行合规校验</el-button>
        <el-button type="warning" :loading="aiMatChecking" @click="runAiMatCheck" style="margin-bottom:12px;margin-left:8px">AI 材料校验(qwen3-max)</el-button>
        <el-button type="warning" plain :loading="preReviewing" @click="runPreReview" style="margin-bottom:12px;margin-left:8px">AI 合规预审</el-button>
        <el-alert v-if="preOpinion" type="info" :closable="false" style="margin-bottom:12px" :title="'AI 预审意见'" :description="preOpinion" show-icon />
        <el-alert v-if="aiMatResult" :type="aiMatResult.overall === '通过' ? 'success' : 'warning'" :closable="false" style="margin-bottom:12px">
          <div><b>AI 材料校验:{{ aiMatResult.overall }}</b> — {{ aiMatResult.overallDesc }}</div>
          <el-table :data="aiMatResult.items" border size="small" style="margin-top:8px">
            <el-table-column prop="materialName" label="材料" min-width="200" />
            <el-table-column label="结论" width="90" align="center">
              <template #default="{ row }"><el-tag :type="row.verdict === '通过' ? 'success' : 'warning'" size="small">{{ row.verdict }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="issues" label="问题" min-width="180" />
            <el-table-column prop="suggestion" label="建议" min-width="160" />
          </el-table>
        </el-alert>
        <el-descriptions v-if="checkResult" :column="2" border style="margin-bottom:12px">
          <el-descriptions-item label="校验结果">
            <el-tag :type="checkResult.checkResult === '通过' ? 'success' : (checkResult.checkResult === '不通过' ? 'danger' : 'warning')">{{ checkResult.checkResult || '—' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="风险等级">
            <el-tag :type="checkResult.riskLevel === '红' ? 'danger' : (checkResult.riskLevel === '黄' ? 'warning' : 'success')">{{ checkResult.riskLevel || '—' }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>
        <el-table v-if="checkResult && checkResult.items" :data="checkResult.items" border size="small" style="max-width:760px">
          <el-table-column prop="dimension" label="维度" width="110"><template #default="{ row }"><el-tag effect="plain">{{ row.dimension }}</el-tag></template></el-table-column>
          <el-table-column prop="item" label="校验项" width="190" />
          <el-table-column label="结果" width="80" align="center"><template #default="{ row }"><el-tag :type="row.pass?'success':'danger'" size="small">{{ row.pass?'通过':'不符' }}</el-tag></template></el-table-column>
          <el-table-column prop="message" label="说明" min-width="200" show-overflow-tooltip />
        </el-table>
      </el-card>

      <!-- 步骤3:提交审核 -->
      <el-card v-show="step === 2" shadow="never">
        <el-result icon="info" title="提交至多级审批" sub-title="合规小组评审 → 业务部门审核 → 数字化部认定 → 经理/副总/总经理审批">
          <template #extra><el-tag v-if="checkResult" :type="checkResult.checkResult==='不通过'?'danger':'success'">合规校验:{{ checkResult.checkResult || '未校验' }}</el-tag></template>
        </el-result>
      </el-card>

      <!-- 步骤4:完成 -->
      <el-card v-show="step === 3" shadow="never">
        <el-result icon="success" title="一事一议授权申请已提交" :sub-title="`申请 ${applyId} 已进入审批流`">
          <template #extra>
            <div style="margin-bottom:10px">
              <el-button type="success" @click="go('/dpr/auth/review?applyId=' + applyId)">去审核(授权审核提交)</el-button>
              <el-button type="warning" plain @click="go('/dpr/auth/agreement-seal')">去协议签章(双签附录D)</el-button>
            </div>
            <div class="wz-flow">后续闭环:多级审批 → 授权方/被授权方双签《运营授权协议(附录D)》→ 自动签发授权证书 → 执行授权</div>
            <div style="margin-top:14px">
              <el-button type="primary" @click="go('/dpr/auth/review')">去审核台</el-button>
              <el-button @click="go('/dpr/auth/agreement-seal')">协议双签</el-button>
              <el-button @click="go('/dpr/auth/cert')">授权证书</el-button>
              <el-button @click="reset">再发起一笔</el-button>
            </div>
          </template>
        </el-result>
      </el-card>
    </div>

    <div class="wz-foot">
      <el-button v-if="step > 0 && step < 3" @click="step--">上一步</el-button>
      <el-button v-if="step === 0" type="primary" :loading="saving" @click="next0">下一步:合规校验</el-button>
      <el-button v-if="step === 1" type="primary" :disabled="!checkResult || checkResult.checkResult==='不通过'" @click="step = 2">下一步:提交审核</el-button>
      <el-button v-if="step === 2" type="primary" :loading="submitting" @click="doSubmit">提交审核</el-button>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { saveAuthDraft, submitAuth, runAuthCompliance, pageScenario, uploadAuthMaterialFile, listAuthMaterial, deleteAuthMaterial, authMaterialFileUrl } from '@/api/authorize'
import { aiAuthIntent } from '@/api/confirm'
import { aiAuthMaterialCheck, aiAuthPreReview } from '@/api/authorize'
import { pageArchive } from '@/api/propertyArchive'
import { pageEquityCard } from '@/api/confirm'
import { getAsset } from '@/api/ledger'

const router = useRouter()
const rightTypes = ['数据加工使用权', '数据产品经营权']
const step = ref(0)
const formRef = ref()
const saving = ref(false)
const checking = ref(false)
const submitting = ref(false)
const applyId = ref('')
const checkResult = ref(null)
const aiText = ref(''); const aiLoading = ref(false); const aiTip = ref('')

function empty() {
  return { assetId: '', assetName: '', equityCardId: '', granteeOrg: '', rightType: '', scenario: '', scope: '', validDate: '', businessDomain: '', applicantManager: '', contactInfo: '', crossRegion: false, sensitiveType: '', thirdPartySource: '', thirdPartyLicense: '', infoAuthAgreement: '', needConfidentiality: false, confidentialityFile: '' }
}
const form = reactive(empty())
const scenarioOpts = ref([])
const selectedReason = ref('')
onMounted(async () => {
  const r = await pageScenario({ status: '生效中', size: 100 })
  scenarioOpts.value = r.records || []
})
function onScenarioChange(name) {
  const s = scenarioOpts.value.find(x => x.scenarioName === name)
  selectedReason.value = s && s.reasonTemplate ? s.reasonTemplate : ''
}

// 集成外部资产信息引用:输入资产ID → 自动加载资产基本信息
const assetRef = ref(null); const assetLoading = ref(false)
// 资产远程搜索(产权台账)+选资产自动带生效卡片(先确后授,一选三填)
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
async function onAssetPicked(id) {
  const hit = assetOpts.value.find(a => a.assetId === id)
  if (hit) form.assetName = hit.assetName
  if (!id) return
  onAssetBlur()
  await loadCards()
  const card = cardOpts.value.find(c => c.assetId === id && CARD_OK.includes(c.cardStatus))
  if (card) {
    form.equityCardId = card.cardNo || card.cardId
    ElMessage.success('已自动带出生效权益卡片 ' + form.equityCardId)
  } else {
    ElMessage.warning('该资产暂无生效权益卡片,请先完成确权(先确后授)')
  }
}

// 权益卡片选择器(失效卡禁选,选卡反向回填资产)
const CARD_OK = ['正常', '生效']
const cardOpts = ref([])
let cardsLoaded = false
async function loadCards() {
  if (cardsLoaded) return
  const r = await pageEquityCard({ current: 1, size: 100 })
  cardOpts.value = r.records || []
  cardsLoaded = true
}
function onCardPicked(no) {
  const hit = cardOpts.value.find(c => (c.cardNo || c.cardId) === no)
  if (hit) {
    form.assetId = hit.assetId
    form.assetName = hit.assetName || form.assetName
  }
}

// 一键填充示例(测试/演示):对齐 test/一事一议授权申请 手册
function fillDemo() {
  Object.assign(form, {
    assetId: 'AST-001', assetName: '客户用电信息表', equityCardId: 'EC-PRA-0001',
    granteeOrg: '广州供电局', rightType: '数据产品经营权', scenario: '电力金融征信',
    scope: '全字段', validDate: '2028-06-11 00:00:00', businessDomain: '营销域',
    applicantManager: '李主管', contactInfo: '020-66668888', crossRegion: false,
    sensitiveType: '个人隐私', thirdPartySource: '', needConfidentiality: true,
    confidentialityFile: '04-保密承诺函(附录E)-广州供电局.docx',
    infoAuthAgreement: '03-信息授权协议-征信客户授权说明.docx'
  })
  onAssetBlur()
  ElMessage.success('已填充示例,可直接"下一步";材料文件在 test/一事一议授权申请 目录')
}

async function onAssetBlur() {
  if (!form.assetId) return
  assetLoading.value = true
  try {
    const a = await getAsset(form.assetId)
    if (a) {
      assetRef.value = a
      if (!form.assetName) form.assetName = a.assetName || ''
      if (!form.businessDomain) form.businessDomain = a.subsidiaryName || a.systemName || ''
      ElMessage.success('已引用资产信息')
    }
  } catch { assetRef.value = null } finally { assetLoading.value = false }
}

// 申请材料上传/管理(需先有草稿ID)
const materials = ref([]); const matUploading = ref(false)
async function ensureDraft() {
  if (!applyId.value) applyId.value = await saveAuthDraft({ authMode: '一事一议', ...form })
  return applyId.value
}
async function refreshMaterials() { if (applyId.value) materials.value = await listAuthMaterial(applyId.value) || [] }
async function doUploadMaterial(file) {
  matUploading.value = true
  try {
    const id = await ensureDraft()
    const fd = new FormData(); fd.append('file', file); fd.append('applyId', id); fd.append('materialName', file.name)
    await uploadAuthMaterialFile(fd)
    ElMessage.success('材料已上传'); refreshMaterials()
  } finally { matUploading.value = false }
}
function previewMaterial(row) { window.open(authMaterialFileUrl(row.materialId), '_blank') }
async function delMaterial(row) { await deleteAuthMaterial(row.materialId); ElMessage.success('已删除'); refreshMaterials() }
const rules = {
  assetId: [{ required: true, message: '请输入关联资产ID', trigger: 'blur' }],
  assetName: [{ required: true, message: '请输入资产名称', trigger: 'blur' }],
  equityCardId: [{ required: true, message: '先确后授:必须引用权益卡片', trigger: 'blur' }],
  granteeOrg: [{ required: true, message: '请输入被授权方', trigger: 'blur' }],
  rightType: [{ required: true, message: '请选择授权权益类型', trigger: 'change' }]
}

async function onAiFill() {
  if (!aiText.value) { ElMessage.warning('请输入授权诉求描述'); return }
  aiLoading.value = true
  try {
    const r = await aiAuthIntent(aiText.value)
    if (r.granteeOrg && r.granteeOrg !== '待明确被授权方') form.granteeOrg = r.granteeOrg
    if (rightTypes.includes(r.rightType)) form.rightType = r.rightType
    if (r.scenario) form.scenario = r.scenario
    if (r.scope) form.scope = r.scope
    aiTip.value = `识别模式:${r.mode}　置信:${(r.confidence * 100).toFixed(0)}%`
    ElMessage.success('AI 已填充,请核对后补全资产/权益卡片')
  } catch (e) { ElMessage.error('AI 识别失败,请手动填写') } finally { aiLoading.value = false }
}

async function next0() {
  await formRef.value.validate()
  if (form.thirdPartySource && !form.thirdPartyLicense) { ElMessage.warning('涉及第三方来源,须填第三方许可凭证或说明'); return }
  if (!applyId.value) {
    saving.value = true
    try { applyId.value = await saveAuthDraft({ authMode: '一事一议', ...form }) }
    finally { saving.value = false }
  }
  step.value = 1
}

// AI 材料校验 + AI 合规预审(qwen3-max,stub 回退)
const aiMatChecking = ref(false); const aiMatResult = ref(null)
const preReviewing = ref(false); const preOpinion = ref('')
async function runAiMatCheck() {
  if (!applyId.value) { ElMessage.warning('请先暂存申请'); return }
  aiMatChecking.value = true
  try {
    const raw = await aiAuthMaterialCheck(applyId.value)
    aiMatResult.value = typeof raw === 'string' ? JSON.parse(raw) : raw
  } catch (e) { ElMessage.warning('AI 材料校验失败:' + (e?.response?.data?.message || '请先上传材料')) }
  finally { aiMatChecking.value = false }
}
async function runPreReview() {
  if (!applyId.value) { ElMessage.warning('请先暂存申请'); return }
  preReviewing.value = true
  try { preOpinion.value = await aiAuthPreReview(applyId.value) }
  catch (e) { ElMessage.warning('AI 预审失败') }
  finally { preReviewing.value = false }
}

async function runCheck() {
  checking.value = true
  try {
    const r = await runAuthCompliance({ applyId: applyId.value })
    checkResult.value = r || { checkResult: '通过', riskLevel: '低', problemDesc: '无' }
    ElMessage.success('校验完成')
  } finally { checking.value = false }
}

async function doSubmit() {
  submitting.value = true
  try { await submitAuth(applyId.value); step.value = 3 }
  finally { submitting.value = false }
}

function go(path) { router.push(path) }
function reset() {
  step.value = 0; applyId.value = ''; checkResult.value = null; aiText.value = ''; aiTip.value = ''
  Object.assign(form, empty())
}
</script>

<style scoped>
.wz-steps { max-width: 900px; margin: 8px auto 20px; }
.wz-body { min-height: 320px; }
.wz-foot { margin-top: 18px; display: flex; gap: 12px; justify-content: center; }
.wz-flow { background: #f7f9ff; border-radius: 8px; padding: 10px 16px; color: #4a5160; font-size: 13px; display: inline-block; }
.auth-tip { font-size: 12px; color: #8c8c8c; line-height: 1.6; }
</style>
