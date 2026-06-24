<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
<template>
  <div class="prm-page">
    <div class="prm-query-bar">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="模板名称"><el-input v-model="q.templateName" placeholder="模板名称" clearable style="width:180px" /></el-form-item>
        <el-form-item label="授权类型">
          <el-select v-model="q.authType" placeholder="全部" clearable style="width:130px">
            <el-option v-for="t in authTypes" :key="t" :label="t" :value="t" />
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
        <el-table-column prop="templateName" label="模板名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="authType" label="授权类型" width="110" align="center">
          <template #default="{ row }"><el-tag>{{ row.authType }}</el-tag></template>
        </el-table-column>
        <el-table-column label="字段数" width="80" align="center"><template #default="{ row }">{{ fieldCount(row) }}</template></el-table-column>
        <el-table-column prop="templateVersion" label="版本" width="70" align="center" />
        <el-table-column prop="templateStatus" label="状态" width="90" align="center">
          <template #default="{ row }"><el-tag :type="row.templateStatus==='生效中'?'success':'warning'">{{ row.templateStatus }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="onView(row)">查看</el-button>
            <el-button link type="primary" @click="onEdit(row)">修改</el-button>
            <el-button link type="success" :disabled="row.templateStatus==='生效中'" @click="onEnable(row)">启用</el-button>
            <el-button link type="warning" :disabled="row.templateStatus==='停用'" @click="onDisable(row)">停用</el-button>
            <el-button link type="danger" @click="onDel(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="prm-table-note">注:按授权类型(独占/共享/委托)配置申请表单的字段、流程与验证规则;修改保存自动版本自增。</div>
      <el-pagination style="margin-top:16px;justify-content:flex-end" background layout="total, sizes, prev, pager, next, jumper" :page-sizes="[10, 20, 50, 100]"
        :total="total" :current-page="q.current" :page-size="q.size" @current-change="p=>{q.current=p;load()}" @size-change="s=>{q.size=s;q.current=1;load()}" />
    </div>

    <!-- 新增/修改 -->
    <el-dialog v-model="dlg" :title="form.templateId ? '修改申请模板（保存自增版本）' : '新增申请模板'" width="760px" align-center>
      <el-form :model="form" label-width="100px">
        <el-form-item label="模板名称"><el-input v-model="form.templateName" /></el-form-item>
        <el-form-item label="授权类型">
          <el-select v-model="form.authType" style="width:220px">
            <el-option v-for="t in authTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="字段配置">
          <div style="width:100%">
            <el-button size="small" type="primary" plain @click="addField" style="margin-bottom:8px">+ 添加字段</el-button>
            <el-table :data="form.fields" border size="small">
              <el-table-column label="字段名" width="120"><template #default="{ row }"><el-input v-model="row.name" size="small" /></template></el-table-column>
              <el-table-column label="标签" width="130"><template #default="{ row }"><el-input v-model="row.label" size="small" /></template></el-table-column>
              <el-table-column label="类型" width="120"><template #default="{ row }">
                <el-select v-model="row.type" size="small"><el-option v-for="t in fieldTypes" :key="t" :label="t" :value="t" /></el-select>
              </template></el-table-column>
              <el-table-column label="必填" width="70" align="center"><template #default="{ row }"><el-switch v-model="row.required" size="small" /></template></el-table-column>
              <el-table-column label="验证规则"><template #default="{ row }"><el-input v-model="row.rule" size="small" placeholder="如:非空/不得超出确权边界" /></template></el-table-column>
              <el-table-column label="" width="50" align="center"><template #default="{ $index }"><el-button link type="danger" @click="form.fields.splice($index,1)">删</el-button></template></el-table-column>
            </el-table>
          </div>
        </el-form-item>
        <el-form-item label="流程说明"><el-input v-model="form.flowDesc" type="textarea" maxlength="500" show-word-limit :rows="2" placeholder="如:填报申请 → 合规审核 → 主管审批 → 签订协议 → 发证" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" /></el-form-item>
      </el-form>
      <template #footer><el-button type="primary" :loading="saving" @click="onSave">确定</el-button><el-button @click="dlg=false">取消</el-button></template>
    </el-dialog>

    <!-- 查看 -->
    <el-dialog v-model="viewDlg" title="申请模板详情" width="700px" align-center>
      <el-descriptions v-if="cur" :column="2" border size="small">
        <el-descriptions-item label="模板名称" :span="2">{{ cur.templateName }}</el-descriptions-item>
        <el-descriptions-item label="授权类型">{{ cur.authType }}</el-descriptions-item>
        <el-descriptions-item label="版本">{{ cur.templateVersion }}</el-descriptions-item>
        <el-descriptions-item label="流程说明" :span="2">{{ cur.flowDesc || '-' }}</el-descriptions-item>
      </el-descriptions>
      <div style="font-weight:600;margin:14px 0 8px">字段与验证规则</div>
      <el-table :data="curFields" border size="small">
        <el-table-column prop="label" label="标签" width="140" />
        <el-table-column prop="name" label="字段名" width="120" />
        <el-table-column prop="type" label="类型" width="100" align="center" />
        <el-table-column label="必填" width="70" align="center"><template #default="{ row }"><el-tag :type="row.required?'danger':'info'" size="small">{{ row.required?'必填':'选填' }}</el-tag></template></el-table-column>
        <el-table-column prop="rule" label="验证规则" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  pageApplyTemplate, getApplyTemplate, createApplyTemplate, updateApplyTemplate,
  deleteApplyTemplate, enableApplyTemplate, disableApplyTemplate
} from '@/api/authorize'

