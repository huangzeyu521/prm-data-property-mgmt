import request from './request'

// 数据授权申请与审批
export const pageAuthApply = (query) => request.post('/dpr/auth/apply/page', query)
export const saveAuthDraft = (data) => request.post('/dpr/auth/apply/draft', data)
// 删除授权申请(仅草稿态,如批量清单加错/重复的明细项)
export const deleteAuthApply = (applyId) => request.delete(`/dpr/auth/apply/${applyId}`)
export const submitAuth = (applyId) => request.post(`/dpr/auth/apply/${applyId}/submit`)
export const approveAuth = (applyId, opinion) =>
  request.post(`/dpr/auth/apply/${applyId}/approve`, null, { params: { opinion } })
export const rejectAuth = (applyId, reason) =>
  request.post(`/dpr/auth/apply/${applyId}/reject`, null, { params: { reason } })
export const batchApproveAuth = (ids) => request.post('/dpr/auth/apply/batch-approve', ids)
export const batchRejectAuth = (ids, reason) => request.post('/dpr/auth/apply/batch-reject', ids, { params: { reason } })
export const getAuthFlowLog = (applyId) => request.get(`/dpr/auth/apply/${applyId}/flow-log`)
// 按 id 取整单(草稿就地续填回填)
export const getAuthApply = (applyId) => request.get(`/dpr/auth/apply/${applyId}`)
// 申请人主动撤回(审批中 -> 已撤回);行级 + 一事一议整单级
export const withdrawAuth = (applyId, reason) => request.post(`/dpr/auth/apply/${applyId}/withdraw`, null, { params: { reason } })
export const withdrawAuthForm = (formNo) => request.post(`/dpr/auth/apply/form/${formNo}/withdraw`)

// 一事一议「单场景·多表」申请单(同 formNo 多张数据表,对齐表5 多行)
export const createAuthForm = () => request.post('/dpr/auth/apply/form')
export const listAuthByForm = (formNo) => request.get(`/dpr/auth/apply/by-form/${formNo}`)
export const submitAuthForm = (formNo) => request.post(`/dpr/auth/apply/form/${formNo}/submit`)
export const checkAuthFormCompliance = (formNo) => request.post(`/dpr/auth/apply/form/${formNo}/compliance-check`)

// 授权证书
export const pageAuthCert = (query) => request.post('/dpr/auth/cert/page', query)
export const getAuthCertRender = (certId) => request.get(`/dpr/auth/cert/${certId}/render`)
export const revokeAuthCert = (certId) => request.post(`/dpr/auth/cert/${certId}/revoke`)
// 监测联动熔断:暂停某资产下生效证书
export const suspendCertByAsset = (assetId, reason, sourceAlertId, violationType) =>
  request.post('/dpr/auth/cert/suspend-by-asset', null, { params: { assetId, reason, sourceAlertId, violationType } })
// 到期续签
export const renewAuthCert = (certId, validDate) =>
  request.post(`/dpr/auth/cert/${certId}/renew`, null, { params: { validDate } })
// 到期预警(days 天内)
export const expiringAuthCerts = (days = 30) => request.get('/dpr/auth/cert/expiring', { params: { days } })

// 授权看板
export const getAuthDashboard = (params) => request.get('/dpr/auth/dashboard', { params })

// 授权域目录项(指引/场景/申请表单模板/协议模板库)
export const pageCatalog = (params) => request.get('/dpr/auth/catalog/page', { params })
export const saveCatalog = (data) => request.post('/dpr/auth/catalog', data)
export const enableCatalog = (id) => request.post(`/dpr/auth/catalog/${id}/enable`)
export const disableCatalog = (id) => request.post(`/dpr/auth/catalog/${id}/disable`)

// 授权合规校验
export const runAuthCompliance = (params) => request.post('/dpr/auth/compliance/check', null, { params })
export const pageAuthCompliance = (params) => request.get('/dpr/auth/compliance/page', { params })
export const authComplianceExportUrl = (params) => {
  const qs = new URLSearchParams(Object.entries(params || {}).filter(([, v]) => v)).toString()
  return `/api/dpr/auth/compliance/export${qs ? '?' + qs : ''}`
}

