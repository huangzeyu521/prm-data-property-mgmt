import request from './request'

// 系统管理:用户 / 角色 / 操作日志(后端 dpr-confirm-service,/api/dpr/system/*,仅管理员)
const USER = '/dpr/system/user'

// —— 用户管理 ——
export const pageUser = (query) => request.post(`${USER}/page`, query)
export const createUser = (data) => request.post(USER, data)
export const updateUser = (data) => request.put(USER, data)
export const deleteUser = (userId) => request.delete(`${USER}/${userId}`)
export const resetPassword = (userId) => request.post(`${USER}/${userId}/reset-password`)
export const toggleUserStatus = (userId) => request.post(`${USER}/${userId}/toggle-status`)

// —— 角色管理(只读目录 + 用户数统计)——
export const listRoles = () => request.get('/dpr/system/role')

// —— 操作日志 ——
export const pageOpLog = (query) => request.post('/dpr/system/oplog/page', query)
