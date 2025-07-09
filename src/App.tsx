import { App as AntdApp } from 'antd';
import { ProLayout } from '@ant-design/pro-components';
import { Link, Outlet, useLocation } from 'react-router-dom';
import Profile from '@/components/Profile';
import { useState, useMemo, useEffect } from 'react';

import { colorPrimary } from '@/utils/utils';
import { getAuthorizedRoutes } from './routes';
import { useSelector } from 'react-redux';

// 获取本地存储的collapsed状态
const getStoredCollapsed = () => {
  try {
    const stored = localStorage.getItem('siderCollapsed');
    return stored ? JSON.parse(stored) : true;
  } catch (error) {
    console.error('读取collapsed状态失败:', error);
    return true;
  }
};

// 设置collapsed状态到本地存储
const setStoredCollapsed = (value: boolean) => {
  try {
    localStorage.setItem('siderCollapsed', JSON.stringify(value));
  } catch (error) {
    console.error('保存collapsed状态失败:', error);
  }
};

const App = () => {
  const [collapsed, setCollapsed] = useState(getStoredCollapsed());
  const location = useLocation();
  const userRole = useSelector((state: { user: IStoreUser }) => state.user.role);
  const [openKeys, setOpenKeys] = useState<string[]>([]);

  // 监听collapsed变化，更新本地存储
  const handleCollapse = (value: boolean) => {
    setCollapsed(value);
    setStoredCollapsed(value);
  };

  // 根据用户角色获取有权限的路由
  const authorizedRoutes = useMemo(() => {
    const routes = getAuthorizedRoutes(userRole);
    // 为路由添加 key 字段，确保 ProLayout 能正确识别
    const addKeys = (routes: any[]): any[] => {
      return routes.map((route) => ({
        ...route,
        key: route.path,
        ...(route.children && { children: addKeys(route.children) }),
      }));
    };
    return addKeys(routes);
  }, [userRole]);

  useEffect(() => {
    document.documentElement.style.setProperty('--primary-color', colorPrimary);
    // 设置选中菜单项的高亮颜色
    document.documentElement.style.setProperty('--ant-primary-color', '#0038ff');
    document.documentElement.style.setProperty('--ant-primary-color-hover', '#1646ff');
  }, []);

  // 计算当前选中的菜单项
  const selectedKeys = useMemo(() => {
    // 获取当前路径并规范化
    let pathname = location.pathname;

    if (pathname === '' || pathname === '/') {
      return ['/'];
    }

    // 返回当前路径作为选中的菜单项
    return [pathname];
  }, [location.pathname]);

  // 初始化打开的菜单项
  useEffect(() => {
    // 如果是系统管理的子路径，自动展开系统管理菜单
    if (location.pathname.startsWith('/system/')) {
      setOpenKeys(['/system']);
    }
  }, [location.pathname]);

  // 处理菜单展开/收起
  const handleOpenChange = (keys: string[] | boolean) => {
    if (Array.isArray(keys)) {
      setOpenKeys(keys);
    }
  };

  return (
    <div
      style={
        {
          '--ant-menu-item-selected-color': '#0038ff',
          '--ant-menu-item-selected-bg': 'rgba(0, 56, 255, 0.06)',
          '--ant-menu-item-active-bg': 'rgba(0, 56, 255, 0.06)',
        } as React.CSSProperties
      }
    >
      <style>
        {`
          .ant-pro-sider-menu .ant-menu-item-selected .ant-menu-title-content,
          .ant-pro-sider-menu .ant-menu-item-selected .ant-menu-item-icon,
          .ant-pro-sider-menu .ant-menu-item-selected a {
            color: #0038ff !important;
            font-weight: 600 !important;
          }
          
          .ant-pro-sider-menu .ant-menu-item-selected {
            background-color: rgba(0, 56, 255, 0.06) !important;
          }
          
          .ant-pro-sider-menu .ant-menu-item:hover .ant-menu-title-content,
          .ant-pro-sider-menu .ant-menu-item:hover .ant-menu-item-icon,
          .ant-pro-sider-menu .ant-menu-item:hover a {
            color: #1646ff !important;
          }
          
          .ant-pro-sider-menu .ant-menu-item:hover {
            background-color: rgba(0, 56, 255, 0.04) !important;
          }

          /* 确保菜单项的选中状态样式生效 */
          .ant-pro-sider-menu .ant-menu-item[data-menu-id="/"] {
            color: #0038ff !important;
          }
          
          .ant-pro-sider-menu .ant-menu-item[data-menu-id="/"].ant-menu-item-selected {
            background-color: rgba(0, 56, 255, 0.06) !important;
          }
        `}
      </style>
      <ProLayout
        title="秒查"
        logo="/logo.png"
        collapsed={collapsed}
        defaultCollapsed={getStoredCollapsed()}
        onCollapse={handleCollapse}
        breakpoint={false}
        siderWidth={200}
        colorPrimary="#0038ff"
        location={{ pathname: location.pathname }}
        selectedKeys={selectedKeys}
        openKeys={openKeys}
        onOpenChange={handleOpenChange}
        defaultSelectedKeys={['/']}
        fixSiderbar={true}
        menuProps={{
          selectedKeys,
          defaultSelectedKeys: ['/'],
          theme: 'light',
          mode: 'inline',
        }}
        postMenuData={(menuData) => {
          return menuData || [];
        }}
        route={{
          path: '/',
          routes: authorizedRoutes,
        }}
        menuItemRender={(item, dom) => {
          return <Link to={item.path ?? '/'}>{dom}</Link>;
        }}
        avatarProps={{
          render: () => <Profile collapsed={collapsed} />,
        }}
      >
        <Outlet />
      </ProLayout>
    </div>
  );
};

export default function AppWrapper() {
  return (
    <AntdApp>
      <App />
    </AntdApp>
  );
}
