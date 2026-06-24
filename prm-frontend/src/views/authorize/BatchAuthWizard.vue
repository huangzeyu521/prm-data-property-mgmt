<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
<template>
  <div class="prm-page">
    <div class="prm-table-note" style="margin:0 0 12px">
      注:一站式批量授权——建清单(表6) → 逐条加授权项 → 提交清单审批,一条流程办完。后续领导小组决策批准 → 双签《运营授权协议(附录D)》→ 自动发证(对齐附录F §4.2)。
    </div>

    <el-steps :active="step" finish-status="success" align-center class="wz-steps">
      <el-step title="建批量清单" description="表6 清单头" />
      <el-step title="逐条加授权项" description="表6 明细行" />
      <el-step title="提交清单审批" description="申报稿→决策批准" />
      <el-step title="完成" description="决策→双签→发证" />
    </el-steps>

    <div class="wz-body">
      <!-- 步骤1:建清单 -->
      <el-card v-show="step === 0" shadow="never">
        <el-form :model="listForm" label-width="140px" style="max-width:580px">
          <el-alert v-if="!batchListId" type="info" :closable="false" style="margin-bottom:12px;max-width:680px">
            <template #title>
              第一次用?可
              <el-button link type="primary" style="vertical-align:baseline" :loading="demoFilling" @click="fillDemo">一键示例:建清单并自动加入3条明细(测试/演示)</el-button>
              生效卡片自动匹配,材料见 test/批量授权申请
            </template>
          </el-alert>
          <el-form-item label="授权年度" required><el-input v-model="listForm.listYear" placeholder="如 2026" /></el-form-item>
          <el-form-item label="申请主体(被授权方)" required>
            <el-input v-model="listForm.granteeOrg" placeholder="表5/表6 申请单位:本批数据统一授给谁(批量共享,逐条沿用)" clearable />
          </el-form-item>
          <el-divider content-position="left" style="margin:8px 0">
            <span style="font-size:12px;color:#909399">批量默认(逐条加项时自动带入,可逐项调整)</span>
          </el-divider>
          <el-form-item label="默认权益类型(单选)">
            <el-select v-model="listForm.rightType" clearable style="width:100%" placeholder="整批默认权益类型(单选;不同表可逐项调整)">
              <el-option v-for="t in rightTypes" :key="t" :label="t" :value="t" />
            </el-select>
            <div style="font-size:12px;color:#909399;line-height:1.5;margin-top:2px">
              授权仅授「使用权 / 经营权」两类;数据持有权经确权认定取得,不在授权范围(三权分置)。
            </div>
          </el-form-item>
          <el-form-item label="默认使用场景"><el-input v-model="listForm.scenario" placeholder="整批默认使用场景及目的" clearable /></el-form-item>
          <el-form-item label="默认业务域"><el-input v-model="listForm.businessDomain" placeholder="营销/生产/调度/财务...(整批默认,表5/表6 所属业务域)" clearable /></el-form-item>
          <el-form-item label="默认授权时效">
            <el-select v-model="listForm.validTerm" style="width:100%" placeholder="默认两年(时长)">
              <el-option v-for="t in validTerms" :key="t" :label="t" :value="t" />
            </el-select>
          </el-form-item>
          <el-form-item label="清单备注"><el-input v-model="listForm.remark" type="textarea" maxlength="500" show-word-limit :rows="2" placeholder="如:综能板块年度批量授权" /></el-form-item>
        </el-form>
        <el-alert v-if="batchListId" type="success" :closable="false" show-icon :title="`清单已创建(${listNo})，开始逐条添加授权项`" style="max-width:520px;margin-top:8px" />
      </el-card>

      <!-- 步骤2:逐条加授权项 -->
      <el-card v-show="step === 1" shadow="never">
        <!-- DC4 主路径:批量任务首选"目录多选"(勾多张表→套清单头默认+确权带出→一次加入) -->
        <div class="batch-primary">
          <el-button type="primary" size="large" @click="openPicker">① 从确权目录批量选取数据资产</el-button>
          <span class="batch-primary-hint">
            推荐:勾选多张已确权数据表 → 自动套用清单头默认(被授权方/权益类型/场景/时效/业务域)+ 确权带出(第三方/隐私)→ 一次加入。
          </span>
        </div>
        <!-- ② AI 批量填单(qwen3-max,stub 回退):一段话解析出共享字段+多条明细 -->
        <div class="ai-batch">
          <el-input v-model="aiBatchText" type="textarea" maxlength="500" show-word-limit :rows="2"
            placeholder="AI 批量填单:如 向南网综合能源股份有限公司授权台区负荷数据、充电桩运营数据、线损分析数据用于综合能源服务,数据加工使用权,批量授权" />
          <el-button type="primary" :loading="aiParsing" style="margin-top:6px" @click="runAiBatch">大瓦特 AI 解析并预填明细</el-button>
          <el-button v-if="aiItems.length" type="primary" plain :loading="aiAdding" :disabled="aiAddableCount === 0" style="margin-top:6px;margin-left:8px" @click="addAiItems">
            一键加入可授 {{ aiAddableCount }} 项{{ aiItems.length > aiAddableCount ? `(跳过未确权 ${aiItems.length - aiAddableCount} 项)` : '' }}
          </el-button>
          <AiThinking v-bind="aiThink.state" />
          <el-table v-if="aiItems.length" :data="aiItems" border size="small" style="margin-top:8px;max-width:760px">
            <el-table-column prop="assetName" label="解析出的数据资产" min-width="160" />
            <el-table-column label="权益卡片ID" min-width="150">
              <template #default="{ row }"><el-input v-model="row.equityCardId" size="small" placeholder="先确后授:填卡片ID" /></template>
            </el-table-column>
            <el-table-column label="状态" width="150" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.equityCardId" type="success" size="small" effect="light">可加入</el-tag>
                <template v-else>
                  <el-tag type="warning" size="small" effect="light">未确权</el-tag>
                  <el-button link type="primary" size="small" style="margin-left:6px" @click="goConfirm(row)">去确权</el-button>
                </template>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="aiItems.length && aiItems.length > aiAddableCount" style="margin-top:4px;color:#e6a23c;font-size:12px">
            未确权项不能授权(先确后授):点「去确权」完成确权生成生效卡片后,回来重新解析即可加入;无需为其手填卡片ID。
          </div>
        </div>
        <div v-if="requiredChecklist.length" style="margin:8px 0 4px">
          <div style="font-weight:600;margin-bottom:8px">应交材料清单（按全单授权项自动判定）— 请按下表逐项上传</div>
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
                <el-upload :show-file-list="false" :http-request="(o)=>doUploadBatchItem(row.materialName, o.file)" accept=".pdf,.doc,.docx,.jpg,.jpeg,.png" style="display:inline-block">
                  <el-button link type="primary" :loading="matUploading">上传</el-button>
                </el-upload>
                <el-button v-if="row.uploaded" link type="success" style="margin-left:6px" @click="previewBatchItem(row.materialName)">预览</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div style="margin-top:6px;color:#909399;font-size:12px">必填项须上传;"视情况"项按全单是否涉第三方/隐私商密上传。</div>
        </div>
        <el-divider content-position="left">
          <span style="font-size:13px;color:#909399">③ 逐条补充 / 微调(可选;单条精确控制)</span>
        </el-divider>
        <el-row :gutter="16">
          <el-col :span="11">
            <el-form ref="itemRef" :model="item" :rules="itemRules" label-width="130px">
              <el-form-item label="关联数据资产卡片" prop="assetId">
                <el-select v-model="item.assetId" filterable remote clearable
                  :remote-method="searchAssets" :loading="assetSearching" style="width:100%"
                  placeholder="搜索已确权资产(名称/卡片号)选取,先确后授" @change="onItemAssetPicked">
                  <el-option v-for="a in assetOpts" :key="a.assetId" :value="a.assetId" :label="a.assetName || a.assetId">
                    <span>{{ a.assetName || a.assetId }}</span><span style="float:right;color:#8a8a8a;font-size:12px">{{ a.cardNo }}</span>
                  </el-option>
                </el-select>
              </el-form-item>
              <el-divider content-position="left" style="margin:6px 0">
                <span style="font-size:12px;color:#909399">数据信息(第三方/隐私 由确权带出,跨域自判)</span>
              </el-divider>
              <el-form-item label="数据表(资产名)" prop="assetName"><el-input v-model="item.assetName" readonly placeholder="选取卡片后自动带出" /></el-form-item>
              <el-form-item label="所属系统"><el-input v-model="item.systemName" readonly placeholder="选取资产后自动带出" /></el-form-item>
              <el-form-item label="模式"><el-input v-model="item.schemaName" readonly placeholder="选取资产后自动带出" /></el-form-item>
              <el-form-item label="生效权益卡片" prop="equityCardId"><el-input v-model="item.equityCardId" readonly placeholder="选取资产后自动匹配生效卡片(先确后授)" /></el-form-item>
              <el-form-item label="第三方来源"><el-input v-model="item.thirdPartySource" readonly placeholder="选取资产后由确权记录自动带出(不涉及则空)" /></el-form-item>
              <el-form-item label="隐私/商密"><el-input v-model="item.sensitiveType" readonly placeholder="选取资产后由确权记录自动带出" /></el-form-item>
              <el-form-item label="是否跨域"><el-switch v-model="item.crossRegion" /></el-form-item>
              <el-divider content-position="left" style="margin:6px 0">
                <span style="font-size:12px;color:#909399">授权内容(默认取清单头,可调)</span>
              </el-divider>
              <el-form-item label="权益类型(单选)" prop="rightType">
                <el-select v-model="item.rightType" style="width:100%" placeholder="单选(表5/表6:权益名称选一个)">
                  <el-option v-for="t in rightTypes" :key="t" :label="t" :value="t" />
                </el-select>
              </el-form-item>
              <el-form-item label="使用场景及目的"><el-input v-model="item.scenario" /></el-form-item>
              <el-form-item label="授权时效">
                <el-select v-model="item.validTerm" style="width:100%" placeholder="默认两年(时长)">
                  <el-option v-for="t in validTerms" :key="t" :label="t" :value="t" />
                </el-select>
                <div style="font-size:12px;color:#909399;line-height:1.5;margin-top:2px">
                  映射到期日(预期):{{ expiryOf(item.validTerm) ? expiryOf(item.validTerm).slice(0,10) : '—' }};协议签订时按附录D最终落定
                </div>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="adding" @click="addItem">加入这一条</el-button>
                <span style="margin-left:10px;color:#c0c4cc;font-size:12px">批量请用上方「① 目录批量选取」</span>
              </el-form-item>
            </el-form>
          </el-col>
          <el-col :span="13">
            <div class="prm-table-note" style="margin-bottom:6px">已加入明细({{ items.length }} 项)</div>
            <el-table :data="items" border size="small" max-height="420">
              <el-table-column type="index" label="#" width="44" align="center" />
              <el-table-column prop="assetName" label="数据表" min-width="120" show-overflow-tooltip />
              <el-table-column prop="rightType" label="权益" width="130" />
              <el-table-column prop="scenario" label="使用场景" min-width="120" show-overflow-tooltip>
                <template #default="{ row }">{{ row.scenario || '—' }}</template>
              </el-table-column>
              <el-table-column label="操作" width="70" align="center">
                <template #default="{ row, $index }">
                  <el-button link type="danger" size="small" @click="removeItem(row, $index)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-if="items.length===0" :image-size="60" description="尚未添加授权项" />
          </el-col>
        </el-row>
      </el-card>

      <!-- 步骤3:提交清单审批 -->
      <el-card v-show="step === 2" shadow="never">
        <el-result icon="info" :title="`提交《批量授权清单》申报稿（${items.length} 项）`" sub-title="合规校验 → 清单审核审批 → 领导小组决策批准" />
        <!-- 单一裁决状态条:一句话说清"能不能提交、还差什么" -->
        <div style="text-align:center;margin:4px 0 10px">
          校验状态:<el-tag :type="checkStatus.type" effect="dark">{{ checkStatus.text }}</el-tag>
        </div>
        <!-- 主操作:一键合规校验(批量为单检:逐项合规试跑) -->
        <div style="text-align:center">
          <el-button :type="complianceResult && !complianceResult.allPass ? 'danger' : 'primary'" :loading="complianceChecking" @click="runComplianceCheck">
            {{ complianceResult ? '重新一键校验' : '一键合规校验(全部明细)' }}
          </el-button>
        </div>
        <!-- 次要:AI 辅助(可选,不影响提交门禁) -->
        <div style="text-align:center;margin:4px 0 0;color:#909399;font-size:12px">
          AI 辅助(可选,不影响提交):
          <el-button link type="primary" :loading="listReviewing" @click="runListPreReview">AI 清单预审(qwen3-max)</el-button>
        </div>
        <AiThinking v-bind="aiThink.state" />
        <!-- 统一待处理清单(单一闭环):被拦明细逐项「去修正」(回 step1)→ 重新一键校验,直至通过 -->
        <el-card v-if="complianceResult && !complianceResult.allPass && complianceResult.blocked.length" shadow="never" style="margin-top:12px;border:1px solid #fde2e2;background:#fff8f8">
          <div style="font-weight:600;color:#f56c6c;margin-bottom:8px">需处理以下 {{ complianceResult.blocked.length }} 项后方可提交(处理完点上方「重新一键校验」)</div>
          <el-table :data="complianceResult.blocked" border size="small">
            <el-table-column label="来源" width="76" align="center"><template #default><el-tag type="danger" size="small">合规</el-tag></template></el-table-column>
            <el-table-column prop="assetName" label="数据资产" min-width="180" show-overflow-tooltip />
            <el-table-column prop="reason" label="被拦原因" min-width="240" show-overflow-tooltip />
            <el-table-column label="就地处理" width="120" align="center"><template #default><el-button link type="primary" @click="step = 1">去修正</el-button></template></el-table-column>
          </el-table>
        </el-card>
        <el-alert v-if="listOpinion" type="info" :closable="false" style="margin-top:12px" title="AI 清单预审意见" :description="listOpinion" show-icon />
      </el-card>

      <!-- 步骤4:完成 -->
      <el-card v-show="step === 3" shadow="never">
        <el-result icon="success" title="批量授权清单已提交申报稿" :sub-title="`清单 ${listNo}（${items.length} 项）已进入审批`">
          <template #extra>
            <div class="wz-flow">后续闭环:领导小组决策批准 → 授权方/被授权方双签《运营授权协议(附录D)》→ 自动签发授权证书 → 执行授权</div>
            <div style="margin-top:14px">
              <el-button type="primary" @click="go('/dpr/auth/batch-list')">去清单管理</el-button>
              <el-button @click="go('/dpr/auth/cert')">授权证书</el-button>
              <el-button @click="reset">再建一份清单</el-button>
            </div>
          </template>
        </el-result>
      </el-card>
    </div>

    <PageActions>
      <el-button v-if="step > 0 && step < 3" @click="step--">上一步</el-button>
      <el-button v-if="step === 0" type="primary" :loading="creating" @click="next0">下一步</el-button>
      <el-button v-if="step === 1" type="primary" :disabled="items.length===0" @click="step = 2">下一步</el-button>
      <el-tooltip v-if="step === 2 && !canSubmit" content="请先通过合规校验(全部明细合规)" placement="top">
        <span><el-button type="primary" disabled>提交审批</el-button></span>
      </el-tooltip>
      <el-button v-else-if="step === 2" type="primary" :loading="submitting" @click="doSubmit">提交审批</el-button>
    </PageActions>

    <!-- 从目录多选资产 -->
    <el-dialog v-model="pickerDlg" title="从确权目录多选数据资产" width="560px" align-center>
      <el-alert type="info" :closable="false" style="margin-bottom:10px">
        勾选多个数据集，统一加入清单。被授权方取清单头(批量共享)，权益类型/场景/时效取清单头默认(可逐条调整)。
      </el-alert>
      <el-tree ref="pickTreeRef" :data="tree" :props="{ label: 'label', children: 'children' }" node-key="id"
        show-checkbox default-expand-all style="max-height:380px;overflow:auto">
        <template #default="{ data }">
          <span>{{ data.label }}</span>
          <span v-if="data.assetId" style="color:#909399;font-size:12px;margin-left:8px">{{ data.assetId }} · {{ data.confirmStatus }}</span>
        </template>
      </el-tree>
      <template #footer>
        <el-button type="primary" :loading="picking" @click="confirmPick">加入选中资产</el-button>
        <el-button @click="pickerDlg=false">取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createBatchList, saveAuthDraft, deleteAuthApply, submitBatchList, aiBatchIntent, aiBatchPreReview, listAuthMaterialRules, checkBatchCompliance, uploadAuthMaterialFile, listAuthMaterial, authMaterialFileUrl } from '@/api/authorize'
