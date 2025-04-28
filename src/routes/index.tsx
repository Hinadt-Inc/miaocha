import { createBrowserRouter } from 'react-router-dom'
import { Suspense, useState, useEffect } from 'react'
import Loading from '../components/Loading'
import LoginPage from '../pages/LoginPage'
import App from '../App'
import HomePage from '../pages/HomePage'
import DashboardPage from '../pages/DashboardPage'
import UserManagementPage from '../pages/system/UserManagementPage'
import DataSourceManagementPage from '../pages/system/DataSourceManagementPage'
import PermissionManagementPage from '../pages/system/PermissionManagementPage'
import SQLEditorPage from '../pages/SQLEditorPage'
import MachineManagementPage from '../pages/system/MachineManagementPage'
import LogstashManagementPage from '../pages/system/LogstashManagementPage'

// 简单的错误边界组件
const ErrorBoundary = ({ children }: { children: React.ReactNode }) => {
  const [hasError, setHasError] = useState(false);
  
  useEffect(() => {
    const errorHandler = (error: ErrorEvent) => {
      if (error.message.includes('Failed to fetch dynamically imported module')) {
        setHasError(true);
      }
    };
    window.addEventListener('error', errorHandler);
    return () => window.removeEventListener('error', errorHandler);
  }, []);

  if (hasError) {
    return (
      <div style={{ padding: 20 }}>
        <h2>模块加载失败</h2>
        <p>无法加载请求的页面，请检查网络连接后刷新页面重试</p>
      </div>
    );
  }

  return <>{children}</>;
};

// 创建路由
export const router = createBrowserRouter([
  {
    path: '/login',
    element: (
      <ErrorBoundary>
        <Suspense fallback={<Loading delay={300} />}>
          <LoginPage />
        </Suspense>
      </ErrorBoundary>
    ),
  },
  {
    path: '/',
    element: (
      <ErrorBoundary>
        <Suspense fallback={<Loading delay={300} />}>
          <App />
        </Suspense>
      </ErrorBoundary>
    ),
    children: [
      {
        index: true,
        element: (
          <ErrorBoundary>
            <Suspense fallback={<Loading delay={300} />}>
              <HomePage />
            </Suspense>
          </ErrorBoundary>
        ),
      },
      {
        path: 'dashboard',
        element: (
          <ErrorBoundary>
            <Suspense fallback={<Loading delay={300} />}>
              <DashboardPage />
            </Suspense>
          </ErrorBoundary>
        ),
      },
      {
        path: 'system/user',
        element: (
          <ErrorBoundary>
            <Suspense fallback={<Loading delay={300} />}>
              <UserManagementPage />
            </Suspense>
          </ErrorBoundary>
        ),
      },
      {
        path: 'system/datasource',
        element: (
          <ErrorBoundary>
            <Suspense fallback={<Loading delay={300} />}>
              <DataSourceManagementPage />
            </Suspense>
          </ErrorBoundary>
        ),
      },
      {
        path: 'system/permission',
        element: (
          <ErrorBoundary>
            <Suspense fallback={<Loading delay={300} />}>
              <PermissionManagementPage />
            </Suspense>
          </ErrorBoundary>
        ),
      },
      {
        path: 'sql-editor',
        element: (
          <ErrorBoundary>
            <Suspense fallback={<Loading delay={300} />}>
              <SQLEditorPage />
            </Suspense>
          </ErrorBoundary>
        ),
      },
      {
        path: 'system/machine',
        element: (
          <ErrorBoundary>
            <Suspense fallback={<Loading delay={300} />}>
              <MachineManagementPage />
            </Suspense>
          </ErrorBoundary>
        ),
      },
      {
        path: 'system/logstash',
        element: (
          <ErrorBoundary>
            <Suspense fallback={<Loading delay={300} />}>
              <LogstashManagementPage />
            </Suspense>
          </ErrorBoundary>
        ),
      },
    ],
  },
])