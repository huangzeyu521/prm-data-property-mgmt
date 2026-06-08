import request from './request'

const BASE = '/dpr/ledger/archive'

export const pageArchive = (query) => request.post(`${BASE}/page`, query)
export const createArchive = (data) => request.post(BASE, data)
export const updateArchive = (data) => request.put(BASE, data)
export const deleteArchive = (archiveId) => request.delete(`${BASE}/${archiveId}`)
export const getArchive = (archiveId) => request.get(`${BASE}/${archiveId}`)
