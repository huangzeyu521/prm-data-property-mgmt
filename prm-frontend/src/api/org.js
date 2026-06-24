import request from './request'

// 组织机构主数据(只读,平台/4A 同步;部门/归口下拉、Dashboard 部门筛选、组织树选择消费)
// 层级取值:网级 / 省级 / 地市
export const ORG_LEVEL = { GRID: '网级', PROVINCE: '省级', BUREAU: '地市' }

// 组织树(根=网级,逐层 children)
export const getOrgTree = () => request.get('/dpr/org/tree')

// 组织清单;level 可选(网级/省级/地市),用于按层级取下拉项
export const listOrg = (level) => request.get('/dpr/org/list', { params: level ? { level } : {} })

// 归口网级解析:传组织名/缩写/ID,返回省/地市归属(provinceCode/bureauCode)
export const resolveOrg = (org) => request.get('/dpr/org/resolve', { params: { org } })
