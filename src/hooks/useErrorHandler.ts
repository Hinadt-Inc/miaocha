import { message, notification } from 'antd';
import { useCallback, createContext, useContext } from 'react';

// 错误类型枚举
export enum ErrorType {
  NETWORK = 'NETWORK',
  AUTH = 'AUTH',
  BUSINESS = 'BUSINESS',
  VALIDATION = 'VALIDATION',
  PERMISSION = 'PERMISSION',
  SYSTEM = 'SYSTEM',
}

// 错误配置接口
export interface ErrorConfig {
  type: ErrorType;
  code?: string;
  message: string;
  showType?: 'message' | 'notification' | 'silent';
  duration?: number;
  action?: () => void;
}

// API Context
interface APIContext {
  messageApi: any;
  notificationApi: any;
}

const APIContext = createContext<APIContext | null>(null);

export { APIContext };

// 默认错误消息映射
const DEFAULT_ERROR_MESSAGES: Record<ErrorType, string> = {
  [ErrorType.NETWORK]: '网络连接异常，请检查网络设置',
  [ErrorType.AUTH]: '身份验证失败，请重新登录',
  [ErrorType.BUSINESS]: '业务操作失败',
  [ErrorType.VALIDATION]: '数据验证失败',
  [ErrorType.PERMISSION]: '权限不足，无法执行此操作',
  [ErrorType.SYSTEM]: '系统异常，请稍后重试',
};

// 错误码映射规则
const ERROR_CODE_MAPPING: Record<string, ErrorConfig> = {
  // 网络相关
  ERR_NETWORK: {
    type: ErrorType.NETWORK,
    message: '网络连接失败，请检查网络连接',
    showType: 'notification',
  },
  ERR_TIMEOUT: {
    type: ErrorType.NETWORK,
    message: '请求超时，请稍后重试',
    showType: 'message',
  },
  ERR_CANCELED: {
    type: ErrorType.NETWORK,
    message: '请求已取消',
    showType: 'silent',
  },

  // 认证相关
  '401': {
    type: ErrorType.AUTH,
    message: '登录状态已过期，即将跳转到登录页',
    showType: 'notification',
    duration: 3,
    action: () => {
      setTimeout(() => {
        window.location.href = '/login';
      }, 1500);
    },
  },
  '3102': {
    type: ErrorType.AUTH,
    message: 'Token已过期，正在自动刷新...',
    showType: 'message',
    duration: 2,
  },

  // 权限相关
  '403': {
    type: ErrorType.PERMISSION,
    message: '权限不足，无法访问此资源',
    showType: 'notification',
  },

  // 业务相关
  '400': {
    type: ErrorType.VALIDATION,
    message: '请求参数有误，请检查输入信息',
    showType: 'message',
  },
  '404': {
    type: ErrorType.BUSINESS,
    message: '请求的资源不存在',
    showType: 'message',
  },

  // 系统相关
  '500': {
    type: ErrorType.SYSTEM,
    message: '服务器内部错误，请联系管理员',
    showType: 'notification',
  },
  '502': {
    type: ErrorType.SYSTEM,
    message: '服务暂时不可用，请稍后重试',
    showType: 'notification',
  },
  '503': {
    type: ErrorType.SYSTEM,
    message: '服务维护中，请稍后重试',
    showType: 'notification',
  },
};

