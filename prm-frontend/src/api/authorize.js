import request from './request'

// 数据授权申请与审批
export const pageAuthApply = (query) => request.post('/dpr/auth/apply/page', query)
export const saveAuthDraft = (data) => request.post('/dpr/auth/apply/draft', data)
export const submitAuth = (applyId) => request.post(`/dpr/auth/apply/${applyId}/submit`)
export const approveAuth = (applyId, opinion) =>
  request.post(`/dpr/auth/apply/${applyId}/approve`, null, { params: { opinion } })
export const rejectAuth = (applyId, reason) =>
  request.post(`/dpr/auth/apply/${applyId}/reject`, null, { params: { reason } })
export const batchApproveAuth = (ids) => request.post('/dpr/auth/apply/batch-approve', ids)
export const batchRejectAuth = (ids, reason) => request.post('/dpr/auth/apply/batch-reject', ids, { params: { reason } })
export const getAuthFlowLog = (applyId) => request.get(`/dpr/auth/apply/${applyId}/flow-log`)

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
export const signAgreementGrantor = (id, fileUrl) => request.post(`/dpr/auth/agreement/${id}/sign-grantor`, null, { params: { fileUrl } })
export const signAgreementGrantee = (id, fileUrl) => request.post(`/dpr/auth/agreement/${id}/sign-grantee`, null, { params: { fileUrl } })
export const reviewAgreement = (id, pass, opinion) => request.post(`/dpr/auth/agreement/${id}/review`, null, { params: { pass, opinion } })
export const getAgreementReviewLogs = (id) => request.get(`/dpr/auth/agreement/${id}/review-logs`)
export const getAgreementArchiveLogs = (id) => request.get(`/dpr/auth/agreement/${id}/archive-logs`)
export const recordAgreementAccess = (id, action) => request.post(`/dpr/auth/agreement/${id}/access`, null, { params: { action } })
export const archiveAgreement = (id) => request.post(`/dpr/auth/agreement/${id}/archive`)
export const pageAgreement = (params) => request.get('/dpr/auth/agreement/page', { params })
export const uploadAgreementSeal = (id, fd) => request.post(`/dpr/auth/agreement/${id}/upload-seal`, fd)
export const getAgreementSealLogs = (id) => request.get(`/dpr/auth/agreement/${id}/upload-logs`)
export const agreementSealFileUrl = (logId) => `/api/dpr/auth/agreement/upload-log/${logId}/file`

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
