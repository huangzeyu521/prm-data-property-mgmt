<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
<!-- 指引中心:工作指引(官方原件存档+在线阅览,admin 上传/删除)/流程表单(表1-6)/确权指引/授权指引。取代原顶栏抽屉+两套分散管理页。 -->
<template>
  <div class="prm-page gc">
    <el-tabs v-model="tab" class="gc-tabs">
      <!-- 工作指引:官方原件存档 + 在线阅览 -->
      <el-tab-pane label="工作指引" name="guide">
        <!-- 当前生效 -->
        <div class="wg-hero">
          <div class="wg-hero-main">
            <el-tag type="success" size="small" effect="dark">当前生效</el-tag>
            <div class="wg-hero-title">{{ current.title }}</div>
            <div class="wg-hero-meta">
              <span v-if="current.fileName">{{ current.fileName }}</span>
              <template v-if="current.publishDate"> · 上传 {{ fmt(current.publishDate) }}</template>
              <template v-if="current.publisher"> · {{ current.publisher }}</template>
              <el-tag v-if="current.bundled" size="small" type="info" effect="plain" style="margin-left:8px">系统内置·官方PDF</el-tag>
            </div>
          </div>
          <div class="wg-hero-actions">
            <el-button type="primary" @click="onPreview(current)"><el-icon><View /></el-icon> 在线阅览</el-button>
            <el-button v-if="!current.bundled" @click="onDownload(current)"><el-icon><Download /></el-icon> 下载</el-button>
            <el-button v-if="canManage" type="primary" plain @click="openUpload"><el-icon><Upload /></el-icon> 上传新版本</el-button>
          </div>
        </div>

        <!-- 历史存档 -->
        <div class="wg-arch">
          <div class="wg-arch-head">
            <span>存档文件（{{ list.length }}）</span>
            <el-button v-if="canManage" size="small" type="primary" @click="openUpload">上传存档</el-button>
          </div>
          <el-table v-if="list.length" :data="list" v-loading="loading" border stripe size="small">
            <el-table-column type="index" label="序号" width="56" align="center" />
            <el-table-column prop="title" label="名称" min-width="220" show-overflow-tooltip />
            <el-table-column label="文件" min-width="200" show-overflow-tooltip>
              <template #default="{ row }"><span v-if="row.fileName">{{ row.fileName }}</span><span v-else style="color:var(--prm-color-text-disabled)">（无原件）</span></template>
            </el-table-column>
            <el-table-column prop="publisher" label="上传人" width="110" />
            <el-table-column label="上传时间" width="160"><template #default="{ row }">{{ fmt(row.publishDate) }}</template></el-table-column>
            <el-table-column label="操作" :width="canManage ? 210 : 150" fixed="right">
              <template #default="{ row, $index }">
                <el-tag v-if="$index === 0" size="small" type="success" effect="plain" style="margin-right:6px">生效</el-tag>
                <el-button link type="primary" :disabled="!row.fileName" @click="onPreview(row)">阅览</el-button>
                <el-button link type="success" :disabled="!row.fileName" @click="onDownload(row)">下载</el-button>
                <el-button v-if="canManage" link type="danger" @click="onDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div v-else v-loading="loading" class="gc-empty">
            暂无上传的存档文件{{ canManage ? '，点击「上传存档」添加官方工作指引原件;' : '；' }}当前展示系统内置官方 PDF。
          </div>
          <div class="wg-tip">提示:PDF 文件支持在线阅览(浏览器内嵌,可搜索/翻页/打印);Word 等文档点击阅览将自动下载查看。</div>
        </div>
      </el-tab-pane>

      <!-- 确权指引(查阅+维护;排除工作指引存档,二者隔离) -->
      <el-tab-pane label="确权指引" name="confirm">
        <div class="gc-hint">数据确权操作指引材料(政策/流程图/样例/操作说明/FAQ)。{{ manageHint }}</div>
        <GuidanceTable domain="confirm" exclude-type="工作指引" />
      </el-tab-pane>

      <!-- 授权指引(查阅+维护) -->
      <el-tab-pane label="授权指引" name="auth">
        <div class="gc-hint">数据授权操作指引材料(政策/流程图/常见问答/申请步骤/样例)。{{ manageHint }}</div>
        <GuidanceTable domain="auth" />
      </el-tab-pane>
    </el-tabs>

    <!-- 在线阅览弹层(PDF 内嵌) -->
    <el-dialog v-model="pvDlg" :title="pvTitle" width="82%" top="5vh" class="wg-pv">
      <iframe v-if="pvUrl" :src="pvUrl" class="wg-frame" title="工作指引在线阅览"></iframe>
    </el-dialog>

    <!-- 上传存档(admin) -->
    <el-dialog v-model="uploadDlg" title="上传工作指引存档" width="500px" align-center>
      <el-form label-width="72px">
        <el-form-item label="名称"><el-input v-model="upTitle" placeholder="如:南方电网公司数据确权授权工作指引(试行)" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="upRemark" placeholder="版本/文号/说明(可选)" /></el-form-item>
        <el-form-item label="文件">
          <el-upload :auto-upload="false" :limit="1" :on-change="onPick" :on-remove="() => { picked = null }">
            <el-button>选择文件(PDF 可在线阅览;Word 走下载)</el-button>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer><el-button type="primary" :loading="uploading" @click="doUpload">上传存档</el-button><el-button @click="uploadDlg = false">取消</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { openFilePreview } from '@/composables/useFilePreview'
