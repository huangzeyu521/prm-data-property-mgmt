<template>
  <div class="prm-page">
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
          <el-button type="primary" @click="onAdd">新增</el-button>
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
          <template #default="{ row }"><span v-if="row.fileName">{{ row.fileName }}</span><span v-else style="color:#bbb">（纯文本）</span></template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="onView(row)">查看</el-button>
            <el-button link type="primary" @click="onEdit(row)">修改</el-button>
            <el-button link type="success" :disabled="!row.fileName" @click="onDownload(row)">下载</el-button>
            <el-button link type="primary" @click="onVersions(row)">版本历史</el-button>
            <el-button link type="danger" @click="onDel(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="prm-table-note">注:列表仅显示各标题最新版本;同标题再次保存/上传将自动版本号自增并保留历史版本(可"版本历史"查看与回设)。</div>
      <el-pagination style="margin-top:16px;justify-content:flex-end" background layout="total, sizes, prev, pager, next, jumper" :page-sizes="[10, 20, 50, 100]"
        :total="total" :current-page="q.current" :page-size="q.size" @current-change="p=>{q.current=p;load()}" @size-change="s=>{q.size=s;q.current=1;load()}" />
    </div>

    <!-- 新增/修改 -->
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
          <span v-else style="color:#bbb">纯文本指引</span>
        </el-descriptions-item>
        <el-descriptions-item label="内容" :span="2"><div style="white-space:pre-wrap">{{ cur.content || '（无）' }}</div></el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 历史版本 -->
    <el-dialog v-model="verDlg" :title="`历史版本 — ${verTitle}`" width="620px" align-center>
      <el-table :data="verRows" border size="small">
        <el-table-column prop="version" label="版本" width="80" align="center" />
        <el-table-column prop="publisher" label="发布人" width="120" />
        <el-table-column label="发布日期" width="170"><template #default="{ row }">{{ fmt(row.publishDate) }}</template></el-table-column>
        <el-table-column label="最新" width="70" align="center"><template #default="{ row }"><el-tag v-if="row.isLatest" type="success" size="small">最新</el-tag></template></el-table-column>
        <el-table-column label="操作" width="170">
          <template #default="{ row }">
            <el-button link type="success" :disabled="!row.fileName" @click="onDownload(row)">下载</el-button>
            <el-button link type="primary" :disabled="row.isLatest" @click="onSetLatest(row)">设为最新</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { openFilePreview } from '@/composables/useFilePreview'
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  pageGuidance, saveGuidance, deleteGuidance, getGuidance,
  uploadGuidanceFile, guidanceVersions, setGuidanceLatest, guidanceDownloadUrl
} from '@/api/confirm'

const types = ['政策文件', '流程图', '材料样例', '操作说明', 'FAQ']
const q = reactive({ current: 1, size: 10, title: '', guidanceType: '' })
const rows = ref([]); const total = ref(0); const loading = ref(false)
const dlg = ref(false); const saving = ref(false)
const form = reactive({ guidanceId: '', title: '', guidanceType: '', publisher: '', content: '' })
let pickedFile = null
const viewDlg = ref(false); const cur = ref(null)
const verDlg = ref(false); const verTitle = ref(''); const verRows = ref([])

function fmt(t) { return t ? String(t).replace('T', ' ').slice(0, 19) : '-' }

async function load() {
  loading.value = true
  try { const r = await pageGuidance({ ...q, latestOnly: true }); rows.value = r.records || []; total.value = r.total || 0 }
  finally { loading.value = false }
}
function onSearch() { q.current = 1; load() }
function onReset() { q.title = ''; q.guidanceType = ''; onSearch() }

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
      await uploadGuidanceFile(fd)
      ElMessage.success('已上传并入库为新版本')
    } else {
      await saveGuidance({ ...form })
      ElMessage.success(form.guidanceId ? '已修改' : '已保存为新版本')
    }
    dlg.value = false; load()
  } finally { saving.value = false }
}

async function onView(row) { cur.value = await getGuidance(row.guidanceId); viewDlg.value = true }
function onDownload(row) { openFilePreview(guidanceDownloadUrl(row.guidanceId), row.fileName) }
async function onVersions(row) {
  verTitle.value = row.title
  verRows.value = await guidanceVersions(row.title)
  verDlg.value = true
}
async function onSetLatest(row) {
  await setGuidanceLatest(row.guidanceId)
  ElMessage.success(`已将 ${row.version} 设为最新`)
  verRows.value = await guidanceVersions(verTitle.value)
  load()
}
function onDel(row) {
  ElMessageBox.confirm('确认删除该指引版本吗', '提示', { type: 'warning' })
    .then(async () => { await deleteGuidance(row.guidanceId); ElMessage.success('已删除'); load() }).catch(() => {})
}
onMounted(load)
</script>
