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
        <el-form ref="formRef" :model="form" :rules="rules" label-width="130px" :disabled="!!applyId" style="max-width:680px">
          <el-form-item label="AI 智能填单">
            <div style="width:100%">
              <el-input v-model="aiText" type="textarea" :rows="2" placeholder="用自然语言描述授权诉求,如:拟向广州供电局开放数据用于电力金融征信,全字段" />
              <el-button type="primary" plain size="small" :loading="aiLoading" style="margin-top:6px" @click="onAiFill">大瓦特 AI 识别意图并填单</el-button>
              <span v-if="aiTip" style="margin-left:10px;color:#909399;font-size:12px">{{ aiTip }}</span>
            </div>
          </el-form-item>
          <el-divider style="margin:4px 0" />
          <el-form-item label="关联资产ID" prop="assetId">
            <el-input v-model="form.assetId" placeholder="输入资产ID后失焦，自动引用资产信息" @blur="onAssetBlur">
              <template #append><el-button :loading="assetLoading" @click="onAssetBlur">引用资产信息</el-button></template>
            </el-input>
          </el-form-item>
          <el-form-item label="资产名称" prop="assetName"><el-input v-model="form.assetName" /></el-form-item>
          <el-alert v-if="assetRef" type="success" :closable="false" style="margin:0 0 12px 0">
            已引用外部资产信息 — 系统:{{ assetRef.systemName || '-' }} / 模式:{{ assetRef.schemaName || '-' }} / 安全等级:{{ assetRef.securityLevel || '-' }} / 责任部门:{{ assetRef.respDept || '-' }}
          </el-alert>
          <el-form-item label="权益卡片ID" prop="equityCardId"><el-input v-model="form.equityCardId" placeholder="先确后授:引用已确权权益卡片" /></el-form-item>
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
          <el-form-item label="有效期(权益时效)"><el-date-picker v-model="form.validDate" type="date" value-format="YYYY-MM-DDTHH:mm:ss" style="width:100%" /></el-form-item>
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
</style>
