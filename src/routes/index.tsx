import { createBrowserRouter } from 'react-router-dom'
import { lazy, Suspense } from 'react'

// 通用的加载组件
const PageLoader = () => (
  <div style={{ 
    display: 'flex', 
    justifyContent: 'center', 
    alignItems: 'center', 
    height: '100vh',
    color: 'var(--text-primary)'
  }}>
    加载中...
  </div>
);

// 修改懒加载方式，确保正确处理默认导出
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
    })
  );

  return (
    <Suspense fallback={<PageLoader />}>
      <Component />
    </Suspense>
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
