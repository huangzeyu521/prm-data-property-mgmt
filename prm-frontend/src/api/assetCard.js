import request from './request'

// 数据资产卡片集成:档案只读查询 + 单卡产权/权益明细(确权服务暴露,assetId 为键)
const BASE = '/dpr/confirm/asset'

/** 按 名称/编码/系统·表 搜索可关联的数据资产卡片(选卡片而非手填ID;平台为源,台账兜底) */
export const searchAssetCards = (keyword, limit = 10) => request.get(`${BASE}/cards`, { params: { keyword, limit } })

/** 选卡片→自动带库表清单(确权粒度到库表,对齐附录F表2/表3;平台未接入时返回桩合成清单) */
export const listAssetTables = (assetId, assetName) => request.get(`${BASE}/${assetId}/tables`, { params: { assetName } })

// 数据目录树(确权范围:业务域→系统→一级功能模块→库表卡片;懒加载)
// status:unconfirmed=仅未确权(初始确权)/ confirmed=仅已确权(确权变更)
export const catalogTree = (type, id, status) => request.get('/dpr/confirm/data-catalog/tree', { params: { type, id, status } })
// 按系统(+一级功能模块筛)列出库表卡片(确权范围 = 一个系统的多张表)
export const cardsBySystem = (sysName, modules, status) =>
  request.get('/dpr/confirm/data-catalog/cards', { params: { sysName, modules: modules && modules.length ? modules.join(',') : undefined, status } })
// 系统名→业务域映射(授权资源池「所属业务域」按系统逐表带出)
export const systemDomains = () => request.get('/dpr/confirm/data-catalog/system-domains')
// 数据资产目录全树(非懒加载):业务域→系统→功能模块→库表卡片(数据资产确权目录管理)
export const fullCatalogTree = () => request.get('/dpr/confirm/data-catalog/full-tree')
// 数据资产卡片明细(对齐 TW_DATA_CARD 卡片表结构 + AU_TABLE_META_DATA 产权元数据)
export const cardDetail = (sysName, tableCode) => request.get('/dpr/confirm/data-catalog/card-detail', { params: { sysName, tableCode } })
// 确权变更基线:某系统现有确权结论快照(变更前→后 diff 与预填用)
export const fetchChangeBaseline = (sysName) => request.get('/dpr/confirm/data-catalog/baseline', { params: { sysName } })
// 确权变更·完整基线:反查上一版真实确权结论(表3逐表行 + 权益卡片 + 认定意见)作变更底版
export const fetchChangeBaselineFull = (sysName, assetId) =>
  request.get('/dpr/confirm/data-catalog/baseline-full', { params: { sysName, assetId } })
// 确权变更·授权影响(逐表精确):入参=本次选中库表代码 + 变更触发,返回受影响授权+处置建议
export const fetchAuthImpact = (sysName, tableCodes, trigger) =>
  request.get('/dpr/confirm/data-catalog/auth-impact', {
    params: { sysName, tableCodes: tableCodes && tableCodes.length ? tableCodes.join(',') : undefined, trigger }
  })

/** 档案分页查询(只读):params = { current, size, keyword, state } */
export const pageAssetArchive = (params) => request.get(`${BASE}/archive`, { params })

/** 单卡「产权信息」契约 */
export const getAssetProperty = (assetId) => request.get(`${BASE}/${assetId}/property`)

/** 单卡「权益基本信息」条目列表 */
export const getAssetEquity = (assetId) => request.get(`${BASE}/${assetId}/equity`)