const authTypes = ['独占', '共享', '委托']
const fieldTypes = ['文本', '多行文本', '数字', '日期', '下拉']
const q = reactive({ current: 1, size: 10, templateName: '', authType: '' })
const rows = ref([]); const total = ref(0); const loading = ref(false)
const dlg = ref(false); const saving = ref(false)
const form = reactive({ templateId: '', templateName: '', authType: '独占', fields: [], flowDesc: '', remark: '' })
const viewDlg = ref(false); const cur = ref(null); const curFields = ref([])

function parseFields(json) { try { return JSON.parse(json || '[]') } catch { return [] } }
function fieldCount(row) { return parseFields(row.fieldsJson).length }

async function load() {
  loading.value = true
  try { const r = await pageApplyTemplate({ ...q }); rows.value = r.records || []; total.value = r.total || 0 }
  finally { loading.value = false }
}
function onSearch() { q.current = 1; load() }
function onReset() { q.templateName = ''; q.authType = ''; onSearch() }

function addField() { form.fields.push({ name: '', label: '', type: '文本', required: true, rule: '' }) }
function onAdd() { Object.assign(form, { templateId: '', templateName: '', authType: '独占', fields: [], flowDesc: '', remark: '' }); dlg.value = true }
function onEdit(row) {
  Object.assign(form, {
    templateId: row.templateId, templateName: row.templateName, authType: row.authType,
    fields: parseFields(row.fieldsJson), flowDesc: row.flowDesc || '', remark: row.remark || ''
  })
  dlg.value = true
}
async function onSave() {
  if (!form.templateName) { ElMessage.warning('请填写模板名称'); return }
  saving.value = true
  const payload = { ...form, fieldsJson: JSON.stringify(form.fields) }
  delete payload.fields
  try {
    if (form.templateId) { await updateApplyTemplate(payload); ElMessage.success('已修改，版本自增') }
    else { await createApplyTemplate(payload); ElMessage.success('已新增') }
    dlg.value = false; load()
  } finally { saving.value = false }
}
async function onView(row) { cur.value = await getApplyTemplate(row.templateId); curFields.value = parseFields(cur.value.fieldsJson); viewDlg.value = true }
async function onEnable(row) { await enableApplyTemplate(row.templateId); ElMessage.success('已启用'); load() }
async function onDisable(row) { await disableApplyTemplate(row.templateId); ElMessage.success('已停用'); load() }
function onDel(row) {
  ElMessageBox.confirm('确认删除该申请模板吗', '提示', { type: 'warning' })
    .then(async () => { await deleteApplyTemplate(row.templateId); ElMessage.success('已删除'); load() }).catch(() => {})
}
onMounted(load)
</script>
