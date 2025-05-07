import { createBrowserRouter } from 'react-router-dom'
import { Suspense, lazy } from 'react'
import Loading from '../components/Loading'
import App from '../App'

// 动态导入页面组件
const LoginPage = lazy(() => import('../pages/LoginPage'))
const HomePage = lazy(() => import('../pages/HomePage'))
const UserManagementPage = lazy(() => import('../pages/system/UserManagementPage'))
const DataSourceManagementPage = lazy(() => import('../pages/system/DataSourceManagementPage'))
const PermissionManagementPage = lazy(() => import('../pages/system/PermissionManagementPage'))
const SQLEditorPage = lazy(() => import('../pages/SQLEditorPage'))
const MachineManagementPage = lazy(() => import('../pages/system/MachineManagementPage'))
const LogstashManagementPage = lazy(() => import('../pages/system/LogstashManagementPage'))

const withSuspense = (Component: React.ComponentType) => (
  <Suspense fallback={<Loading delay={300} />}>
    <Component />
  </Suspense>
);

// 创建路由
export const router = createBrowserRouter([
  {
    path: '/login',
    element: withSuspense(LoginPage),
  },
  {
    path: '/',
    element: withSuspense(App),
    children: [
      {
        index: true,
        element: withSuspense(HomePage),
      },
      {
        path: 'system/user',
        element: withSuspense(UserManagementPage),
      },
      {
        path: 'system/datasource',
        element: withSuspense(DataSourceManagementPage),
      },
      {
        path: 'system/permission',
        element: withSuspense(PermissionManagementPage),
      },
      {
        path: 'sql-editor',
        element: withSuspense(SQLEditorPage),
      },
      {
        path: 'system/machine',
        element: withSuspense(MachineManagementPage),
      },
      {
        path: 'system/logstash',
        element: withSuspense(LogstashManagementPage),
      },
    ],
  },
])
