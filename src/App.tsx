import './App.less'
import { App as AntdApp } from 'antd'
import { ProLayout } from '@ant-design/pro-components'
import { Link, Outlet, useLocation } from 'react-router-dom'
import { 
  CompassOutlined,
  DashboardOutlined,
  ConsoleSqlOutlined,
  SettingOutlined,
  UserOutlined,
} from '@ant-design/icons'
import { useSelector } from 'react-redux'
import UserProfile from './components/User/UserProfile'

function AppWrapper() {
  const location = useLocation()
  const user = useSelector((state: { user: { name: string; isLoggedIn: boolean } }) => state.user)
  
  return (
    <ProLayout
      location={location}
      route={{
        path: '/',
        routes: [
          {
            path: '/',
            name: 'Discover',
            icon: <CompassOutlined />,
          },
          {
            path: '/sql-editor',
            name: 'SQL编辑',
            icon: <ConsoleSqlOutlined />
          },
          {
            path: '/dashboard',
            name: '仪表盘',
            icon: <DashboardOutlined />,
          },
          {
            path: '/system',
            name: '系统设置',
            icon: <SettingOutlined />,
            routes: [
              {
                path: '/system/user',
                name: '用户管理',
                icon: <UserOutlined />,
              },
              {
                path: '/system/datasource',
                name: '数据源管理',
                icon: <UserOutlined />,
              },
              {
                path: '/system/permission',
                name: '数据源权限管理',
                icon: <UserOutlined />,
              },
              {
                path: '/system/machine',
                name: '机器管理',
                icon: <UserOutlined />,
              },
            ]
          }
        ]
      }}
      menuItemRender={(item, dom) => (
        <Link to={item.path || '/'}>{dom}</Link>
      )}
      avatarProps={{
        render: () => (
          user.isLoggedIn && <UserProfile />
        )
      }}
    >
      <div>
        <Outlet />
      </div>
    </ProLayout>
  )
}

export default function App() {
  return (
    <AntdApp>
      <AppWrapper />
    </AntdApp>
  );
}
