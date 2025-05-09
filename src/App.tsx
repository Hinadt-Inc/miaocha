import { App as AntdApp, Space } from 'antd';
import { ProLayout } from '@ant-design/pro-components';
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom';
import {
  CompassOutlined,
  ConsoleSqlOutlined,
  SettingOutlined,
  UserOutlined,
  DatabaseOutlined,
  SafetyCertificateOutlined,
  DesktopOutlined,
  CloudServerOutlined,
} from '@ant-design/icons';
import { useSelector } from 'react-redux';
import UserProfile from './components/User/UserProfile';
import { useTheme } from './providers/ThemeProvider';
import { useState } from 'react';

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
      fixSiderbar
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
        children: [
          {
            path: '/',
            name: '数据发现',
            icon: <CompassOutlined />,
          },
          {
            path: '/sql-editor',
            name: 'SQL编辑器',
            icon: <ConsoleSqlOutlined />,
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
                icon: <UserOutlined />,
              },
              {
                path: '/system/datasource',
                name: '数据源管理',
                icon: <DatabaseOutlined />,
              },
              {
                path: '/system/permission',
                name: '权限管理',
                icon: <SafetyCertificateOutlined />,
              },
              {
                path: '/system/machine',
                name: '服务器管理',
                icon: <DesktopOutlined />,
              },
              {
                path: '/system/logstash',
                name: 'Logstash管理',
                icon: <CloudServerOutlined />,
              },
            ],
          },
        ],
      }}
      menuProps={{
        defaultSelectedKeys: [currentPath],
        selectedKeys: [currentPath],
        openKeys: openKeys,
        onOpenChange: (keys) => {
          const newOpenKeys = keys.filter((key) => key !== currentPath);
          setOpenKeys(newOpenKeys);
        },
        items: [
          {
            key: '/',
            label: '数据发现',
            icon: <CompassOutlined />,
          },
          {
            key: '/sql-editor',
            label: 'SQL编辑器',
            icon: <ConsoleSqlOutlined />,
          },
          {
            key: '/system',
            label: '系统管理',
            icon: <SettingOutlined />,
            children: [
              {
                key: '/system/user',
                label: '用户管理',
                icon: <UserOutlined />,
              },
              {
                key: '/system/datasource',
                label: '数据源管理',
                icon: <DatabaseOutlined />,
              },
              {
                key: '/system/permission',
                label: '权限管理',
                icon: <SafetyCertificateOutlined />,
              },
              {
                key: '/system/machine',
                label: '服务器管理',
                icon: <DesktopOutlined />,
              },
              {
                key: '/system/logstash',
                label: 'Logstash管理',
                icon: <CloudServerOutlined />,
              },
            ],
          },
        ],
        onClick: (info) => {
          // 使用React Router进行导航
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
