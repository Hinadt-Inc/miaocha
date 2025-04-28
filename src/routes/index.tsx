import { createBrowserRouter } from 'react-router-dom'
import { lazy, Suspense, useState, useEffect } from 'react'
import Loading from '../components/Loading'
import LoginPage from '../pages/LoginPage'
import HomePage from '../pages/HomePage'

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

// 懒加载函数
const lazyLoad = (path: string, isApp: boolean = false) => {
  // 使用动态导入，并明确解构默认导出
  const Component = lazy(() =>
    import(`../${isApp ? '' : 'pages/'}${path}`).then(module => {
      if (module && module.default) {
        return { default: module.default };
      }
      // 处理命名导出
      const key = path.split('/').pop();
      return { default: key ? module[key] || (() => <div>Failed to load</div>) : (() => <div>Failed to load</div>) };
    })
  );

  return (
    <ErrorBoundary>
      <Suspense fallback={<Loading delay={300} />}>
        <Component />
      </Suspense>
    </ErrorBoundary>
  );
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
    element: lazyLoad('App', true),
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
        element: lazyLoad('DashboardPage'),
      },
      {
        path: 'system/user',
        element: lazyLoad('system/UserManagementPage'),
      },
      {
        path: 'system/datasource',
        element: lazyLoad('system/DataSourceManagementPage'),
      },
      {
        path: 'system/permission',
        element: lazyLoad('system/PermissionManagementPage'),
      },
      {
        path: 'sql-editor',
        element: lazyLoad('SQLEditorPage'),
      },
      {
        path: 'system/machine',
        element: lazyLoad('system/MachineManagementPage'),
      },
      {
        path: 'system/logstash',
        element: lazyLoad('system/LogstashManagementPage'),
      },
    ],
  },
])