export function useErrorHandler() {
  // 尝试从 Context 获取 API，如果没有则使用默认的
  const apiContext = useContext(APIContext);

  let messageApi: any;
  let notificationApi: any;

  if (apiContext) {
    messageApi = apiContext.messageApi;
    notificationApi = apiContext.notificationApi;
  } else {
    // 如果没有 Context，使用默认的 hooks（这可能不会显示消息）
    const [defaultMessageApi] = message.useMessage();
    const [defaultNotificationApi] = notification.useNotification();
    messageApi = defaultMessageApi;
    notificationApi = defaultNotificationApi;
  }

  // 处理错误的核心方法
  const handleError = useCallback(
    (error: Error | string, customConfig?: Partial<ErrorConfig>) => {
      let errorConfig: ErrorConfig;

      if (typeof error === 'string') {
        // 如果是字符串，创建默认配置
        errorConfig = {
          type: ErrorType.BUSINESS,
          message: error,
          showType: 'message',
          duration: 3,
        };
      } else {
        // 如果是Error对象，尝试根据错误信息匹配配置
        const errorMessage = error.message || '';
        const errorCode = extractErrorCode(error);

        errorConfig = ERROR_CODE_MAPPING[errorCode] || {
          type: ErrorType.BUSINESS,
          message: errorMessage || DEFAULT_ERROR_MESSAGES[ErrorType.BUSINESS],
          showType: 'message',
          duration: 3,
        };
      }

      // 合并自定义配置
      const finalConfig = { ...errorConfig, ...customConfig };

      // 根据显示类型展示错误
      showError(finalConfig);

      // 执行自定义动作
      if (finalConfig.action) {
        finalConfig.action();
      }

      // 可以在这里添加错误日志上报
      logError(error, finalConfig);
    },
    [messageApi, notificationApi],
  );

  // 显示错误信息
  const showError = useCallback(
    (config: ErrorConfig) => {
      const { showType, message: msg, duration = 3, type } = config;

      switch (showType) {
        case 'message':
          messageApi.error({
            content: msg,
            duration,
          });
          break;

        case 'notification':
          notificationApi.error({
            message: getErrorTitle(type),
            description: msg,
            duration,
            placement: 'topRight',
          });
          break;

        case 'silent':
          // 静默处理，只记录日志
          console.warn('Silent error:', msg);
          break;
      }
    },
    [messageApi, notificationApi],
  );

  // 快捷方法
  const showSuccess = useCallback(
    (message: string, duration = 2) => {
      messageApi.success({
        content: message,
        duration,
      });
    },
    [messageApi],
  );

  const showWarning = useCallback(
    (message: string, duration = 3) => {
      messageApi.warning({
        content: message,
        duration,
      });
    },
    [messageApi],
  );

  const showInfo = useCallback(
    (message: string, duration = 2) => {
      messageApi.info({
        content: message,
        duration,
      });
    },
    [messageApi],
  );

  return {
    handleError,
    showSuccess,
    showWarning,
    showInfo,
    showError,
  };
}

// 辅助函数：从错误对象中提取错误码
function extractErrorCode(error: Error): string {
  // 从错误消息中提取状态码
  const statusRegex = /(\d{3})/;
  const statusMatch = statusRegex.exec(error.message);
  if (statusMatch) {
    return statusMatch[1];
  }

  // 检查特定的错误类型
  if (error.message.includes('Network Error')) {
    return 'ERR_NETWORK';
  }
  if (error.message.includes('timeout')) {
    return 'ERR_TIMEOUT';
  }
  if (error.message.includes('canceled')) {
    return 'ERR_CANCELED';
  }

  return 'UNKNOWN';
}

// 获取错误标题
function getErrorTitle(type: ErrorType): string {
  const titles: Record<ErrorType, string> = {
    [ErrorType.NETWORK]: '网络错误',
    [ErrorType.AUTH]: '认证错误',
    [ErrorType.BUSINESS]: '操作失败',
    [ErrorType.VALIDATION]: '验证错误',
    [ErrorType.PERMISSION]: '权限错误',
    [ErrorType.SYSTEM]: '系统错误',
  };
  return titles[type] || '错误';
}

// 错误日志记录
function logError(error: Error | string, config: ErrorConfig) {
  // 这里可以集成错误监控服务，如 Sentry、LogRocket 等
  console.error('Error logged:', {
    error: typeof error === 'string' ? error : error.message,
    config,
    stack: typeof error === 'object' ? error.stack : undefined,
    timestamp: new Date().toISOString(),
    userAgent: navigator.userAgent,
    url: window.location.href,
  });
}
