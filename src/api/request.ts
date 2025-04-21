import axios, { type InternalAxiosRequestConfig } from 'axios'
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import { setTokens } from '../store/userSlice'
import { store } from '../store/store'

// 创建axios实例
const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // 添加token到请求头
    const token = localStorage.getItem('accessToken')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 是否正在刷新token
let isRefreshing = false
// 重试队列
let retryQueue: Array<() => void> = []

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data
    // 根据后端接口返回结构调整
    if (res.code !== '0000') {
      // 处理业务错误
      return Promise.reject(new Error(res.message || 'Error'))
    }
    return res
  },
  async (error) => {
    const originalRequest = error.config
    
    // 如果是401错误且不是刷新token请求
    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        // 如果正在刷新token，将请求加入队列
        return new Promise((resolve) => {
          retryQueue.push(() => {
            resolve(service(originalRequest))
          })
        })
      }
      
      originalRequest._retry = true
      isRefreshing = true
      
      try {
        const refreshToken = localStorage.getItem('refreshToken')
        if (!refreshToken) {
          throw new Error('No refresh token available')
        }
        
        // 调用刷新token接口
        const { data } = await service.post('/api/auth/refresh', {
          refreshToken
        })
        
        // 存储新token并更新store
        localStorage.setItem('accessToken', data.accessToken)
        localStorage.setItem('refreshToken', data.refreshToken)
        store.dispatch(setTokens({
          accessToken: data.accessToken,
          refreshToken: data.refreshToken
        }))
        
        // 重试队列中的请求
        retryQueue.forEach(cb => cb())
        retryQueue = []
        
        // 重试原始请求
        return service(originalRequest)
      } catch (err) {
        // 刷新token失败，跳转到登录页
        retryQueue = []
        window.location.href = '/login'
        return Promise.reject(err)
      } finally {
        isRefreshing = false
      }
    }
    
    // 处理其他HTTP错误
    return Promise.reject(error)
  }
)

// 封装通用请求方法
export function request<T = unknown>(config: AxiosRequestConfig): Promise<T> {
  return service(config)
}

// 封装GET请求
export function get<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return request({ ...config, method: 'GET', url })
}

// 封装POST请求
export function post<T = unknown>(
  url: string,
  data?: unknown,
  config?: AxiosRequestConfig
): Promise<T> {
  return request({ ...config, method: 'POST', url, data })
}

// 其他请求方法可以根据需要继续封装...
