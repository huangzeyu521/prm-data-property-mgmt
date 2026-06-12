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
        <el-form :model="listForm" label-width="100px" style="max-width:520px">
          <el-alert v-if="!batchListId" type="info" :closable="false" style="margin-bottom:12px;max-width:680px">
            <template #title>
              第一次用?可
              <el-button link type="primary" style="vertical-align:baseline" :loading="demoFilling" @click="fillDemo">一键示例:建清单并自动加入3条明细(测试/演示)</el-button>
              生效卡片自动匹配,材料见 test/批量授权申请
            </template>
          </el-alert>
          <el-form-item label="授权年度" required><el-input v-model="listForm.listYear" placeholder="如 2026" /></el-form-item>
          <el-form-item label="清单备注"><el-input v-model="listForm.remark" type="textarea" :rows="2" placeholder="如:综能板块年度批量授权" /></el-form-item>
        </el-form>
        <el-alert v-if="batchListId" type="success" :closable="false" show-icon :title="`清单已创建(${listNo})，开始逐条添加授权项`" style="max-width:520px;margin-top:8px" />
      </el-card>

      <!-- 步骤2:逐条加授权项 -->
      <el-card v-show="step === 1" shadow="never">
        <!-- AI 批量填单(qwen3-max,stub 回退):一段话解析出共享字段+多条明细 -->
        <div class="ai-batch">
          <el-input v-model="aiBatchText" type="textarea" :rows="2"
            placeholder="AI 批量填单:如 向南网综合能源股份有限公司授权台区负荷数据、充电桩运营数据、线损分析数据用于综合能源服务,数据加工使用权,批量授权" />
          <el-button type="warning" :loading="aiParsing" style="margin-top:6px" @click="runAiBatch">大瓦特 AI 解析并预填明细</el-button>
          <el-button v-if="aiItems.length" type="primary" plain :loading="aiAdding" style="margin-top:6px;margin-left:8px" @click="addAiItems">
            一键加入清单({{ aiItems.length }} 项)
          </el-button>
          <el-table v-if="aiItems.length" :data="aiItems" border size="small" style="margin-top:8px;max-width:680px">
            <el-table-column prop="assetName" label="解析出的数据资产" min-width="180" />
            <el-table-column label="权益卡片ID" min-width="160">
              <template #default="{ row }"><el-input v-model="row.equityCardId" size="small" placeholder="先确后授:填卡片ID" /></template>
            </el-table-column>
          </el-table>
        </div>
        <el-divider />
        <el-row :gutter="16">
          <el-col :span="11">
            <el-form ref="itemRef" :model="item" :rules="itemRules" label-width="100px">
              <el-form-item label="关联资产ID" prop="assetId">
                <el-select v-model="item.assetId" filterable remote allow-create default-first-option clearable
                  :remote-method="searchAssets" :loading="assetSearching" style="width:100%"
                  placeholder="输名称/ID 搜台账,如 台区 / AST-002" @change="onItemAssetPicked">
                  <el-option v-for="a in assetOpts" :key="a.assetId" :value="a.assetId" :label="a.assetId + '　' + a.assetName">
                    <span>{{ a.assetId }}</span><span style="float:right;color:#8c8c8c;font-size:12px">{{ a.assetName }}</span>
                  </el-option>
                </el-select>
              </el-form-item>
              <el-form-item label="资产名称" prop="assetName"><el-input v-model="item.assetName" /></el-form-item>
              <el-form-item label="权益卡片ID" prop="equityCardId"><el-input v-model="item.equityCardId" placeholder="先确后授,如 EC-BAT-0002" /></el-form-item>
              <el-form-item label="被授权方" prop="granteeOrg"><el-input v-model="item.granteeOrg" /></el-form-item>
              <el-form-item label="权益类型" prop="rightType">
                <el-select v-model="item.rightType" style="width:100%">
                  <el-option v-for="t in rightTypes" :key="t" :label="t" :value="t" />
                </el-select>
              </el-form-item>
              <el-form-item label="使用场景"><el-input v-model="item.scenario" /></el-form-item>
              <el-form-item label="第三方来源"><el-input v-model="item.thirdPartySource" placeholder="涉及第三方时填" /></el-form-item>
              <el-form-item label="隐私/商密"><el-input v-model="item.sensitiveType" placeholder="个人隐私/商业秘密/无" /></el-form-item>
              <el-form-item label="是否跨域"><el-switch v-model="item.crossRegion" /></el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="adding" @click="addItem">加入清单</el-button>
                <el-button @click="openPicker">从目录多选资产</el-button>
              </el-form-item>
            </el-form>
          </el-col>
          <el-col :span="13">
            <div class="prm-table-note" style="margin-bottom:6px">已加入明细({{ items.length }} 项)</div>
            <el-table :data="items" border size="small" max-height="420">
              <el-table-column type="index" label="#" width="44" align="center" />
              <el-table-column prop="granteeOrg" label="被授权方" min-width="110" show-overflow-tooltip />
              <el-table-column prop="assetName" label="数据表" min-width="110" show-overflow-tooltip />
              <el-table-column prop="rightType" label="权益" width="120" />
            </el-table>
            <el-empty v-if="items.length===0" :image-size="60" description="尚未添加授权项" />
          </el-col>
        </el-row>
      </el-card>

      <!-- 步骤3:提交清单审批 -->
      <el-card v-show="step === 2" shadow="never">
        <el-result icon="info" :title="`提交《批量授权清单》申报稿（${items.length} 项）`" sub-title="资料审核 → 清单审核审批 → 领导小组决策批准" />
        <div style="text-align:center;margin-top:4px">
          <el-button type="warning" plain :loading="listReviewing" @click="runListPreReview">AI 清单预审(qwen3-max)</el-button>
        </div>
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

    <div class="wz-foot">
      <el-button v-if="step > 0 && step < 3" @click="step--">上一步</el-button>
      <el-button v-if="step === 0" type="primary" :loading="creating" @click="next0">下一步:加授权项</el-button>
      <el-button v-if="step === 1" type="primary" :disabled="items.length===0" @click="step = 2">下一步:提交审批</el-button>
      <el-button v-if="step === 2" type="primary" :loading="submitting" @click="doSubmit">提交清单审批</el-button>
    </div>

    <!-- 从目录多选资产 -->
    <el-dialog v-model="pickerDlg" title="从确权目录多选数据资产" width="560px" align-center>
      <el-alert type="info" :closable="false" style="margin-bottom:10px">
        勾选多个数据集，统一加入清单。共享字段（被授权方/权益类型/场景）取左侧表单当前值，加入后可逐条调整。
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
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createBatchList, saveAuthDraft, submitBatchList, aiBatchIntent, aiBatchPreReview } from '@/api/authorize'
import { pageArchive } from '@/api/propertyArchive'
import { pageEquityCard } from '@/api/confirm'
import { getPropertyTree } from '@/api/ledger'

