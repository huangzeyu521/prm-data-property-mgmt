import axios from 'axios'
import { ElMessage } from 'element-plus'

// 统一请求封装:对接后端统一响应信封(data_pod 规范){code, message, data, timestamp};code=200 表示成功
const request = axios.create({
  baseURL: '/api',
  timeout: 60000 // 真调大模型接口耗时数十秒,普通接口本就秒回不受影响
})

// 请求拦截:带 JWT(内建登录) + 透传用户上下文头(4A 网关注入/本地联调兼容)
request.interceptors.request.use((config) => {
  const token = localStorage.getItem('prm-token')
  if (token) config.headers['Authorization'] = 'Bearer ' + token
  const userId = localStorage.getItem('X-User-Id')
  if (userId) {
    config.headers['X-User-Id'] = userId
    config.headers['X-User-Name'] = localStorage.getItem('X-User-Name') || ''
    config.headers['X-Province-Code'] = localStorage.getItem('X-Province-Code') || ''
  }
  // 分页入参契约对齐 data_pod 基准 PageRequest:前端内部沿用 current/size,在出站边界(仅 /page 端点)
  // 统一映射为 pageNum/pageSize,避免逐页改造;el-pagination 仍按 current/size 工作。
  if (config.url && config.url.includes('/page')) {
    const toBaseline = (o) => {
      if (!o || typeof o !== 'object' || Array.isArray(o)) return o
      const r = { ...o }
      if ('current' in r) { r.pageNum = r.current; delete r.current }
      if ('size' in r) { r.pageSize = r.size; delete r.size }
      return r
    }
    if (config.params) config.params = toBaseline(config.params)
    if (config.data && typeof config.data === 'object') config.data = toBaseline(config.data)
  }
  return config
})

// 响应拦截:成功返回 data;失败用浮动提示(符合界面提示规范),并向上抛出
request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res && typeof res.code !== 'undefined') {
      if (res.code === 200) {
        return res.data
      }
      ElMessage({ type: 'error', message: res.message || '请求处理失败', grouping: true })
      return Promise.reject(new Error(res.message || 'error'))
    }
    return res
  },
  (error) => {
    const code = error?.response?.data?.code
    if (code === 401) {
      // 登录失效:清票据并回登录页
      localStorage.removeItem('prm-token')
      ElMessage({ type: 'warning', message: '登录已失效,请重新登录', grouping: true })
      if (location.pathname !== '/login') location.href = '/login'
      return Promise.reject(error)
    }
    if (code === 403) {
      ElMessage({ type: 'error', message: error?.response?.data?.message || '无访问权限', grouping: true })
      return Promise.reject(error)
    }
    ElMessage({ type: 'error', message: '网络异常,请稍后重试', grouping: true })
    return Promise.reject(error)
  }
)

export default request
