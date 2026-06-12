<template>
  <div class="prm-page">
    <div class="prm-query-bar">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="资产名称">
          <el-input v-model="query.assetName" placeholder="请输入资产名称" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="授权模式">
          <el-select v-model="query.authMode" placeholder="全部" clearable style="width: 130px">
            <el-option v-for="m in modes" :key="m" :label="m" :value="m" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 130px">
            <el-option v-for="s in statuses" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">查询</el-button>
          <el-button @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="prm-table-card">
      <div style="margin-bottom: 12px"><el-button type="primary" @click="onCreate">新增</el-button></div>
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column type="index" label="序号" width="64" align="center" />
        <el-table-column prop="applyNo" label="申请编号" width="150" show-overflow-tooltip />
        <el-table-column prop="authMode" label="模式" width="100" align="center" />
        <el-table-column prop="assetName" label="资产名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="granteeOrg" label="被授权方" min-width="140" show-overflow-tooltip />
        <el-table-column prop="rightType" label="授权权益" width="130" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }"><el-tag :type="statusTag(row.status)">{{ row.status }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" v-if="row.status === '草稿'" @click="onSubmit(row)">提交</el-button>
            <el-button link type="success" v-if="row.status === '审核中'" @click="onApprove(row)">审批</el-button>
            <el-button link type="danger" v-if="row.status === '审核中'" @click="onReject(row)">驳回</el-button>
            <span v-if="row.status === '已生效'" style="color:#18a058">已签发证书</span>
          </template>
        </el-table-column>
      </el-table>
      <div class="prm-table-note">注:先确后授——授权必须引用有效权益卡片;卡片冻结/失效将熔断授权。</div>
      <el-pagination style="margin-top:16px;justify-content:flex-end" background layout="total, prev, pager, next"
        :total="total" :current-page="query.current" :page-size="query.size" @current-change="onPage" />
    </div>

    <el-dialog v-model="dialogVisible" title="新增授权申请" width="600px" align-center :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="150px">
        <el-form-item label="授权模式">
          <el-select v-model="form.authMode" style="width:100%">
            <el-option v-for="m in modes" :key="m" :label="m" :value="m" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联资产ID" prop="assetId"><el-input v-model="form.assetId" /></el-form-item>
        <el-form-item label="资产名称" prop="assetName"><el-input v-model="form.assetName" /></el-form-item>
        <el-form-item label="权益卡片ID" prop="equityCardId">
          <el-input v-model="form.equityCardId" placeholder="先确后授:引用已确权的权益卡片编码" />
        </el-form-item>
        <el-form-item label="被授权方" prop="granteeOrg"><el-input v-model="form.granteeOrg" /></el-form-item>
        <el-form-item label="授权权益类型" prop="rightType">
          <el-select v-model="form.rightType" style="width:100%">
            <el-option v-for="t in rightTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="使用场景"><el-input v-model="form.scenario" /></el-form-item>
        <el-form-item label="授权范围"><el-input v-model="form.scope" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="onSubmitForm">确定</el-button>
        <el-button @click="dialogVisible = false">取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageAuthApply, saveAuthDraft, submitAuth, approveAuth, rejectAuth } from '@/api/authorize'

const modes = ['一事一议', '批量']
const statuses = ['草稿', '审核中', '已生效', '已驳回']
const rightTypes = ['数据加工使用权', '数据产品经营权']
const query = reactive({ current: 1, size: 10, assetName: '', authMode: '', status: '' })
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const dialogVisible = ref(false)
const formRef = ref()
const form = reactive(emptyForm())
const rules = {
  assetId: [{ required: true, message: '请输入关联资产ID', trigger: 'blur' }],
  assetName: [{ required: true, message: '请输入资产名称', trigger: 'blur' }],
  equityCardId: [{ required: true, message: '先确后授:必须引用权益卡片', trigger: 'blur' }],
  granteeOrg: [{ required: true, message: '请输入被授权方', trigger: 'blur' }],
  rightType: [{ required: true, message: '请选择授权权益类型', trigger: 'change' }]
}

function emptyForm() {
  return { authMode: '一事一议', assetId: '', assetName: '', equityCardId: '', granteeOrg: '', rightType: '', scenario: '', scope: '' }
}
function statusTag(s) {
  return { 已生效: 'success', 已驳回: 'danger', 草稿: 'info' }[s] || 'warning'
}

async function load() {
  loading.value = true
  try {
    const res = await pageAuthApply({ ...query })
    rows.value = res.records || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}
function onSearch() { query.current = 1; load() }
function onReset() { query.assetName = ''; query.authMode = ''; query.status = ''; onSearch() }
function onPage(p) { query.current = p; load() }
function onCreate() { Object.assign(form, emptyForm()); dialogVisible.value = true }

async function onSubmitForm() {
  await formRef.value.validate()
  const id = await saveAuthDraft({ ...form })
  await submitAuth(id)
  ElMessage.success('已提交授权申请')
  dialogVisible.value = false
  load()
}
async function onSubmit(row) { await submitAuth(row.applyId); ElMessage.success('已提交'); load() }
async function onApprove(row) {
  await approveAuth(row.applyId)
  ElMessage.success('审批通过,已签发授权证书')
  load()
}
function onReject(row) {
  ElMessageBox.prompt('请输入驳回原因', '驳回', { confirmButtonText: '确定', cancelButtonText: '取消' })
    .then(async ({ value }) => { await rejectAuth(row.applyId, value); ElMessage.success('已驳回'); load() })
    .catch(() => {})
}

onMounted(load)
</script>
