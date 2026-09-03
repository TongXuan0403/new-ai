import axios from 'axios'
import { ElMessage } from 'element-plus'

// 创建axios实例
const service = axios.create({
  baseURL: '/api', // 请求的前缀
  timeout: 15000, // 请求的超时时间
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    // 在发送请求之前做些什么
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['token'] = token
    }
    return config
  },
  (error) => {
    // 对请求错误做些什么
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response) => {
    // 对响应数据做点什么
    const { data, config } = response
    // 非 JSON 结构（如文件流）直接返回
    if (!data || typeof data !== 'object' || !('code' in data)) {
      return data
    }
    // 处理业务状态码
    if (String(data.code) === '200') {
      return data.data
    }
    // -1：登录态失效
    if (String(data.code) === '-1') {
      if (!config.url?.includes('/login')) {
        ElMessage.error(data.msg || data.message || '登录过期，请重新登录')
        // 清除登录信息
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        window.location.href = '/auth/login'
      } else {
        ElMessage.error(data.msg || data.message || '用户名或密码错误')
      }
      return Promise.reject(data)
    }
    // 其他业务错误：统一提示并 reject，保证调用方拿到的结构一致
    ElMessage.error(data.msg || data.message || '请求失败，请稍后重试')
    return Promise.reject(data)
  },
  (error) => {
    // 网络层错误
    const status = error?.response?.status
    const hasToken = !!localStorage.getItem('token')
    // 401：未认证；403 且本地有登录态：登录失效/权限不足，清除并跳登录
    if (status === 401 || (status === 403 && hasToken)) {
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      if (window.location.pathname.startsWith('/back')) {
        window.location.href = '/auth/login'
      } else if (status === 401) {
        window.location.href = '/auth/login'
      }
      return Promise.reject(error)
    }
    // 游客访问前台接口被拒（如未登录 403）：静默处理，由页面维持空态
    if (status === 403 && !hasToken) {
      return Promise.reject(error)
    }
    ElMessage.error(error?.message?.includes('timeout') ? '请求超时，请稍后重试' : '网络异常，请检查网络连接')
    return Promise.reject(error)
  }
)

export default service
