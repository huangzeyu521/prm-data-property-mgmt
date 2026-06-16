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
// #5 多粒度解析片段(granularity 可空=全部:PAGE/PARAGRAPH/CELL/TABLE/TITLE)
export const aitSegments = (id, granularity) => request.get(`/dpr/confirm/aitool/material/${id}/segments`, { params: { granularity } })
// #4 按数据表归集关联(applyId 或 dataTableRef)
export const aitAggregate = (params) => request.get('/dpr/confirm/aitool/material/aggregate', { params })
// #7 解析准确度评测(整体/分字段 + 是否达标≥95%)
export const aitAccuracy = () => request.get('/dpr/confirm/aitool/material/accuracy', { timeout: 120000 })

// 1.2 数据清洗与标准化
// 运行清洗(rows 可空 → 后端按解析结果自动派生;useModel 控制规则+模型混合)
export const aitClean = (id, body) => request.post(`/dpr/confirm/aitool/clean/${id}`, body || {}, { timeout: 120000 })
export const aitAuditBase = (id) => request.get(`/dpr/confirm/aitool/clean/${id}/audit-base`)
export const aitCleanPending = (id) => request.get(`/dpr/confirm/aitool/clean/${id}/pending`)
export const aitCleanLog = (id) => request.get(`/dpr/confirm/aitool/clean/${id}/log`)
// 1.1.1.1#4 结构化模板上传对比(多模板)/ 对比日志 / 下载对比结果
export const aitTplCompareUpload = (id, formData) =>
  request.post(`/dpr/confirm/aitool/clean/${id}/template-compare`, formData, { headers: { 'Content-Type': 'multipart/form-data' }, timeout: 120000 })
export const aitTplCompareLog = (id) => request.get(`/dpr/confirm/aitool/clean/${id}/template-compare`)
export const aitTplCompareExportUrl = (id) => `/api/dpr/confirm/aitool/clean/${id}/template-compare/export`

// 1.3 确权要素识别与特征抽取
export const aitExtractElements = (id, useModel) => request.post(`/dpr/confirm/aitool/element/${id}/extract`, null, { params: { useModel }, timeout: 120000 })
export const aitProfile = (id) => request.get(`/dpr/confirm/aitool/element/${id}/profile`)
export const aitProfileView = (params) => request.get('/dpr/confirm/aitool/element/view', { params })

// 1.4 材料解析与管理
// #1 解析记录档
export const aitRecordPage = (params) => request.get('/dpr/confirm/aitool/record/page', { params })
export const aitRecordExportUrl = (params) => {
  const q = new URLSearchParams(Object.entries(params || {}).filter(([, v]) => v != null && v !== '')).toString()
  return `/api/dpr/confirm/aitool/record/export?${q}`
}
// #2 批量解析
export const aitBatchParse = (batchNo) => request.post('/dpr/confirm/aitool/material/batch-parse', null, { params: { batchNo } })
export const aitBatchProgress = (batchNo) => request.get('/dpr/confirm/aitool/material/batch-progress', { params: { batchNo } })
// #3 资料模板库
export const aitTemplatePage = (params) => request.get('/dpr/confirm/aitool/template/page', { params })
export const aitTemplateGet = (id) => request.get(`/dpr/confirm/aitool/template/${id}`)
export const aitTemplateCreate = (data) => request.post('/dpr/confirm/aitool/template', data)
export const aitTemplateUpdate = (data) => request.put('/dpr/confirm/aitool/template', data)
export const aitTemplateNewVersion = (data) => request.post('/dpr/confirm/aitool/template/new-version', data)
export const aitTemplateVersions = (templateName) => request.get('/dpr/confirm/aitool/template/versions', { params: { templateName } })
export const aitTemplateDownloadUrl = (id) => `/api/dpr/confirm/aitool/template/${id}/download`
// #4 解析元数据配置
export const aitParseConfigList = () => request.get('/dpr/confirm/aitool/parse-config')
export const aitParseConfigSave = (data) => request.post('/dpr/confirm/aitool/parse-config', data)
export const aitParseConfigDelete = (id) => request.delete(`/dpr/confirm/aitool/parse-config/${id}`)

// M2 权属冲突识别
export const aitAddClaim = (data) => request.post('/dpr/confirm/aitool/conflict/claim', data)
// #9 条款语义分析自动建主张 + 知识图谱结构化输出
export const buildAitClaimFromMaterial = (materialId) => request.post('/dpr/confirm/aitool/conflict/claim-from-material', null, { params: { materialId } })
export const aitKgGraph = (assetId) => request.get('/dpr/confirm/aitool/conflict/graph', { params: { assetId } })
// #10 知识图谱动态更新:人工修改/删除节点 + 历史案例自动同步
export const updateAitClaim = (data) => request.put('/dpr/confirm/aitool/conflict/claim', data)
export const deleteAitClaim = (claimId) => request.delete(`/dpr/confirm/aitool/conflict/claim/${claimId}`)
export const syncAitHistoryClaims = (assetId) => request.post('/dpr/confirm/aitool/conflict/sync-history', null, { params: { assetId } })
export const aitDetectConflict = (data) => request.post('/dpr/confirm/aitool/conflict/detect', data)
export const aitClaims = (assetId) => request.get('/dpr/confirm/aitool/conflict/claims', { params: { assetId } })
// #17 冲突列表/报告:支持多维筛选(params: assetId/conflictType/riskLevel/startTime/endTime)
export const aitConflicts = (params) => request.get('/dpr/confirm/aitool/conflict/list', { params })
export const aitResolveConflict = (id, feedback) => request.post(`/dpr/confirm/aitool/conflict/${id}/resolve`, null, { params: { feedback } })
// #16 冲突解决方案建议:规则建议 + 法规依据 + AI 建议
export const aitConflictAdvice = (conflictId) => request.get(`/dpr/confirm/aitool/conflict/${conflictId}/advice`, { timeout: 120000 })
export const aitConflictReport = (params) => request.get('/dpr/confirm/aitool/conflict/report', { params })
// #17 冲突报告导出 Word(直链下载)
export const aitConflictReportExportUrl = (params) => {
  const q = new URLSearchParams(Object.entries(params || {}).filter(([, v]) => v != null && v !== '')).toString()
  return `/api/dpr/confirm/aitool/conflict/report/export?${q}`
}
// 权属冲突识别与分析:#6 冲突记录导出 Excel(直链)
export const aitConflictExcelUrl = (params) => {
  const q = new URLSearchParams(Object.entries(params || {}).filter(([, v]) => v != null && v !== '')).toString()
  return `/api/dpr/confirm/aitool/conflict/export?${q}`
}
// #1 冲突识别规则配置(管理员)
export const aitConflictRuleList = () => request.get('/dpr/confirm/aitool/conflict-rule')
export const aitConflictRuleSave = (data) => request.post('/dpr/confirm/aitool/conflict-rule', data)
export const aitConflictRuleToggle = (ruleId, on) => request.post(`/dpr/confirm/aitool/conflict-rule/${ruleId}/toggle`, null, { params: { on } })

