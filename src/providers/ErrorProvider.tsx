import { createContext, useContext, useEffect, ReactNode, useMemo, useCallback } from 'react';
import { message, notification } from 'antd';
import { setGlobalErrorHandler } from '../api/request';
import ErrorBoundary from '../components/ErrorBoundary';

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

interface ErrorProviderProps {
  readonly children: ReactNode;
}

// 创建一个完整的错误处理上下文
interface ErrorContextType {
  handleError: (error: Error | string, customConfig?: Partial<ErrorConfig>) => void;
  showSuccess: (message: string, duration?: number) => void;
  showWarning: (message: string, duration?: number) => void;
  showInfo: (message: string, duration?: number) => void;
}

const ErrorContext = createContext<ErrorContextType | null>(null);

export function ErrorProvider({ children }: ErrorProviderProps) {
  // 在这里创建 message 和 notification 的实例
  const [messageApi, messageContextHolder] = message.useMessage();
  const [notificationApi, notificationContextHolder] = notification.useNotification();
  // 默认错误消息映射
  const DEFAULT_ERROR_MESSAGES: Record<ErrorType, string> = useMemo(
    () => ({
      [ErrorType.NETWORK]: '网络连接异常，请检查网络设置',
      [ErrorType.AUTH]: '身份验证失败，请重新登录',
      [ErrorType.BUSINESS]: '业务操作失败',
      [ErrorType.VALIDATION]: '数据验证失败',
      [ErrorType.PERMISSION]: '权限不足，无法执行此操作',
      [ErrorType.SYSTEM]: '系统错误，请稍后重试',
    }),
    [],
  );

  // 错误码映射配置
  const ERROR_CODE_MAPPING: Record<string, Omit<ErrorConfig, 'message'>> = useMemo(
    () => ({
      // 认证相关
      '401': {
        type: ErrorType.AUTH,
        showType: 'notification',
        duration: 5,
      },
      '403': {
        type: ErrorType.PERMISSION,
        showType: 'notification',
      },
      '400': {
        type: ErrorType.VALIDATION,
        showType: 'message',
      },
      '500': {
        type: ErrorType.SYSTEM,
        showType: 'notification',
        duration: 5,
      },
      NETWORK_ERROR: {
        type: ErrorType.NETWORK,
        showType: 'notification',
        duration: 5,
      },
    }),
    [],
  );

  // 从错误对象中提取错误码
  const extractErrorCode = useCallback((error: Error): string | null => {
    if (error.message.includes('Network Error') || error.message.includes('网络')) {
      return 'NETWORK_ERROR';
    }

    // 尝试从错误消息中提取HTTP状态码
    const statusRegex = /\b([4-5]\d{2})\b/;
    const statusMatch = statusRegex.exec(error.message);
    if (statusMatch) {
      return statusMatch[1];
    }

    return null;
  }, []);

  // 获取错误类型对应的标题
  const getErrorTitle = useCallback((type: ErrorType): string => {
    const titles: Record<ErrorType, string> = {
      [ErrorType.NETWORK]: '网络错误',
      [ErrorType.AUTH]: '认证错误',
      [ErrorType.BUSINESS]: '操作失败',
      [ErrorType.VALIDATION]: '数据验证错误',
      [ErrorType.PERMISSION]: '权限错误',
      [ErrorType.SYSTEM]: '系统错误',
    };
    return titles[type];
  }, []);

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

        if (errorCode && ERROR_CODE_MAPPING[errorCode]) {
          const mapping = ERROR_CODE_MAPPING[errorCode];
          errorConfig = {
            ...mapping,
            code: errorCode,
            message: errorMessage || DEFAULT_ERROR_MESSAGES[mapping.type],
          };
        } else {
          // 如果没有匹配的错误码，使用默认配置
          errorConfig = {
            type: ErrorType.BUSINESS,
            message: errorMessage || '未知错误',
            showType: 'message',
            duration: 3,
          };
        }
      }

      // 合并自定义配置
      if (customConfig) {
        errorConfig = { ...errorConfig, ...customConfig };
      }

      // 记录错误日志
      console.error('Error handled:', {
        type: errorConfig.type,
        code: errorConfig.code,
        message: errorConfig.message,
        originalError: error,
        timestamp: new Date().toISOString(),
      });

      // 根据显示类型展示错误
      if (errorConfig.showType === 'notification') {
        notificationApi.error({
          message: getErrorTitle(errorConfig.type),
          description: errorConfig.message,
          duration: errorConfig.duration || 4.5,
          placement: 'topRight',
        });
      } else if (errorConfig.showType === 'message') {
        messageApi.error({
          content: errorConfig.message,
          duration: errorConfig.duration || 3,
        });
      }
      // 如果是 'silent'，则不显示任何内容，只记录日志

      // 执行自定义动作
      if (errorConfig.action) {
        errorConfig.action();
      }
    },
    [messageApi, notificationApi, ERROR_CODE_MAPPING, DEFAULT_ERROR_MESSAGES, extractErrorCode, getErrorTitle],
  );

  // 成功消息
  const showSuccess = useCallback(
    (msg: string, duration = 3) => {
      messageApi.success({
        content: msg,
        duration,
      });
    },
    [messageApi],
  );

  // 警告消息
  const showWarning = useCallback(
    (msg: string, duration = 3) => {
      messageApi.warning({
        content: msg,
        duration,
      });
    },
    [messageApi],
  );

  // 信息消息
  const showInfo = useCallback(
    (msg: string, duration = 3) => {
      messageApi.info({
        content: msg,
        duration,
      });
    },
    [messageApi],
  );

  const contextValue = useMemo(
    () => ({
      handleError,
      showSuccess,
      showWarning,
      showInfo,
    }),
    [handleError, showSuccess, showWarning, showInfo],
  );

  useEffect(() => {
    // 设置全局错误处理器
    setGlobalErrorHandler(handleError);

    // 监听未捕获的Promise错误
    const handleUnhandledRejection = (event: PromiseRejectionEvent) => {
      console.error('Unhandled promise rejection:', event.reason);
      handleError(event.reason instanceof Error ? event.reason : new Error(String(event.reason)));
      // 阻止默认行为，避免在控制台显示错误
      event.preventDefault();
    };

    // 监听全局JavaScript错误
    const handleGlobalError = (event: ErrorEvent) => {
      console.error('Global error:', event.error);
      handleError(event.error instanceof Error ? event.error : new Error(event.message));
    };

    // 监听资源加载错误
    const handleResourceError = (event: Event) => {
      const target = event.target as HTMLElement;
      if (target) {
        const errorMessage = `资源加载失败: ${target.tagName}`;
        console.error('Resource loading error:', target);
        handleError(new Error(errorMessage));
      }
    };

    window.addEventListener('unhandledrejection', handleUnhandledRejection);
    window.addEventListener('error', handleGlobalError);
    window.addEventListener('error', handleResourceError, true);

    return () => {
      window.removeEventListener('unhandledrejection', handleUnhandledRejection);
      window.removeEventListener('error', handleGlobalError);
      window.removeEventListener('error', handleResourceError, true);
    };
  }, [handleError]);

  return (
    <ErrorContext.Provider value={contextValue}>
      <ErrorBoundary onError={(error) => handleError(error)}>
        {/* Antd 的 message 和 notification 容器 */}
        {messageContextHolder}
        {notificationContextHolder}
        {children}
      </ErrorBoundary>
    </ErrorContext.Provider>
  );
}

// 导出 hook 供组件使用
export function useErrorContext() {
  const context = useContext(ErrorContext);
  if (!context) {
    throw new Error('useErrorContext must be used within an ErrorProvider');
  }
  return context;
}
