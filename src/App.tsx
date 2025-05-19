// filepath: /Users/zhangyongjian/project/log-manage-web/src/App.tsx
import { App as AntdApp } from 'antd';
import { ProLayout } from '@ant-design/pro-components';
import { Link, Outlet, useLocation } from 'react-router-dom';
import Profile from '@/components/Profile';
import { useState, useMemo, useEffect } from 'react';
import { getAuthorizedRoutes } from './routes';
import { useSelector } from 'react-redux';

const App = () => {
  const [collapsed, setCollapsed] = useState(true);
  const location = useLocation();
  const userRole = useSelector((state: { user: IStoreUser }) => state.user.role);

  // 根据用户角色获取有权限的路由
  const authorizedRoutes = useMemo(() => {
    return getAuthorizedRoutes(userRole);
  }, [userRole]);

  // 计算当前选中的菜单项和打开的菜单
  const { selectedKeys, openKeys } = useMemo(() => {
    // 获取当前路径并规范化
    let pathname = location.pathname;
    if (pathname === '' || pathname === '/') {
      // 确保根路径一定会高亮
      console.log('当前处于首页路径');
      return {
        selectedKeys: ['/'], // 一定要匹配菜单项的 key
        openKeys: [],
      };
    }

    // 如果是子路径，需要同时选中父菜单
    if (pathname.startsWith('/system/')) {
      return {
        selectedKeys: [pathname],
        openKeys: ['/system'],
      };
    }

    // 处理其他路径
    return {
      selectedKeys: [pathname],
      openKeys: [],
    };
  }, [location.pathname]);

  // 强制重新计算当前项
  useEffect(() => {
    // 这里可以添加额外的处理逻辑
    console.log('路径变更:', location.pathname, '选中项:', selectedKeys);
  }, [location.pathname, selectedKeys]);

  return (
    <ProLayout
      title="秒查"
      logo="/logo.png"
      collapsed={collapsed}
      defaultCollapsed={true}
      onCollapse={setCollapsed}
      breakpoint={false}
      siderWidth={200}
      colorPrimary="#0038FF"
      location={{ pathname: location.pathname }} // 确保 location 对象只包含 pathname
      selectedKeys={selectedKeys}
      openKeys={openKeys}
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
