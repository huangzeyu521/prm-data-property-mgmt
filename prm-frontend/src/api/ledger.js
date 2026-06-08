import request from './request'

// 产权信息台账相关接口
export const getOverview = () => request.get('/dpr/ledger/overview')
export const getPropertyTree = () => request.get('/dpr/ledger/tree')
export const pageChangeRecord = (query) => request.post('/dpr/ledger/change-record/page', query)
export const pageAsset = (query) => request.post('/dpr/ledger/asset/page', query)
export const getAsset = (assetId) => request.get(`/dpr/ledger/asset/${assetId}`)
export const syncAssets = (list) => request.post('/dpr/ledger/asset/sync', list)
export const getLedgerStatistics = () => request.get('/dpr/ledger/statistics')

// 国密能力(⑩ SM2/SM3/SM4)+ 等保基线
export const gmInfo = () => request.get('/dpr/security/gm/info')
export const gmSm4Encrypt = (plain) => request.post('/dpr/security/gm/sm4-encrypt', null, { params: { plain } })
export const gmSm4Decrypt = (cipher) => request.post('/dpr/security/gm/sm4-decrypt', null, { params: { cipher } })
export const gmSm2Sign = (data) => request.post('/dpr/security/gm/sm2-sign', null, { params: { data } })
export const gmSm2Verify = (data, sign) => request.post('/dpr/security/gm/sm2-verify', null, { params: { data, sign } })

// 敏感信息保险箱(SM4 加密留痕)
export const pageVault = (params) => request.get('/dpr/security/vault/page', { params })
export const storeVault = (params) => request.post('/dpr/security/vault/store', null, { params })
export const readVault = (id) => request.get(`/dpr/security/vault/${id}/read`)
export const verifyVault = (id) => request.get(`/dpr/security/vault/${id}/verify`)
