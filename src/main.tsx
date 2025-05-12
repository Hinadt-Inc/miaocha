import { StrictMode, useEffect } from 'react';
import { createRoot } from 'react-dom/client';
import { Provider, useDispatch } from 'react-redux';
import { RouterProvider } from 'react-router-dom';
import { QueryProvider } from './providers/QueryProvider';
import { ThemeProvider } from './providers/ThemeProvider';
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

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <Provider store={store}>
      <SessionInitializer />
      <QueryProvider>
        <ThemeProvider>
          <LoadingProvider>
            <RouterProvider router={router} />
          </LoadingProvider>
        </ThemeProvider>
      </QueryProvider>
    </Provider>
  </StrictMode>,
);
