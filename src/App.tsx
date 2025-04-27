import './App.less'
import { App as AntdApp, Button, Space, Tooltip } from 'antd'
import { ProLayout } from '@ant-design/pro-components'
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { 
  CompassOutlined,
  DashboardOutlined,
  ConsoleSqlOutlined,
  SettingOutlined,
  UserOutlined,
  DatabaseOutlined,
  SafetyCertificateOutlined,
  DesktopOutlined,
  CloudServerOutlined,
  LogoutOutlined,
  BulbOutlined,
  BulbFilled,
} from '@ant-design/icons'
import { useSelector, useDispatch } from 'react-redux'
import UserProfile from './components/User/UserProfile'
import { useTheme } from './providers/ThemeProvider'
import { logout } from './store/userSlice'

function AppWrapper() {
  const location = useLocation()
  const navigate = useNavigate()
  const dispatch = useDispatch()
  const { mode, toggleTheme } = useTheme()
  const user = useSelector((state: { user: { name: string; isLoggedIn: boolean } }) => state.user)
  
  // 当前路径判断，用于菜单高亮和展开
  const currentPath = location.pathname
  
  // 确定需要展开的菜单
  const openKeys = currentPath.startsWith('/system') ? ['/system'] : []
  
  const handleLogout = () => {
    dispatch(logout())
    navigate('/login')
  }
  
  return (
    <ProLayout
      location={location}
      className='app-wrapper'
      siderMenuType="group"
      fixSiderbar
      fixedHeader
      layout="mix"
      splitMenus
      title="日志查询平台"
      contentStyle={{ 
        background: mode === 'dark' ? '#141414' : '#f0f2f5',
        padding: '16px',
        minHeight: 'calc(100vh - 64px)'
      }}
      openKeys={openKeys}
      logo="/logo.png"
      breakpoint={false}
      defaultCollapsed={true}
      siderWidth={220}
      colorPrimary='#0038FF'
      route={{
        path: '/',
        routes: [
          {
            path: '/',
            name: '数据发现',
            icon: <CompassOutlined />,
          },
          {
            path: '/sql-editor',
            name: 'SQL编辑器',
            icon: <ConsoleSqlOutlined />
          },
          {
            path: '/dashboard',
            name: '仪表盘',
            icon: <DashboardOutlined />,
          },
          {
            path: '/system/user',
            name: '系统管理',
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
            ]
          }
        ]
      }}
      menuItemRender={(item, dom) => (
        <Link to={item.path || '/'}>{dom}</Link>
      )}
      // actionsRender={() => [
      //   <Tooltip title={mode === 'dark' ? '切换到亮色模式' : '切换到暗色模式'}>
      //     <Button 
      //       type="text" 
      //       icon={mode === 'dark' ? <BulbOutlined /> : <BulbFilled />} 
      //       onClick={toggleTheme}
      //     />
      //   </Tooltip>,
      // ]}
      avatarProps={{
        render: () => (
          user.isLoggedIn && (
            <Space size={12}>
              <UserProfile />
              {/* <Tooltip title="退出登录">
                <Button 
                  icon={<LogoutOutlined />}
                  type="text" 
                  danger 
                  onClick={handleLogout}
                />
              </Tooltip> */}
            </Space>
          )
        )
      }}
    >
      <div className="app-content">
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
