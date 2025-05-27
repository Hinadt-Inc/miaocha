import axios, { type InternalAxiosRequestConfig } from 'axios';
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios';
import { setTokens } from '../store/userSlice';
import { store } from '../store/store';

// 创建axios实例
const service: AxiosInstance = axios.create({
  timeout: 100000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 请求拦截器
service.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`;
  }
  return config;
});

// 是否正在刷新token
let isRefreshing = false;
// 重试队列
let retryQueue: Array<() => void> = [];

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse) => {
    // 如果是blob类型，直接返回原始响应
    if (response.config.responseType === 'blob') {
      return response;
    }

    const res = response.data;
    const { success, errorMessage } = res?.data || {}; // 后端接口返回结构调整
    const isBizError = !success && errorMessage;
    const isCodeError = res.code !== '0000';
    const message = errorMessage || res.message || '操作失败';
    // 根据后端接口返回结构调整
    if (isBizError || isCodeError) {
      window.dispatchEvent(
        new CustomEvent('unhandledrejection', {
          detail: {
            reason: new Error(message),
          },
        }),
      );
      return Promise.reject(new Error(message));
    }
    return res.data;
  },
  async (error) => {
    const originalRequest = error.config;
    const res = error.response?.data || {};
    const status = error.response?.status;
    let errorMessage = error.response?.data?.message || error.message || '请求失败';

    // 如果是401错误且不是刷新token请求
    if (status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        // 如果正在刷新token，将请求加入队列
        return new Promise((resolve) => {
          retryQueue.push(() => {
            resolve(service(originalRequest));
          });
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        // 判断code
        if (res.code !== '3102') {
          throw new Error(res.message || '无效的令牌');
        }

        const refreshToken = localStorage.getItem('refreshToken');
        if (!refreshToken) {
          throw new Error('No refresh token available');
        }

        // 调用刷新token接口
        const data = await service.post<IRefreshTokenParams, ILoginResponse>('/api/auth/refresh', {
          refreshToken,
        });

        // 存储新token并更新store
        localStorage.setItem('accessToken', data.token);
        localStorage.setItem('refreshToken', data.refreshToken);
        store.dispatch(
          setTokens({
            accessToken: data.token,
            refreshToken: data.refreshToken,
            expiresAt: data.expiresAt,
            refreshExpiresAt: data.refreshExpiresAt,
          }),
        );

        // 重试队列中的请求
        retryQueue.forEach((cb) => cb());
        retryQueue = [];

        // 重试原始请求
        return service(originalRequest);
      } catch (err) {
        // 刷新token失败，跳转到登录页
        retryQueue = [];
        debugger;
        window.location.href = '/login';
        return Promise.reject(err);
      } finally {
        isRefreshing = false;
      }
    } else {
      // 处理其他HTTP错误

      switch (status) {
        case 400:
          errorMessage = '请求参数错误';
          break;
        case 403:
          errorMessage = '无权限访问该资源';
          break;
        case 404:
          errorMessage = '请求的资源不存在';
          break;
        case 408:
          errorMessage = '请求超时';
          break;
        case 500:
          errorMessage = '服务器内部错误';
          break;
        case 502:
          errorMessage = '网关错误';
          break;
        case 503:
          errorMessage = '服务不可用';
          break;
        case 504:
          errorMessage = '网关超时';
          break;
        default:
          errorMessage = errorMessage;
      }
      window.dispatchEvent(
        new CustomEvent('unhandledrejection', {
          detail: {
            reason: new Error(errorMessage),
          },
        }),
      );
    }
    return Promise.reject(new Error(errorMessage));
  },
);

// 封装通用请求方法
export function request<T = unknown>(config: AxiosRequestConfig): Promise<T> {
  return service(config);
}

// 封装GET请求
export function get<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return request({ ...config, method: 'GET', url });
}

// 封装POST请求
export function post<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return request({ ...config, method: 'POST', url, data });
}
