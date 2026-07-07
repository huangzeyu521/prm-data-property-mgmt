import request from './request'
import { ElMessage } from 'element-plus'
import { sanitizeLoginRole, isForbiddenLoginRole } from '@/lib/roles'

// 内建认证(生产由 4A 接管登录)
export const login = (username, password) => request.post('/auth/login', { username, password })
export const fetchMe = () => request.get('/auth/me')

// 本地票据/身份管理
export function saveSession(token, user) {
  localStorage.setItem('prm-token', token)
  // 软护栏:登录账号角色若为 'all'/空(误配成超级视角=关闭 RBAC),告警并降级为只读 view。
  const role = sanitizeLoginRole(user.role)
  if (isForbiddenLoginRole(user.role)) {
    const who = user.username || user.realName || user.userId || '该账号'
    console.warn(`[RBAC 软护栏] 登录账号「${who}」返回角色「${user.role || '空'}」:'all'=超级视角不应赋给真实账号(等于关闭全部 RBAC),已降级为只读「${role}」。请核查账号角色配置。`)
    try { ElMessage.warning({ message: `账号角色配置异常(${user.role || '空角色'}),已按「只读」视角登录;请联系管理员核查角色分配。`, duration: 6000, grouping: true }) } catch (e) { /* 无 DOM 容错 */ }
  }
  localStorage.setItem('prm-role', role)
  localStorage.setItem('X-User-Id', user.userId || '')
  localStorage.setItem('X-User-Name', user.realName || '')
  localStorage.setItem('X-Province-Code', user.provinceCode || '')
  localStorage.setItem('prm-user', JSON.stringify({ ...user, role }))
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