import { openFilePreview } from '@/composables/useFilePreview'
import AiThinking from '@/components/AiThinking.vue'
import PageActions from '@/components/PageActions.vue'
import { useAiThinking } from '@/composables/useAiThinking'
import { AI_PHASES } from '@/lib/aiPhases'
const aiThink = useAiThinking()
import { pageEquityCard, getRightsFacts } from '@/api/confirm'
import { getPropertyTree, getAsset } from '@/api/ledger'

const router = useRouter()
const rightTypes = ['数据加工使用权', '数据产品经营权']
// 授权时效:申报阶段填「时长」(表5/表6 默认两年),保存时映射为预期到期日(validDate);
// 协议签订时按附录D「自签订日起+时长,一般3年最长5年」最终落定。
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
const creating = ref(false); const adding = ref(false); const submitting = ref(false)
const batchListId = ref(''); const listNo = ref('')
const items = ref([])
const itemRef = ref()
// 清单头(批量级):被授权方为批量共享;权益类型/场景/时效为整批默认,逐条加项时带入可调
const listForm = reactive({ listYear: '', granteeOrg: '', rightType: '', scenario: '', validTerm: '两年', businessDomain: '', remark: '' })
const item = reactive(emptyItem())
const itemRules = {
  assetId: [{ required: true, message: '请输入资产ID', trigger: 'blur' }],
  assetName: [{ required: true, message: '请输入资产名称', trigger: 'blur' }],
  equityCardId: [{ required: true, message: '先确后授:必须引用权益卡片', trigger: 'blur' }],
  rightType: [{ required: true, message: '请选择权益类型', trigger: 'change' }]
}
function emptyItem() {
  return { assetId: '', assetName: '', systemName: '', schemaName: '', equityCardId: '', granteeOrg: '', rightType: '', scenario: '', validTerm: '两年', validDate: '', businessDomain: '', thirdPartySource: '', sensitiveType: '', crossRegion: false }
}
// 重置明细表单:清空数据信息,授权内容(被授权方/权益类型/场景/时效)默认取清单头
function resetItem() {
  Object.assign(item, emptyItem(), {
    granteeOrg: listForm.granteeOrg,
    rightType: listForm.rightType,
    scenario: listForm.scenario,
    validTerm: listForm.validTerm || '两年',
    businessDomain: listForm.businessDomain
  })
}
// 去重键:被授权方批量恒定,故行唯一性=数据表+权益类型+场景(对齐表4/表6:同表可多权益/多场景,三者全同才是真重复)
function dupKey(x) {
  return `${x.assetId || x.assetName}|${x.rightType || ''}|${(x.scenario || '').trim()}`
}

