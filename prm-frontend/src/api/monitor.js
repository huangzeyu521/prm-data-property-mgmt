import request from './request'

// 监测规则
export const pageRule = (query) => request.post('/dpr/monitor/rule/page', query)
export const createRule = (data) => request.post('/dpr/monitor/rule', data)
export const updateRule = (data) => request.put('/dpr/monitor/rule', data)
export const enableRule = (ruleId) => request.post(`/dpr/monitor/rule/${ruleId}/enable`)
export const disableRule = (ruleId) => request.post(`/dpr/monitor/rule/${ruleId}/disable`)
export const deleteRule = (ruleId) => request.delete(`/dpr/monitor/rule/${ruleId}`)

// 风险预警
export const pageAlert = (query) => request.post('/dpr/monitor/alert/page', query)
export const disposeAlert = (alertId, feedback) =>
  request.post(`/dpr/monitor/alert/${alertId}/dispose`, null, { params: { feedback } })
export const closeAlert = (alertId, feedback) =>
  request.post(`/dpr/monitor/alert/${alertId}/close`, null, { params: { feedback } })
export const getAlertStats = () => request.get('/dpr/monitor/alert/stats')
export const pushAlert = (alertId) => request.post(`/dpr/monitor/alert/${alertId}/push`)

// 风险预警通知(定向推送 / 我的未读)
export const pageNotification = (query) => request.post('/dpr/monitor/notification/page', query)
export const unreadNotifyCount = (recipient) =>
  request.get('/dpr/monitor/notification/unread-count', { params: { recipient } })
export const markNotifyRead = (notifyId) => request.post(`/dpr/monitor/notification/${notifyId}/read`)
export const markAllNotifyRead = (recipient) =>
  request.post('/dpr/monitor/notification/read-all', null, { params: { recipient } })

// 合规检查
export const runComplianceCheck = () => request.post('/dpr/monitor/compliance/check')
export const getComplianceReport = (reportId) => request.get(`/dpr/monitor/compliance/report/${reportId}`)
export const pageCompliance = (query) => request.post('/dpr/monitor/compliance/page', query)

// 监测联动熔断(违规上报 -> 预警 + 联动暂停授权 + 追责,附录F 3.4.5)
export const reportViolation = (assetId, ruleId, violationType, desc) =>
  request.post('/dpr/monitor/linkage/violation', null, { params: { assetId, ruleId, violationType, desc } })
// 权属变动联动重确权(数据新增/来源变更/到期 -> 派生重确权工单,附录F 3.3.2)
export const triggerReConfirm = (assetId, assetName, rightType, triggerType, desc) =>
  request.post('/dpr/monitor/linkage/re-confirm', null, { params: { assetId, assetName, rightType, triggerType, desc } })