// 2.1 知识库构建与检索增强
export const aitKbSearch = (body) => request.post('/dpr/confirm/aitool/kb/search', body)
export const aitKbRag = (body) => request.post('/dpr/confirm/aitool/kb/rag', body, { timeout: 120000 })
export const aitKbDocPage = (params) => request.get('/dpr/confirm/aitool/kb/doc/page', { params })
export const aitKbVersions = (title) => request.get('/dpr/confirm/aitool/kb/doc/versions', { params: { title } })
export const aitKbAddDoc = (data) => request.post('/dpr/confirm/aitool/kb/doc', data)
export const aitKbNewVersion = (data) => request.post('/dpr/confirm/aitool/kb/doc/new-version', data)
export const aitKbReplaceClause = (docId, body) => request.post(`/dpr/confirm/aitool/kb/doc/${docId}/replace-clause`, body)
export const aitKbInvalidate = (docId) => request.post(`/dpr/confirm/aitool/kb/doc/${docId}/invalidate`)
export const aitKbRollback = (docId) => request.post(`/dpr/confirm/aitool/kb/doc/${docId}/rollback`)

// 3.1 智能确权 Agent 审核与推理决策
export const aitAgentAudit = (applyId) => request.post('/dpr/confirm/aitool/agent/audit', null, { params: { applyId }, timeout: 120000 })
export const aitAgentGet = (applyId) => request.get(`/dpr/confirm/aitool/agent/${applyId}`)
// 3.2 审核结果生成与台账管理
export const aitAgentBatchAudit = (applyIds) => request.post('/dpr/confirm/aitool/agent/batch-audit', applyIds, { timeout: 180000 })
export const aitAgentFieldLevel = (applyId) => request.get(`/dpr/confirm/aitool/agent/${applyId}/field-level`)
export const aitAgentEvidence = (applyId) => request.get(`/dpr/confirm/aitool/agent/${applyId}/evidence`)
export const aitAgentLedgerPage = (params) => request.get('/dpr/confirm/aitool/agent/ledger/page', { params })
export const aitAgentLedgerStats = () => request.get('/dpr/confirm/aitool/agent/ledger/stats')
export const aitAgentLedgerExportUrl = () => '/api/dpr/confirm/aitool/agent/ledger/export'
export const aitAgentReportUrl = (id) => `/api/dpr/confirm/aitool/agent/${id}/report`
export const aitAgentRegistrationUrl = (id) => `/api/dpr/confirm/aitool/agent/${id}/registration-doc`
export const aitAgentLegalUrl = (id) => `/api/dpr/confirm/aitool/agent/${id}/legal-opinion`

// 3.3 开放对接与运行支撑
export const aitOpsCapabilities = () => request.get('/dpr/confirm/aitool/ops/tools/capabilities')
export const aitOpsInvoke = (body) => request.post('/dpr/confirm/aitool/ops/tools/invoke', body, { timeout: 120000 })
export const aitOpsModelConfig = () => request.get('/dpr/confirm/aitool/ops/tools/model-config')
export const aitOpsTaskCreate = (body) => request.post('/dpr/confirm/aitool/ops/task/create', body)
export const aitOpsTaskRun = (id) => request.post(`/dpr/confirm/aitool/ops/task/${id}/run`, null, { timeout: 180000 })
export const aitOpsTaskPage = (params) => request.get('/dpr/confirm/aitool/ops/task/page', { params })
export const aitOpsTaskGet = (id) => request.get(`/dpr/confirm/aitool/ops/task/${id}`)
export const aitOpsRunLogPage = (params) => request.get('/dpr/confirm/aitool/ops/runlog/page', { params })
export const aitOpsRunLogStats = () => request.get('/dpr/confirm/aitool/ops/runlog/stats')
export const aitOpsAlerts = (params) => request.get('/dpr/confirm/aitool/ops/alerts', { params })

// M3 确权决策支持
export const aitAnalyze = (applyId) => request.post('/dpr/confirm/aitool/decision/analyze', null, { params: { applyId }, timeout: 120000 })
export const aitDecisionByApply = (applyId) => request.get(`/dpr/confirm/aitool/decision/by-apply/${applyId}`)
export const pageAitDecision = (params) => request.get('/dpr/confirm/aitool/decision/page', { params })
