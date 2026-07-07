<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
<!-- 指引材料管理(参数化):domain=confirm|auth 决定接口与类型枚举。消费(查看/下载)全员可用,维护(增删改/版本)仅 admin。 -->
<template>
  <div>
    <div class="prm-query-bar">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="标题"><el-input v-model="q.title" placeholder="指引标题" clearable style="width:200px" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="q.guidanceType" placeholder="全部" clearable style="width:140px">
            <el-option v-for="t in types" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">查询</el-button>
          <el-button @click="onReset">重置</el-button>
          <el-button v-if="canManage" type="primary" @click="onAdd">新增</el-button>
        </el-form-item>
      </el-form>
    </div>
    <div class="prm-table-card">
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column type="index" label="序号" width="56" align="center" />
        <el-table-column prop="title" label="标题" min-width="190" show-overflow-tooltip />
        <el-table-column prop="guidanceType" label="类型" width="110" />
        <el-table-column prop="version" label="版本" width="70" align="center" />
        <el-table-column prop="publisher" label="发布人" width="110" />
        <el-table-column label="原件" width="120" show-overflow-tooltip>
          <template #default="{ row }"><span v-if="row.fileName">{{ row.fileName }}</span><span v-else style="color:var(--prm-color-text-disabled)">（纯文本）</span></template>
        </el-table-column>
        <el-table-column label="操作" :width="canManage ? 340 : 190" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :disabled="!row.fileName" @click="onPreview(row)">阅览</el-button>
            <el-button link type="primary" @click="onView(row)">详情</el-button>
            <el-button link type="success" :disabled="!row.fileName" @click="onDownload(row)">下载</el-button>
            <template v-if="canManage">
              <el-button link type="primary" @click="onEdit(row)">修改</el-button>
              <el-button link type="primary" @click="onVersions(row)">版本历史</el-button>
              <el-button link type="danger" @click="onDel(row)">删除</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
      <div class="prm-table-note">注:列表仅显示各标题最新版本;同标题再次保存/上传将自动版本号自增并保留历史版本(可"版本历史"查看与回设)。</div>
      <el-pagination style="margin-top:20px;justify-content:flex-end" background layout="total, sizes, prev, pager, next, jumper" :page-sizes="[10, 20, 50, 100]"
        :total="total" :current-page="q.current" :page-size="q.size" @current-change="p=>{q.current=p;load()}" @size-change="s=>{q.size=s;q.current=1;load()}" />
    </div>

    <!-- 新增/修改(仅 admin) -->
    <el-dialog v-model="dlg" :title="form.guidanceId ? '修改指引材料' : '新增指引材料'" width="540px" align-center>
      <el-form :model="form" label-width="100px">
        <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.guidanceType" style="width:100%">
            <el-option v-for="t in types" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="发布人"><el-input v-model="form.publisher" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="form.content" type="textarea" maxlength="500" show-word-limit :rows="3" /></el-form-item>
        <el-form-item v-if="!form.guidanceId" label="上传文件">
          <el-upload :auto-upload="false" :limit="1" :on-change="onFilePick" :on-remove="()=>{ pickedFile=null }">
            <el-button>选择文件(流程图/样例/政策PDF)</el-button>
            <template #tip><span style="color:var(--prm-color-text-secondary);font-size:12px;margin-left:8px">选文件则按"上传"入库为新版本;不选则纯文本</span></template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer><el-button type="primary" :loading="saving" @click="onSave">确定</el-button><el-button @click="dlg=false">取消</el-button></template>
    </el-dialog>

    <!-- 在线查看 -->
    <el-dialog v-model="viewDlg" title="指引材料详情" width="560px" align-center>
      <el-descriptions v-if="cur" :column="2" border size="small">
        <el-descriptions-item label="标题" :span="2">{{ cur.title }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ cur.guidanceType }}</el-descriptions-item>
        <el-descriptions-item label="版本">{{ cur.version }}</el-descriptions-item>
        <el-descriptions-item label="发布人">{{ cur.publisher }}</el-descriptions-item>
        <el-descriptions-item label="发布日期">{{ fmt(cur.publishDate) }}</el-descriptions-item>
        <el-descriptions-item label="原件" :span="2">
          <el-link v-if="cur.fileName" type="primary" @click="onDownload(cur)">{{ cur.fileName }}（下载）</el-link>
          <span v-else style="color:var(--prm-color-text-disabled)">纯文本指引</span>
        </el-descriptions-item>
        <el-descriptions-item label="内容" :span="2"><div style="white-space:pre-wrap">{{ cur.content || '（无）' }}</div></el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 历史版本(仅 admin) -->
    <el-dialog v-model="verDlg" :title="`历史版本 — ${verTitle}`" width="620px" align-center>
      <el-table :data="verRows" border size="small">
        <el-table-column prop="version" label="版本" width="80" align="center" />
        <el-table-column prop="publisher" label="发布人" width="120" />
        <el-table-column label="发布日期" width="170"><template #default="{ row }">{{ fmt(row.publishDate) }}</template></el-table-column>
        <el-table-column label="最新" width="70" align="center"><template #default="{ row }"><span v-if="row.isLatest" class="prm-c-success">最新</span></template></el-table-column>
        <el-table-column label="操作" width="170">
          <template #default="{ row }">
            <el-button link type="success" :disabled="!row.fileName" @click="onDownload(row)">下载</el-button>
            <el-button link type="primary" :disabled="row.isLatest" @click="onSetLatest(row)">设为最新</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 在线阅览(PDF 内嵌) -->
    <el-dialog v-model="pvDlg" :title="pvTitle" width="82%" top="5vh" class="gt-pv">
      <iframe v-if="pvUrl" :src="pvUrl" class="gt-frame" title="指引在线阅览"></iframe>
    </el-dialog>
  </div>
