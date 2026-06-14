import request from './request'

// 内建认证(生产由 4A 接管登录)
export const login = (username, password) => request.post('/auth/login', { username, password })
export const fetchMe = () => request.get('/auth/me')

// 本地票据/身份管理
export function saveSession(token, user) {
  localStorage.setItem('prm-token', token)
  localStorage.setItem('prm-role', user.role || 'all')
  localStorage.setItem('X-User-Id', user.userId || '')
  localStorage.setItem('X-User-Name', user.realName || '')
  localStorage.setItem('X-Province-Code', user.provinceCode || '')
  localStorage.setItem('prm-user', JSON.stringify(user))
}

export function clearSession() {
  for (const k of ['prm-token', 'prm-role', 'X-User-Id', 'X-User-Name', 'X-Province-Code', 'prm-user']) {
    localStorage.removeItem(k)
  }
}

export function currentUser() {
  try { return JSON.parse(localStorage.getItem('prm-user') || 'null') } catch { return null }
}

export function isLoggedIn() {
  return !!localStorage.getItem('prm-token')
}
