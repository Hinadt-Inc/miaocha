import { Button, Space } from 'antd'
import { UserOutlined, LogoutOutlined } from '@ant-design/icons'
import { useSelector, useDispatch } from 'react-redux'
import { RootState } from '../store/store'
import { logout } from '../store/userSlice'

const Header = () => {
  const user = useSelector((state: RootState) => state.user)
  const dispatch = useDispatch()

  const handleLogout = () => {
    dispatch(logout())
  }

  return (
    <div style={{ 
      display: 'flex',
      justifyContent: 'flex-end',
      alignItems: 'center',
      padding: '0 24px',
      background: '#fff',
      boxShadow: '0 1px 4px rgba(0, 21, 41, 0.08)',
      height: '48px',
      position: 'fixed',
      top: 0,
      right: 0,
      left: 0,
      zIndex: 100
    }}>
      <Space>
        {user.isLoggedIn ? (
          <>
            <span>
              <UserOutlined /> {user.name}
            </span>
            <Button 
              type="text" 
              icon={<LogoutOutlined />}
              onClick={handleLogout}
            >
              退出
            </Button>
          </>
        ) : null}
      </Space>
    </div>
  )
}

export default Header
