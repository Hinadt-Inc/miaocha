import { useEffect } from 'react';

import { App as AntdApp } from 'antd';
import { createRoot } from 'react-dom/client';
import { Provider, useDispatch } from 'react-redux';
import { RouterProvider } from 'react-router-dom';

import GlobalErrorListener from '@/providers/GlobalErrorListener';

import { router } from './routes';
import { store, type AppDispatch } from './store/store';
import { restoreSession } from './store/userSlice';

import 'nprogress/nprogress.css';
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
    <AntdApp>
      <SessionInitializer />
      <GlobalErrorListener />
      <RouterProvider router={router} />
    </AntdApp>
  </Provider>,
);

requestAnimationFrame(() => {
  const loader = document.getElementById('app-loader');
  if (loader) loader.remove();
});
