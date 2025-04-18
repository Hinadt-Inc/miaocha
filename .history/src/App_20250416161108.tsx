import { ProLayout } from '@ant-design/pro-components'
import { Link, Outlet, useLocation } from 'react-router-dom'
import { 
  CompassOutlined,
  DashboardOutlined,
  SettingOutlined,
  UserOutlined 
} from '@ant-design/icons'

function App() {
  const location = useLocation()
  
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
              }
            ]
          }
        ]
      }}
      menuItemRender={(item, dom) => (
        <Link to={item.path || '/'}>{dom}</Link>
      )}
      footerRender={() => (
        <div style={{ textAlign: 'center', padding: 16 }}>
          Hina Cloud BI ©2025
        </div>
      )}
    >
      <Outlet />
    </ProLayout>
  )
}

export default App
