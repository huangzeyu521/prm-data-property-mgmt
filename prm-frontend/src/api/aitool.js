import request from './request'

// 智能确权辅助工具 M1 材料智能解析
export const pageAitMaterial = (params) => request.get('/dpr/confirm/aitool/material/page', { params })
export const uploadAitMaterial = (data) => request.post('/dpr/confirm/aitool/material/upload', data)
// #1 真实文件上传:单文件 / 批量(≤50)。multipart FormData,后端做格式(pdf/doc/docx/jpg/jpeg/png)+大小(100KB–500MB)强校验
export const uploadAitMaterialFile = (formData) =>
  request.post('/dpr/confirm/aitool/material/upload-file', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
export const uploadAitMaterialBatch = (formData) =>
  request.post('/dpr/confirm/aitool/material/upload-batch', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
export const aitMaterialFileUrl = (id) => `/api/dpr/confirm/aitool/material/${id}/file`
export const parseAitMaterial = (id) => request.post(`/dpr/confirm/aitool/material/${id}/parse`)
export const getAitParse = (id) => request.get(`/dpr/confirm/aitool/material/${id}/parse`)
export const aitTermCheck = (id) => request.get(`/dpr/confirm/aitool/material/${id}/term-check`)
// #4 人工确认修改:采用标准术语写回解析结果
export const confirmAitTerm = (id, field, standardTerm) =>
  request.post(`/dpr/confirm/aitool/material/${id}/term-confirm`, null, { params: { field, standardTerm } })
export const aitCompares = (id) => request.get(`/dpr/confirm/aitool/material/${id}/compares`)
// #2 解析进度轮询
export const aitProgress = (id) => request.get(`/dpr/confirm/aitool/material/${id}/progress`)
// #8 解析结果导出 Excel(直链下载,经 vite 代理)
export const aitParseExportUrl = (id) => `/api/dpr/confirm/aitool/material/${id}/export`

// M2 权属冲突识别
export const aitAddClaim = (data) => request.post('/dpr/confirm/aitool/conflict/claim', data)
export const aitDetectConflict = (data) => request.post('/dpr/confirm/aitool/conflict/detect', data)
export const aitClaims = (assetId) => request.get('/dpr/confirm/aitool/conflict/claims', { params: { assetId } })
// #17 冲突列表/报告:支持多维筛选(params: assetId/conflictType/riskLevel/startTime/endTime)
export const aitConflicts = (params) => request.get('/dpr/confirm/aitool/conflict/list', { params })
export const aitResolveConflict = (id, feedback) => request.post(`/dpr/confirm/aitool/conflict/${id}/resolve`, null, { params: { feedback } })
export const aitConflictReport = (params) => request.get('/dpr/confirm/aitool/conflict/report', { params })
// #17 冲突报告导出 Word(直链下载)
export const aitConflictReportExportUrl = (params) => {
  const q = new URLSearchParams(Object.entries(params || {}).filter(([, v]) => v != null && v !== '')).toString()
  return `/api/dpr/confirm/aitool/conflict/report/export?${q}`
}

// M3 确权决策支持
export const aitAnalyze = (applyId) => request.post('/dpr/confirm/aitool/decision/analyze', null, { params: { applyId } })
export const aitDecisionByApply = (applyId) => request.get(`/dpr/confirm/aitool/decision/by-apply/${applyId}`)
export const pageAitDecision = (params) => request.get('/dpr/confirm/aitool/decision/page', { params })
