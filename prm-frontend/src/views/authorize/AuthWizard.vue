<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
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
        <el-form ref="formRef" :model="form" :rules="rules" label-width="150px" :disabled="!!applyId && !editing" style="max-width:680px">
          <el-form-item label="AI 智能填单">
            <div style="width:100%">
              <el-input v-model="aiText" type="textarea" maxlength="500" show-word-limit :rows="2" placeholder="用自然语言描述授权诉求,如:拟向广州供电局开放数据用于电力金融征信,全字段" />
              <el-button type="primary" plain size="small" :loading="aiLoading" style="margin-top:6px" @click="onAiFill">大瓦特 AI 识别意图并填单</el-button>
              <span v-if="aiTip" style="margin-left:10px;color:var(--prm-color-text-weak);font-size:12px">{{ aiTip }}</span>
              <AiThinking v-bind="aiThink.state" />
            </div>
          </el-form-item>
          <el-divider style="margin:4px 0" />
          <el-form-item label="关联数据资产卡片" prop="assetId">
            <div style="display:flex;gap:8px;width:100%">
              <el-select v-model="form.assetId" filterable remote clearable
                :remote-method="searchAssets" :loading="assetSearching" style="flex:1"
                placeholder="搜索已确权资产(名称/卡片号)选取,先确后授" @change="onAssetPicked">
                <el-option v-for="a in assetOpts" :key="a.assetId" :value="a.assetId" :label="a.assetName || a.assetId">
                  <span>{{ a.assetName || a.assetId }}</span>
                  <span style="float:right;color:#8a8a8a;font-size:12px">{{ a.cardNo }}</span>
                </el-option>
              </el-select>
              <el-button :loading="assetLoading" @click="onAssetBlur">引用资产信息</el-button>
            </div>
            <div class="auth-tip">从已确权资产中选取,自动带出"生效"权益卡片(先确后授);仅已确权资产可被授权</div>
          </el-form-item>
          <el-form-item label="数据表(资产名)" prop="assetName"><el-input v-model="form.assetName" readonly placeholder="选取卡片后自动带出" /></el-form-item>
          <el-alert v-if="assetRef" type="success" :closable="false" style="margin:0 0 12px 0">
            已引用外部资产信息 — 系统:{{ assetRef.systemName || '-' }} / 模式:{{ assetRef.schemaName || '-' }} / 安全等级:{{ assetRef.securityLevel || '-' }} / 责任部门:{{ assetRef.respDept || '-' }}
          </el-alert>
          <el-form-item label="生效权益卡片" prop="equityCardId"><el-input v-model="form.equityCardId" readonly placeholder="选取资产后自动匹配生效卡片(先确后授)" /></el-form-item>
          <el-divider content-position="left" style="margin:8px 0"><span style="font-size:12px;color:var(--prm-color-text-weak)">数据信息(第三方/隐私 由确权带出)</span></el-divider>
          <el-form-item label="第三方来源方式"><el-input v-model="form.thirdPartySource" readonly placeholder="选取资产后由确权记录自动带出(不涉及则空)" /></el-form-item>
          <el-form-item label="涉个人隐私/商密"><el-input v-model="form.sensitiveType" readonly placeholder="选取资产后由确权记录自动带出" /></el-form-item>
          <el-form-item v-if="form.thirdPartySource" label="第三方许可凭证">
            <el-input v-model="form.thirdPartyLicense" type="textarea" maxlength="500" show-word-limit :rows="2" placeholder="填写许可凭证/说明,或在下方应交清单上传《第三方许可凭证或说明》" />
            <div style="font-size:12px;color:var(--prm-color-link);line-height:1.5;margin-top:2px">确权识别涉第三方,二选一即可:① 此处填说明　② 应交清单上传同名材料(上传后自动回填引用)</div>
          </el-form-item>
          <el-form-item label="信息授权协议">
            <el-input v-model="form.infoAuthAgreement" placeholder="填写协议名称/地址,或在下方应交清单上传《信息授权协议》" />
            <div v-if="form.sensitiveType && form.sensitiveType.trim() && form.sensitiveType !== '无'" style="font-size:12px;color:var(--prm-color-link);line-height:1.5;margin-top:2px">涉个人隐私/商密,二选一即可:① 此处填名称/地址　② 应交清单上传同名材料(上传后自动回填引用)</div>
          </el-form-item>
          <el-form-item label="所属业务域"><el-input v-model="form.businessDomain" placeholder="营销/生产/调度/财务..." /></el-form-item>
          <el-divider content-position="left" style="margin:8px 0"><span style="font-size:12px;color:var(--prm-color-text-weak)">授权内容</span></el-divider>
          <el-form-item label="申请主体(被授权方)" prop="granteeOrg">
            <el-autocomplete v-model="form.granteeOrg" :fetch-suggestions="queryOrg" placeholder="表5 申请主体:输入并从真实组织树中选取(可自定义外部主体)" clearable style="width:100%" />
          </el-form-item>
          <el-form-item label="授权权益类型(单选)" prop="rightType">
            <el-select v-model="form.rightType" style="width:100%" placeholder="单选(授权仅授使用权/经营权;持有权经确权认定取得)">
              <el-option v-for="t in rightTypes" :key="t" :label="t" :value="t" />
            </el-select>
          </el-form-item>
          <el-form-item label="使用场景及目的">
            <el-select v-model="form.scenario" filterable clearable :placeholder="form.rightType ? `选择「${form.rightType}」适用的应用场景` : '选择或搜索应用场景(选后自动带出申请原因)'"
              style="width:100%" @change="onScenarioChange">
              <el-option v-for="s in filteredScenarios" :key="s.scenarioId" :label="`${s.scenarioName}（${s.category}）`" :value="s.scenarioName">
                <span>{{ s.scenarioName }}</span>
                <span style="float:right;color:#8492a6;font-size:12px">{{ s.category }}{{ s.rightType && s.rightType !== '通用' ? ' · ' + s.rightType : '' }}</span>
              </el-option>
            </el-select>
            <div v-if="form.rightType" style="font-size:12px;color:var(--prm-color-text-weak);line-height:1.5;margin-top:2px">仅列适用「{{ form.rightType }}」及通用的场景(应用场景管理按权益类型配置)</div>
          </el-form-item>
          <el-form-item v-if="selectedReason" label="申请原因模板">
            <el-alert :closable="false" type="info" style="width:100%">{{ selectedReason }}</el-alert>
          </el-form-item>
          <el-form-item label="授权范围"><el-input v-model="form.scope" /></el-form-item>
          <el-form-item label="授权时效">
            <el-select v-model="form.validTerm" style="width:100%" placeholder="默认两年(时长)">
              <el-option v-for="t in validTerms" :key="t" :label="t" :value="t" />
            </el-select>
            <div style="font-size:12px;color:var(--prm-color-text-weak);line-height:1.5;margin-top:2px">
              映射到期日(预期):{{ expiryOf(form.validTerm) ? expiryOf(form.validTerm).slice(0,10) : '—' }};协议签订时按附录D最终落定
            </div>
          </el-form-item>
          <el-divider content-position="left" style="margin:8px 0"><span style="font-size:12px;color:var(--prm-color-text-weak)">授权协议要素(附录D §3.4.4)</span></el-divider>
          <el-form-item label="利益分配约定">
            <el-input v-model="form.benefitAllocation" type="textarea" :rows="2" maxlength="500" show-word-limit
              placeholder="附录D 须约定:如 免费内部共享 / 按调用次数计费 / 收益按比例分成 等" />
          </el-form-item>
          <el-form-item label="安全保障要求">
            <el-input v-model="form.securityReq" type="textarea" :rows="2" maxlength="500" show-word-limit
              placeholder="附录D 须约定:如 加密传输、最小授权访问控制、操作留痕审计、数据脱敏、不得转授第三方 等" />
          </el-form-item>
          <el-divider content-position="left" style="margin:8px 0"><span style="font-size:12px;color:var(--prm-color-text-weak)">范围与联系</span></el-divider>
          <el-form-item label="是否跨区域/跨域"><el-switch v-model="form.crossRegion" /></el-form-item>
          <el-form-item label="申请单位主管"><el-input v-model="form.applicantManager" /></el-form-item>
          <el-form-item label="联系方式"><el-input v-model="form.contactInfo" placeholder="电话 / 邮箱" /></el-form-item>
          <el-form-item label="需保密承诺函"><el-switch v-model="form.needConfidentiality" /><span style="margin-left:8px;color:var(--prm-color-text-weak);font-size:12px">附录E</span></el-form-item>
          <el-form-item v-if="form.needConfidentiality" label="保密承诺函"><el-input v-model="form.confidentialityFile" placeholder="保密承诺函文件地址" /></el-form-item>
        </el-form>
        <el-divider />
        <div v-if="requiredChecklist.length" style="margin-bottom:12px">
          <div style="font-weight:600;margin-bottom:8px">应交材料清单（按当前申请自动判定）— 请按下表逐项上传</div>
          <el-table :data="requiredChecklist" border size="small" style="max-width:880px">
            <el-table-column type="index" label="序号" width="56" align="center" />
            <el-table-column prop="materialName" label="应交材料" min-width="170" show-overflow-tooltip />
            <el-table-column prop="required" label="要求" width="84" align="center">
              <template #default="{ row }">
                <el-tag :type="row.required === '必填' ? 'danger' : 'warning'" effect="light" size="small">{{ row.required }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="detail" label="内容与要求明细" min-width="220" show-overflow-tooltip />
            <el-table-column label="状态" width="86" align="center">
              <template #default="{ row }"><el-tag :type="row.uploaded ? 'success' : 'info'" effect="light" size="small">{{ row.uploaded ? '已上传' : '待上传' }}</el-tag></template>
            </el-table-column>
            <el-table-column label="操作" width="156" align="center">
              <template #default="{ row }">
                <el-upload :show-file-list="false" :http-request="(o)=>doUploadForItem(row.materialName, o.file)" accept=".pdf,.doc,.docx,.jpg,.jpeg,.png" style="display:inline-block">
                  <el-button link type="primary" :loading="matUploading">上传</el-button>
                </el-upload>
                <el-button v-if="row.uploaded" link type="primary" style="margin-left:6px" @click="previewChecklistItem(row.materialName)">预览</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div style="margin-top:6px;color:var(--prm-color-text-weak);font-size:12px">必填项须上传;"视情况"项按是否涉第三方/隐私商密上传。也可在下方上传其他补充材料。</div>
        </div>
        <div style="font-weight:600;margin-bottom:8px">其他补充材料（可选，先暂存草稿后上传）</div>
        <el-upload :show-file-list="false" :http-request="(o)=>doUploadMaterial(o.file)" accept=".pdf,.doc,.docx,.jpg,.jpeg,.png">
          <el-button type="primary" plain :loading="matUploading">上传其他材料</el-button>
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
        <!-- 单一裁决状态条:一句话说清"能不能提交、还差什么" -->
        <div style="margin-bottom:10px">
          校验状态:<el-tag :type="checkStatus.type" effect="dark">{{ checkStatus.text }}</el-tag>
        </div>
        <!-- 主操作:一键校验(合规 + AI材料),降认知、显性正确路径 -->
        <el-button :type="needRecheck || pendingItems.length ? 'danger' : 'primary'" :loading="checking || aiMatChecking" @click="runFullCheck" style="margin-bottom:6px">
          {{ (checkResult || aiMatResult) ? '重新一键校验' : '一键校验(合规 + AI材料)' }}
        </el-button>
        <!-- 次要:AI 辅助(可选,不影响提交门禁) -->
        <div style="margin:2px 0 12px;color:var(--prm-color-text-weak);font-size:12px">
          AI 辅助(可选,不影响提交):
          <el-button link type="primary" :loading="preReviewing" @click="runPreReview">AI 合规预审</el-button>
        </div>
        <AiThinking v-bind="aiThink.state" />
        <!-- 统一待处理清单(单一闭环):合规不符/AI 存疑·不通过 → 去修正(回填报含应交材料)或复核确认 -->
        <el-card v-if="pendingItems.length" shadow="never" style="margin-bottom:12px;border:1px solid #fde2e2;background:#fff8f8">
          <div style="font-weight:600;color:var(--prm-color-danger);margin-bottom:8px">需处理以下 {{ pendingItems.length }} 项后方可提交(处理完点上方「重新一键校验」)</div>
          <el-table :data="pendingItems" border size="small">
            <el-table-column label="来源" width="76" align="center">
              <template #default="{ row }"><el-tag :type="row.source === 'ai' ? 'warning' : 'danger'" size="small">{{ row.source === 'ai' ? 'AI' : '合规' }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="name" label="校验项 / 材料" min-width="180" show-overflow-tooltip />
            <el-table-column prop="kind" label="问题" width="96" align="center">
              <template #default="{ row }"><el-tag type="danger" size="small">{{ row.kind }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="suggestion" label="说明 / 建议" min-width="240" show-overflow-tooltip />
            <el-table-column label="就地处理" width="190" align="center">
              <template #default="{ row }">
                <el-button link type="primary" @click="goFixFields">去修正</el-button>
                <el-button v-if="row.source === 'ai'" link type="success" @click="ackAi(row.name)" style="margin-left:6px">复核确认</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
        <!-- 佐证摘要(明细已并入上方待处理清单) -->
        <el-alert v-if="preOpinion" type="info" :closable="false" style="margin-bottom:12px" :title="'AI 预审意见'" :description="preOpinion" show-icon />
        <el-alert v-if="aiMatResult" :type="aiMatResult.overall === '通过' ? 'success' : 'warning'" :closable="false" style="margin-bottom:12px">
          <div><b>AI 材料校验:{{ aiMatResult.overall }}</b> — {{ aiMatResult.overallDesc }}</div>
        </el-alert>
        <el-descriptions v-if="checkResult" :column="2" border style="margin-bottom:12px">
          <el-descriptions-item label="合规校验">
            <el-tag :type="checkResult.checkResult === '通过' ? 'success' : (checkResult.checkResult === '不通过' ? 'danger' : 'warning')">{{ checkResult.checkResult || '—' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="风险等级">
            <el-tag :type="checkResult.riskLevel === '红' ? 'danger' : (checkResult.riskLevel === '黄' ? 'warning' : 'success')">{{ checkResult.riskLevel || '—' }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>
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
              <el-button type="primary" plain @click="go('/dpr/auth/agreement-seal')">去协议签章(双签附录D)</el-button>
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

    <PageActions>
      <el-button v-if="step > 0 && step < 3" @click="step--">上一步</el-button>
      <el-button v-if="step === 0" type="primary" :loading="saving" @click="next0">下一步</el-button>
      <el-button v-if="step === 1" type="primary" :disabled="!canSubmit" @click="step = 2">下一步</el-button>
      <el-tooltip v-if="step === 2 && !canSubmit" content="请先通过合规校验(全部红线合规)" placement="top">
        <span><el-button type="primary" disabled>提交审核</el-button></span>
      </el-tooltip>
      <el-button v-else-if="step === 2" type="primary" :loading="submitting" @click="doSubmit">提交审核</el-button>
    </PageActions>
  </div>
</template>

<script setup>
import { reactive, ref, computed, nextTick, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { saveAuthDraft, submitAuth, runAuthCompliance, pageScenario, uploadAuthMaterialFile, listAuthMaterial, deleteAuthMaterial, authMaterialFileUrl, listAuthMaterialRules, saveAuthAiSnapshot } from '@/api/authorize'
import { aiAuthIntent } from '@/api/confirm'
import { aiAuthMaterialCheck, aiAuthPreReview } from '@/api/authorize'
import AiThinking from '@/components/AiThinking.vue'
import PageActions from '@/components/PageActions.vue'
import { useAiThinking } from '@/composables/useAiThinking'
import { openFilePreview } from '@/composables/useFilePreview'
import { AI_PHASES } from '@/lib/aiPhases'
const aiThink = useAiThinking()
import { pageEquityCard, getRightsFacts } from '@/api/confirm'
import { getAsset } from '@/api/ledger'
import { listOrg } from '@/api/org'

const router = useRouter()
const route = useRoute()
const rightTypes = ['数据加工使用权', '数据产品经营权']
// 授权时效:申报阶段填「时长」(表5 默认两年),保存时映射为预期到期日(validDate);协议签订时按附录D最终落定
const validTerms = ['两年', '三年', '五年']
const TERM_YEARS = { 两年: 2, 三年: 3, 五年: 5 }
function expiryOf(term) {
  const years = TERM_YEARS[term]
  if (!years) return ''
  const d = new Date()
  d.setFullYear(d.getFullYear() + years)
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}
const step = ref(0)
const formRef = ref()
const saving = ref(false)
const checking = ref(false)
const submitting = ref(false)
const applyId = ref('')
const checkResult = ref(null)
const needRecheck = ref(false) // 申请/材料变更后置脏:必须重新校验才能提交
const editing = ref(false)     // 解锁表单以修正字段(草稿可编辑)

const aiAck = ref([]) // 已复核接受的 AI 存疑/不通过项(材料名),解除其阻断
const ruleDone = computed(() => !!checkResult.value)
// 合规校验不符项(维度未通过)
const ruleFail = computed(() => {
  const items = checkResult.value && Array.isArray(checkResult.value.items) ? checkResult.value.items : []
  return items.filter(it => it.pass === false)
})
// AI 材料校验存疑/不通过项(含是否已复核)
const aiIssues = computed(() => {
  const items = aiMatResult.value && Array.isArray(aiMatResult.value.items) ? aiMatResult.value.items : []
  return items.filter(it => it.verdict === '存疑' || it.verdict === '不通过')
    .map(it => ({ ...it, acked: aiAck.value.includes(it.materialName) }))
})
const aiUnresolved = computed(() => aiIssues.value.filter(i => !i.acked))

// 合规"不通过(红)"才硬拦;"警告(黄)"为建议性(跨域/范围未填/敏感建议等),不阻断提交(对齐后端 submit:警告不拦)
const isHardFail = computed(() => ruleDone.value && checkResult.value.checkResult === '不通过')

// 必处理清单:仅"不通过"时的合规不符项 + AI 存疑/不通过(未复核);警告项不进此清单、不阻断
const pendingItems = computed(() => [
  ...(isHardFail.value ? ruleFail.value.map(it => ({ source: 'rule', name: it.item || it.dimension || '合规项', kind: '不符', suggestion: it.message || '请按规则修正' })) : []),
  ...aiUnresolved.value.map(i => ({ source: 'ai', name: i.materialName, kind: i.verdict === '不通过' ? 'AI不通过' : 'AI存疑', suggestion: [i.issues, i.suggestion].filter(Boolean).join(' / ') || 'AI 提示需核实' }))
])

// 单一裁决:合规非"不通过"(通过/警告均可提交) + AI 无未结存疑/不通过 + 无未结变更,才点亮提交
const canSubmit = computed(() => ruleDone.value && checkResult.value.checkResult !== '不通过' && aiUnresolved.value.length === 0 && !needRecheck.value)
// 统一状态条:一句话说清"能不能提交、还差什么"
const checkStatus = computed(() => {
  if (needRecheck.value) return { type: 'warning', text: '已变更,请重新一键校验' }
  if (!ruleDone.value && !aiMatResult.value) return { type: 'info', text: '未校验 — 请点「一键校验」' }
  if (canSubmit.value) {
    return (checkResult.value && checkResult.value.checkResult === '警告')
      ? { type: 'warning', text: '⚠ 有警告项(可提交,建议复核)' }
      : { type: 'success', text: '✅ 全部通过,可提交' }
  }
  const lack = []
  if (isHardFail.value) lack.push('合规不通过(风险' + (checkResult.value.riskLevel || '红') + ')')
  if (pendingItems.value.length) lack.push(`${pendingItems.value.length} 项待处理`)
  return { type: 'danger', text: '还差:' + (lack.join(' · ') || '处理待办') }
})

// 返回填写申请修正字段/材料(一事一议:字段与应交材料都在 step0):解锁表单,回第一步
function goFixFields() { editing.value = true; step.value = 0 }
// 一键校验:合规校验 + AI 材料校验(降认知,正确路径一步到位)
async function runFullCheck() {
  if (!applyId.value) { ElMessage.warning('请先暂存申请'); return }
  aiAck.value = [] // 重新校验,既往复核作废
  try { await runCheck() } catch (e) { /* 拦截器已提示;不阻断 AI 校验 */ }
  await runAiMatCheck() // 自带 try/catch,不抛
}
// 复核确认:申请人对该 AI 存疑/不通过项已核实接受 → 解除阻断
function ackAi(name) { if (!aiAck.value.includes(name)) aiAck.value.push(name); ElMessage.success('已复核确认:' + name) }
const aiText = ref(''); const aiLoading = ref(false); const aiTip = ref('')

function empty() {
  return { assetId: '', assetName: '', schemaName: '', equityCardId: '', granteeOrg: '', rightType: '', scenario: '', scope: '', validTerm: '两年', validDate: '', businessDomain: '', applicantManager: '', contactInfo: '', crossRegion: false, sensitiveType: '', thirdPartySource: '', thirdPartyLicense: '', infoAuthAgreement: '', benefitAllocation: '', securityReq: '', needConfidentiality: false, confidentialityFile: '' }
}
// 确权带出(B-1):按资产取最新已完成确权的第三方来源/隐私商密事实(只读),堵人工低报击穿合规
async function deriveFacts(assetId) {
  try {
    const f = await getRightsFacts(assetId)
    form.thirdPartySource = f?.thirdPartySource || ''
    form.sensitiveType = f?.sensitiveType || '无'
  } catch { form.thirdPartySource = ''; form.sensitiveType = '无' }
}
const form = reactive(empty())
const scenarioOpts = ref([])
const selectedReason = ref('')
// 使用场景按所选授权权益类型过滤(通用恒显;未选权益类型时全显)——与应用场景管理「适用权益类型」联动
const filteredScenarios = computed(() => {
  if (!form.rightType) return scenarioOpts.value
  return scenarioOpts.value.filter(s => !s.rightType || s.rightType === '通用' || s.rightType === form.rightType)
})

// 应交材料清单由后端可配置规则(单一真源·场景一事一议)生成;规则不可用时回退内置默认,保证申请人永远看得到"该传哪些材料"
const materialRules = ref([])
// 内置兜底(对齐联调材料清单 Excel·一事一议):表5必填 + 涉三方→许可凭证 + 涉隐私商密→信息授权协议
const FALLBACK_RULES = [
  { triggerType: 'ALWAYS', materialName: '《表5 数据授权申请单》', required: '必填', detail: '申请主体、所属系统、模式及数据表、申请权益类型(持有权/使用权/经营权)、使用场景及目的、权益时效(默认两年)、是否跨区域跨域等' },
  { triggerType: 'THIRD_PARTY', materialName: '第三方许可凭证或说明', required: '视情况', detail: '申请数据涉及第三方来源方式时必须提供:第三方关于数据授权的许可文件或详细情况说明' },
  { triggerType: 'SENSITIVE', materialName: '信息授权协议', required: '视情况', detail: '申请数据涉及个人隐私或商业秘密时必须提供:相应的信息授权协议附件(如个人隐私授权协议范本)' }
]
// 材料名模糊匹配:应交项名 ⇄ 已上传材料名(双向 includes),供清单状态与门禁校验双轨复用
function matchMaterial(name) {
  return materials.value.find(m => {
    const mn = m.materialName || ''
    return mn === name || mn.includes(name) || name.includes(mn)
  })
}
const requiredChecklist = computed(() => {
  const tp = !!(form.thirdPartySource && form.thirdPartySource.trim())
  const sv = !!(form.sensitiveType && form.sensitiveType.trim() && form.sensitiveType !== '无')
  const hit = (r) => r.triggerType === 'ALWAYS'
    || (r.triggerType === 'THIRD_PARTY' && tp)
    || (r.triggerType === 'SENSITIVE' && sv)
  const src = materialRules.value.length ? materialRules.value : FALLBACK_RULES
  return src.filter(hit).map(r => ({ ...r, uploaded: !!matchMaterial(r.materialName) }))
})
async function loadAuthMaterialRules() {
  try {
    const rules = await listAuthMaterialRules('一事一议')
    if (Array.isArray(rules) && rules.length) materialRules.value = rules
  } catch (e) { /* 规则接口不可用 → requiredChecklist 自动用内置兜底,不阻断、不丢指引 */ }
}
// 按应交清单逐项上传(材料名=该应交项名),申请人不再瞎传
async function doUploadForItem(materialName, file) {
  matUploading.value = true
  try {
    const id = await ensureDraft()
    const fd = new FormData(); fd.append('file', file); fd.append('applyId', id); fd.append('materialName', materialName)
    await uploadAuthMaterialFile(fd)
    if (checkResult.value || aiMatResult.value) needRecheck.value = true
    ElMessage.success(`已上传「${materialName}」`); refreshMaterials()
  } finally { matUploading.value = false }
}

// 被授权方建议:取真实组织树(网/省/地市),输入时按名称/简称过滤;允许自定义外部主体
const orgOptions = ref([])
function queryOrg(queryString, cb) {
  const kw = (queryString || '').trim()
  const list = orgOptions.value
    .filter(o => !kw || (o.bizOrgName || '').includes(kw) || (o.shortName || '').includes(kw))
    .map(o => ({ value: o.bizOrgName }))
  cb(list)
}

onMounted(async () => {
  loadAuthMaterialRules()
  try { orgOptions.value = (await listOrg()) || [] } catch { orgOptions.value = [] }
  const r = await pageScenario({ status: '生效中', size: 100 })
  scenarioOpts.value = r.records || []
  // 先确后授一键衔接:从权益卡片页"发起授权"带来 资产+卡号,直接预填(免去重新搜索选卡)
  const q = route.query
  if (q.assetId) {
    form.assetId = String(q.assetId)
    if (q.assetName) form.assetName = String(q.assetName)
    if (q.cardNo) {
      const cardNo = String(q.cardNo)
      // 先载入卡片选项(+兜底合成项),再设 v-model,确保 el-select 能映射出 label(否则先设值后加 option 不刷新,显示为空)
      await loadCards()
      if (!cardOpts.value.some(c => (c.cardNo || c.cardId) === cardNo)) {
        cardOpts.value = [{ cardNo, assetName: form.assetName, assetId: form.assetId, cardStatus: '生效' }, ...cardOpts.value]
      }
      if (!assetOpts.value.some(c => c.assetId === form.assetId)) {
        assetOpts.value = [{ assetId: form.assetId, assetName: form.assetName, cardNo }, ...assetOpts.value]
      }
      await nextTick()
      form.equityCardId = cardNo
      ElMessage.success('已从权益卡片带入资产与生效卡片,可直接补全授权信息')
    }
    else await onAssetPicked(form.assetId)
  }
  // 基于原单修改重提:从被驳回授权单带入字段(新申请,旧单保留已驳回)
  if (q.reopen) {
    try {
      const o = JSON.parse(sessionStorage.getItem('prm-reopen') || '{}')
      if (o.domain === '授权' && o.raw) {
        const r = o.raw
        Object.assign(form, {
          assetId: r.assetId || '', assetName: r.assetName || '', equityCardId: r.equityCardId || '',
          granteeOrg: r.granteeOrg || '', rightType: r.rightType || '', scenario: r.scenario || '',
          scope: r.scope || '', validDate: r.validDate || '', businessDomain: r.businessDomain || '',
          applicantManager: r.applicantManager || '', contactInfo: r.contactInfo || '',
          // 还原合规相关字段,否则重提丢失:涉敏感/第三方/信息授权协议/保密承诺函/跨域
          crossRegion: !!r.crossRegion, sensitiveType: r.sensitiveType || '',
          thirdPartySource: r.thirdPartySource || '', thirdPartyLicense: r.thirdPartyLicense || '',
          infoAuthAgreement: r.infoAuthAgreement || '',
          needConfidentiality: !!r.needConfidentiality, confidentialityFile: r.confidentialityFile || '',
        })
        ElMessage.warning('已带入被驳回原单内容,请修改后重新提交(将作为新申请)')
      }
    } catch (e) { /* ignore */ }
    sessionStorage.removeItem('prm-reopen')
  }
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
// 选择源=已确权资产(可用权益卡片),扣住先确后授;不再搜台账、不再手填
async function searchAssets(kw) {
  assetSearching.value = true
  try {
    await loadCards()
    const k = (kw || '').trim()
    const seen = new Set()
    assetOpts.value = cardOpts.value
      .filter(c => CARD_OK.includes(c.cardStatus))
      .filter(c => !k || (c.assetName || '').includes(k) || (c.assetId || '').includes(k) || (c.cardNo || '').includes(k))
      .filter(c => { if (seen.has(c.assetId)) return false; seen.add(c.assetId); return true })
      .map(c => ({ assetId: c.assetId, assetName: c.assetName, cardNo: c.cardNo }))
      .slice(0, 20)
  } finally { assetSearching.value = false }
}
async function onAssetPicked(id) {
  const hit = assetOpts.value.find(a => a.assetId === id)
  if (hit) form.assetName = hit.assetName
  if (!id) { form.thirdPartySource = ''; form.sensitiveType = ''; return }
  onAssetBlur()
  await loadCards()
  const card = cardOpts.value.find(c => c.assetId === id && CARD_OK.includes(c.cardStatus))
  if (card) {
    form.equityCardId = card.cardNo || card.cardId
    ElMessage.success('已自动带出生效权益卡片 ' + form.equityCardId)
  } else {
    ElMessage.warning('该资产暂无生效权益卡片,请先完成确权(先确后授)')
  }
  await deriveFacts(id) // 确权带出:第三方来源/隐私商密(只读)
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
async function fillDemo() {
  Object.assign(form, {
    assetId: 'AST-001', assetName: '客户用电信息表', schemaName: 'MKT', equityCardId: 'EC-PRA-0001',
    granteeOrg: '广州供电局', rightType: '数据产品经营权', scenario: '电力金融征信',
    scope: '全字段', validTerm: '三年', businessDomain: '营销域',
    benefitAllocation: '按数据产品调用次数计费,收益由授权方与被授权方按 7:3 分成',
    securityReq: '加密传输 + 最小授权访问控制 + 操作留痕审计;不得转授第三方,到期数据销毁',
    applicantManager: '李主管', contactInfo: '020-66668888', crossRegion: false,
    needConfidentiality: true,
    confidentialityFile: '04-保密承诺函(附录E)-广州供电局.docx',
    infoAuthAgreement: '03-信息授权协议-征信客户授权说明.docx'
  })
  onAssetBlur()
  await deriveFacts('AST-001') // 第三方/隐私 由确权带出(只读)
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
      if (!form.schemaName) form.schemaName = a.schemaName || '' // 表5 模式名称:由数据资产卡片带出
      if (!form.businessDomain) form.businessDomain = a.subsidiaryName || a.systemName || ''
      ElMessage.success('已引用资产信息')
    }
  } catch { assetRef.value = null } finally { assetLoading.value = false }
}

// 申请材料上传/管理(需先有草稿ID)
const materials = ref([]); const matUploading = ref(false)
async function ensureDraft() {
  if (!applyId.value) {
    form.validDate = expiryOf(form.validTerm) // 时长→预期到期日(映射存储)
    applyId.value = await saveAuthDraft({ authMode: '一事一议', ...form })
  }
  return applyId.value
}
async function refreshMaterials() { if (applyId.value) materials.value = await listAuthMaterial(applyId.value) || [] }
async function doUploadMaterial(file) {
  matUploading.value = true
  try {
    const id = await ensureDraft()
    const fd = new FormData(); fd.append('file', file); fd.append('applyId', id); fd.append('materialName', file.name)
    await uploadAuthMaterialFile(fd)
    if (checkResult.value || aiMatResult.value) needRecheck.value = true // 已校验过又改材料 → 置脏
    ElMessage.success('材料已上传'); refreshMaterials()
  } finally { matUploading.value = false }
}
function previewMaterial(row) { openFilePreview(authMaterialFileUrl(row.materialId), row.fileName) }
// 应交清单行在线预览(按材料名匹配已上传材料)
function previewChecklistItem(materialName) {
  const m = materials.value.find(x => {
    const mn = x.materialName || ''
    return mn === materialName || mn.includes(materialName) || materialName.includes(mn)
  })
  if (m && m.materialId) openFilePreview(authMaterialFileUrl(m.materialId), m.fileName || materialName)
  else ElMessage.info('请先上传该材料')
}
async function delMaterial(row) { await deleteAuthMaterial(row.materialId); if (checkResult.value || aiMatResult.value) needRecheck.value = true; ElMessage.success('已删除'); refreshMaterials() }
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
    const r = await aiThink.run(() => aiAuthIntent(aiText.value),
      { phases: AI_PHASES.intent, title: '大模型识别授权意图中' })
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
  form.validDate = expiryOf(form.validTerm) // 时长→预期到期日(映射存储)
  // 凭证/协议双轨:内联说明 与 应交清单同名材料 二者其一即满足。仅上传材料时回填文件引用到字段,保持单一数据源(前端门禁 + 后端提交门禁同源通过)
  const blank = (v) => !v || !String(v).trim()
  if (form.thirdPartySource && form.thirdPartySource.trim()) {
    if (blank(form.thirdPartyLicense)) {
      const m = matchMaterial('第三方许可凭证或说明')
      if (m) form.thirdPartyLicense = '见附件:' + (m.fileName || m.materialName)
    }
    if (blank(form.thirdPartyLicense)) { ElMessage.warning('涉及第三方来源:请在「第三方许可凭证」填写说明,或在下方应交材料清单上传《第三方许可凭证或说明》(二选一)'); return }
  }
  if (form.sensitiveType && form.sensitiveType.trim() && form.sensitiveType !== '无') {
    if (blank(form.infoAuthAgreement)) {
      const m = matchMaterial('信息授权协议')
      if (m) form.infoAuthAgreement = m.fileName || m.materialName
    }
    if (blank(form.infoAuthAgreement)) { ElMessage.warning('涉个人隐私/商密:请在「信息授权协议」填写名称/地址,或在下方上传《信息授权协议》(二选一)'); return }
  }
  if (!applyId.value) {
    saving.value = true
    try { applyId.value = await saveAuthDraft({ authMode: '一事一议', ...form }) }
    finally { saving.value = false }
  } else if (editing.value) {
    // 修正后更新草稿(saveDraft 按 applyId 更新),并置脏要求重新校验
    saving.value = true
    try {
      await saveAuthDraft({ authMode: '一事一议', applyId: applyId.value, ...form })
      editing.value = false
      if (checkResult.value || aiMatResult.value) needRecheck.value = true
      ElMessage.success('申请已更新,请重新执行合规校验')
    } finally { saving.value = false }
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
    const raw = await aiThink.run(() => aiAuthMaterialCheck(applyId.value),
      { phases: AI_PHASES.materialCheck, title: '大模型材料校验中' })
    aiMatResult.value = typeof raw === 'string' ? JSON.parse(raw) : raw
  } catch (e) { ElMessage.warning('AI 材料校验失败:' + (e?.response?.data?.message || '请先上传材料')) }
  finally { aiMatChecking.value = false }
}
async function runPreReview() {
  if (!applyId.value) { ElMessage.warning('请先暂存申请'); return }
  preReviewing.value = true
  try {
    preOpinion.value = await aiThink.run(() => aiAuthPreReview(applyId.value),
      { phases: AI_PHASES.preReview, title: '大模型合规预审中' })
  } catch (e) { ElMessage.warning('AI 预审失败') }
  finally { preReviewing.value = false }
}

async function runCheck() {
  checking.value = true
  try {
    const r = await runAuthCompliance({ applyId: applyId.value })
    checkResult.value = r || { checkResult: '通过', riskLevel: '低', problemDesc: '无' }
    needRecheck.value = false // 校验已刷新,清除脏标
    const cr = checkResult.value.checkResult
    if (cr === '通过') ElMessage.success('合规校验通过,可提交审核')
    else if (cr === '不通过') ElMessage.warning('合规校验不通过,请按提示就地修正后重新校验')
    else ElMessage.info('合规校验有警告项(可提交,建议复核)')
  } finally { checking.value = false }
}

async function doSubmit() {
  submitting.value = true
  try {
    // 提交前固化防篡改 AI 校验快照(材料AI校验 + 合规规则结果),服务端计 SM3 + 上链,供人工审核复核·可审计
    try {
      const snapshot = { checkedAt: new Date().toISOString(), materialCheck: aiMatResult.value || null, ruleReport: checkResult.value || null }
      await saveAuthAiSnapshot(applyId.value, JSON.stringify(snapshot))
    } catch (e) { /* 快照失败不阻断提交 */ }
    await submitAuth(applyId.value); step.value = 3
  } finally { submitting.value = false }
}

function go(path) { router.push(path) }
function reset() {
  step.value = 0; applyId.value = ''; checkResult.value = null; needRecheck.value = false; editing.value = false; aiText.value = ''; aiTip.value = ''
  Object.assign(form, empty())
}
</script>

<style scoped>
.wz-steps { max-width: 900px; margin: 8px auto 20px; }
.wz-body { min-height: 320px; }
.wz-flow { background: #f7f9ff; border-radius: 8px; padding: 10px 16px; color: #4a5160; font-size: 13px; display: inline-block; }
.auth-tip { font-size: 12px; color: #8a8a8a; line-height: 1.6; }
</style>
