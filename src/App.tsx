import { App as AntdApp, Space } from 'antd';
import { ProLayout } from '@ant-design/pro-components';
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useSelector } from 'react-redux';
import UserProfile from './components/User/UserProfile';
import { useTheme } from './providers/ThemeProvider';
import { useState } from 'react';
import { routes, convertToMenuItems } from './routes';

function AppWrapper() {
  const location = useLocation();
  const navigate = useNavigate();
  const { mode } = useTheme();
  const user = useSelector((state: { user: { name: string; isLoggedIn: boolean } }) => state.user);

  // 当前路径判断，用于菜单高亮和展开
  const currentPath = location.pathname;

  // 确定需要展开的菜单，使用useState管理
  const [openKeys, setOpenKeys] = useState<string[]>(
    currentPath.startsWith('/system') ? ['/system'] : [],
  );

  return (
    <ProLayout
      location={location}
      className="app-wrapper"
      fixedHeader
      layout="top"
      splitMenus
      title="日志查询平台"
      openKeys={openKeys}
      logo="/logo.png"
      breakpoint={false}
      defaultCollapsed={true}
      siderWidth={220}
      colorPrimary="#0038FF"
      route={{
        path: '/',
        type: 'group',
        children: routes,
      }}
      menuProps={{
        defaultSelectedKeys: [currentPath],
        selectedKeys: [currentPath],
        openKeys: openKeys,
        onOpenChange: (keys) => {
          const newOpenKeys = keys.filter((key) => key !== currentPath);
          setOpenKeys(newOpenKeys);
        },
        items: convertToMenuItems(routes),
        onClick: (info) => {
          const path = info.key;
          if (path && location.pathname !== path) {
            void navigate(path);
          }
        },
      }}
      menuItemRender={(item, dom) => <Link to={item.path ?? '/'}>{dom}</Link>}
      avatarProps={{
        render: () =>
          user.isLoggedIn && (
            <Space size={12}>
              <UserProfile />
            </Space>
          ),
      }}
    >
      <div className="app-content">
        <Outlet />
      </div>
    </ProLayout>
  );
}

export default function App() {
  return (
    <AntdApp>
      <AppWrapper />
    </AntdApp>
  );
}