// 应交材料清单由后端可配置规则(单一真源·场景批量)生成;规则不可用时回退内置默认,保证申请人永远看得到"该传哪些材料"
const materialRules = ref([])
const matUploading = ref(false)
const batchMaterials = ref([]) // 清单级材料(挂在 batchListId 上)
// 内置兜底(对齐联调材料清单 Excel·批量):表5必填 + 涉三方→许可凭证 + 涉隐私商密→信息授权协议
const FALLBACK_RULES = [
  { triggerType: 'ALWAYS', materialName: '《表5 数据授权申请单》', required: '必填', detail: '逐条填写申请数据信息:申请主体、所属系统、模式及数据表、申请权益类型、使用场景及目的、权益时效(默认两年)、是否跨区域跨域等' },
  { triggerType: 'THIRD_PARTY', materialName: '第三方许可凭证或说明', required: '视情况', detail: '清单中任一授权项涉及第三方来源方式时必须提供:第三方关于数据授权的许可文件或情况说明' },
  { triggerType: 'SENSITIVE', materialName: '信息授权协议', required: '视情况', detail: '清单中任一授权项涉及个人隐私或商业秘密时必须提供:相应的信息授权协议附件(如个人隐私授权协议范本)' }
]
const requiredChecklist = computed(() => {
  const all = [...items.value, item]
  const tp = all.some(x => x.thirdPartySource && String(x.thirdPartySource).trim())
  const sv = all.some(x => x.sensitiveType && String(x.sensitiveType).trim() && x.sensitiveType !== '无')
  const hit = (r) => r.triggerType === 'ALWAYS'
    || (r.triggerType === 'THIRD_PARTY' && tp)
    || (r.triggerType === 'SENSITIVE' && sv)
  const src = materialRules.value.length ? materialRules.value : FALLBACK_RULES
  return src.filter(hit).map(r => ({
    ...r,
    uploaded: batchMaterials.value.some(m => {
      const mn = m.materialName || ''
      return mn === r.materialName || mn.includes(r.materialName) || r.materialName.includes(mn)
    })
  }))
})
async function refreshBatchMaterials() {
  if (batchListId.value) batchMaterials.value = await listAuthMaterial(batchListId.value) || []
}
// 在线预览已上传的清单级材料(按应交项名匹配)
function previewBatchItem(materialName) {
  const m = batchMaterials.value.find(x => {
    const mn = x.materialName || ''
    return mn === materialName || mn.includes(materialName) || materialName.includes(mn)
  })
  if (m && m.materialId) openFilePreview(authMaterialFileUrl(m.materialId), m.fileName || materialName)
}
// 按应交清单逐项上传(清单级材料挂 batchListId,材料名=应交项名)
async function doUploadBatchItem(materialName, file) {
  if (!batchListId.value) { ElMessage.warning('请先建批量清单'); return }
  matUploading.value = true
  try {
    const fd = new FormData(); fd.append('file', file); fd.append('applyId', batchListId.value); fd.append('materialName', materialName)
    await uploadAuthMaterialFile(fd)
    ElMessage.success(`已上传「${materialName}」`); refreshBatchMaterials()
  } finally { matUploading.value = false }
}
onMounted(async () => {
  try {
    const rules = await listAuthMaterialRules('批量')
    if (Array.isArray(rules) && rules.length) materialRules.value = rules
  } catch (e) { /* 规则接口不可用 → requiredChecklist 自动用内置兜底,不丢指引 */ }
})

