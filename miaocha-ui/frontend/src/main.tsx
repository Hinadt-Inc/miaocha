import { useEffect } from 'react';
import { createRoot } from 'react-dom/client';
import { Provider, useDispatch } from 'react-redux';
import { RouterProvider } from 'react-router-dom';
import { QueryProvider } from './providers/QueryProvider';
import { LoadingProvider } from './providers/LoadingProvider';
import { ErrorProvider } from './providers/ErrorProvider';
import { store, type AppDispatch } from './store/store';
import { restoreSession } from './store/userSlice';
import { router } from './routes';
import './index.less';

// 配置 Monaco Editor Workers
import { setupMonacoWorkers } from './utils/monacoWorker';
setupMonacoWorkers();

// 会话恢复组件
const SessionInitializer = () => {
  const dispatch = useDispatch<AppDispatch>();

  useEffect(() => {
    // 在应用启动时恢复会话
    dispatch(restoreSession());
  }, [dispatch]);

  return null;
};

createRoot(document.getElementById('root')!).render(
  <Provider store={store}>
    <ErrorProvider>
      <SessionInitializer />
      <QueryProvider>
        <LoadingProvider>
          <RouterProvider router={router} />
        </LoadingProvider>
      </QueryProvider>
    </ErrorProvider>
  </Provider>,
);
