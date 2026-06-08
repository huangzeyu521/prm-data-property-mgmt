<template>
  <div class="prm-page">
    <!-- 查询条件区(查询条件不带必填星号) -->
    <div class="prm-query-bar">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="资产名称">
          <el-input v-model="query.assetName" placeholder="请输入资产名称" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="产权类型">
          <el-select v-model="query.rightType" placeholder="全部" clearable style="width: 180px">
            <el-option v-for="t in rightTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="确权状态">
          <el-select v-model="query.confirmStatus" placeholder="全部" clearable style="width: 160px">
            <el-option v-for="s in confirmStatuses" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">查询</el-button>
          <el-button @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 列表区 -->
    <div class="prm-table-card">
      <div style="margin-bottom: 12px">
        <el-button type="primary" @click="onCreate">新增</el-button>
        <el-button @click="onExport">导出</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column type="index" label="序号" width="64" align="center" />
        <el-table-column prop="assetName" label="资产名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="rightType" label="产权类型" width="140" />
        <el-table-column prop="rightSubject" label="权利主体" min-width="160" show-overflow-tooltip />
        <el-table-column prop="respDept" label="责任部门" width="140" show-overflow-tooltip />
        <el-table-column prop="confirmStatus" label="确权状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.confirmStatus)">{{ row.confirmStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="onEdit(row)">修改</el-button>
            <el-button link type="danger" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="prm-table-note">注:产权档案依据"三权分置"建立"一数一档",删除前需先解除关联授权。</div>

      <el-pagination
        style="margin-top: 16px; justify-content: flex-end"
        background
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        :current-page="query.current"
        :page-size="query.size"
        :page-sizes="[10, 20, 50, 100]"
        @current-change="onPageChange"
        @size-change="onSizeChange"
      />
    </div>

    <!-- 新增/修改弹窗(屏幕居中、屏蔽背景) -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" align-center :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="关联资产ID" prop="assetId">
          <el-input v-model="form.assetId" placeholder="请输入关联资产ID" />
        </el-form-item>
        <el-form-item label="资产名称" prop="assetName">
          <el-input v-model="form.assetName" placeholder="请输入资产名称" />
        </el-form-item>
        <el-form-item label="产权类型">
          <el-select v-model="form.rightType" placeholder="请选择" style="width: 100%">
            <el-option v-for="t in rightTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="权利主体">
          <el-input v-model="form.rightSubject" placeholder="请输入权利主体" />
        </el-form-item>
        <el-form-item label="责任部门">
          <el-input v-model="form.respDept" placeholder="请输入责任部门" />
        </el-form-item>
        <el-form-item label="确权状态">
          <el-select v-model="form.confirmStatus" placeholder="请选择" style="width: 100%">
            <el-option v-for="s in confirmStatuses" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="onSubmit">确定</el-button>
        <el-button @click="dialogVisible = false">取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageArchive, createArchive, updateArchive, deleteArchive } from '@/api/propertyArchive'

const rightTypes = ['数据资源持有权', '数据加工使用权', '数据产品经营权']
const confirmStatuses = ['未确权', '申请中', '已确权', '失败']

const query = reactive({ current: 1, size: 10, assetName: '', rightType: '', confirmStatus: '' })
const rows = ref([])
const total = ref(0)
const loading = ref(false)

const dialogVisible = ref(false)
const dialogTitle = ref('新增产权档案')
const formRef = ref()
const form = reactive(emptyForm())
const rules = {
  assetId: [{ required: true, message: '请输入关联资产ID', trigger: 'blur' }],
  assetName: [{ required: true, message: '请输入资产名称', trigger: 'blur' }]
}

function emptyForm() {
  return { archiveId: '', assetId: '', assetName: '', rightType: '', rightSubject: '', respDept: '', confirmStatus: '' }
}

function statusTag(s) {
  return { 已确权: 'success', 申请中: 'warning', 失败: 'danger' }[s] || 'info'
}

async function load() {
  loading.value = true
  try {
    const res = await pageArchive({ ...query })
    rows.value = res.records || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

function onSearch() {
  query.current = 1
  load()
}
function onReset() {
  query.assetName = ''
  query.rightType = ''
  query.confirmStatus = ''
  onSearch()
}
function onPageChange(p) {
  query.current = p
  load()
}
function onSizeChange(s) {
  query.size = s
  query.current = 1
  load()
}

function onCreate() {
  Object.assign(form, emptyForm())
  dialogTitle.value = '新增产权档案'
  dialogVisible.value = true
}
function onEdit(row) {
  Object.assign(form, row)
  dialogTitle.value = '修改产权档案'
  dialogVisible.value = true
}

async function onSubmit() {
  await formRef.value.validate()
  if (form.archiveId) {
    await updateArchive({ ...form })
    ElMessage.success('保存成功')
  } else {
    await createArchive({ ...form })
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  load()
}

function onDelete(row) {
  ElMessageBox.confirm(`确认删除产权档案"${row.assetName}"吗`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await deleteArchive(row.archiveId)
    ElMessage.success('删除成功')
    load()
  }).catch(() => {})
}

function onExport() {
  ElMessage.info('导出功能将按当前筛选条件生成 Excel(后续接入导出服务)')
}

onMounted(load)
</script>
