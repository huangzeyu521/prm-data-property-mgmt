<template>
  <div class="prm-page">
    <div class="prm-query-bar">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="证书类型">
          <el-select v-model="q.certType" placeholder="全部" clearable style="width:150px">
            <el-option label="专项授权证书" value="专项授权证书" />
            <el-option label="批量授权证书" value="批量授权证书" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="q.templateStatus" placeholder="全部" clearable style="width:120px">
            <el-option label="生效中" value="生效中" /><el-option label="停用" value="停用" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
          <el-button type="primary" @click="onAdd">新增模板</el-button>
        </el-form-item>
      </el-form>
    </div>
    <div class="prm-table-card">
      <div class="prm-table-note" style="margin:0 0 12px 0">
        注:可研 3.2.2.1.1.3.4.2 —— 授权权益证书模板,支持专项/批量授权证书模板的配置与版本管理;出证时按模板自动填充。已用于出证的模板仅停用不删除。
      </div>
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column type="index" label="序号" width="64" align="center" />
        <el-table-column prop="templateName" label="模板名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="certType" label="证书类型" width="130" align="center">
          <template #default="{ row }"><el-tag :type="row.certType==='批量授权证书'?'warning':'primary'">{{ row.certType }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="rightType" label="适用权益类型" width="150" />
        <el-table-column prop="templateVersion" label="版本" width="70" align="center" />
        <el-table-column prop="templateStatus" label="状态" width="90" align="center">
          <template #default="{ row }"><el-tag :type="row.templateStatus==='生效中'?'success':'info'">{{ row.templateStatus }}</el-tag></template>
        </el-table-column>
        <el-table-column label="套版文件" min-width="150">
          <template #default="{ row }">
            <el-link v-if="row.fileName" type="primary" @click="onDownload(row)">{{ row.fileName }}</el-link>
            <span v-else style="color:#bbb">未上传</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="onEdit(row)">编辑</el-button>
            <el-upload :show-file-list="false" :http-request="(o)=>doUpload(row, o.file)" accept=".pdf,.doc,.docx,.png,.jpg,.jpeg" style="display:inline-block;margin:0 8px">
              <el-button link type="primary">上传</el-button>
            </el-upload>
            <el-button v-if="row.templateStatus==='生效中'" link type="warning" @click="onDisable(row)">停用</el-button>
            <el-button v-else link type="success" @click="onEnable(row)">启用</el-button>
            <el-button link type="danger" @click="onDel(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:16px;justify-content:flex-end" background layout="total, sizes, prev, pager, next, jumper" :page-sizes="[10, 20, 50, 100]"
        :total="total" :current-page="q.current" :page-size="q.size" @current-change="p=>{q.current=p;load()}" @size-change="s=>{q.size=s;q.current=1;load()}" />
    </div>

    <el-dialog v-model="dlg" :title="editing ? '编辑授权证书模板（更新将自增版本）' : '新增授权证书模板'" width="560px" align-center>
      <el-form :model="form" label-width="150px">
        <el-form-item label="模板名称" required><el-input v-model="form.templateName" /></el-form-item>
        <el-form-item label="证书类型">
          <el-select v-model="form.certType" style="width:100%">
            <el-option label="专项授权证书" value="专项授权证书" />
            <el-option label="批量授权证书" value="批量授权证书" />
          </el-select>
        </el-form-item>
        <el-form-item label="适用权益类型">
          <el-select v-model="form.rightType" style="width:100%">
            <el-option label="数据加工使用权" value="数据加工使用权" />
            <el-option label="数据产品经营权" value="数据产品经营权" />
          </el-select>
        </el-form-item>
        <el-form-item label="证书正文模板"><el-input v-model="form.templateContent" type="textarea" :rows="4" placeholder="可含占位符,如 {资产名称}/{被授权方}/{授权范围}/{有效期}" /></el-form-item>
      </el-form>
      <template #footer><el-button type="primary" @click="onSave">确定</el-button><el-button @click="dlg=false">取消</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { openFilePreview } from '@/composables/useFilePreview'
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  pageAuthCertTemplate, createAuthCertTemplate, updateAuthCertTemplate,
  enableAuthCertTemplate, disableAuthCertTemplate,
  deleteAuthCertTemplate, uploadAuthCertTemplateFile, authCertTemplateFileUrl
} from '@/api/authorize'

const q = reactive({ current: 1, size: 10, certType: '', templateStatus: '' })
const rows = ref([]); const total = ref(0); const loading = ref(false)
const dlg = ref(false); const editing = ref(false)
const form = reactive(empty())
function empty() { return { templateId: '', templateName: '', certType: '专项授权证书', rightType: '数据加工使用权', templateContent: '' } }

async function load() {
  loading.value = true
  try { const r = await pageAuthCertTemplate({ ...q }); rows.value = r.records || []; total.value = r.total || 0 }
  finally { loading.value = false }
}
function onAdd() { Object.assign(form, empty()); editing.value = false; dlg.value = true }
function onEdit(row) { Object.assign(form, { templateId: row.templateId, templateName: row.templateName, certType: row.certType, rightType: row.rightType, templateContent: row.templateContent || '' }); editing.value = true; dlg.value = true }
async function onSave() {
  if (!form.templateName) { ElMessage.warning('请输入模板名称'); return }
  if (editing.value) { await updateAuthCertTemplate({ ...form }); ElMessage.success('已更新（版本自增）') }
  else { await createAuthCertTemplate({ ...form }); ElMessage.success('已新增（v1 生效中）') }
  dlg.value = false; load()
}
async function onEnable(row) { await enableAuthCertTemplate(row.templateId); ElMessage.success('已启用'); load() }
async function onDisable(row) { await disableAuthCertTemplate(row.templateId); ElMessage.success('已停用'); load() }
async function doUpload(row, file) {
  const fd = new FormData(); fd.append('file', file)
  await uploadAuthCertTemplateFile(row.templateId, fd)
  ElMessage.success('套版文件已上传'); load()
}
function onDownload(row) { if (row.fileName) openFilePreview(authCertTemplateFileUrl(row.templateId), row.fileName) }
function onDel(row) {
  ElMessageBox.confirm('确认删除该证书模板吗', '提示', { type: 'warning' })
    .then(async () => { await deleteAuthCertTemplate(row.templateId); ElMessage.success('已删除'); load() }).catch(() => {})
}
onMounted(load)
</script>
