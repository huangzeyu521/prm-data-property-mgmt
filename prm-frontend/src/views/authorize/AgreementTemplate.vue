<template>
  <div class="prm-page">
    <div class="prm-query-bar">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="模板名称"><el-input v-model="q.templateName" placeholder="模板名称" clearable style="width:160px" /></el-form-item>
        <el-form-item label="授权类型">
          <el-select v-model="q.authType" placeholder="全部" clearable style="width:120px">
            <el-option v-for="t in authTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="使用目的">
          <el-select v-model="q.purpose" placeholder="全部" clearable style="width:120px">
            <el-option v-for="p in purposes" :key="p" :label="p" :value="p" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">查询</el-button>
          <el-button @click="onReset">重置</el-button>
          <el-button type="primary" @click="onAdd">新增模板</el-button>
        </el-form-item>
      </el-form>
    </div>
    <div class="prm-table-card">
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column type="index" label="序号" width="56" align="center" />
        <el-table-column prop="templateName" label="模板名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="authType" label="授权类型" width="100" align="center"><template #default="{ row }"><el-tag>{{ row.authType }}</el-tag></template></el-table-column>
        <el-table-column prop="purpose" label="使用目的" width="110" align="center" />
        <el-table-column prop="templateVersion" label="版本" width="70" align="center" />
        <el-table-column prop="templateStatus" label="状态" width="90" align="center">
          <template #default="{ row }"><el-tag :type="row.templateStatus==='生效中'?'success':'warning'">{{ row.templateStatus }}</el-tag></template>
        </el-table-column>
        <el-table-column label="套版文件" min-width="150">
          <template #default="{ row }">
            <el-link v-if="row.fileName" type="primary" @click="onDownload(row)">{{ row.fileName }}</el-link>
            <span v-else style="color:#bbb">未上传</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="onView(row)">查看</el-button>
            <el-button link type="primary" @click="onEdit(row)">修改</el-button>
            <el-upload :show-file-list="false" :http-request="(o)=>doUpload(row, o.file)" accept=".pdf,.doc,.docx,.png,.jpg,.jpeg" style="display:inline-block;margin:0 8px">
              <el-button link type="primary">上传</el-button>
            </el-upload>
            <el-button link type="success" :disabled="row.templateStatus==='生效中'" @click="onEnable(row)">启用</el-button>
            <el-button link type="warning" :disabled="row.templateStatus==='停用'" @click="onDisable(row)">停用</el-button>
            <el-button link type="danger" @click="onDel(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="prm-table-note">注:按授权类型/使用目的分类集中管理协议模板;修改保存自动版本自增;申请/签署时按类型选用匹配模板。</div>
      <el-pagination style="margin-top:16px;justify-content:flex-end" background layout="total, prev, pager, next"
        :total="total" :current-page="q.current" :page-size="q.size" @current-change="p=>{q.current=p;load()}" />
    </div>

    <el-dialog v-model="dlg" :title="form.templateId ? '修改协议模板（保存自增版本）' : '新增协议模板'" width="640px" align-center>
      <el-form :model="form" label-width="90px">
        <el-form-item label="模板名称"><el-input v-model="form.templateName" /></el-form-item>
        <el-form-item label="授权类型">
          <el-select v-model="form.authType" allow-create filterable style="width:220px"><el-option v-for="t in authTypes" :key="t" :label="t" :value="t" /></el-select>
        </el-form-item>
        <el-form-item label="使用目的">
          <el-select v-model="form.purpose" allow-create filterable style="width:220px"><el-option v-for="p in purposes" :key="p" :label="p" :value="p" /></el-select>
        </el-form-item>
        <el-form-item label="协议内容"><el-input v-model="form.templateContent" type="textarea" :rows="5" placeholder="协议正文/条款" /></el-form-item>
      </el-form>
      <template #footer><el-button type="primary" @click="onSave">确定</el-button><el-button @click="dlg=false">取消</el-button></template>
    </el-dialog>

    <el-dialog v-model="viewDlg" title="协议模板详情" width="620px" align-center>
      <el-descriptions v-if="cur" :column="2" border size="small">
        <el-descriptions-item label="模板名称" :span="2">{{ cur.templateName }}</el-descriptions-item>
        <el-descriptions-item label="授权类型">{{ cur.authType }}</el-descriptions-item>
        <el-descriptions-item label="使用目的">{{ cur.purpose }}</el-descriptions-item>
        <el-descriptions-item label="版本">{{ cur.templateVersion }}</el-descriptions-item>
        <el-descriptions-item label="套版文件"><el-link v-if="cur.fileName" type="primary" @click="onDownload(cur)">{{ cur.fileName }}</el-link><span v-else style="color:#bbb">未上传</span></el-descriptions-item>
        <el-descriptions-item label="协议内容" :span="2"><div style="white-space:pre-wrap">{{ cur.templateContent || '—' }}</div></el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  pageAgrTemplate, getAgrTemplate, createAgrTemplate, updateAgrTemplate, deleteAgrTemplate,
  enableAgrTemplate, disableAgrTemplate, uploadAgrTemplateFile, agrTemplateFileUrl
} from '@/api/authorize'

const authTypes = ['独占', '共享', '委托', '运营']
const purposes = ['内部分析', '对外服务', '联合建模', '监管报送']
const q = reactive({ current: 1, size: 10, templateName: '', authType: '', purpose: '' })
const rows = ref([]); const total = ref(0); const loading = ref(false)
const dlg = ref(false); const form = reactive({ templateId: '', templateName: '', authType: '运营', purpose: '对外服务', templateContent: '' })
const viewDlg = ref(false); const cur = ref(null)

async function load() {
  loading.value = true
  try { const r = await pageAgrTemplate({ ...q }); rows.value = r.records || []; total.value = r.total || 0 }
  finally { loading.value = false }
}
function onSearch() { q.current = 1; load() }
function onReset() { Object.assign(q, { templateName: '', authType: '', purpose: '' }); onSearch() }

function onAdd() { Object.assign(form, { templateId: '', templateName: '', authType: '运营', purpose: '对外服务', templateContent: '' }); dlg.value = true }
function onEdit(row) { Object.assign(form, { templateId: row.templateId, templateName: row.templateName, authType: row.authType, purpose: row.purpose, templateContent: row.templateContent || '' }); dlg.value = true }
async function onSave() {
  if (!form.templateName) { ElMessage.warning('请填写模板名称'); return }
  if (form.templateId) { await updateAgrTemplate({ ...form }); ElMessage.success('已修改，版本自增') }
  else { await createAgrTemplate({ ...form }); ElMessage.success('已新增') }
  dlg.value = false; load()
}
async function onView(row) { cur.value = await getAgrTemplate(row.templateId); viewDlg.value = true }
async function doUpload(row, file) {
  const fd = new FormData(); fd.append('file', file)
  await uploadAgrTemplateFile(row.templateId, fd)
  ElMessage.success('套版文件已上传'); load()
}
function onDownload(row) { if (row.fileName) window.open(agrTemplateFileUrl(row.templateId), '_blank') }
async function onEnable(row) { await enableAgrTemplate(row.templateId); ElMessage.success('已启用'); load() }
async function onDisable(row) { await disableAgrTemplate(row.templateId); ElMessage.success('已停用'); load() }
function onDel(row) {
  ElMessageBox.confirm('确认删除该协议模板吗', '提示', { type: 'warning' })
    .then(async () => { await deleteAgrTemplate(row.templateId); ElMessage.success('已删除'); load() }).catch(() => {})
}
onMounted(load)
</script>
