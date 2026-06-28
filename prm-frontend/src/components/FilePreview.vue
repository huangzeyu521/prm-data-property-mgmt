<!--
  Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
  中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
  本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
-->
<template>
  <el-dialog v-model="state.visible" :title="title" width="78%" top="6vh" align-center
    class="file-preview-dlg" @opened="load" @closed="onClosed">
    <div v-loading="loading" class="fp-body">
      <el-alert v-if="error" type="error" :closable="false" :title="error" show-icon style="margin-bottom:10px" />
      <!-- PDF:浏览器内置阅读器 -->
      <iframe v-if="kind === 'pdf' && objectUrl" :src="objectUrl" class="fp-frame" />
      <!-- 图片 -->
      <div v-else-if="kind === 'image' && objectUrl" class="fp-img-wrap">
        <img :src="objectUrl" class="fp-img" :alt="state.fileName" />
      </div>
      <!-- docx:docx-preview 渲染为带样式 HTML -->
      <div v-else-if="kind === 'docx'" ref="docxBox" class="fp-docx"></div>
      <!-- 纯文本 -->
      <pre v-else-if="kind === 'text'" class="fp-text">{{ textContent }}</pre>
      <!-- 其他类型:无法在线渲染,引导下载 -->
      <el-empty v-else-if="kind === 'other' && !loading" :description="`该格式（${ext || '未知'}）暂不支持在线渲染,请下载后查看`">
        <el-button type="primary" @click="download">下载文件</el-button>
      </el-empty>
    </div>
    <template #footer>
      <span style="float:left;color:var(--prm-color-text-weak);font-size:12px;line-height:32px">{{ state.fileName }}</span>
      <el-button @click="state.visible = false">关闭</el-button>
      <el-button type="primary" :disabled="!objectUrl && kind !== 'docx'" @click="download">下载</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { renderAsync } from 'docx-preview'
import { filePreviewState as state, fetchFileBlob } from '@/composables/useFilePreview'

const loading = ref(false)
const error = ref('')
const objectUrl = ref('')
const textContent = ref('')
const docxBox = ref(null)
let blobRef = null

const ext = computed(() => {
  const n = state.fileName || ''
  const i = n.lastIndexOf('.')
  return i >= 0 ? n.slice(i + 1).toLowerCase() : ''
})
const kind = computed(() => {
  const e = ext.value
  if (e === 'pdf') return 'pdf'
  if (['png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp'].includes(e)) return 'image'
  if (e === 'docx') return 'docx'
  if (['txt', 'csv', 'json', 'md', 'log', 'xml'].includes(e)) return 'text'
  return 'other'
})
const title = computed(() => '在线预览' + (state.fileName ? ' · ' + state.fileName : ''))

function revoke() {
  if (objectUrl.value) { URL.revokeObjectURL(objectUrl.value); objectUrl.value = '' }
}

async function load() {
  revoke(); error.value = ''; textContent.value = ''; blobRef = null
  if (docxBox.value) docxBox.value.innerHTML = ''
  if (!state.url) return
  loading.value = true
  try {
    const blob = await fetchFileBlob(state.url)
    blobRef = blob
    if (kind.value === 'pdf' || kind.value === 'image') {
      objectUrl.value = URL.createObjectURL(blob)
    } else if (kind.value === 'text') {
      textContent.value = await blob.text()
    } else if (kind.value === 'docx') {
      await nextTick()
      await renderAsync(blob, docxBox.value, null, { className: 'docx', inWrapper: true, ignoreWidth: false })
    }
  } catch (e) {
    error.value = '预览加载失败:' + (e?.message || '请稍后重试或下载查看')
  } finally {
    loading.value = false
  }
}

function download() {
  if (!blobRef) { ElMessage.warning('文件尚未就绪'); return }
  const u = URL.createObjectURL(blobRef)
  const a = document.createElement('a')
  a.href = u; a.download = state.fileName || 'download'
  document.body.appendChild(a); a.click(); a.remove()
  setTimeout(() => URL.revokeObjectURL(u), 1500)
}

function onClosed() {
  revoke(); textContent.value = ''; error.value = ''
  if (docxBox.value) docxBox.value.innerHTML = ''
}
</script>

<style scoped>
.fp-body { min-height: 60vh; max-height: 78vh; overflow: auto; }
.fp-frame { width: 100%; height: 74vh; border: 0; }
.fp-img-wrap { text-align: center; }
.fp-img { max-width: 100%; max-height: 74vh; }
.fp-docx { background: #f5f5f5; padding: 12px; }
.fp-docx :deep(.docx-wrapper) { background: #f5f5f5; padding: 0; }
.fp-docx :deep(.docx) { box-shadow: 0 0 6px rgba(0,0,0,.12); margin: 0 auto; }
.fp-text { white-space: pre-wrap; word-break: break-all; font-size: 13px; line-height: 1.6; padding: 8px; }
</style>