// 合规校验闭环:清单明细变化即让上次校验失效(必须重新校验才能提交)
const complianceResult = ref(null)
const complianceChecking = ref(false)
watch(items, () => { complianceResult.value = null }, { deep: true })

// 提交门禁:合规校验通过才放行(消灭"点了必然被拒"的死路)
const canSubmit = computed(() => !!complianceResult.value && complianceResult.value.allPass)
const checkStatus = computed(() => {
  if (!complianceResult.value) return { type: 'info', text: '未校验' }
  if (complianceResult.value.allPass) return { type: 'success', text: '✅ 全部合规,可提交' }
  return { type: 'danger', text: `未通过(${complianceResult.value.blockedCount} 项被拦)` }
})

// 只读合规校验(与提交门禁同源),失败项就地暴露,引导修正后重新校验直至通过
async function runComplianceCheck() {
  if (!batchListId.value) { ElMessage.warning('请先建清单并加入授权项'); return }
  if (!items.value.length) { ElMessage.warning('清单为空,请先加入授权项'); return }
  complianceChecking.value = true
  try {
    complianceResult.value = await checkBatchCompliance(batchListId.value)
    if (complianceResult.value.allPass) ElMessage.success('合规校验通过,可提交清单审批')
    else ElMessage.warning(`合规校验未通过:${complianceResult.value.blockedCount} 项被拦,请逐条修正后重新校验`)
  } finally { complianceChecking.value = false }
}

