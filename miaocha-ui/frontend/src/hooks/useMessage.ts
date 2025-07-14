import { message } from 'antd';
import { useErrorContext } from '../providers/ErrorProvider';

export function useMessage() {
  const [messageApi] = message.useMessage();

  // 尝试获取新的错误处理上下文，如果不存在则使用原有逻辑
  let errorHandler: ReturnType<typeof useErrorContext> | null = null;
  try {
    errorHandler = useErrorContext();
  } catch {
    // 如果不在 ErrorProvider 上下文中，使用原有逻辑
  }

  const showMessage = (type: 'success' | 'error' | 'info' | 'warning', content: string) => {
    messageApi[type]({
      content,
      duration: 2,
    });
  };

  return {
    success: (content: string) => {
      if (errorHandler) {
        errorHandler.showSuccess(content);
      } else {
        showMessage('success', content);
      }
    },
    error: (content: string) => {
      if (errorHandler) {
        errorHandler.handleError(content);
      } else {
        showMessage('error', content);
      }
    },
    info: (content: string) => {
      if (errorHandler) {
        errorHandler.showInfo(content);
      } else {
        showMessage('info', content);
      }
    },
    warning: (content: string) => {
      if (errorHandler) {
        errorHandler.showWarning(content);
      } else {
        showMessage('warning', content);
      }
    },
    // 保持向后兼容
    messageApi,
  };
}