// 授权协议(双签/审核/存档/下载)
export const generateAgreement = (params) => request.post('/dpr/auth/agreement/generate', null, { params })
// 批量授权:一份批量清单生成一份《运营授权协议》(一清单一协议)
export const generateAgreementForBatch = (batchListId) => request.post('/dpr/auth/agreement/generate-for-batch', null, { params: { batchListId } })
export const signAgreementGrantor = (id, fileUrl) => request.post(`/dpr/auth/agreement/${id}/sign-grantor`, null, { params: { fileUrl } })
export const signAgreementGrantee = (id, fileUrl) => request.post(`/dpr/auth/agreement/${id}/sign-grantee`, null, { params: { fileUrl } })
export const reviewAgreement = (id, pass, opinion) => request.post(`/dpr/auth/agreement/${id}/review`, null, { params: { pass, opinion } })
export const getAgreementReviewLogs = (id) => request.get(`/dpr/auth/agreement/${id}/review-logs`)
export const getAgreementArchiveLogs = (id) => request.get(`/dpr/auth/agreement/${id}/archive-logs`)
export const recordAgreementAccess = (id, action) => request.post(`/dpr/auth/agreement/${id}/access`, null, { params: { action } })
export const archiveAgreement = (id) => request.post(`/dpr/auth/agreement/${id}/archive`)
export const pageAgreement = (params) => request.get('/dpr/auth/agreement/page', { params })
// 协议要素核对(附录D §3.4.4):协议 + 来源申请单 数据范围/场景/目的/利益分配/安全保障
export const getAgreementElements = (id) => request.get(`/dpr/auth/agreement/${id}/elements`)
export const uploadAgreementSeal = (id, fd) => request.post(`/dpr/auth/agreement/${id}/upload-seal`, fd)
export const getAgreementSealLogs = (id) => request.get(`/dpr/auth/agreement/${id}/upload-logs`)
export const agreementSealFileUrl = (logId) => `/api/dpr/auth/agreement/upload-log/${logId}/file`
// 协议文档(附录D《南方电网数据授权运营协议》)下载端点;正式稿返回锁定快照
export const agreementAppendixDUrl = (id) => `/api/dpr/auth/agreement/${id}/appendix-d`
// 协议要素落定(附录D 协商项:草案填空→正式稿锁定→才可签章)
export const getAgreementNegotiation = (id) => request.get(`/dpr/auth/agreement/${id}/negotiation`)
export const saveAgreementNegotiation = (id, data) => request.post(`/dpr/auth/agreement/${id}/negotiation`, data)
export const finalizeAgreementDoc = (id) => request.post(`/dpr/auth/agreement/${id}/finalize-doc`)
export const revertAgreementDraft = (id) => request.post(`/dpr/auth/agreement/${id}/revert-draft`)
// 保密承诺函(附录E,乙方必签):双签✚承诺函齐才自动归档开权限
export const uploadAgreementConfidentiality = (id, fd) => request.post(`/dpr/auth/agreement/${id}/confidentiality`, fd)
// 期限管理(动态跟踪):续期/终止
export const renewAgreement = (id, validUntil) => request.post(`/dpr/auth/agreement/${id}/renew`, null, { params: { validUntil } })
export const terminateAgreement = (id, reason) => request.post(`/dpr/auth/agreement/${id}/terminate`, null, { params: { reason } })

// 授权权益证书模板(可研 3.2.2.1.1.3.4.2:专项/批量授权证书模板)
export const pageAuthCertTemplate = (params) => request.get('/dpr/auth/cert-template/page', { params })
export const createAuthCertTemplate = (data) => request.post('/dpr/auth/cert-template', data)
export const updateAuthCertTemplate = (data) => request.put('/dpr/auth/cert-template', data)
export const enableAuthCertTemplate = (id) => request.post(`/dpr/auth/cert-template/${id}/enable`)
export const disableAuthCertTemplate = (id) => request.post(`/dpr/auth/cert-template/${id}/disable`)
export const deleteAuthCertTemplate = (id) => request.delete(`/dpr/auth/cert-template/${id}`)
export const uploadAuthCertTemplateFile = (id, fd) => request.post(`/dpr/auth/cert-template/${id}/upload-file`, fd)
export const authCertTemplateFileUrl = (id) => `/api/dpr/auth/cert-template/${id}/file`

