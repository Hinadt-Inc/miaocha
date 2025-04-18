import { Layout, Menu } from 'antd'
import { RouterProvider } from 'react-router-dom'
import { router } from './routes'
import { Header, Content, Footer } from 'antd/es/layout/layout'
import { Link } from 'react-router-dom'

const { Item } = Menu

function App() {
  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header>
        <Menu theme="dark" mode="horizontal" defaultSelectedKeys={['home']}>
          <Item key="home">
            <Link to="/">首页</Link>
          </Item>
          <Item key="dashboard">
            <Link to="/dashboard">仪表盘</Link>
          </Item>
        </Menu>
      </Header>
      <Content style={{ padding: '0 50px' }}>
        <RouterProvider router={router} />
      </Content>
      <Footer style={{ textAlign: 'center' }}>
        Hina Cloud BI ©2025
      </Footer>
    </Layout>
  )
}

export default App
