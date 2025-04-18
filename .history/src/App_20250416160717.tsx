import { ProLayout } from '@ant-design/pro-components'
import { Link, Outlet, useLocation } from 'react-router-dom'

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
          },
          {
            path: '/dashboard',
            name: '仪表盘',
          },
          {
            path: '/system',
            name: '系统设置',
            routes: [
              {
                path: '/system/user',
                name: '用户管理',
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