// 表6 数据批量授权清单(草案->申报稿->批准)
export const pageBatchList = (params) => request.get('/dpr/auth/batch-list/page', { params })
export const createBatchList = (data) => request.post('/dpr/auth/batch-list', data)
export const submitBatchList = (id) => request.post(`/dpr/auth/batch-list/${id}/submit`)
export const approveBatchList = (id) => request.post(`/dpr/auth/batch-list/${id}/approve`)
export const getBatchList = (id) => request.get(`/dpr/auth/batch-list/${id}`)
// 草案删除(级联草稿明细) / 申报稿撤回(退回草案 + 明细回草稿)
export const deleteBatchList = (id) => request.delete(`/dpr/auth/batch-list/${id}`)
export const withdrawBatchList = (id) => request.post(`/dpr/auth/batch-list/${id}/withdraw`)
// 批量清单明细(表6 明细行:清单下所有授权项)
export const listAuthByBatch = (batchListId) => request.get(`/dpr/auth/apply/by-batch/${batchListId}`)

// 对外经营权授权备案(附录G / 附录F §3.4.6:仅经营权对外授权需备案)
export const pageFiling = (params) => request.get('/dpr/auth/filing/page', { params })
export const createFiling = (data) => request.post('/dpr/auth/filing', data)
export const fileFiling = (id) => request.post(`/dpr/auth/filing/${id}/file`)

