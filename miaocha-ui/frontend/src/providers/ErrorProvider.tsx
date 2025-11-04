// todo 删除

import { App } from 'antd';

// 错误类型枚举
export enum ErrorType {
  NETWORK = 'NETWORK', // 网络错误
  AUTH = 'AUTH', // 身份认证错误
  BUSINESS = 'BUSINESS', // 业务操作错误
  VALIDATION = 'VALIDATION', // 数据验证错误
  PERMISSION = 'PERMISSION', // 权限错误
  SYSTEM = 'SYSTEM', // 系统错误
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

// 导出 hook 供组件使用
export const useErrorContext = () => {
  const { message } = App.useApp();
  const handleError = (content: string, errorConfig: any) => {
    message.error(content);
  };
  const showSuccess = (content: string) => {
    message.success(content);
  };
  return {
    handleError,
    showSuccess
  };
};
