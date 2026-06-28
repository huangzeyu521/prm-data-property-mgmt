import request from './request'

// 数据确权申请与审批
export const pageConfirmApply = (query) => request.post('/dpr/confirm/apply/page', query)
// 确权申请概览统计(总/在途/已完成/已驳回/初始/变更),按过滤条件聚合(忽略 status)
export const statsConfirmApply = (query) => request.post('/dpr/confirm/apply/stats', query)
export const saveConfirmDraft = (data) => request.post('/dpr/confirm/apply/draft', data)
export const submitConfirm = (applyId) => request.post(`/dpr/confirm/apply/${applyId}/submit`)
// 固化提交前 AI 校验结果快照(JSON 字符串),供人工预审完整复核
export const saveAiSnapshot = (applyId, snapshotJson) => request.post(`/dpr/confirm/apply/${applyId}/ai-snapshot`, snapshotJson, { headers: { 'Content-Type': 'application/json' } })
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
// 申请人主动撤回(审批中 -> 已撤回中间态),撤回后可重新编辑提交
export const withdrawConfirm = (applyId, reason) =>
  request.post(`/dpr/confirm/apply/${applyId}/withdraw`, null, { params: { reason } })

// 确权表单 元数据自动填充(含质量评分,P1)
export const autofillConfirm = (assetId) => request.get('/dpr/confirm/apply/autofill', { params: { assetId } })

// 权益事实(确权信息带出):按资产取最新已完成确权的第三方来源/隐私商密事实,供授权侧只读带出
export const getRightsFacts = (assetId) => request.get('/dpr/confirm/apply/rights-facts', { params: { assetId } })

// 权益卡片(P4 生命周期:冻结/解冻/注销/变更历史)
export const pageEquityCard = (query) => request.post('/dpr/confirm/card/page', query)
// 权益卡片概览统计(总/正常/冻结/失效/即将到期),按过滤聚合忽略 status
export const statsEquityCard = (query) => request.post('/dpr/confirm/card/stats', query)
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
// 在线阅览(inline:PDF 浏览器内嵌)。供「工作指引存档」在线查看
export const guidancePreviewUrl = (id) => `/api/dpr/confirm/guidance/${id}/preview`

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
// 先从数据资产管理平台元数据(AU_TABLE_META_DATA)同步已上传材料(平台命中项免上传),返回同步报告
export const syncPlatformMaterials = (applyId) => request.post('/dpr/confirm/material/sync-platform', null, { params: { applyId } })
// 大模型校验机制完善:① 校验规则可视化 ② 校验过程回放 ③ 快照完整性验真(防篡改)
export const getAiCheckLogic = (applyId) => request.get('/dpr/confirm/ai/check-logic', { params: { applyId } })
export const getAiRunlog = (applyId) => request.get('/dpr/confirm/ai/runlog', { params: { applyId } })
export const verifyAiSnapshot = (applyId) => request.get(`/dpr/confirm/apply/${applyId}/ai-snapshot/verify`)
export const pushMaterialReview = (applyId) => request.post('/dpr/confirm/material/push-review', null, { params: { applyId } })
export const materialExportUrl = (applyId) => `/api/dpr/confirm/material/export?applyId=${applyId}`
export const pageMaterial = (params) => request.get('/dpr/confirm/material/page', { params })
// 应交材料清单规则(可配置·单一真源):前端据此生成 A–J 应交清单,不再硬编码
export const listMaterialRules = (scene = '确权') => request.get('/dpr/confirm/material/rule', { params: { scene } })

// 确权内生 AI 能力(走 confirm-service 自有 /ai/*,共享 prm-common 大瓦特网关,不依赖独立工具)
export const aiParseConfirm = (applyId) => request.post('/dpr/confirm/ai/parse', null, { params: { applyId }, timeout: 120000 })
export const aiDecisionConfirm = (applyId) => request.post('/dpr/confirm/ai/decision', null, { params: { applyId }, timeout: 120000 })
export const aiConflictConfirm = (applyId) => request.post('/dpr/confirm/ai/conflict', null, { params: { applyId }, timeout: 120000 })

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
// 不落库试算:step1 内联预览经营权归集判定
export const previewConsolidation = (params) => request.get('/dpr/confirm/consolidation/preview', { params })
export const confirmSummaryExportUrl = () => '/api/dpr/confirm/summary/confirm-export'
export const equityConsolidationExportUrl = () => '/api/dpr/confirm/summary/equity-export'
// 材料 AI 校验(qwen3-max 逐份校验,stub 回退)
export const aiMaterialCheck = (applyId) => request.post('/dpr/confirm/material/ai-check', null, { params: { applyId }, timeout: 120000 })
