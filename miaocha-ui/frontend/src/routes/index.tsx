import { createBrowserRouter } from 'react-router-dom';
import { Suspense, lazy } from 'react';
import Loading from '@/components/Loading';
import App from '@/App';
import { CompassOutlined, ConsoleSqlOutlined, SettingOutlined } from '@ant-design/icons';

// 动态导入页面组件
const LoginPage = lazy(() => import('@/pages/Login'));
const HomePage = lazy(() => import('@/pages/Home'));
const UserManagementPage = lazy(() => import('@/pages/system/UserManagement'));
const DataSourceManagementPage = lazy(() => import('@/pages/system/DataSourceManagement'));
const SQLEditorPage = lazy(() => import('@/pages/SQLEditor'));
const MachineManagementPage = lazy(() => import('@/pages/system/MachineManagement'));
const LogstashManagementPage = lazy(() => import('@/pages/system/LogstashManagement'));
const ModuleManagementPage = lazy(() => import('@/pages/system/ModuleManagement'));

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
  access?: string[]; // 新增: 定义访问该路由所需的角色权限
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
  {
    path: '/system',
    name: '系统管理',
    icon: <SettingOutlined />,
    type: 'group',
    access: ['ADMIN', 'SUPER_ADMIN'], // 只有管理员和超级管理员可以访问
    children: [
      {
        path: '/system/user',
        name: '用户管理',
        element: withSuspense(UserManagementPage),
        access: ['ADMIN', 'SUPER_ADMIN'],
      },
      {
        path: '/system/datasource',
        name: '数据源管理',
        element: withSuspense(DataSourceManagementPage),
        access: ['ADMIN', 'SUPER_ADMIN'],
      },
      {
        path: '/system/machine',
        name: '服务器管理',
        element: withSuspense(MachineManagementPage),
        access: ['ADMIN', 'SUPER_ADMIN'],
      },
      {
        path: '/system/logstash',
        name: 'Logstash管理',
        element: withSuspense(LogstashManagementPage),
        access: ['ADMIN', 'SUPER_ADMIN'],
      },
      {
        path: '/system/module',
        name: '模块管理',
        element: withSuspense(ModuleManagementPage),
        access: ['ADMIN', 'SUPER_ADMIN'],
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

// 过滤路由配置，根据用户角色显示相应的菜单项
export const getAuthorizedRoutes = (userRole: string) => {
  // 如果没有角色信息，只返回不需要权限的路由
  if (!userRole) {
    return routes.filter((route) => !route.access);
  }

  // 递归过滤路由配置
  const filterRoutes = (routes: RouteConfig[]): RouteConfig[] => {
    return routes
      .filter((route) => !route.access || route.access.includes(userRole))
      .map((route) => {
        if (route.children) {
          return {
            ...route,
            children: filterRoutes(route.children),
          };
        }
        return route;
      });
  };

  return filterRoutes(routes);
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

// 创建路由 - 注意这里不做权限校验，因为路由访问权限需要在组件内进行控制
// 这里创建完整的路由配置，但菜单显示会根据权限过滤
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