const router = useRouter()
const rightTypes = ['数据加工使用权', '数据产品经营权']
const step = ref(0)
const creating = ref(false); const adding = ref(false); const submitting = ref(false)
const batchListId = ref(''); const listNo = ref('')
const items = ref([])
const itemRef = ref()
const listForm = reactive({ listYear: '', remark: '' })
const item = reactive(emptyItem())
const itemRules = {
  assetId: [{ required: true, message: '请输入资产ID', trigger: 'blur' }],
  assetName: [{ required: true, message: '请输入资产名称', trigger: 'blur' }],
  equityCardId: [{ required: true, message: '先确后授:必须引用权益卡片', trigger: 'blur' }],
  granteeOrg: [{ required: true, message: '请输入被授权方', trigger: 'blur' }],
  rightType: [{ required: true, message: '请选择权益类型', trigger: 'change' }]
}
function emptyItem() {
  return { assetId: '', assetName: '', equityCardId: '', granteeOrg: '', rightType: '', scenario: '', thirdPartySource: '', sensitiveType: '', crossRegion: false }
}

async function next0() {
  if (!listForm.listYear) { ElMessage.warning('请填写授权年度'); return }
  if (!batchListId.value) {
    creating.value = true
    try {
      batchListId.value = await createBatchList({ listYear: listForm.listYear, remark: listForm.remark })
      listNo.value = listForm.listYear + ' 批量授权清单'
    } finally { creating.value = false }
  }
  step.value = 1
}

// 从目录多选资产批量加入清单
const pickerDlg = ref(false); const picking = ref(false); const tree = ref([]); const pickTreeRef = ref()
async function openPicker() {
  if (!item.granteeOrg || !item.rightType) { ElMessage.warning('请先在左侧表单填写"被授权方"和"权益类型"作为共享字段'); return }
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
    for (const lf of leaves) {
      // 先确后授:自动匹配生效权益卡片,无卡项跳过并提示(后端会拦截空卡)
      const card = findUsableCard(lf.assetId)
      if (!card) { skipped.push(lf.label || lf.assetId); continue }
      const it = { ...emptyItem(), assetId: lf.assetId, assetName: lf.label, equityCardId: card,
        granteeOrg: item.granteeOrg, rightType: item.rightType, scenario: item.scenario }
      await saveAuthDraft({ authMode: '批量', batchListId: batchListId.value, ...it })
      items.value.push(it)
      ok++
    }
    ElMessage[skipped.length ? 'warning' : 'success'](
      `已加入 ${ok} 项(生效卡自动匹配)` + (skipped.length ? `;跳过未确权 ${skipped.length} 项:${skipped.slice(0, 3).join('、')}` : ''))
    pickerDlg.value = false
  } finally { picking.value = false }
}