async function next0() {
  if (!listForm.listYear) { ElMessage.warning('请填写授权年度'); return }
  if (!listForm.granteeOrg) { ElMessage.warning('请填写被授权方(本批数据统一授给谁)'); return }
  if (!batchListId.value) {
    creating.value = true
    try {
      batchListId.value = await createBatchList({ listYear: listForm.listYear, remark: listForm.remark })
      listNo.value = listForm.listYear + ' 批量授权清单'
    } finally { creating.value = false }
  }
  resetItem() // 进入逐条加项:用清单头默认预填明细
  step.value = 1
}

// 从目录多选资产批量加入清单
const pickerDlg = ref(false); const picking = ref(false); const tree = ref([]); const pickTreeRef = ref()
async function openPicker() {
  if (!listForm.granteeOrg) { ElMessage.warning('请先在步骤1清单头填写"被授权方"(批量共享)'); return }
  if (!item.rightType && !listForm.rightType) { ElMessage.warning('请先选择"权益类型"(可在清单头设默认或左侧明细选)'); return }
  if (!tree.value.length) tree.value = await getPropertyTree() || []
  pickerDlg.value = true
}
async function confirmPick() {
  const leaves = pickTreeRef.value.getCheckedNodes().filter(n => n.type === 'DATASET' && n.assetId)
  if (!leaves.length) { ElMessage.warning('请至少勾选一个数据集'); return }
  picking.value = true
  try {
    await loadCards()
    let ok = 0
    const skipped = []
    const dups = []
    const seen = new Set(items.value.map(dupKey))
    for (const lf of leaves) {
      // 先确后授:自动匹配生效权益卡片,无卡项跳过并提示(后端会拦截空卡)
      const card = findUsableCard(lf.assetId)
      if (!card) { skipped.push(lf.label || lf.assetId); continue }
      const f = await deriveFacts(lf.assetId)
      const it = { ...emptyItem(), assetId: lf.assetId, assetName: lf.label, equityCardId: card,
        granteeOrg: listForm.granteeOrg, rightType: item.rightType || listForm.rightType,
        scenario: item.scenario || listForm.scenario, validDate: expiryOf(item.validTerm || listForm.validTerm),
        businessDomain: listForm.businessDomain, thirdPartySource: f.thirdPartySource, sensitiveType: f.sensitiveType }
      const k = dupKey(it)
      if (seen.has(k)) { dups.push(lf.label || lf.assetId); continue } // 同表+权益+场景已在清单,去重
      seen.add(k)
      const id = await saveAuthDraft({ authMode: '批量', batchListId: batchListId.value, ...it })
      items.value.push({ ...it, applyId: id })
      ok++
    }
    ElMessage[(skipped.length || dups.length) ? 'warning' : 'success'](
      `已加入 ${ok} 项(生效卡自动匹配)`
      + (skipped.length ? `;跳过未确权 ${skipped.length} 项:${skipped.slice(0, 3).join('、')}` : '')
      + (dups.length ? `;跳过重复 ${dups.length} 项:${dups.slice(0, 3).join('、')}` : ''))
    pickerDlg.value = false
  } finally { picking.value = false }
}