// 授权指引管理(可研 3.2.2.1.1.3.1.1)
export const pageAuthGuidance = (params) => request.get('/dpr/auth/guidance/page', { params })
export const saveAuthGuidance = (data) => request.post('/dpr/auth/guidance', data)
export const deleteAuthGuidance = (id) => request.delete(`/dpr/auth/guidance/${id}`)
export const getAuthGuidance = (id) => request.get(`/dpr/auth/guidance/${id}`)
export const uploadAuthGuidanceFile = (formData) =>
  request.post('/dpr/auth/guidance/upload-file', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
export const authGuidanceVersions = (title) => request.get('/dpr/auth/guidance/versions', { params: { title } })
export const setAuthGuidanceLatest = (id) => request.post(`/dpr/auth/guidance/${id}/set-latest`)
export const authGuidanceDownloadUrl = (id) => `/api/dpr/auth/guidance/${id}/download`
// 在线阅览(inline:PDF 浏览器内嵌)。供「指引存档」在线查看
export const authGuidancePreviewUrl = (id) => `/api/dpr/auth/guidance/${id}/preview`

// 授权申请表单模板(可研 3.2.2.1.1.3.1.2)
export const pageApplyTemplate = (params) => request.get('/dpr/auth/apply-template/page', { params })
export const getApplyTemplate = (id) => request.get(`/dpr/auth/apply-template/${id}`)
export const createApplyTemplate = (data) => request.post('/dpr/auth/apply-template', data)
export const updateApplyTemplate = (data) => request.put('/dpr/auth/apply-template', data)
export const deleteApplyTemplate = (id) => request.delete(`/dpr/auth/apply-template/${id}`)
export const enableApplyTemplate = (id) => request.post(`/dpr/auth/apply-template/${id}/enable`)
export const disableApplyTemplate = (id) => request.post(`/dpr/auth/apply-template/${id}/disable`)

// 授权应用场景配置(可研 3.2.2.1.1.3.1.3)
export const pageScenario = (params) => request.get('/dpr/auth/scenario/page', { params })
export const createScenario = (data) => request.post('/dpr/auth/scenario', data)
export const updateScenario = (data) => request.put('/dpr/auth/scenario', data)
export const deleteScenario = (id) => request.delete(`/dpr/auth/scenario/${id}`)
export const enableScenario = (id) => request.post(`/dpr/auth/scenario/${id}/enable`)
export const disableScenario = (id) => request.post(`/dpr/auth/scenario/${id}/disable`)

// 授权申请材料(可研 3.2.2.1.1.3.1.4)
export const uploadAuthMaterialFile = (formData) =>
  request.post('/dpr/auth/material/upload-file', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
export const listAuthMaterial = (applyId) => request.get(`/dpr/auth/material/by-apply/${applyId}`)
export const deleteAuthMaterial = (id) => request.delete(`/dpr/auth/material/${id}`)
// 应交材料清单规则(可配置·单一真源):前端据此生成应交清单,不再硬编码。scene=批量/一事一议
export const listAuthMaterialRules = (scene = '批量') => request.get('/dpr/auth/material/rule', { params: { scene } })
// 批量清单只读合规校验(试跑):整单是否可提交+逐项被拦原因(与提交门禁同源),支撑"通过才放行"闭环
export const checkBatchCompliance = (batchListId) => request.post(`/dpr/auth/batch-list/${batchListId}/compliance-check`)
// 可授权资源池-对外开放目录批量过滤(经营权资源池前置裁剪):入参 assetId[] → 返回在对外开放目录中的子集
export const grantableOpenFilter = (assetIds) => request.post('/dpr/auth/grantable/open-filter', assetIds)
export const authMaterialFileUrl = (id) => `/api/dpr/auth/material/${id}/file`

// 授权协议模板库(可研 3.2.2.1.1.3.3.1)
export const pageAgrTemplate = (params) => request.get('/dpr/auth/agreement-template/page', { params })
export const getAgrTemplate = (id) => request.get(`/dpr/auth/agreement-template/${id}`)
export const createAgrTemplate = (data) => request.post('/dpr/auth/agreement-template', data)
export const updateAgrTemplate = (data) => request.put('/dpr/auth/agreement-template', data)
export const deleteAgrTemplate = (id) => request.delete(`/dpr/auth/agreement-template/${id}`)
export const enableAgrTemplate = (id) => request.post(`/dpr/auth/agreement-template/${id}/enable`)
export const disableAgrTemplate = (id) => request.post(`/dpr/auth/agreement-template/${id}/disable`)
export const uploadAgrTemplateFile = (id, fd) => request.post(`/dpr/auth/agreement-template/${id}/upload-file`, fd)
export const agrTemplateFileUrl = (id) => `/api/dpr/auth/agreement-template/${id}/file`

// 授权大模型能力(qwen3-max,stub 回退)
export const aiAuthMaterialCheck = (applyId) => request.post('/dpr/auth/material/ai-check', null, { params: { applyId }, timeout: 120000 })
export const aiAuthPreReview = (applyId) => request.post('/dpr/auth/compliance/pre-review', null, { params: { applyId }, timeout: 120000 })
export const aiBatchIntent = (text) => request.post('/dpr/auth/batch-list/ai-intent', null, { params: { text }, timeout: 120000 })
export const aiBatchPreReview = (batchListId) => request.post(`/dpr/auth/batch-list/${batchListId}/pre-review`, null, { timeout: 120000 })
// 授权大模型校验机制完善:① 校验规则可视化 ② 校验过程回放 ③ 快照固化/验真(防篡改)
export const getAuthAiCheckLogic = (applyId) => request.get('/dpr/auth/ai/check-logic', { params: { applyId } })
export const getAuthAiRunlog = (applyId) => request.get('/dpr/auth/ai/runlog', { params: { applyId } })
export const saveAuthAiSnapshot = (applyId, snapshotJson) => request.post(`/dpr/auth/ai/${applyId}/ai-snapshot`, snapshotJson, { headers: { 'Content-Type': 'application/json' } })
export const verifyAuthAiSnapshot = (applyId) => request.get(`/dpr/auth/ai/${applyId}/ai-snapshot/verify`)
