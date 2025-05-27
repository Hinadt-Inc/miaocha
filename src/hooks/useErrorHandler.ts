import { notification } from 'antd';
import { useEffect } from 'react';

const useErrorHandler = () => {
  const [notificationApi, contextHolder] = notification.useNotification();

  const notificationConfig = {
    message: '提示',
    duration: 3,
    showProgress: true,
  };

  const handleUnhandledRejection = (event: any) => {
    event.preventDefault();
    const description = event?.reason?.message || event?.detail?.reason?.message || '业务发生未知错误，请联系开发人员';
    console.error('【全局1】======Unhandled promise rejection:', description);
    notificationApi.error({
      description,
      ...notificationConfig,
    });
  };

  const handleGlobalError = (event: ErrorEvent) => {
    event.preventDefault();
    const description = event.message || '发生未知错误，请联系开发人员';
    console.error('【全局2】======Uncaught error:', description);
    notificationApi.error({
      description,
      ...notificationConfig,
    });
  };

  useEffect(() => {
    window.addEventListener('unhandledrejection', handleUnhandledRejection);
    window.addEventListener('error', handleGlobalError);

    return () => {
      window.removeEventListener('unhandledrejection', handleUnhandledRejection);
      window.removeEventListener('error', handleGlobalError);
    };
  }, []);

  return { contextHolder };
};

export default useErrorHandler;