import { currentRole } from '@/lib/roles'
import { pageGuidance, uploadGuidanceFile, deleteGuidance, guidancePreviewUrl, guidanceDownloadUrl } from '@/api/confirm'
import GuidanceTable from './GuidanceTable.vue'

const route = useRoute()
const tab = ref('guide')
// 维护权:配置管理员(admin)及"全部·管理员视图"(all,全站约定全权)可上传/删除/版本
const canManage = computed(() => ['all', 'admin'].includes(currentRole()))

// 深链:/dpr/guidance?tab=auth 直达某 Tab
if (route.query.tab && ['guide', 'confirm', 'auth'].includes(String(route.query.tab))) {
  tab.value = String(route.query.tab)
}
watch(tab, (t) => { if (t === 'guide') loadWg() })

const manageHint = computed(() => canManage.value
  ? '可在下表新增/修改/删除并维护历史版本。'
  : '本页为查阅,维护(新增/修改/版本)由管理员在此 Tab 操作。')

function fmt(t) { return t ? String(t).replace('T', ' ').slice(0, 19) : '-' }

/* ===== 工作指引存档 ===== */
const WG_TYPE = '工作指引'
// 无任何上传时的兜底:系统内置官方 PDF(public/工作指引.pdf),Tab 永不空白
const BUNDLED = { bundled: true, title: '南方电网公司数据确权授权工作指引(试行)', fileName: '工作指引.pdf', previewUrl: '/工作指引.pdf', publisher: '系统内置' }
const list = ref([])
const loading = ref(false)
const current = computed(() => list.value[0] || BUNDLED) // 列表按上传时间倒序,首条即"当前生效"

async function loadWg() {
  loading.value = true
  try {
    const r = await pageGuidance({ guidanceType: WG_TYPE, latestOnly: false, current: 1, size: 100 })
    list.value = r.records || []
  } finally { loading.value = false }
}

// 阅览:PDF 内嵌弹层;非 PDF 自动下载查看(不引入 docx 转换,守住"简单")
function onPreview(row) {
  if (row.bundled) { pvUrl.value = row.previewUrl; pvTitle.value = row.title; pvDlg.value = true; return }
  if (!row.fileName) { ElMessage.warning('该存档无可阅览的原件'); return }
  if (row.fileName.toLowerCase().endsWith('.pdf')) {
    pvUrl.value = guidancePreviewUrl(row.guidanceId); pvTitle.value = row.title; pvDlg.value = true
  } else {
    ElMessage.info('Word 等文档暂不支持内嵌预览,已为您下载查看')
    openFilePreview(guidanceDownloadUrl(row.guidanceId), row.fileName)
  }
}
function onDownload(row) {
  if (row.bundled) { window.open(row.previewUrl, '_blank'); return }
  openFilePreview(guidanceDownloadUrl(row.guidanceId), row.fileName)
}

const pvDlg = ref(false); const pvUrl = ref(''); const pvTitle = ref('')

const uploadDlg = ref(false); const uploading = ref(false)
const upTitle = ref(''); const upRemark = ref(''); let picked = null
function openUpload() { upTitle.value = ''; upRemark.value = ''; picked = null; uploadDlg.value = true }
function onPick(file) { picked = file.raw }
async function doUpload() {
  if (!picked) { ElMessage.warning('请选择文件'); return }
  if (!upTitle.value.trim()) { ElMessage.warning('请填写名称'); return }
  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', picked)
    fd.append('title', upTitle.value.trim())
    fd.append('guidanceType', WG_TYPE)
    fd.append('publisher', '管理员')
    fd.append('content', upRemark.value || '')
    await uploadGuidanceFile(fd)
    ElMessage.success('已上传存档')
    uploadDlg.value = false
    loadWg()
  } finally { uploading.value = false }
}
function onDelete(row) {
  ElMessageBox.confirm(`确认删除存档「${row.title}」吗?`, '提示', { type: 'warning' })
    .then(async () => { await deleteGuidance(row.guidanceId); ElMessage.success('已删除'); loadWg() }).catch(() => {})
}

onMounted(loadWg)
</script>

<style scoped>
.gc { background: #fff; }
.gc-tabs { padding: 4px 4px 0; }
/* 工作指引存档 */
.wg-hero { display: flex; align-items: center; justify-content: space-between; gap: 20px; flex-wrap: wrap; margin: 4px 4px 16px; padding: 18px 22px; border-radius: 10px; background: linear-gradient(135deg, #eef4ff, #f7faff); border: 1px solid #e1ebff; }
.wg-hero-title { font-size: 18px; font-weight: 700; color: var(--prm-color-text); margin: 8px 0 4px; }
.wg-hero-meta { font-size: 12.5px; color: var(--prm-color-text-weak); }
.wg-hero-actions { display: flex; gap: 8px; flex-wrap: wrap; }
.wg-arch { margin: 0 4px; }
.wg-arch-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; font-size: 14px; font-weight: 600; color: var(--prm-color-text); }
.wg-tip { margin-top: 8px; font-size: 12px; color: var(--prm-color-text-weak); line-height: 1.6; }
.gc-empty { padding: 28px 16px; text-align: center; color: var(--prm-color-text-weak); font-size: 13px; background: var(--prm-color-bg); border: 1px dashed var(--prm-color-border); border-radius: 8px; }
.wg-frame { width: 100%; height: 76vh; border: none; }
.gc-hint { padding: 4px 4px 12px; font-size: 12.5px; color: var(--prm-color-text-secondary); }
</style>
<style>
.wg-pv .el-dialog__body { padding: 8px 16px 16px; }
</style>