// AI 批量填单(qwen3-max,stub 回退)
const aiBatchText = ref(''); const aiParsing = ref(false); const aiItems = ref([]); const aiAdding = ref(false)
// 可加入条数(有生效卡=已确权);未确权项不计入,按钮与提示按此显示
const aiAddableCount = computed(() => aiItems.value.filter(x => x.equityCardId).length)
const aiShared = ref({ granteeOrg: '', rightType: '', scenario: '' })
async function runAiBatch() {
  if (!aiBatchText.value) { ElMessage.warning('请先描述批量授权诉求'); return }
  aiParsing.value = true
  try {
    const raw = await aiThink.run(() => aiBatchIntent(aiBatchText.value),
      { phases: AI_PHASES.batchIntent, title: '大模型解析批量诉求中' })
    const r = typeof raw === 'string' ? JSON.parse(raw) : raw
    aiShared.value = { granteeOrg: r.granteeOrg || '', rightType: r.rightType || '', scenario: r.scenario || '' }
    // 共享字段回填清单头(批量级),并同步当前明细默认
    if (r.granteeOrg) listForm.granteeOrg = r.granteeOrg
    if (r.rightType) listForm.rightType = r.rightType
    if (r.scenario) listForm.scenario = r.scenario
    Object.assign(item, { granteeOrg: r.granteeOrg || item.granteeOrg, rightType: r.rightType || item.rightType, scenario: r.scenario || item.scenario })
    const parsed = []
    await loadCards()
    for (const x of (r.items || [])) {
      const asset = await resolveAssetByName(x.assetName)
      const card = asset ? findUsableCard(asset.assetId) : ''
      parsed.push({ assetName: x.assetName, assetId: asset ? asset.assetId : x.assetName, equityCardId: card })
    }
    aiItems.value = parsed
    const matched = parsed.filter(x => x.equityCardId).length
    ElMessage.success(`AI 解析 ${parsed.length} 条,已自动匹配生效卡片 ${matched} 条` + (matched < parsed.length ? ',其余请补卡片ID' : ',可直接一键加入'))
  } catch (e) { ElMessage.warning('AI 解析失败:' + (e?.message || '')) }
  finally { aiParsing.value = false }
}
// 未确权项去确权(一站式):带资产名/ID 跳转确权向导,完成确权生成生效卡片后回来重新解析即可加入
function goConfirm(row) {
  router.push({ path: '/dpr/confirm/wizard', query: { assetId: row.assetId || '', assetName: row.assetName || '' } })
}
// 部分加入(对齐"目录多选"):有生效卡的加入,未确权(无卡)的跳过并保留可见,不再"全有或全无"卡死
async function addAiItems() {
  const addable = aiItems.value.filter(x => x.equityCardId)
  const skipped = aiItems.value.filter(x => !x.equityCardId)
  if (!addable.length) {
    ElMessage.warning('无可加入项:所选数据资产均未确权(无生效权益卡片)。请先完成确权(先确后授)')
    return
  }
  aiAdding.value = true
  try {
    let ok = 0
    const dups = []
    const seen = new Set(items.value.map(dupKey))
    for (const x of addable) {
      const f = await deriveFacts(x.assetId || x.assetName)
      const it = { ...emptyItem(), assetId: x.assetId || x.assetName, assetName: x.assetName, equityCardId: x.equityCardId,
        granteeOrg: aiShared.value.granteeOrg || listForm.granteeOrg, rightType: aiShared.value.rightType || listForm.rightType,
        scenario: aiShared.value.scenario || listForm.scenario, validDate: expiryOf(listForm.validTerm),
        businessDomain: listForm.businessDomain, thirdPartySource: f.thirdPartySource, sensitiveType: f.sensitiveType }
      const k = dupKey(it)
      if (seen.has(k)) { dups.push(x.assetName); continue } // 同表+权益+场景已在清单,去重
      seen.add(k)
      const id = await saveAuthDraft({ authMode: '批量', batchListId: batchListId.value, ...it })
      items.value.push({ ...it, applyId: id })
      ok++
    }
    ElMessage[(skipped.length || dups.length) ? 'warning' : 'success'](
      `已加入 ${ok} 项`
      + (skipped.length ? `;跳过未确权 ${skipped.length} 项:${skipped.map(s => s.assetName).slice(0, 3).join('、')} — 请先完成确权再授权` : '')
      + (dups.length ? `;跳过重复 ${dups.length} 项:${dups.slice(0, 3).join('、')}` : ''))
    // 已加入的清掉,仅保留未确权项可见,引导用户去确权
    aiItems.value = skipped
  } finally { aiAdding.value = false }
}

