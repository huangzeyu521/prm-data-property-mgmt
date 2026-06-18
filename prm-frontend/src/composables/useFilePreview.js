import { reactive } from 'vue'
import request from '@/api/request'

/**
 * 全局在线文件预览服务(单例)。任意页面 openFilePreview(url, fileName) 即可弹窗预览。
 * url 为后端文件端点(/api/... 全路径);取 blob 时去掉 /api 前缀走统一 request(带 JWT)。
 */
export const filePreviewState = reactive({
  visible: false,
  url: '',
  fileName: ''
})

export function openFilePreview(url, fileName) {
  if (!url) return
  filePreviewState.url = url
  filePreviewState.fileName = fileName || ''
  filePreviewState.visible = true
}

export function closeFilePreview() {
  filePreviewState.visible = false
}

/** 取文件 blob(带鉴权头;request 拦截器对非信封响应原样返回) */
export function fetchFileBlob(url) {
  const path = String(url).replace(/^\/api/, '')
  return request.get(path, { responseType: 'blob', timeout: 120000 })
}
