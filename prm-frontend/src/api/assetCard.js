import request from './request'

// 数据资产卡片集成:档案只读查询 + 单卡产权/权益明细(确权服务暴露,assetId 为键)
const BASE = '/dpr/confirm/asset'

/** 按 名称/编码/系统·表 搜索可关联的数据资产卡片(选卡片而非手填ID;平台为源,台账兜底) */
export const searchAssetCards = (keyword, limit = 10) => request.get(`${BASE}/cards`, { params: { keyword, limit } })

/** 选卡片→自动带库表清单(确权粒度到库表,对齐附录F表2/表3;平台未接入时返回桩合成清单) */
export const listAssetTables = (assetId, assetName) => request.get(`${BASE}/${assetId}/tables`, { params: { assetName } })

/** 档案分页查询(只读):params = { current, size, keyword, state } */
export const pageAssetArchive = (params) => request.get(`${BASE}/archive`, { params })

/** 单卡「产权信息」契约 */
export const getAssetProperty = (assetId) => request.get(`${BASE}/${assetId}/property`)

/** 单卡「权益基本信息」条目列表 */
export const getAssetEquity = (assetId) => request.get(`${BASE}/${assetId}/equity`)
