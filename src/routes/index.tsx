import { createBrowserRouter } from 'react-router-dom';
import { Suspense, lazy } from 'react';
import MainLayout from '@/layouts/MainLayout';
import Loading from '@/components/Loading';
import App from '@/App';
import {
  CompassOutlined,
  ConsoleSqlOutlined,
  BarChartOutlined,
  SettingOutlined,
  UserOutlined,
  DatabaseOutlined,
  SafetyCertificateOutlined,
  DesktopOutlined,
  CloudServerOutlined,
} from '@ant-design/icons';

// 动态导入页面组件
const LoginPage = lazy(() => import('@/pages/Login'));
const Demo = lazy(() => import('@/pages/Demo'));
const HomePage = lazy(() => import('@/pages/Home'));
const UserManagementPage = lazy(() => import('@/pages/system/UserManagementPage'));
const DataSourceManagementPage = lazy(() => import('@/pages/system/DataSourceManagementPage'));
const PermissionManagementPage = lazy(() => import('@/pages/system/PermissionManagementPage'));
const SQLEditorPage = lazy(() => import('@/pages/SQLEditor/SQLEditorImpl'));
const MachineManagementPage = lazy(() => import('@/pages/system/MachineManagementPage'));
const LogstashManagementPage = lazy(() => import('@/pages/system/LogstashManagementPage'));
const LogAnalysisPage = lazy(() => import('@/pages/LogAnalysis'));

const withSuspense = (Component: React.ComponentType) => (
  <Suspense
    fallback={
      <Loading
        style={{
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          height: '100vh',
        }}
      />
    }
  >
    <Component />
  </Suspense>
);

// 定义路由配置接口
export interface RouteConfig {
  path: string;
  name: string;
  icon?: React.ReactNode;
  element?: React.ReactNode;
  children?: RouteConfig[];
  type?: 'group';
}

// 统一的路由配置
export const routes: RouteConfig[] = [
  {
    path: '/',
    name: '数据发现',
    icon: <CompassOutlined />,
    element: withSuspense(HomePage),
  },
  {
    path: '/sql-editor',
    name: 'SQL编辑器',
    icon: <ConsoleSqlOutlined />,
    element: withSuspense(SQLEditorPage),
  },
  // {
  //   path: '/demo',
  //   name: 'Demo',
  //   icon: <ConsoleSqlOutlined />,
  //   element: withSuspense(Demo),
  // },
  {
    path: '/log-analysis',
    name: '日志分析',
    icon: <BarChartOutlined />,
    element: (
      <MainLayout>
        <LogAnalysisPage />
      </MainLayout>
    ),
  },
  {
    path: '/system',
    name: '系统管理',
    icon: <SettingOutlined />,
    type: 'group',
    children: [
      {
        path: '/system/user',
        name: '用户管理',
        element: withSuspense(UserManagementPage),
      },
      {
        path: '/system/datasource',
        name: '数据源管理',
        element: withSuspense(DataSourceManagementPage),
      },
      {
        path: '/system/permission',
        name: '权限管理',
        element: withSuspense(PermissionManagementPage),
      },
      {
        path: '/system/machine',
        name: '服务器管理',
        element: withSuspense(MachineManagementPage),
      },
      {
        path: '/system/logstash',
        name: 'Logstash管理',
        element: withSuspense(LogstashManagementPage),
      },
    ],
  },
];

// 将路由配置转换为 React Router 的路由配置
const convertToRouterConfig = (
  routes: RouteConfig[],
): Array<{
  path: string;
  element?: React.ReactNode;
  children?: Array<{
    path: string;
    element?: React.ReactNode;
    children?: any;
  }>;
}> => {
  return routes.map((route) => ({
    path: route.path,
    element: route.element,
    children: route.children ? convertToRouterConfig(route.children) : undefined,
  }));
};

// 将路由配置转换为菜单项
export const convertToMenuItems = (
  routes: RouteConfig[],
): Array<{
  key: string;
  label: string;
  icon?: React.ReactNode;
  children?: Array<{
    key: string;
    label: string;
    icon?: React.ReactNode;
    children?: any;
  }>;
}> => {
  return routes.map((route) => ({
    key: route.path,
    label: route.name,
    icon: route.icon,
    children: route.children ? convertToMenuItems(route.children) : undefined,
  }));
};

// 创建路由
export const router = createBrowserRouter([
  {
    path: '/login',
    element: withSuspense(LoginPage),
  },
  {
    path: '/',
    element: withSuspense(App),
    children: convertToRouterConfig(routes),
  },
]);