</template>

<script setup>
import { openFilePreview } from '@/composables/useFilePreview'
import { useTablePage } from '@/composables/useTablePage'
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { confirmAsync } from '@/utils/confirmAsync'
import { currentRole } from '@/lib/roles'
import {
  pageGuidance, saveGuidance, deleteGuidance, getGuidance,
  uploadGuidanceFile, guidanceVersions, setGuidanceLatest, guidanceDownloadUrl, guidancePreviewUrl
} from '@/api/confirm'
import {
  pageAuthGuidance, saveAuthGuidance, deleteAuthGuidance, getAuthGuidance,
  uploadAuthGuidanceFile, authGuidanceVersions, setAuthGuidanceLatest, authGuidanceDownloadUrl, authGuidancePreviewUrl
} from '@/api/authorize'

const props = defineProps({
  domain: { type: String, required: true }, // 'confirm' | 'auth'
  excludeType: { type: String, default: '' } // 排除某类型(确权 Tab 排除"工作指引"存档)
})
const isAuth = props.domain === 'auth'

// 类型枚举按域差异(对齐各自可研口径)
const types = isAuth
  ? ['政策文件', '流程图', '常见问答', '申请步骤', '材料样例']
  : ['政策文件', '流程图', '材料样例', '操作说明', 'FAQ']

// 接口按域取(授权侧统一走 /dpr/auth/guidance,与抽屉旧 catalog 源不一致问题由此消除)
const api = isAuth
  ? { page: pageAuthGuidance, save: saveAuthGuidance, del: deleteAuthGuidance, get: getAuthGuidance, upload: uploadAuthGuidanceFile, versions: authGuidanceVersions, setLatest: setAuthGuidanceLatest, downloadUrl: authGuidanceDownloadUrl, previewUrl: authGuidancePreviewUrl }
  : { page: pageGuidance, save: saveGuidance, del: deleteGuidance, get: getGuidance, upload: uploadGuidanceFile, versions: guidanceVersions, setLatest: setGuidanceLatest, downloadUrl: guidanceDownloadUrl, previewUrl: guidancePreviewUrl }

// 维护权:配置管理员(admin)及"全部·管理员视图"(all,全站约定全权)可新增/修改/版本/删除
const canManage = computed(() => ['all', 'admin'].includes(currentRole()))
const { query: q, rows, total, loading, load, search: onSearch, reset: onReset } = useTablePage(
  (p) => api.page({ ...p, excludeType: props.excludeType || undefined, latestOnly: true }),
  { title: '', guidanceType: '' }
)
const dlg = ref(false); const saving = ref(false)
const form = reactive({ guidanceId: '', title: '', guidanceType: '', publisher: '', content: '' })
let pickedFile = null
const viewDlg = ref(false); const cur = ref(null)
const verDlg = ref(false); const verTitle = ref(''); const verRows = ref([])

function fmt(t) { return t ? String(t).replace('T', ' ').slice(0, 19) : '-' }

function onAdd() { Object.assign(form, { guidanceId: '', title: '', guidanceType: '', publisher: '', content: '' }); pickedFile = null; dlg.value = true }
function onEdit(row) { Object.assign(form, { guidanceId: row.guidanceId, title: row.title, guidanceType: row.guidanceType, publisher: row.publisher, content: row.content || '' }); pickedFile = null; dlg.value = true }
function onFilePick(file) { pickedFile = file.raw }

async function onSave() {
  if (!form.title) { ElMessage.warning('请填写标题'); return }
  saving.value = true
  try {
    if (!form.guidanceId && pickedFile) {
      const fd = new FormData()
      fd.append('file', pickedFile)
      fd.append('title', form.title)
      fd.append('guidanceType', form.guidanceType || '')
      fd.append('publisher', form.publisher || '')
      fd.append('content', form.content || '')
      await api.upload(fd)
      ElMessage.success('已上传并入库为新版本')
    } else {
      await api.save({ ...form })
      ElMessage.success(form.guidanceId ? '已修改' : '已保存为新版本')
    }
    dlg.value = false; load()
  } finally { saving.value = false }
}

async function onView(row) { cur.value = await api.get(row.guidanceId); viewDlg.value = true }
function onDownload(row) { openFilePreview(api.downloadUrl(row.guidanceId), row.fileName) }

// 在线阅览:PDF 内嵌弹层;非 PDF 自动下载查看(与工作指引一致)
const pvDlg = ref(false); const pvUrl = ref(''); const pvTitle = ref('')
function onPreview(row) {
  if (!row.fileName) { ElMessage.warning('该指引无可阅览的原件(纯文本)'); return }
  if (row.fileName.toLowerCase().endsWith('.pdf')) {
    pvUrl.value = api.previewUrl(row.guidanceId); pvTitle.value = row.title; pvDlg.value = true
  } else {
    ElMessage.info('Word 等文档暂不支持内嵌预览,已为您下载查看')
    openFilePreview(api.downloadUrl(row.guidanceId), row.fileName)
  }
}
async function onVersions(row) {
  verTitle.value = row.title
  verRows.value = await api.versions(row.title)
  verDlg.value = true
}
async function onSetLatest(row) {
  await api.setLatest(row.guidanceId)
  ElMessage.success(`已将 ${row.version} 设为最新`)
  verRows.value = await api.versions(verTitle.value)
  load()
}
function onDel(row) {
  confirmAsync('确认删除该指引版本吗', '提示',
    async () => { await api.del(row.guidanceId); ElMessage.success('已删除'); load() }).catch(() => {})
}
onMounted(load)
</script>

<style scoped>
.gt-frame { width: 100%; height: 76vh; border: none; }
</style>
<style>
.gt-pv .el-dialog__body { padding: 8px 16px 16px; }
</style>
