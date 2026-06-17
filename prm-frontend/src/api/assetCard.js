import request from './request'

// 数据资产卡片集成:档案只读查询 + 单卡产权/权益明细(确权服务暴露,assetId 为键)
const BASE = '/dpr/confirm/asset'

/** 档案分页查询(只读):params = { current, size, keyword, state } */
export const pageAssetArchive = (params) => request.get(`${BASE}/archive`, { params })

/** 单卡「产权信息」契约 */
export const getAssetProperty = (assetId) => request.get(`${BASE}/${assetId}/property`)

/** 单卡「权益基本信息」条目列表 */
export const getAssetEquity = (assetId) => request.get(`${BASE}/${assetId}/equity`)
