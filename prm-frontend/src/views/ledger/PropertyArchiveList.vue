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
        <el-button type="primary" plain @click="batchDlg = true">批量新增</el-button>
        <el-button @click="onExport">导出</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column type="index" label="序号" width="64" align="center" />
        <el-table-column prop="assetName" label="资产名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="rightType" label="产权类型(三权分置·可多)" min-width="200">
          <template #default="{ row }">
            <el-tag v-for="t in splitRights(row.rightType)" :key="t" size="small" style="margin:1px 4px 1px 0">{{ t }}</el-tag>
            <span v-if="!splitRights(row.rightType).length" style="color:#8a8a8a">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="rightSubject" label="权利主体" min-width="150" show-overflow-tooltip />
        <el-table-column prop="respDept" label="责任部门" width="130" show-overflow-tooltip />
        <el-table-column prop="confirmStatus" label="确权状态(按权)" width="130" align="center">
          <template #default="{ row }">
            <el-tooltip v-if="row.confirmDetail" placement="top">
              <template #content>
                <div v-for="d in parseDetail(row.confirmDetail)" :key="d.right">{{ d.right }}：{{ d.status }}</div>
              </template>
              <el-tag :type="statusTag(row.confirmStatus)">{{ row.confirmStatus }}</el-tag>
            </el-tooltip>
            <el-tag v-else :type="statusTag(row.confirmStatus)">{{ row.confirmStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="onEdit(row)">修改</el-button>
            <el-button link type="danger" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="prm-table-note">注:产权档案依据"三权分置"建立"一数一档",删除前需先解除关联授权;新增/批量新增仅作登记(确权状态为"未确权"),确权状态由确权流程驱动;分省上报的数据产权确权通过后统一归口中国南方电网有限责任公司。</div>

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
          <el-select v-model="form.rightType" multiple placeholder="可多选(三权分置:一个数据集可同时主张多权)" style="width: 100%">
            <el-option v-for="t in rightTypes" :key="t" :label="t" :value="t" />
          </el-select>
          <div class="form-tip">三权分置:一个数据集可同时主张持有/使用/经营多权;登记仅为申报,各权确权状态由确权流程逐权驱动</div>
        </el-form-item>
        <el-form-item label="权利主体">
          <el-input v-model="form.rightSubject" placeholder="申报权利主体(分省上报最终归口网公司)" />
        </el-form-item>
        <el-form-item label="责任部门">
          <el-input v-model="form.respDept" placeholder="请输入责任部门" />
        </el-form-item>
        <el-form-item label="确权状态">
          <el-tag :type="statusTag(form.confirmStatus || '未确权')">{{ form.confirmStatus || '未确权' }}</el-tag>
          <span class="form-tip" style="margin-left:8px">确权状态由"数据确权管理"流程驱动变更,不可手工设定</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="onSubmit">确定</el-button>
        <el-button @click="dialogVisible = false">取消</el-button>
      </template>
    </el-dialog>

    <!-- 批量新增(评审7.3):多行粘贴登记,确权状态统一"未确权" -->
    <el-dialog v-model="batchDlg" title="批量新增产权档案" width="640px" align-center :close-on-click-modal="false">
      <el-alert type="info" :closable="false" style="margin-bottom:10px"
        title="每行一条:资产ID,资产名称[,产权类型][,权利主体][,责任部门];产权类型可多权用「|」分隔(如 数据资源持有权|数据加工使用权);批量登记各权确权状态统一「未确权」,由确权流程逐权驱动" />
      <el-input v-model="batchText" type="textarea" :rows="8"
        placeholder="DA-1001,营销用电量明细,数据资源持有权|数据加工使用权,中国南方电网有限责任公司,市场部&#10;DA-1002,配网负荷曲线" />
      <template #footer>
        <el-button type="primary" :loading="batchLoading" @click="onBatchCreate">批量登记</el-button>
        <el-button @click="batchDlg = false">取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageArchive, createArchive, updateArchive, deleteArchive } from '@/api/propertyArchive'

const rightTypes = ['数据资源持有权', '数据加工使用权', '数据产品经营权']
const confirmStatuses = ['未确权', '部分确权', '已确权']

// 产权类型多值串拆分(顿号/竖线等分隔,去重保序)——三权分置一数集可多权
function splitRights(joined) {
  if (!joined) return []
  return [...new Set(String(joined).split(/[、,，;；/|]/).map(s => s.trim()).filter(Boolean))]
}
// 按权确权明细解析:"持有权:已确权;使用权:未确权" -> [{right,status}]
function parseDetail(detail) {
  if (!detail) return []
  return String(detail).split(';').map(s => s.trim()).filter(Boolean).map(seg => {
    const i = seg.lastIndexOf(':')
    return i < 0 ? { right: seg, status: '未确权' } : { right: seg.slice(0, i).trim(), status: seg.slice(i + 1).trim() }
  })
}

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
  // rightType 在表单内为数组(多选),提交/编辑时与后端顿号串互转
  return { archiveId: '', assetId: '', assetName: '', rightType: [], rightSubject: '', respDept: '', confirmStatus: '' }
}

