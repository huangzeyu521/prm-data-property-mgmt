import axios from 'axios'
import { ElMessage } from 'element-plus'

// 统一请求封装:对接后端统一响应信封 {code, msg, data}
const request = axios.create({
  baseURL: '/api',
  timeout: 60000 // 真调大模型接口耗时数十秒,普通接口本就秒回不受影响
})

// 请求拦截:透传用户上下文(正式环境由 4A 网关注入,此处便于本地联调)
request.interceptors.request.use((config) => {
  const userId = localStorage.getItem('X-User-Id')
  if (userId) {
    config.headers['X-User-Id'] = userId
    config.headers['X-User-Name'] = localStorage.getItem('X-User-Name') || ''
    config.headers['X-Province-Code'] = localStorage.getItem('X-Province-Code') || ''
  }
  return config
})

// 响应拦截:成功返回 data;失败用浮动提示(符合界面提示规范),并向上抛出
request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res && typeof res.code !== 'undefined') {
      if (res.code === 0) {
        return res.data
      }
      ElMessage({ type: 'error', message: res.msg || '请求处理失败', grouping: true })
      return Promise.reject(new Error(res.msg || 'error'))
    }
    return res
  },
  (error) => {
    ElMessage({ type: 'error', message: '网络异常,请稍后重试', grouping: true })
    return Promise.reject(error)
  }
)

export default request
