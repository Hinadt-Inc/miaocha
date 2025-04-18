import { Layout, Menu } from 'antd'
import { Header, Content, Footer } from 'antd/es/layout/layout'
import { Link, Outlet } from 'react-router-dom'

function App() {
  const items = [
    {
      key: 'home',
      label: <Link to="/">首页</Link>,
    },
    {
      key: 'dashboard', 
      label: <Link to="/dashboard">仪表盘</Link>,
    }
  ]

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header>
        <Menu 
          theme="dark" 
          mode="horizontal" 
          defaultSelectedKeys={['home']}
          items={items}
        />
      </Header>
      <Content style={{ padding: '0 50px' }}>
        <Outlet />
      </Content>
      <Footer style={{ textAlign: 'center' }}>
        Hina Cloud BI ©2025
      </Footer>
    </Layout>
  )
}

export default App