// AI 清单预审(qwen3-max,stub 回退)
const listReviewing = ref(false); const listOpinion = ref('')
async function runListPreReview() {
  listReviewing.value = true
  try {
    listOpinion.value = await aiThink.run(() => aiBatchPreReview(batchListId.value),
      { phases: AI_PHASES.batchPreReview, title: '大模型清单预审中' })
  } catch (e) { ElMessage.warning('AI 预审失败') }
  finally { listReviewing.value = false }
}

// 自动配卡引擎:台账搜索 + 按资产匹配生效权益卡片(先确后授),贯穿手填/目录多选/AI 三条录入通道
const CARD_OK = ['正常', '生效']
const assetOpts = ref([])
const assetSearching = ref(false)
const cardOpts = ref([])
let cardsLoaded = false
async function loadCards() {
  if (cardsLoaded) return
  const r = await pageEquityCard({ current: 1, size: 200 })
  cardOpts.value = r.records || []
  cardsLoaded = true
}
function findUsableCard(assetId) {
  const c = cardOpts.value.find(x => x.assetId === assetId && CARD_OK.includes(x.cardStatus))
  return c ? (c.cardNo || c.cardId) : ''
}
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
// 确权信息带出:按资产取最新已完成确权的第三方来源/隐私商密事实(只读,堵人工低报)
async function deriveFacts(assetId) {
  try {
    const f = await getRightsFacts(assetId)
    return { thirdPartySource: f?.thirdPartySource || '', sensitiveType: f?.sensitiveType || '无' }
  } catch { return { thirdPartySource: '', sensitiveType: '无' } }
}
async function onItemAssetPicked(id) {
  const hit = assetOpts.value.find(a => a.assetId === id)
  if (hit) item.assetName = hit.assetName
  if (!id) { item.systemName = ''; item.schemaName = ''; item.thirdPartySource = ''; item.sensitiveType = ''; return }
  await loadCards()
  const card = findUsableCard(id)
  if (card) { item.equityCardId = card; ElMessage.success('已自动带出生效权益卡片 ' + card) }
  else ElMessage.warning('该资产暂无生效权益卡片,请先完成确权(先确后授)')
  // 确权带出:第三方来源/隐私商密(只读)
  const f = await deriveFacts(id)
  item.thirdPartySource = f.thirdPartySource
  item.sensitiveType = f.sensitiveType
  // 资产带出:所属系统/模式(只读,表5/表6 数据信息)
  try {
    const a = await getAsset(id)
    item.systemName = a?.systemName || ''
    item.schemaName = a?.schemaName || ''
  } catch { item.systemName = ''; item.schemaName = '' }
}
async function resolveAssetByName(name) {
  await loadCards()
  const usable = cardOpts.value.filter(c => CARD_OK.includes(c.cardStatus))
  const c = usable.find(x => x.assetName === name) || usable.find(x => (x.assetName || '').includes(name))
  return c ? { assetId: c.assetId, assetName: c.assetName } : null
}

