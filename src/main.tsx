import { useEffect } from 'react';
import { notification } from 'antd';
import { createRoot } from 'react-dom/client';
import { Provider, useDispatch } from 'react-redux';
import { RouterProvider } from 'react-router-dom';
import { QueryProvider } from './providers/QueryProvider';
import { LoadingProvider } from './providers/LoadingProvider';
import { store, type AppDispatch } from './store/store';
import { restoreSession } from './store/userSlice';
import { router } from './routes';
import './index.less';

// 会话恢复组件
const SessionInitializer = () => {
  const dispatch = useDispatch<AppDispatch>();

  useEffect(() => {
    // 在应用启动时恢复会话
    dispatch(restoreSession());
  }, [dispatch]);

  return null;
};

const Error = ({ children }: any) => {
  const [notificationApi, contextHolder] = notification.useNotification();
  const notificationConfig: any = {
    message: '提示',
    showProgress: true,
    placement: 'top',
  };

  const handleUnhandledRejection = (event: any) => {
    event.preventDefault();
    const description = event?.detail?.reason?.message || '业务发生未知错误，请联系开发人员';
    console.error('【全局1】======Unhandled promise rejection:', description);
    notificationApi.error({
      description,
      ...notificationConfig,
    });
  };

  // 未捕获的错误
  const handleGlobalError = (event: ErrorEvent) => {
    // 阻止默认错误处理(如控制台输出)
    event.preventDefault();
    const description = event?.message || '发生未知错误，请联系开发人员';
    console.error('【全局2】======Uncaught error:', description);
    notificationApi.error({
      description,
      ...notificationConfig,
    });
  };

  // 添加事件监听器
  useEffect(() => {
    window.addEventListener('unhandledrejection', handleUnhandledRejection);
    window.addEventListener('error', handleGlobalError);

    return () => {
      window.removeEventListener('unhandledrejection', handleUnhandledRejection);
      window.removeEventListener('error', handleGlobalError);
    };
  }, []);

  return (
    <>
      {contextHolder}
      {children}
    </>
  );
};

createRoot(document.getElementById('root')!).render(
  <Provider store={store}>
    <Error>
      <SessionInitializer />
      <QueryProvider>
        <LoadingProvider>
          <RouterProvider router={router} />
        </LoadingProvider>
      </QueryProvider>
    </Error>
  </Provider>,
);
