import { createBrowserRouter, redirect } from 'react-router-dom';
import { Suspense, lazy } from 'react';
import App from '@/App';
import AdminLayout from '@/layouts/AdminLayout';
import { CompassOutlined, ConsoleSqlOutlined, SettingOutlined } from '@ant-design/icons';
import { ADMIN_ROLES } from '@/constants/roles';
import ErrorBoundary from '@/components/Error/ErrorBoundary';

// 懒加载组件
const LoginPage = lazy(() => import('@/pages/Login'));
const HomePage = lazy(() => import('@/pages/Home'));
const SQLEditorPage = lazy(() => import('@/pages/SQLEditor'));
const User = lazy(() => import('@/pages/system/User'));
const DataSourceManagementPage = lazy(() => import('@/pages/system/DataSourceManagement'));
const MachineManagementPage = lazy(() => import('@/pages/system/MachineManagement'));
const LogstashManagementPage = lazy(() => import('@/pages/system/LogstashManagement'));
const ModuleManagementPage = lazy(() => import('@/pages/system/ModuleManagement'));

// 路由配置接口
interface RouteConfig {
  path: string;
  name: string;
  icon?: React.ReactNode;
  element?: React.ReactNode;
  children?: RouteConfig[];
  access?: any;
  key?: string;
}

// 路由配置，路由配置数据表
const routes: RouteConfig[] = [
  {
    path: '/',
    name: '数据发现',
    icon: <CompassOutlined />,
    element: <HomePage />,
  },
  {
    path: '/sql-editor',
    name: 'SQL编辑器',
    icon: <ConsoleSqlOutlined />,
    element: <SQLEditorPage />,
  },
  {
    path: '/system',
    name: '系统管理',
    icon: <SettingOutlined />,
    access: ADMIN_ROLES,
    children: [
      { path: '/system/user', name: '用户管理', element: <User /> },
      { path: '/system/datasource', name: '数据源管理', element: <DataSourceManagementPage /> },
      { path: '/system/machine', name: '服务器管理', element: <MachineManagementPage /> },
      { path: '/system/module', name: '模块管理', element: <ModuleManagementPage /> },
      { path: '/system/logstash', name: 'Logstash管理', element: <LogstashManagementPage /> },
    ],
  },
];

// 权限过滤，根据用户角色动态过滤路由配置，只返回用户有权限访问的路由。
// 同时为每个路由添加 key 属性，供 ProLayout 使用
export const getAuthorizedRoutes = (userRole: string | null): RouteConfig[] => {
  const filterRoutes = (routes: RouteConfig[]): RouteConfig[] => {
    return routes
      .filter((route) => !route.access || (userRole && route.access.includes(userRole)))
      .map((route) => ({
        ...route,
        key: route.path, // 为 ProLayout 添加 key 字段
        children: route.children ? filterRoutes(route.children) : undefined,
      }))
      .filter((route) => !route.children || route.children.length > 0);
  };

  return filterRoutes(routes);
};

// 菜单项类型
interface MenuItem {
  key: string;
  label: string;
  icon?: React.ReactNode;
  children?: MenuItem[];
}

// 路由器配置类型
interface RouterConfig {
  path: string;
  element?: React.ReactNode;
  children?: RouterConfig[];
}

// 转换为菜单项
const convertToMenuItems = (routes: RouteConfig[]): MenuItem[] => {
  return routes.map((route) => ({
    key: route.path,
    label: route.name,
    icon: route.icon,
    children: route.children ? convertToMenuItems(route.children) : undefined,
  }));
};

// 转换为 React Router 配置
const convertToRouterConfig = (routes: RouteConfig[]): RouterConfig[] => {
  return routes.map((route) => {
    if (route.path === '/system' && route.children) {
      return {
        path: route.path,
        element: <AdminLayout />,
        children: route.children.map((child) => ({
          path: child.path.replace('/system/', ''),
          element: child.element,
        })),
      };
    }

    return {
      path: route.path,
      element: route.element,
      children: route.children ? convertToRouterConfig(route.children) : undefined,
    };
  });
};

// 鉴权：在进入“/”之前检查是否登录（同步检查，避免闪屏）
const authLoader = () => {
  const token = localStorage.getItem('accessToken');
  if (!token) {
    throw redirect('/login');
  }
  return null;
};

export const router = createBrowserRouter([
  {
    path: '/login',
    element: (
      <ErrorBoundary>
        <LoginPage />
      </ErrorBoundary>
    ),
  },
  {
    path: '/',
    loader: authLoader,
    element: (
      <Suspense fallback={null}>
        <App />
      </Suspense>
    ),
    children: convertToRouterConfig(routes),
  },
]);