// 一键示例:建清单+3条明细全自动(生效卡自动匹配,对齐 test/批量授权申请 手册)
const demoFilling = ref(false)
async function fillDemo() {
  demoFilling.value = true
  try {
    listForm.listYear = '2026'
    listForm.granteeOrg = '南网综合能源股份有限公司'
    listForm.rightType = '数据加工使用权'
    listForm.scenario = '综合能源服务'
    listForm.remark = '综能板块年度批量授权(示例)'
    await next0()
    await loadCards()
    const demo = [['AST-001', '客户用电信息表'], ['AST-002', '台区负荷数据'], ['AST-006', '充电桩运营数据']]
    let ok = 0
    const seen = new Set(items.value.map(dupKey))
    for (const [aid, name] of demo) {
      const card = findUsableCard(aid)
      if (!card) continue
      const f = await deriveFacts(aid)
      const it = { ...emptyItem(), assetId: aid, assetName: name, equityCardId: card,
        granteeOrg: listForm.granteeOrg, rightType: listForm.rightType, scenario: listForm.scenario, validDate: expiryOf(listForm.validTerm),
        businessDomain: listForm.businessDomain, thirdPartySource: f.thirdPartySource, sensitiveType: f.sensitiveType }
      const k = dupKey(it)
      if (seen.has(k)) continue // 去重:同表+权益+场景已在清单
      seen.add(k)
      const id = await saveAuthDraft({ authMode: '批量', batchListId: batchListId.value, ...it })
      items.value.push({ ...it, applyId: id })
      ok++
    }
    ElMessage.success(`示例完成:清单已建,自动加入 ${ok} 条明细(生效卡已配),可直接"提交清单审批"`)
  } finally { demoFilling.value = false }
}

async function addItem() {
  await itemRef.value.validate()
  if (items.value.some(y => dupKey(y) === dupKey(item))) {
    ElMessage.warning(`「${item.assetName}」(同权益+场景)已在清单中,未重复加入`)
    return
  }
  adding.value = true
  try {
    item.validDate = expiryOf(item.validTerm) // 时长→预期到期日(映射存储)
    const id = await saveAuthDraft({ authMode: '批量', batchListId: batchListId.value, ...item })
    items.value.push({ ...item, applyId: id })
    resetItem()
    ElMessage.success('已加入清单')
  } finally { adding.value = false }
}

// 删行:从清单移除一条授权项(草稿态,删后端 + 移本地);加错/重复项可撤掉
async function removeItem(row, idx) {
  try {
    await ElMessageBox.confirm(`确认从清单移除「${row.assetName}」?`, '删除授权项', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' })
  } catch { return }
  try {
    if (row.applyId) await deleteAuthApply(row.applyId)
    items.value.splice(idx, 1)
    ElMessage.success('已移除')
  } catch (e) { ElMessage.warning('删除失败:' + (e?.message || '')) }
}

async function doSubmit() {
  submitting.value = true
  try { await submitBatchList(batchListId.value); step.value = 3 }
  finally { submitting.value = false }
}

function go(p) { router.push(p) }
function reset() {
  step.value = 0; batchListId.value = ''; listNo.value = ''; items.value = []
  Object.assign(listForm, { listYear: '', granteeOrg: '', rightType: '', scenario: '', validTerm: '两年', businessDomain: '', remark: '' })
  Object.assign(item, emptyItem())
}
</script>

<style scoped>
.wz-steps { max-width: 900px; margin: 8px auto 20px; }
.wz-body { min-height: 340px; }
.wz-flow { background: #f7f9ff; border-radius: 8px; padding: 10px 16px; color: #4a5160; font-size: 13px; display: inline-block; }
.batch-primary { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; padding: 12px 16px; margin-bottom: 14px; background: linear-gradient(180deg, #eef4ff, #f7faff); border: 1px solid #d6e4ff; border-radius: 8px; }
.batch-primary-hint { color: #606266; font-size: 13px; line-height: 1.5; flex: 1; min-width: 240px; }
</style>
