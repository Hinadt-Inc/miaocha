import { Avatar, Button, Dropdown, Layout, Space } from 'antd'
import { UserOutlined, LogoutOutlined, DownOutlined, DashboardOutlined, AppstoreOutlined } from '@ant-design/icons'
import { useSelector, useDispatch } from 'react-redux'
import { RootState } from '../store/store'
import { logout } from '../store/userSlice'
import { Link, useLocation } from 'react-router-dom'
import { useState, useEffect } from 'react'

const { Header: AntHeader } = Layout

const Header = () => {
  const user = useSelector((state: RootState) => state.user)
  const dispatch = useDispatch()
  const location = useLocation()

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

  const navItems = [
    {
      key: 'home',
      label: <Link to="/">首页</Link>
    },
    {
      key: 'dashboard',
      label: <Link to="/dashboard">仪表盘</Link>,
      icon: <DashboardOutlined />
    },
    {
      key: 'system',
      label: '系统管理',
      children: [
        {
          key: '/system/user',
          label: <Link to="/system/user">用户管理</Link>
        }
      ]
    }
  ]

  return (
    <AntHeader
      style={{
        display: 'flex',
        alignItems: 'center',
        padding: '0 24px',
        background: '#fff',
        boxShadow: '0 1px 4px rgba(0, 21, 41, 0.08)',
        height: '64px',
        position: 'fixed',
        top: 0,
        right: 0,
        left: 0,
        zIndex: 100
      }}
    >
{/*       
      <Menu
        mode="horizontal"
        selectedKeys={[selectedKey]}
        items={navItems}
        style={{ 
          flex: 1,
          borderBottom: 'none'
        }}
      /> */}
      
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