function statusTag(s) {
  return { 已确权: 'success', 部分确权: 'warning', 申请中: 'warning', 失败: 'danger' }[s] || 'info'
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
  form.confirmStatus = '未确权'
  dialogTitle.value = '新增产权档案'
  dialogVisible.value = true
}

// 批量新增(评审7.3):多行解析逐条登记,确权状态统一未确权
const batchDlg = ref(false)
const batchText = ref('')
const batchLoading = ref(false)
async function onBatchCreate() {
  const lines = batchText.value.split('\n').map(l => l.trim()).filter(Boolean)
  if (!lines.length) { ElMessage.warning('请粘贴至少一行数据'); return }
  batchLoading.value = true
  let ok = 0
  const fails = []
  try {
    for (const [i, line] of lines.entries()) {
      const [assetId, assetName, rightType, rightSubject, respDept] = line.split(/[,，]/).map(s => (s || '').trim())
      if (!assetId || !assetName) { fails.push(`第${i + 1}行:缺少资产ID或资产名称`); continue }
      // 产权类型多权用「|」分隔 -> 统一顿号串落库
      const rightTypeJoined = splitRights(rightType).join('、')
      try {
        await createArchive({ assetId, assetName, rightType: rightTypeJoined, rightSubject: rightSubject || '',
          respDept: respDept || '', confirmStatus: '未确权' })
        ok++
      } catch (e) { fails.push(`第${i + 1}行(${assetId}):${e?.response?.data?.message || '登记失败'}`) }
    }
  } finally { batchLoading.value = false }
  ElMessage[fails.length ? 'warning' : 'success'](`批量登记完成:成功 ${ok} 条` + (fails.length ? `,失败 ${fails.length} 条(${fails[0]}…)` : ''))
  if (ok) { batchDlg.value = false; batchText.value = ''; load() }
}
function onEdit(row) {
  Object.assign(form, row)
  form.rightType = splitRights(row.rightType) // 顿号串 -> 多选数组
  dialogTitle.value = '修改产权档案'
  dialogVisible.value = true
}

async function onSubmit() {
  await formRef.value.validate()
  const rightTypeJoined = Array.isArray(form.rightType) ? form.rightType.join('、') : (form.rightType || '')
  if (form.archiveId) {
    await updateArchive({ ...form, rightType: rightTypeJoined })
    ElMessage.success('保存成功')
  } else {
    // 新增仅登记:各权确权状态由确权流程逐权驱动(评审7.1/7.4)
    await createArchive({ ...form, rightType: rightTypeJoined, confirmStatus: '未确权' })
    ElMessage.success('新增成功(各权未确权,请通过确权申请流程逐权确权)')
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

<style scoped>
.form-tip { font-size: 12px; color: #909399; line-height: 1.6; }
</style>
