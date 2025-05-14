import { App as AntdApp } from 'antd';
import { ProLayout } from '@ant-design/pro-components';
import { Link, Outlet } from 'react-router-dom';
import Profile from '@/components/Profile';
import { useState } from 'react';
import { routes } from './routes';

const App = () => {
  const [collapsed, setCollapsed] = useState(true);

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
      route={{
        // 成菜单和面包屑
        path: '/',
        type: 'group',
        children: routes,
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
