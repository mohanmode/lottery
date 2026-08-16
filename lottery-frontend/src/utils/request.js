import axios from 'axios'
import { ElMessage } from 'element-plus'

// 统一 Axios 封装
const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 30000
})

service.interceptors.request.use((config) => {
  config.headers['X-Requested-With'] = 'XMLHttpRequest'
  return config
}, (err) => Promise.reject(err))

service.interceptors.response.use(
  (resp) => {
    const body = resp.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 0) return body.data
      ElMessage.error(body.msg || `请求失败 (code=${body.code})`)
      return Promise.reject(new Error(body.msg || 'Request Failed'))
    }
    return body
  },
  (err) => {
    const msg = err?.response?.data?.msg || err.message || '网络错误'
    ElMessage.error('❌ ' + msg)
    return Promise.reject(err)
  }
)

export default service
