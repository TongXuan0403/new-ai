import axios from 'axios'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('codex-ai-token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

const SUCCESS_CODES = new Set(['200', 200, 0])

request.interceptors.response.use(
  (response) => {
    const body = response.data
    // SSE 等非统一结构直接返回
    if (body == null || typeof body !== 'object' || !('code' in body)) {
      return body
    }
    if (SUCCESS_CODES.has(body.code)) {
      return body.data
    }
    const error = new Error(body.message || '操作失败')
    error.code = body.code
    throw error
  },
  (error) => {
    if (error.response) {
      const status = error.response.status
      if (status === 401) {
        const authEvent = new CustomEvent('codex-unauthorized')
        window.dispatchEvent(authEvent)
      }
      const data = error.response.data
      const message = data?.message || `请求失败（${status}）`
      const wrapped = new Error(message)
      wrapped.code = data?.code || String(status)
      throw wrapped
    }
    throw new Error('网络连接失败，请检查后端服务')
  }
)

export default request
