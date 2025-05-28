import { App as AntdApp } from 'antd';
import { ProLayout } from '@ant-design/pro-components';
import { Link, Outlet, useLocation } from 'react-router-dom';
import Profile from '@/components/Profile';
import { useState, useMemo, useEffect } from 'react';

import { colorPrimary } from '@/utils/utils';
import { getAuthorizedRoutes } from './routes';
import { useSelector } from 'react-redux';

const App = () => {
  const [collapsed, setCollapsed] = useState(true);
  const location = useLocation();
  const userRole = useSelector((state: { user: IStoreUser }) => state.user.role);
  const [openKeys, setOpenKeys] = useState<string[]>([]);

  // 根据用户角色获取有权限的路由
  const authorizedRoutes = useMemo(() => {
    return getAuthorizedRoutes(userRole);
  }, [userRole]);
  useEffect(() => {
    document.documentElement.style.setProperty('--primary-color', colorPrimary);
  }, []);

  // 计算当前选中的菜单项
  const selectedKeys = useMemo(() => {
    // 获取当前路径并规范化
    let pathname = location.pathname;
    if (pathname === '' || pathname === '/') {
      // 确保根路径一定会高亮
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
