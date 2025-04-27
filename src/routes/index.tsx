import { createBrowserRouter } from 'react-router-dom'
import { lazy, Suspense } from 'react'
import LoadingComponent from '../components/Loading'

// 创建统一的懒加载包装器
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const lazyLoad = (Component: React.LazyExoticComponent<any>) => (
  <Suspense fallback={<LoadingComponent />}>
    <Component />
  </Suspense>
);

// 懒加载所有页面组件
const App = lazy(() => import('../App'))
const HomePage = lazy(() => import('../pages/HomePage'))
const DashboardPage = lazy(() => import('../pages/DashboardPage'))
const UserManagementPage = lazy(() => import('../pages/system/UserManagementPage'))
const DataSourceManagementPage = lazy(() => import('../pages/system/DataSourceManagementPage'))
const PermissionManagementPage = lazy(() => import('../pages/system/PermissionManagementPage'))
const LoginPage = lazy(() => import('../pages/LoginPage'))
const SQLEditorPage = lazy(() => import('../pages/SQLEditorPage'))
const MachineManagementPage = lazy(() => import('../pages/system/MachineManagementPage'))
const LogstashManagementPage = lazy(() => import('../pages/system/LogstashManagementPage'))

export const router = createBrowserRouter([
  {
    path: '/login',
    element: lazyLoad(LoginPage),
  },
  {
    path: '/',
    element: <App />,
    children: [
      {
        index: true,
        element: lazyLoad(HomePage),
      },
      {
        path: 'dashboard',
        element: lazyLoad(DashboardPage),
      },
      {
        path: 'system/user',
        element: lazyLoad(UserManagementPage),
      },
      {
        path: 'system/datasource',
        element: lazyLoad(DataSourceManagementPage),
      },
      {
        path: 'system/permission',
        element: lazyLoad(PermissionManagementPage),
      },
      {
        path: 'sql-editor',
        element: lazyLoad(SQLEditorPage),
      },
      {
        path: 'system/machine',
        element: lazyLoad(MachineManagementPage),
      },
      {
        path: 'system/logstash',
        element: lazyLoad(LogstashManagementPage),
      },
    ],
  },
])
