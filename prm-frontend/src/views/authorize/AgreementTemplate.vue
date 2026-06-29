<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
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
            <span v-else style="color:var(--prm-color-text-disabled)">未上传</span>
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
      <el-pagination style="margin-top:16px;justify-content:flex-end" background layout="total, sizes, prev, pager, next, jumper" :page-sizes="[10, 20, 50, 100]"
        :total="total" :current-page="q.current" :page-size="q.size" @current-change="p=>{q.current=p;load()}" @size-change="s=>{q.size=s;q.current=1;load()}" />
    </div>

    <el-dialog v-model="dlg" :title="form.templateId ? '修改协议模板（保存自增版本）' : '新增协议模板'" width="640px" align-center>
      <el-form :model="form" label-width="100px">
        <el-form-item label="模板名称"><el-input v-model="form.templateName" /></el-form-item>
        <el-form-item label="授权类型">
          <el-select v-model="form.authType" allow-create filterable style="width:220px"><el-option v-for="t in authTypes" :key="t" :label="t" :value="t" /></el-select>
        </el-form-item>
        <el-form-item label="使用目的">
          <el-select v-model="form.purpose" allow-create filterable style="width:220px"><el-option v-for="p in purposes" :key="p" :label="p" :value="p" /></el-select>
        </el-form-item>
        <el-form-item label="协议内容">
          <div style="width:100%">
            <div style="margin-bottom:6px">
              <el-button size="small" type="warning" plain @click="loadStdClauses">载入附录D §3.4.4 标准协议条款</el-button>
              <el-tag v-if="missingElements.length===0" type="success" size="small" effect="plain" style="margin-left:8px">§3.4.4 五要素已覆盖 ✓</el-tag>
              <el-tag v-else type="danger" size="small" effect="plain" style="margin-left:8px">缺 §3.4.4 要素:{{ missingElements.join('、') }}</el-tag>
            </div>
            <el-input v-model="form.templateContent" type="textarea" :rows="8" placeholder="协议正文/条款(须含 数据范围/使用场景及目的/利益分配/安全保障)" />
          </div>
        </el-form-item>
      </el-form>
      <template #footer><el-button type="primary" @click="onSave">确定</el-button><el-button @click="dlg=false">取消</el-button></template>
    </el-dialog>

    <el-dialog v-model="viewDlg" title="协议模板详情" width="620px" align-center>
      <el-descriptions v-if="cur" :column="2" border size="small">
        <el-descriptions-item label="模板名称" :span="2">{{ cur.templateName }}</el-descriptions-item>
        <el-descriptions-item label="授权类型">{{ cur.authType }}</el-descriptions-item>
        <el-descriptions-item label="使用目的">{{ cur.purpose }}</el-descriptions-item>
        <el-descriptions-item label="版本">{{ cur.templateVersion }}</el-descriptions-item>
        <el-descriptions-item label="套版文件"><el-link v-if="cur.fileName" type="primary" @click="onDownload(cur)">{{ cur.fileName }}</el-link><span v-else style="color:var(--prm-color-text-disabled)">未上传</span></el-descriptions-item>
        <el-descriptions-item label="协议内容" :span="2"><div style="white-space:pre-wrap">{{ cur.templateContent || '—' }}</div></el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { openFilePreview } from '@/composables/useFilePreview'
import { onMounted, reactive, ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  pageAgrTemplate, getAgrTemplate, createAgrTemplate, updateAgrTemplate, deleteAgrTemplate,
  enableAgrTemplate, disableAgrTemplate, uploadAgrTemplateFile, agrTemplateFileUrl
} from '@/api/authorize'

// 协议=附录D《运营授权协议》,按授权方式(一事一议/批量)分;取代旧 独占/共享/委托/运营 泛化分类
const authTypes = ['一事一议', '批量']
const purposes = ['内部分析', '对外服务', '联合建模', '监管报送']
const q = reactive({ current: 1, size: 10, templateName: '', authType: '', purpose: '' })
const rows = ref([]); const total = ref(0); const loading = ref(false)
const dlg = ref(false); const form = reactive({ templateId: '', templateName: '', authType: '一事一议', purpose: '对外服务', templateContent: '' })

// 附录D §3.4.4 五要素(协议须约定);用于「要素覆盖」提示
const D344_ELEMENTS = ['数据范围', '使用场景及目的', '利益分配', '安全保障']
const missingElements = computed(() => D344_ELEMENTS.filter(e => !(form.templateContent || '').includes(e)))
// 一键载入附录D §3.4.4 标准协议条款骨架(含五要素占位符),按授权方式略有差异
function loadStdClauses() {
  const isBatch = form.authType === '批量'
  const dataRange = isBatch
    ? '甲方授予乙方的数据范围以本协议附件《数据授权清单》逐表列明为准。'
    : '数据范围:{{授权数据表}}(所属系统/模式/库表),授权字段范围 {{授权范围}},不得超出确权边界。'
  form.templateContent =
`《南方电网数据授权运营协议》(附录D)
授权方(甲方):{{授权单位}}　被授权方(乙方):{{被授权单位}}
授权方式:${form.authType}　授权权益类型:{{权益类型}}

一、数据范围
${dataRange}

二、使用场景及目的
乙方仅可将授权数据用于:{{使用场景及目的}};不得超出约定场景/目的使用,不得再授权第三方。

三、授权期限
自协议生效之日起 {{授权期限}}(默认两年,不超确权有效期);到期数据销毁并出具证明。

四、利益分配
双方就授权数据的利益分配约定如下:{{利益分配}}(如:免费内部共享/按调用次数计费/收益按比例分成)。

五、安全保障
乙方应落实数据安全保障措施:{{安全保障}}(加密传输、最小授权访问控制、操作留痕审计、数据脱敏);因乙方原因致数据泄露的,承担相应责任。

六、合规与备案
${isBatch ? '本批量授权依《数据批量授权清单》整体签订一份协议;' : ''}涉数据产品经营权对外提供的,乙方须在甲方处备案(附录G);经营权授权范围仅限对外开放目录。

七、违约责任 / 八、争议解决 / 九、其他
(略,按公司合同范本补充)`
}
const viewDlg = ref(false); const cur = ref(null)

async function load() {
  loading.value = true
  try { const r = await pageAgrTemplate({ ...q }); rows.value = r.records || []; total.value = r.total || 0 }
  finally { loading.value = false }
}
function onSearch() { q.current = 1; load() }
function onReset() { Object.assign(q, { templateName: '', authType: '', purpose: '' }); onSearch() }

function onAdd() { Object.assign(form, { templateId: '', templateName: '', authType: '一事一议', purpose: '对外服务', templateContent: '' }); dlg.value = true }
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
function onDownload(row) { if (row.fileName) openFilePreview(agrTemplateFileUrl(row.templateId), row.fileName) }
async function onEnable(row) { await enableAgrTemplate(row.templateId); ElMessage.success('已启用'); load() }
async function onDisable(row) { await disableAgrTemplate(row.templateId); ElMessage.success('已停用'); load() }
function onDel(row) {
  ElMessageBox.confirm('确认删除该协议模板吗', '提示', { type: 'warning' })
    .then(async () => { await deleteAgrTemplate(row.templateId); ElMessage.success('已删除'); load() }).catch(() => {})
}
onMounted(load)
</script>
