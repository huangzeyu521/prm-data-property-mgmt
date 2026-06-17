import request from './request'

// 数据确权申请与审批
export const pageConfirmApply = (query) => request.post('/dpr/confirm/apply/page', query)
export const saveConfirmDraft = (data) => request.post('/dpr/confirm/apply/draft', data)
export const submitConfirm = (applyId) => request.post(`/dpr/confirm/apply/${applyId}/submit`)
export const deleteConfirmApply = (applyId) => request.delete(`/dpr/confirm/apply/${applyId}`)
export const getConfirmFlowLog = (applyId) => request.get(`/dpr/confirm/apply/${applyId}/flow-log`)
export const confirmHistoryExportUrl = (params) => {
  const qs = new URLSearchParams(Object.entries(params || {}).filter(([, v]) => v)).toString()
  return `/api/dpr/confirm/apply/export${qs ? '?' + qs : ''}`
}
export const batchSubmitConfirm = (ids) => request.post('/dpr/confirm/apply/batch-submit', ids)
export const batchApproveConfirm = (ids) => request.post('/dpr/confirm/apply/batch-approve', ids)
export const batchRejectConfirm = (ids, reason) => request.post('/dpr/confirm/apply/batch-reject', ids, { params: { reason } })
export const approveConfirm = (applyId) => request.post(`/dpr/confirm/apply/${applyId}/approve`)
export const rejectConfirm = (applyId, reason) =>
  request.post(`/dpr/confirm/apply/${applyId}/reject`, null, { params: { reason } })

// 确权表单 元数据自动填充(含质量评分,P1)
export const autofillConfirm = (assetId) => request.get('/dpr/confirm/apply/autofill', { params: { assetId } })

// 权益卡片(P4 生命周期:冻结/解冻/注销/变更历史)
export const pageEquityCard = (query) => request.post('/dpr/confirm/card/page', query)
export const freezeEquityCard = (cardId) => request.post(`/dpr/confirm/card/${cardId}/freeze`)
export const unfreezeEquityCard = (cardId) => request.post(`/dpr/confirm/card/${cardId}/unfreeze`)
export const revokeEquityCard = (cardId, reason) => request.post(`/dpr/confirm/card/${cardId}/revoke`, null, { params: { reason } })
export const equityCardLogs = (cardId) => request.get(`/dpr/confirm/card/${cardId}/logs`)

// 确权看板
export const getConfirmDashboard = (params) => request.get('/dpr/confirm/dashboard', { params })

// 授权意图识别(⑨;OCR权属识别/冲突检测/RAG问答 已收敛至智能确权辅助工具 aitool,见 api/aitool.js)
export const aiAuthIntent = (text) => request.post('/dpr/confirm/ai/auth-intent', null, { params: { text }, timeout: 120000 })

// 确权汇总表(表3/表4)
export const listConfirmSummary = (applyId) => request.get(`/dpr/confirm/summary/by-apply/${applyId}`)

// 确权指引
export const pageGuidance = (params) => request.get('/dpr/confirm/guidance/page', { params })
export const saveGuidance = (data) => request.post('/dpr/confirm/guidance', data)
export const deleteGuidance = (id) => request.delete(`/dpr/confirm/guidance/${id}`)
export const getGuidance = (id) => request.get(`/dpr/confirm/guidance/${id}`)
export const uploadGuidanceFile = (formData) =>
  request.post('/dpr/confirm/guidance/upload-file', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
export const guidanceVersions = (title) => request.get('/dpr/confirm/guidance/versions', { params: { title } })
export const setGuidanceLatest = (id) => request.post(`/dpr/confirm/guidance/${id}/set-latest`)
export const guidanceDownloadUrl = (id) => `/api/dpr/confirm/guidance/${id}/download`

// 确权材料 + 校验
export const uploadMaterial = (data) => request.post('/dpr/confirm/material', data)
export const uploadMaterialFile = (formData) =>
  request.post('/dpr/confirm/material/upload-file', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
export const materialFileUrl = (id) => `/api/dpr/confirm/material/${id}/file`
export const deleteMaterial = (id) => request.delete(`/dpr/confirm/material/${id}`)
export const listMaterialByApply = (applyId) => request.get(`/dpr/confirm/material/by-apply/${applyId}`)
export const checkMaterial = (materialId, pass, abnormalDesc) =>
  request.post(`/dpr/confirm/material/${materialId}/check`, null, { params: { pass, abnormalDesc } })
export const runMaterialCheck = (applyId) => request.post('/dpr/confirm/material/check-run', null, { params: { applyId } })
export const pushMaterialReview = (applyId) => request.post('/dpr/confirm/material/push-review', null, { params: { applyId } })
export const materialExportUrl = (applyId) => `/api/dpr/confirm/material/export?applyId=${applyId}`
export const pageMaterial = (params) => request.get('/dpr/confirm/material/page', { params })
// 应交材料清单规则(可配置·单一真源):前端据此生成 A–J 应交清单,不再硬编码
export const listMaterialRules = (scene = '确权') => request.get('/dpr/confirm/material/rule', { params: { scene } })

// 权益证书 + 模板
export const issueCert = (params) => request.post('/dpr/confirm/cert/issue', null, { params })
export const revokeCert = (certId) => request.post(`/dpr/confirm/cert/${certId}/revoke`)
export const pageCert = (params) => request.get('/dpr/confirm/cert/page', { params })
export const getCertRender = (certId) => request.get(`/dpr/confirm/cert/${certId}/render`)
export const pageCertTemplate = (params) => request.get('/dpr/confirm/cert-template/page', { params })
export const createCertTemplate = (data) => request.post('/dpr/confirm/cert-template', data)
export const updateCertTemplate = (data) => request.put('/dpr/confirm/cert-template', data)
export const enableCertTemplate = (id) => request.post(`/dpr/confirm/cert-template/${id}/enable`)
export const disableCertTemplate = (id) => request.post(`/dpr/confirm/cert-template/${id}/disable`)
export const uploadCertTemplateFile = (id, formData) => request.post(`/dpr/confirm/cert-template/${id}/upload-file`, formData)
export const certTemplateFileUrl = (id) => `/api/dpr/confirm/cert-template/${id}/file`

// 表级确权清单(M02) + 权益归集判定 + 官方汇总表导出(对齐南网评审资料)
export const saveTableItems = (applyId, items) => request.post(`/dpr/confirm/apply/${applyId}/table-items`, items)
export const listTableItems = (applyId) => request.get(`/dpr/confirm/apply/${applyId}/table-items`)
export const getConsolidation = (applyId) => request.get(`/dpr/confirm/apply/${applyId}/consolidation`)
export const confirmSummaryExportUrl = () => '/api/dpr/confirm/summary/confirm-export'
export const equityConsolidationExportUrl = () => '/api/dpr/confirm/summary/equity-export'
// 材料 AI 校验(qwen3-max 逐份校验,stub 回退)
export const aiMaterialCheck = (applyId) => request.post('/dpr/confirm/material/ai-check', null, { params: { applyId }, timeout: 120000 })
