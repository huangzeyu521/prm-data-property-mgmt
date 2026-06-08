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
          <el-form-item label="授权年度" required><el-input v-model="listForm.listYear" placeholder="如 2026" /></el-form-item>
          <el-form-item label="清单备注"><el-input v-model="listForm.remark" type="textarea" :rows="2" placeholder="如:综能板块年度批量授权" /></el-form-item>
        </el-form>
        <el-alert v-if="batchListId" type="success" :closable="false" show-icon :title="`清单已创建(${listNo})，开始逐条添加授权项`" style="max-width:520px;margin-top:8px" />
      </el-card>

      <!-- 步骤2:逐条加授权项 -->
      <el-card v-show="step === 1" shadow="never">
        <el-row :gutter="16">
          <el-col :span="11">
            <el-form ref="itemRef" :model="item" :rules="itemRules" label-width="110px">
              <el-form-item label="关联资产ID" prop="assetId"><el-input v-model="item.assetId" /></el-form-item>
              <el-form-item label="资产名称" prop="assetName"><el-input v-model="item.assetName" /></el-form-item>
              <el-form-item label="权益卡片ID" prop="equityCardId"><el-input v-model="item.equityCardId" placeholder="先确后授" /></el-form-item>
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
              <el-form-item><el-button type="primary" :loading="adding" @click="addItem">加入清单</el-button></el-form-item>
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
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createBatchList, saveAuthDraft, submitBatchList } from '@/api/authorize'

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
