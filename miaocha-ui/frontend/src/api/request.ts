import axios, { type InternalAxiosRequestConfig } from 'axios';
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios';
import NProgress from 'nprogress';
import { setTokens } from '../store/userSlice';
import { store } from '../store/store';
import { ENV_CONFIG } from '../config/env';
import 'nprogress/nprogress.css';

// 定义全局错误处理器
let globalErrorHandler: ((error: Error) => void) | null = null;

export function setGlobalErrorHandler(handler: (error: Error) => void) {
  globalErrorHandler = handler;
}

NProgress.configure({
  showSpinner: false, // 是否显示右上角的转圈加载图标
});

// 创建axios实例
const service: AxiosInstance = axios.create({
  baseURL: ENV_CONFIG.API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 请求拦截器
service.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  NProgress.start(); // 开始进度条
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
    NProgress.done(); // 结束进度条
    // 如果是blob类型，直接返回原始响应
    if (response.config.responseType === 'blob') {
      return response;
    }
    // 处理EventSource响应
    if (response.config.responseType === 'text' && response.headers['content-type']?.includes('text/event-stream')) {
      console.log('EventSource response received:', response);
      // 创建新的EventSource对象
      const eventSource = new EventSource(response.request.responseURL, {
        withCredentials: true,
      });

      // 添加错误处理
      eventSource.onerror = (err) => {
        console.error('EventSource error:', err);
        eventSource.close();
      };

      return eventSource;
    }

    const res = response.data;
    const { success, errorMessage } = res?.data || {}; // 后端接口返回结构调整
    const isBizError = !success && errorMessage;
    const isCodeError = res.code !== '0000';
    const message = errorMessage || res.message || '操作失败';
    // 根据后端接口返回结构调整
    if (isBizError || isCodeError) {
      console.log('isError', response);
      const error = new Error(message);
      // 使用全局错误处理器
      if (globalErrorHandler) {
        globalErrorHandler(error);
      }
      return Promise.reject(error);
    }
    return res.data;
  },
  async (requestError) => {
    NProgress.done(); // 结束进度条
    const originalRequest = requestError.config;
    const res = requestError.response?.data || {};
    const status = requestError.response?.status;
    let errorMessage = requestError.response?.data?.message || requestError.message || '请求失败';

    // 常见HTTP状态码友好提示
    const statusMessageMap: Record<number, string> = {
      400: '请求参数错误',
      401: '未授权或登录已过期',
      403: '没有权限访问',
      404: '请求地址不存在',
      408: '请求超时',
      413: '请求实体过大',
      415: '请求类型不支持',
      429: '请求过于频繁',
      500: '服务器异常',
      502: '网关错误',
      503: '服务不可用',
      504: '网关超时',
    };
    if (status && statusMessageMap[status] && !errorMessage) {
      errorMessage = statusMessageMap[status];
    }

    if (requestError.code === 'ERR_CANCELED') return Promise.reject(new Error('请求已取消'));

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
      } catch (refreshError) {
        // 刷新token失败，跳转到登录页
        retryQueue = [];
        window.location.href = '/login';
        const error = refreshError instanceof Error ? refreshError : new Error('Token refresh failed');
        if (globalErrorHandler) {
          globalErrorHandler(error);
        }
        return Promise.reject(error);
      } finally {
        isRefreshing = false;
      }
    }

    // 创建标准化的错误对象
    const finalError = new Error(errorMessage);
    // 添加状态码信息到错误对象
    if (status) {
      Object.assign(finalError, { status, code: status.toString() });
    }

    // 使用全局错误处理器
    if (globalErrorHandler) {
      globalErrorHandler(finalError);
    }

    return Promise.reject(finalError);
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

// 封装DELETE请求
export function del<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return request({ ...config, method: 'DELETE', url, data });
}
