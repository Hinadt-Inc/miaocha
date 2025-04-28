import { createBrowserRouter } from 'react-router-dom'
import { lazy, Suspense } from 'react'
import Loading from '../components/Loading'

// 修改懒加载方式，确保正确处理默认导出，并使用统一的Loading组件
// 简单的错误边界组件
const ErrorBoundary = ({ children }: { children: React.ReactNode }) => {
  const [hasError, setHasError] = React.useState(false);
  
  React.useEffect(() => {
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

const lazyLoad = (path: string, isApp: boolean = false) => {
  // 使用动态导入，并明确解构默认导出
  const Component = lazy(() => 
    import(`../${isApp ? '' : 'pages/'}${path}`).then(module => {
      if (module && module.default) {
        return { default: module.default };
      }
      // 如果没有默认导出，创建一个错误组件
      return { 
        default: () => (
          <div>Failed to load component: {path}</div>
        ) 
      };
    }).catch(() => {
      // 动态导入失败时返回错误组件
      return { 
        default: () => (
          <div style={{ padding: 20 }}>
            <h2>模块加载失败</h2>
            <p>无法加载请求的页面，请检查网络连接后刷新页面重试</p>
          </div>
        ) 
      };
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

// 使用新的懒加载方式创建路由
export const router = createBrowserRouter([
  {
    path: '/login',
    element: lazyLoad('LoginPage'),
  },
  {
    path: '/',
    element: lazyLoad('App', true),
    children: [
      {
        index: true,
        element: lazyLoad('HomePage'),
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