// AI 批量填单(qwen3-max,stub 回退)
const aiBatchText = ref(''); const aiParsing = ref(false); const aiItems = ref([]); const aiAdding = ref(false)
const aiShared = ref({ granteeOrg: '', rightType: '', scenario: '' })
async function runAiBatch() {
  if (!aiBatchText.value) { ElMessage.warning('请先描述批量授权诉求'); return }
  aiParsing.value = true
  try {
    const raw = await aiBatchIntent(aiBatchText.value)
    const r = typeof raw === 'string' ? JSON.parse(raw) : raw
    aiShared.value = { granteeOrg: r.granteeOrg || '', rightType: r.rightType || '', scenario: r.scenario || '' }
    Object.assign(item, { granteeOrg: r.granteeOrg || '', rightType: r.rightType || '', scenario: r.scenario || '' })
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
async function addAiItems() {
  if (aiItems.value.some(x => !x.equityCardId)) { ElMessage.warning('请为每条明细补全权益卡片ID(先确后授)'); return }
  aiAdding.value = true
  try {
    for (const x of aiItems.value) {
      const it = { ...emptyItem(), assetId: x.assetId || x.assetName, assetName: x.assetName, equityCardId: x.equityCardId,
        granteeOrg: aiShared.value.granteeOrg, rightType: aiShared.value.rightType, scenario: aiShared.value.scenario }
      await saveAuthDraft({ authMode: '批量', batchListId: batchListId.value, ...it })
      items.value.push(it)
    }
    ElMessage.success(`已加入 ${aiItems.value.length} 项`)
    aiItems.value = []
  } finally { aiAdding.value = false }
}

// AI 清单预审(qwen3-max,stub 回退)
const listReviewing = ref(false); const listOpinion = ref('')
async function runListPreReview() {
  listReviewing.value = true
  try { listOpinion.value = await aiBatchPreReview(batchListId.value) }
  catch (e) { ElMessage.warning('AI 预审失败') }
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
async function searchAssets(kw) {
  if (!kw) { assetOpts.value = []; return }
  assetSearching.value = true
  try {
    const r = await pageArchive({ current: 1, size: 10, assetName: kw })
    assetOpts.value = r.records || []
  } finally { assetSearching.value = false }
}
async function onItemAssetPicked(id) {
  const hit = assetOpts.value.find(a => a.assetId === id)
  if (hit) item.assetName = hit.assetName
  if (!id) return
  await loadCards()
  const card = findUsableCard(id)
  if (card) { item.equityCardId = card; ElMessage.success('已自动带出生效权益卡片 ' + card) }
  else ElMessage.warning('该资产暂无生效权益卡片,请先完成确权(先确后授)')
}
async function resolveAssetByName(name) {
  const r = await pageArchive({ current: 1, size: 3, assetName: name })
  const recs = r.records || []
  return recs.find(x => x.assetName === name) || recs[0] || null
}

// 一键示例:建清单+3条明细全自动(生效卡自动匹配,对齐 test/批量授权申请 手册)
const demoFilling = ref(false)
async function fillDemo() {
  demoFilling.value = true
  try {
    listForm.listYear = '2026'
    listForm.remark = '综能板块年度批量授权(示例)'
    await next0()
    await loadCards()
    Object.assign(item, { granteeOrg: '南网综合能源股份有限公司', rightType: '数据加工使用权', scenario: '综合能源服务' })
    const demo = [['AST-001', '客户用电信息表'], ['AST-002', '台区负荷数据'], ['AST-006', '充电桩运营数据']]
    let ok = 0
    for (const [aid, name] of demo) {
      const card = findUsableCard(aid)
      if (!card) continue
      const it = { ...emptyItem(), assetId: aid, assetName: name, equityCardId: card,
        granteeOrg: item.granteeOrg, rightType: item.rightType, scenario: item.scenario }
      await saveAuthDraft({ authMode: '批量', batchListId: batchListId.value, ...it })
      items.value.push(it)
      ok++
    }
    ElMessage.success(`示例完成:清单已建,自动加入 ${ok} 条明细(生效卡已配),可直接"提交清单审批"`)
  } finally { demoFilling.value = false }
}

async function addItem() {
  await itemRef.value.validate()
  adding.value = true
  try {
    await saveAuthDraft({ authMode: '批量', batchListId: batchListId.value, ...item })
    items.value.push({ ...item })
    Object.assign(item, emptyItem())
    ElMessage.success('已加入清单')
  } finally { adding.value = false }
}

async function doSubmit() {
  submitting.value = true
  try { await submitBatchList(batchListId.value); step.value = 3 }
  finally { submitting.value = false }
}

function go(p) { router.push(p) }
function reset() {
  step.value = 0; batchListId.value = ''; listNo.value = ''; items.value = []
  Object.assign(listForm, { listYear: '', remark: '' }); Object.assign(item, emptyItem())
}
</script>

<style scoped>
.wz-steps { max-width: 900px; margin: 8px auto 20px; }
.wz-body { min-height: 340px; }
.wz-foot { margin-top: 18px; display: flex; gap: 12px; justify-content: center; }
.wz-flow { background: #f7f9ff; border-radius: 8px; padding: 10px 16px; color: #4a5160; font-size: 13px; display: inline-block; }
</style>
