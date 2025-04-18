import { ProLayout } from '@ant-design/pro-components'
import { Link, Outlet, useLocation } from 'react-router-dom'
import { 
  CompassOutlined,
  DashboardOutlined,
  SettingOutlined,
  UserOutlined,
  LogoutOutlined
} from '@ant-design/icons'
import { useSelector, useDispatch } from 'react-redux'
import { logout } from './store/userSlice'

function App() {
  const location = useLocation()
  const dispatch = useDispatch()
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
        <div className="copyright">
          Hina Cloud BI ©2025
        </div>
      )}
      menuFooterRender={() => (
        user.isLoggedIn && (
          <div className="menu-user-info">
            <span>{user.name}</span>
            <LogoutOutlined 
              onClick={() => dispatch(logout())}
              className="logout-btn"
            />
          </div>
        )
      )}
    >
      <div>
        <Outlet />
      </div>
    </ProLayout>
  )
}

export default App
