import { App as AntdApp } from 'antd';
import { ProLayout } from '@ant-design/pro-components';
import { Link, Outlet, useLocation } from 'react-router-dom';
import Profile from '@/components/Profile';
import { useState } from 'react';
import useErrorHandler from './hooks/useErrorHandler';
import useRoutePermission from './hooks/useRoutePermission';
import useMenuState from './hooks/useMenuState';
import useThemeColor from './hooks/useThemeColor';

const App = () => {
  const [collapsed, setCollapsed] = useState(true);
  const location = useLocation();
  const { contextHolder } = useErrorHandler();
  const { authorizedRoutes } = useRoutePermission();
  const { openKeys, selectedKeys, handleOpenChange } = useMenuState(location);
  const colorPrimary = useThemeColor();

  return (
    <ProLayout
      title="秒查"
      logo="/logo.png"
      collapsed={collapsed}
      defaultCollapsed={true}
      onCollapse={setCollapsed}
      breakpoint={false}
      siderWidth={200}
      colorPrimary={colorPrimary}
      location={{ pathname: location.pathname }} // 确保 location 对象只包含 pathname
      selectedKeys={selectedKeys}
      openKeys={openKeys}
      onOpenChange={handleOpenChange}
      defaultSelectedKeys={['/']} // 设置默认选中项为首页
      fixSiderbar={true} // 固定侧边栏
      menuProps={{
        selectedKeys, // 传递当前选中的菜单项
        defaultSelectedKeys: ['/'], // 默认选中首页
      }}
      // 确保菜单数据总是返回正确的格式
      postMenuData={(menuData) => {
        // 无论如何都返回菜单数据
        return menuData || [];
      }}
      route={{
        // 成菜单和面包屑
        path: '/',
        type: 'group',
        children: authorizedRoutes,
      }}
      menuItemRender={(item, dom) => <Link to={item.path ?? '/'}>{dom}</Link>}
      avatarProps={{
        render: () => <Profile collapsed={collapsed} />,
      }}
    >
      {contextHolder}
      <Outlet />
    </ProLayout>
  );
};

export default function AppWrapper() {
  return (
    <AntdApp>
      <App />
    </AntdApp>
  );
}
