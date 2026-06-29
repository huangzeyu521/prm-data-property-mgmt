<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
<template>
  <div class="prm-page">
    <div class="prm-table-note" style="margin:0 0 12px">
      注:一站式批量授权——填清单基础信息 → 从确权资源池选授权数据 → 确认提交申报稿,一次办完申报。提交后进入审批链(合规→主管→经理→副总→领导小组决策),经甲乙双签《数据运营授权协议(附录D)》后执行授权·归档(对外经营权另备案附录G)。授权凭证为协议,非"证书"。
    </div>

    <el-steps :active="submitted ? 3 : step" finish-status="success" align-center class="wz-steps">
      <el-step title="清单基础信息" description="被授权方 · 授权年度(表6 清单头)" />
      <el-step title="选择授权数据" description="从确权资源池逐条加入(表6 明细)" />
      <el-step title="确认并提交" description="合规校验 → 提交申报稿" />
    </el-steps>

    <div class="wz-body">
      <!-- 步骤1:建清单 -->
      <el-card v-show="step === 0" shadow="never">
        <el-form :model="listForm" label-width="140px" style="max-width:580px">
          <el-alert v-if="!batchListId" type="info" :closable="false" style="margin-bottom:12px;max-width:680px">
            <template #title>
              第一次用?可
              <el-button link type="primary" style="vertical-align:baseline" :loading="demoFilling" @click="fillDemo">一键示例:建清单并自动加入可授明细(测试/演示)</el-button>
              资源池按权益类型过滤,生效卡片自动匹配,材料见 test/批量授权申请
            </template>
          </el-alert>
          <el-form-item label="授权年度" required><el-input v-model="listForm.listYear" placeholder="如 2026" /></el-form-item>
          <el-form-item label="申请主体(被授权方)" required>
            <el-select v-if="!externalGrantee" v-model="listForm.granteeOrg" filterable allow-create default-first-option clearable
              placeholder="选择被授权方(南网组织;搜不到可直接输入)— 本批数据统一授给谁" style="width:100%">
              <el-option v-for="o in orgOptions" :key="o.id" :label="o.bizOrgName" :value="o.bizOrgName" />
            </el-select>
            <el-input v-else v-model="listForm.granteeOrg" placeholder="外部被授权主体名称(政府/外部企业/社会组织)" clearable />
            <div style="margin-top:4px">
              <el-checkbox v-model="externalGrantee" @change="listForm.granteeOrg = ''">被授权方为外部主体(不在南网组织结构内;对外经营权另备案附录G)</el-checkbox>
            </div>
          </el-form-item>
          <el-form-item label="联系人/单位主管" required>
            <el-input v-model="listForm.contactPerson" placeholder="表6 联络人/单位主管(批量共享)" clearable />
          </el-form-item>
          <el-form-item label="联系方式" required>
            <el-input v-model="listForm.contactInfo" placeholder="表6 联系方式(电话/邮箱)" clearable />
          </el-form-item>
          <el-divider content-position="left" style="margin:8px 0">
            <span style="font-size:12px;color:var(--prm-color-text-weak)">批量默认(逐条加项时自动带入,可逐项调整)</span>
          </el-divider>
          <el-form-item label="默认权益类型(单选)">
            <el-select v-model="listForm.rightType" clearable style="width:100%" placeholder="整批默认权益类型(单选;不同表可逐项调整)">
              <el-option v-for="t in rightTypes" :key="t" :label="t" :value="t" />
            </el-select>
            <div style="font-size:12px;color:var(--prm-color-text-weak);line-height:1.5;margin-top:2px">
              授权仅授「使用权 / 经营权」两类;数据持有权经确权认定取得,不在授权范围(三权分置)。<br/>
              可授数据集合由确权资源池决定(先确后授);此处选整批默认,逐项可调。
            </div>
          </el-form-item>
          <el-form-item label="默认使用场景"><el-input v-model="listForm.scenario" placeholder="整批默认使用场景及目的" clearable /></el-form-item>
          <!-- 业务域=数据/资产属性(表5「所属业务域」),由 step2 选中的数据表从确权目录逐表带出,不在此手填(避免与真实数据域冲突) -->
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
        <!-- 唯一录入入口:从确权目录批量选取(资源池已按 先确后授 + 权属可授 + 经营权对外开放 过滤) -->
        <div class="batch-primary">
          <el-button type="primary" size="large" @click="openPicker">① 从确权目录批量选取数据资产</el-button>
          <span class="batch-primary-hint">
            目录按「选系统 → 选模块 → 选库表」展示,且仅列<b>当前权益类型可授</b>的已确权数据表(经营权另需在对外开放目录);
            勾选后自动套用清单头默认(被授权方/场景/时效)+ 确权带出(业务域/第三方/隐私),可跨系统累加一次加入。
          </span>
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
          <div style="margin-top:6px;color:var(--prm-color-text-weak);font-size:12px">必填项须上传;"视情况"项按全单是否涉第三方/隐私商密上传。</div>
        </div>
        <el-divider content-position="left" style="margin-top:4px">
          <span style="font-size:13px;color:var(--prm-color-text-weak)">已加入明细(数据表 × 权益,清单头默认逐条沿用)</span>
        </el-divider>
        <div class="prm-table-note" style="margin-bottom:6px">已加入明细({{ items.length }} 项)</div>
        <!-- 表6「是否跨系统域、跨地域」:批量可跨多系统,按已加入项自动判定(只读) -->
        <el-alert v-if="items.length" :type="crossSystemInfo.isCross ? 'warning' : 'info'" :closable="false" style="margin-bottom:8px;max-width:880px">
          <template #title>
            <span>是否跨系统域:</span>
            <el-tag :type="crossSystemInfo.isCross ? 'warning' : 'info'" size="small" effect="dark" style="margin:0 6px">{{ crossSystemInfo.isCross ? '是(跨系统域)' : '否(单系统)' }}</el-tag>
            本清单覆盖 {{ crossSystemInfo.systems.length }} 个系统{{ crossSystemInfo.systems.length ? '(' + crossSystemInfo.systems.join('、') + ')' : '' }};批量授权可跨多系统聚合(表6 专设此判定),已自动写入各授权项。
          </template>
        </el-alert>
        <el-table :data="items" border size="small" max-height="420">
          <el-table-column type="index" label="#" width="44" align="center" />
          <el-table-column prop="assetName" label="数据表" min-width="140" show-overflow-tooltip />
          <el-table-column prop="systemName" label="所属系统" min-width="120" show-overflow-tooltip>
            <template #default="{ row }">{{ row.systemName || '—' }}</template>
          </el-table-column>
          <el-table-column prop="schemaName" label="模式名称" min-width="110" show-overflow-tooltip>
            <template #default="{ row }">{{ row.schemaName || '—' }}</template>
          </el-table-column>
          <el-table-column prop="businessDomain" label="业务域" min-width="100" show-overflow-tooltip>
            <template #default="{ row }">{{ row.businessDomain || '—' }}</template>
          </el-table-column>
          <el-table-column prop="rightType" label="权益" width="140" />
          <el-table-column prop="equityCardId" label="生效卡片" min-width="120" show-overflow-tooltip>
            <template #default="{ row }">{{ row.equityCardId || '—' }}</template>
          </el-table-column>
          <el-table-column prop="scenario" label="使用场景" min-width="120" show-overflow-tooltip>
            <template #default="{ row }">{{ row.scenario || '—' }}</template>
          </el-table-column>
          <!-- 表6 合规判定列:第三方/隐私商密 由确权事实带出(只读,堵人工低报);跨域按全清单系统并集判定 -->
          <el-table-column label="涉第三方" width="92" align="center">
            <template #default="{ row }">
              <el-tag :type="row.thirdPartySource && String(row.thirdPartySource).trim() ? 'warning' : 'info'" size="small" effect="plain">
                {{ row.thirdPartySource && String(row.thirdPartySource).trim() ? '涉' : '否' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="涉隐私/商密" width="104" align="center">
            <template #default="{ row }">
              <el-tag :type="row.sensitiveType && String(row.sensitiveType).trim() && row.sensitiveType !== '无' ? 'danger' : 'info'" size="small" effect="plain">
                {{ row.sensitiveType && String(row.sensitiveType).trim() && row.sensitiveType !== '无' ? row.sensitiveType : '否' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="跨域" width="72" align="center">
            <template #default>
              <el-tag :type="crossSystemInfo.isCross ? 'warning' : 'info'" size="small" effect="plain">{{ crossSystemInfo.isCross ? '是' : '否' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="70" align="center">
            <template #default="{ row, $index }">
              <el-button link type="danger" size="small" @click="removeItem(row, $index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="items.length===0" :image-size="60" description="尚未添加授权项 — 点上方「① 从确权目录批量选取数据资产」" />
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
        <div style="text-align:center;margin:4px 0 0;color:var(--prm-color-text-weak);font-size:12px">
          AI 辅助(可选,不影响提交):
          <el-button link type="primary" :loading="listReviewing" @click="runListPreReview">AI 清单预审(qwen3-max)</el-button>
        </div>
        <AiThinking v-bind="aiThink.state" />
        <!-- 统一待处理清单(单一闭环):被拦明细逐项「去修正」(回 step1)→ 重新一键校验,直至通过 -->
        <el-card v-if="complianceResult && !complianceResult.allPass && complianceResult.blocked.length" shadow="never" style="margin-top:12px;border:1px solid #fde2e2;background:#fff8f8">
          <div style="font-weight:600;color:var(--prm-color-danger);margin-bottom:8px">需处理以下 {{ complianceResult.blocked.length }} 项后方可提交(处理完点上方「重新一键校验」)</div>
          <el-table :data="complianceResult.blocked" border size="small">
            <el-table-column label="来源" width="76" align="center"><template #default><el-tag type="danger" size="small">合规</el-tag></template></el-table-column>
            <el-table-column prop="assetName" label="数据资产" min-width="180" show-overflow-tooltip />
            <el-table-column prop="reason" label="被拦原因" min-width="240" show-overflow-tooltip />
            <el-table-column label="就地处理" width="120" align="center"><template #default><el-button link type="primary" @click="step = 1">去修正</el-button></template></el-table-column>
          </el-table>
        </el-card>
        <el-alert v-if="listOpinion" type="info" :closable="false" style="margin-top:12px" title="AI 清单预审意见" :description="listOpinion" show-icon />
      </el-card>

      <!-- 提交成功页 + 后续流转进度时间轴(单一真相,对齐 35号文 附录C 表1) -->
      <el-card v-show="submitted" shadow="never">
        <el-result icon="success" title="批量授权清单申报稿已提交" :sub-title="`清单 ${listNo}（${items.length} 项）已进入审批链,当前待「合规管控小组审核」`">
          <template #extra>
            <div class="wz-progress">
              <div class="wz-progress-t">后续流转进度(实时以「清单管理」为准)</div>
              <AuthFlowProgress mode="batch" current="compliance" />
            </div>
            <div style="margin-top:6px">
              <el-button type="primary" @click="go('/dpr/auth/batch-list')">去清单管理查看进度</el-button>
              <el-button @click="go('/dpr/auth/filing')">对外经营权备案(附录G)</el-button>
              <el-button @click="reset">再建一份清单</el-button>
            </div>
          </template>
        </el-result>
      </el-card>
    </div>

    <PageActions v-if="!submitted">
      <el-button v-if="step > 0" @click="step--">上一步</el-button>
      <el-button v-if="step === 0" type="primary" :loading="creating" @click="next0">下一步</el-button>
      <el-button v-if="step === 1" type="primary" :disabled="items.length===0" @click="step = 2">下一步</el-button>
      <el-tooltip v-if="step === 2 && !canSubmit" content="请先通过合规校验(全部明细合规)" placement="top">
        <span><el-button type="primary" disabled>提交申报稿</el-button></span>
      </el-tooltip>
      <el-button v-else-if="step === 2" type="primary" :loading="submitting" @click="doSubmit">提交申报稿</el-button>
    </PageActions>

    <!-- 从确权目录多选资产(资源池:先确后授 + 权属可授 + 经营权对外开放,展示对齐确权范围树) -->
    <el-dialog v-model="pickerDlg" title="从确权目录批量选取数据资产" width="600px" align-center>
      <el-alert type="info" :closable="false" style="margin-bottom:10px">
        仅列「{{ listForm.rightType || '所选权益' }}」可授的已确权数据表(有生效权益卡片{{ listForm.rightType === '数据产品经营权' ? ' + 在对外开放目录' : '' }});
        被授权方/场景/时效取清单头(批量共享),勾选多个可跨系统累加一次加入。
      </el-alert>
      <GrantableCatalogTree v-if="pickerDlg" :right-type="listForm.rightType" @change="onPickChange" />
      <template #footer>
        <span style="float:left;color:var(--prm-color-text-weak);font-size:12px;line-height:32px">已勾选 {{ pickedLeaves.length }} 张数据表</span>
        <el-button type="primary" :loading="picking" :disabled="!pickedLeaves.length" @click="confirmPick">加入选中资产</el-button>
        <el-button @click="pickerDlg=false">取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createBatchList, saveAuthDraft, deleteAuthApply, submitBatchList, aiBatchPreReview, listAuthMaterialRules, checkBatchCompliance, uploadAuthMaterialFile, listAuthMaterial, authMaterialFileUrl } from '@/api/authorize'
import { openFilePreview } from '@/composables/useFilePreview'
import AiThinking from '@/components/AiThinking.vue'
import PageActions from '@/components/PageActions.vue'
import AuthFlowProgress from '@/components/AuthFlowProgress.vue'
import GrantableCatalogTree from './GrantableCatalogTree.vue'
import { useAiThinking } from '@/composables/useAiThinking'
import { AI_PHASES } from '@/lib/aiPhases'
const aiThink = useAiThinking()
import { pageEquityCard, getRightsFacts } from '@/api/confirm'
import { listOrg } from '@/api/org'

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
// 提交后进入「提交成功页 + 进度时间轴」(创建/流转分离;流转属多主体跨周案件,不再当作向导第4步)
const submitted = ref(false)
const creating = ref(false); const submitting = ref(false)
const batchListId = ref(''); const listNo = ref('')
const items = ref([])
// 清单头(批量级):被授权方为批量共享;权益类型/场景/时效为整批默认,加项时带入(资源池按权益类型过滤)
const listForm = reactive({ listYear: '', granteeOrg: '', contactPerson: '', contactInfo: '', rightType: '', scenario: '', validTerm: '两年', remark: '' })
function emptyItem() {
  return { assetId: '', assetName: '', tableCode: '', systemName: '', schemaName: '', equityCardId: '', granteeOrg: '', rightType: '', scenario: '', validTerm: '两年', validDate: '', businessDomain: '', thirdPartySource: '', sensitiveType: '', crossRegion: false, applicantManager: '', contactInfo: '' }
}
// 表6「是否跨系统域、跨地域」:批量可跨多系统 → 按清单已加入项的系统/业务域去重自动判定(只读呈现 + 写回 crossRegion)
const crossSystemInfo = computed(() => {
  const systems = [...new Set(items.value.map(x => x.systemName).filter(Boolean))]
  const domains = [...new Set(items.value.map(x => x.businessDomain).filter(Boolean))]
  return { systems, domains, isCross: systems.length > 1 || domains.length > 1 }
})
// 去重键:被授权方批量恒定,故行唯一性=库表+权益类型+场景(对齐表4/表6:同表可多权益/多场景,三者全同才是真重复)。
// 注:权益卡片库表级后 assetId=SYS:系统(同系统多库表共享),故须并入 tableCode/库表名 区分,避免同系统不同库表被误判重复。
function dupKey(x) {
  return `${x.assetId || ''}|${x.tableCode || x.assetName || ''}|${x.rightType || ''}|${(x.scenario || '').trim()}`
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
  const all = items.value
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
// 被授权方组织选择器:接南网真实组织树(listOrg);外部主体走自由文本例外(对外经营权附录G)
const orgOptions = ref([])
const externalGrantee = ref(false)
onMounted(async () => {
  try {
    const rules = await listAuthMaterialRules('批量')
    if (Array.isArray(rules) && rules.length) materialRules.value = rules
  } catch (e) { /* 规则接口不可用 → requiredChecklist 自动用内置兜底,不丢指引 */ }
  try {
    orgOptions.value = (await listOrg()) || []
  } catch (e) { /* 组织接口不可用 → 选择器降级为可输入(allow-create),不阻断申报 */ }
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
  if (!listForm.contactPerson) { ElMessage.warning('请填写联系人/申请单位主管(表5/表6 必填)'); return }
  if (!listForm.contactInfo) { ElMessage.warning('请填写联系方式(表5/表6 必填)'); return }
  if (!listForm.rightType) { ElMessage.warning('请选择默认权益类型(确权目录资源池按权益类型过滤可授数据表)'); return }
  if (!batchListId.value) {
    creating.value = true
    try {
      batchListId.value = await createBatchList({ listYear: listForm.listYear, remark: listForm.remark })
      listNo.value = listForm.listYear + ' 批量授权清单'
    } finally { creating.value = false }
  }
  step.value = 1
}

// 唯一录入入口:从确权目录资源池多选(先确后授 + 权属可授 + 经营权对外开放,已在树侧过滤)
const pickerDlg = ref(false); const picking = ref(false); const pickedLeaves = ref([])
async function openPicker() {
  if (!listForm.granteeOrg) { ElMessage.warning('请先在步骤1清单头填写"被授权方"(批量共享)'); return }
  if (!listForm.rightType) { ElMessage.warning('请先在清单头选择"权益类型"(资源池据此过滤可授数据表)'); return }
  pickedLeaves.value = []
  pickerDlg.value = true
}
// 资源池树勾选回传(每个叶子带 assetId/equityCardId/rightType/系统/模块)
function onPickChange(leaves) { pickedLeaves.value = leaves || [] }
async function confirmPick() {
  if (!pickedLeaves.value.length) { ElMessage.warning('请至少勾选一张数据表'); return }
  picking.value = true
  try {
    let ok = 0
    const dups = []
    const seen = new Set(items.value.map(dupKey))
    // 跨系统域自动判定:并入本批所选系统后,清单是否跨 >1 系统/业务域(写回每条 crossRegion)
    const sysUnion = new Set([...items.value.map(x => x.systemName).filter(Boolean),
      ...pickedLeaves.value.map(l => l.systemName).filter(Boolean)])
    const isCross = sysUnion.size > 1
    for (const lf of pickedLeaves.value) {
      const f = await deriveFacts(lf.assetId)
      const it = { ...emptyItem(), assetId: lf.assetId, assetName: lf.assetName, tableCode: lf.tableCode,
        systemName: lf.systemName, schemaName: lf.schemaName, equityCardId: lf.equityCardId,
        granteeOrg: listForm.granteeOrg, rightType: lf.rightType || listForm.rightType,
        scenario: listForm.scenario, validDate: expiryOf(listForm.validTerm),
        businessDomain: lf.businessDomain || f.businessDomain || '', // 业务域由确权目录(lf)优先、确权事实(f)兜底带出,不再用清单头手填默认
        thirdPartySource: f.thirdPartySource, sensitiveType: f.sensitiveType,
        crossRegion: isCross, applicantManager: listForm.contactPerson, contactInfo: listForm.contactInfo }
      const k = dupKey(it)
      if (seen.has(k)) { dups.push(lf.assetName); continue } // 同表+权益+场景已在清单,去重
      seen.add(k)
      const id = await saveAuthDraft({ authMode: '批量', batchListId: batchListId.value, ...it })
      items.value.push({ ...it, applyId: id })
      ok++
    }
    ElMessage[dups.length ? 'warning' : 'success'](
      `已加入 ${ok} 项(资源池均已确权+权属可授,生效卡自动带入)`
      + (dups.length ? `;跳过重复 ${dups.length} 项:${dups.slice(0, 3).join('、')}` : ''))
    pickerDlg.value = false
    pickedLeaves.value = []
  } finally { picking.value = false }
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

// 自动配卡引擎:按资产匹配生效权益卡片(先确后授 + 权属可授:卡片权益==所选权益)。供一键示例配卡。
const CARD_OK = ['正常', '生效']
const cardOpts = ref([])
let cardsLoaded = false
async function loadCards() {
  if (cardsLoaded) return
  const r = await pageEquityCard({ current: 1, size: 100 })
  cardOpts.value = r.records || []
  cardsLoaded = true
}
// 权属可授:仅返回"卡片权益类型 == 指定权益类型"的生效卡片号(授权只授使用权/经营权)
function findUsableCard(assetId, right) {
  const c = cardOpts.value.find(x => x.assetId === assetId && CARD_OK.includes(x.cardStatus) && (!right || x.rightType === right))
  return c ? (c.cardNo || c.cardId) : ''
}
// 确权信息带出:按资产取最新已完成确权的第三方来源/隐私商密事实(只读,堵人工低报)
async function deriveFacts(assetId) {
  try {
    const f = await getRightsFacts(assetId)
    return { thirdPartySource: f?.thirdPartySource || '', sensitiveType: f?.sensitiveType || '无', businessDomain: f?.businessDomain || '' }
  } catch { return { thirdPartySource: '', sensitiveType: '无', businessDomain: '' } }
}

// 一键示例:建清单 + 自动加入可授明细(对齐资源池过滤:使用权下仅 AST-002 台区负荷数据有匹配生效卡片)
const demoFilling = ref(false)
async function fillDemo() {
  demoFilling.value = true
  try {
    listForm.listYear = '2026'
    listForm.granteeOrg = '南网综合能源股份有限公司'
    listForm.contactPerson = '张三'
    listForm.contactInfo = '020-31000000'
    listForm.rightType = '数据加工使用权'
    listForm.scenario = '综合能源服务'
    listForm.remark = '综能板块年度批量授权(示例)'
    await next0()
    await loadCards()
    const demo = [['AST-002', '台区负荷数据', '计量系统']]
    let ok = 0
    const seen = new Set(items.value.map(dupKey))
    for (const [aid, name, sys] of demo) {
      const card = findUsableCard(aid, listForm.rightType) // 权属可授:卡片权益须==所选权益
      if (!card) continue
      const f = await deriveFacts(aid)
      const it = { ...emptyItem(), assetId: aid, assetName: name, systemName: sys, equityCardId: card,
        granteeOrg: listForm.granteeOrg, rightType: listForm.rightType, scenario: listForm.scenario, validDate: expiryOf(listForm.validTerm),
        businessDomain: f.businessDomain, thirdPartySource: f.thirdPartySource, sensitiveType: f.sensitiveType,
        applicantManager: listForm.contactPerson, contactInfo: listForm.contactInfo }
      const k = dupKey(it)
      if (seen.has(k)) continue // 去重:同表+权益+场景已在清单
      seen.add(k)
      const id = await saveAuthDraft({ authMode: '批量', batchListId: batchListId.value, ...it })
      items.value.push({ ...it, applyId: id })
      ok++
    }
    ElMessage.success(`示例完成:清单已建,自动加入 ${ok} 条可授明细(生效卡已配),可直接"提交清单审批"`)
  } finally { demoFilling.value = false }
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
  try { await submitBatchList(batchListId.value); submitted.value = true }
  finally { submitting.value = false }
}

function go(p) { router.push(p) }
function reset() {
  step.value = 0; submitted.value = false; externalGrantee.value = false; batchListId.value = ''; listNo.value = ''; items.value = []; pickedLeaves.value = []
  Object.assign(listForm, { listYear: '', granteeOrg: '', contactPerson: '', contactInfo: '', rightType: '', scenario: '', validTerm: '两年', remark: '' })
}
</script>

<style scoped>
.wz-steps { max-width: 900px; margin: 8px auto 20px; }
.wz-body { min-height: 340px; }
.wz-progress { background: #f7f9ff; border-radius: 8px; padding: 14px 20px 6px; margin: 4px auto 12px; max-width: 560px; text-align: left; }
.wz-progress-t { font-weight: 600; font-size: 13px; color: var(--prm-color-text); margin-bottom: 10px; }
.batch-primary { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; padding: 12px 16px; margin-bottom: 14px; background: linear-gradient(180deg, #eef4ff, #f7faff); border: 1px solid #d6e4ff; border-radius: 8px; }
.batch-primary-hint { color: var(--prm-color-text-secondary); font-size: 13px; line-height: 1.5; flex: 1; min-width: 240px; }
</style>
