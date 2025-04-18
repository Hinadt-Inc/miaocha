import { Avatar, Button, Dropdown, Layout, Space } from 'antd'
import { UserOutlined, LogoutOutlined, DownOutlined, AppstoreOutlined } from '@ant-design/icons'
import { useSelector, useDispatch } from 'react-redux'
import { RootState } from '../store/store'
import { logout } from '../store/userSlice'
import { Link } from 'react-router-dom'

const { Header: AntHeader } = Layout

const Header = () => {
  const user = useSelector((state: RootState) => state.user)
  const dispatch = useDispatch()

  const handleLogout = () => {
    dispatch(logout())
  }

  const userMenuItems = [
    {
      key: 'profile',
      label: '个人资料',
      icon: <UserOutlined />
    },
    {
      key: 'settings',
      label: '账户设置',
      icon: <AppstoreOutlined />
    },
    {
      type: 'divider' as const
    },
    {
      key: 'logout',
      label: '退出登录',
      icon: <LogoutOutlined />,
      danger: true
    }
  ]

  const onUserMenuClick = ({ key }: { key: string }) => {
    if (key === 'logout') {
      handleLogout()
    }
    // 其他菜单项处理逻辑可在此添加
  }

  return (
    <AntHeader
      style={{
        display: 'flex',
        alignItems: 'center',
        padding: '0 24px',
        background: '#fff',
        boxShadow: '0 1px 4px rgba(0, 21, 41, 0.08)',
        height: '64px'
      }}
    >
      
      <div style={{ marginLeft: 'auto' }}>
        {user.isLoggedIn ? (
          <Dropdown
            menu={{ 
              items: userMenuItems,
              onClick: onUserMenuClick 
            }}
            placement="bottomRight"
            arrow
            trigger={['click']}
          >
            <Space style={{ cursor: 'pointer', padding: '0 8px' }}>
              <Avatar 
                style={{ 
                  backgroundColor: '#1677ff' 
                }}
                icon={<UserOutlined />}
              />
              <span>{user.name}</span>
              <DownOutlined />
            </Space>
          </Dropdown>
        ) : (
          <Link to="/login">
            <Button type="primary">
              登录
            </Button>
          </Link>
        )}
      </div>
    </AntHeader>
  )
}

export default Header
