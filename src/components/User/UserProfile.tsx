import React, { useState, useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { useNavigate } from 'react-router-dom';
import { Avatar, Modal, Dropdown, Button, Typography, Space, Spin, Descriptions, Tooltip, Card, Row, Col, Divider, Tag, App } from 'antd';
import type { MenuProps } from 'antd';
import { 
  UserOutlined, 
  LogoutOutlined, 
  SettingOutlined,
  MailOutlined,
  ReloadOutlined,
  IdcardOutlined,
  TeamOutlined,
  ClockCircleOutlined
} from '@ant-design/icons';
import { fetchUserInfo, logoutUser } from '../../store/userSlice';
import type { AppDispatch } from '../../store/store';

// 自定义hook，用于检测菜单栏收缩状态
const useMenuCollapsed = (): boolean => {
  const [collapsed, setCollapsed] = useState<boolean>(() => {
    // 初始化时检查一次收缩状态
    const sider = document.querySelector('.ant-pro-sider');
    return sider ? sider.classList.contains('ant-pro-sider-collapsed') : false;
  });

  useEffect(() => {
    // 检查菜单栏收缩状态
    const checkCollapsedState = () => {
      const sider = document.querySelector('.ant-pro-sider');
      if (sider) {
        const isCollapsed = sider.classList.contains('ant-pro-sider-collapsed');
        setCollapsed(isCollapsed);
      }
    };

    // 使用ResizeObserver监听侧边栏大小变化
    const resizeObserver = new ResizeObserver(() => {
      checkCollapsedState();
    });
    
    // 监听侧边栏元素的变化
    const sider = document.querySelector('.ant-pro-sider');
    if (sider) {
      resizeObserver.observe(sider);
    }

    // 创建MutationObserver来监听侧边栏类名变化
    const mutationObserver = new MutationObserver((mutations) => {
      for (const mutation of mutations) {
        if (mutation.type === 'attributes' && mutation.attributeName === 'class') {
          checkCollapsedState();
          break;
        }
      }
    });
    
    if (sider) {
      mutationObserver.observe(sider, { attributes: true });
    }

    return () => {
      resizeObserver.disconnect();
      mutationObserver.disconnect();
    };
  }, []);

  return collapsed;
};

const { Text } = Typography;

interface UserStateType {
  id: number;
  name: string;
  email: string;
  role: string;
  avatar?: string;
  lastLoginAt?: string;
  isLoggedIn: boolean;
  loading: boolean;
  error: string | null;
  sessionChecked: boolean;
}

const UserProfile: React.FC = () => {
  const dispatch = useDispatch<AppDispatch>();
  const navigate = useNavigate();
  const user = useSelector((state: { user: UserStateType }) => state.user);
  const [isModalVisible, setIsModalVisible] = useState(false);
  const isMenuCollapsed = useMenuCollapsed();
  const { modal } = App.useApp(); // 在组件顶层调用 App.useApp()
  
  // 根据菜单栏收缩状态设置样式
  const avatarStyle = {
    marginRight: isMenuCollapsed ? 0 : 8,
  };

  const confirmLogout = () => {
    modal.confirm({ // 使用之前获取的 modal
      title: '确认退出登录',
      content: '您确定要退出当前账号吗？',
      okText: '确认',
      cancelText: '取消',
      onOk: async () => {
        try {
          await dispatch(logoutUser());
          navigate('/login');
        } catch (error) {
          console.error('退出登录失败:', error);
        }
      },
    });
  };

  // 组件加载时获取用户信息
  useEffect(() => {
    if (user.isLoggedIn && user.sessionChecked && (!user.name || !user.email)) {
      dispatch(fetchUserInfo());
    }
  }, [dispatch, user.isLoggedIn && user.sessionChecked && (!user.name || !user.email)]);

  const handleRetryFetchUserInfo = () => {
    dispatch(fetchUserInfo());
  };

  const showModal = () => {
    if (user.isLoggedIn && (!user.email || !user.lastLoginAt)) {
      dispatch(fetchUserInfo());
    }
    setIsModalVisible(true);
  };

  const handleCancel = () => {
    setIsModalVisible(false);
  };

  const items: MenuProps['items'] = [
    {
      key: '1',
      label: '个人资料',
      icon: <UserOutlined />,
      onClick: () => showModal(),
    },
    {
      key: '2',
      label: '设置',
      icon: <SettingOutlined />,
    },
    {
      key: '3',
      danger: true,
      label: '退出登录',
      icon: <LogoutOutlined />,
      onClick: (e) => {
        e?.domEvent.stopPropagation();
        confirmLogout();
      },
    },
  ];

  const formatDate = (dateString?: string) => {
    if (!dateString) return '未知';
    return new Date(dateString).toLocaleString('zh-CN');
  };

  if (!user.sessionChecked) {
    return (
      <Button type="text" className="user-profile-button">
        <Space align="center">
          <Avatar icon={<UserOutlined />} size="small" />
          <Spin size="small" />
        </Space>
      </Button>
    );
  }

  if (!user.isLoggedIn) {
    return null;
  }

  return (
    <>
      <Dropdown menu={{ items }} arrow>
        <Button type="text" className="user-profile-button" style={{ padding: isMenuCollapsed ? '4px 8px' : undefined }}>
          <Space size={isMenuCollapsed ? 0 : 'small'} align="center" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <Avatar 
              src={user.avatar} 
              icon={!user.avatar ? <UserOutlined /> : undefined} 
              size="small"
              style={{
                ...avatarStyle,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            />
            {user.loading ? (
              <Spin size="small" style={{ display: 'flex', alignItems: 'center' }} />
            ) : (
              !isMenuCollapsed && (
                <Space align="center">
                  <Text ellipsis style={{ maxWidth: 120, verticalAlign: 'middle' }}>
                    {user.name || '用户'}
                  </Text>
                  {user.error && (
                    <Tooltip title="重新获取用户信息">
                      <ReloadOutlined onClick={handleRetryFetchUserInfo} style={{ cursor: 'pointer' }} />
                    </Tooltip>
                  )}
                </Space>
              )
            )}
          </Space>
        </Button>
      </Dropdown>

      <Modal
        title={null}
        open={isModalVisible}
        onCancel={handleCancel}
        footer={null}
        width={700}
        centered
        closable={false}
        className="user-profile-modal"
      >
        {user.loading ? (
          <div style={{ textAlign: 'center', padding: '60px 20px' }}>
            <Spin size="large" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }} />
            <p style={{ marginTop: '16px', fontSize: '16px', color: '#8c8c8c' }}>正在获取用户信息...</p>
          </div>
        ) : user.error ? (
          <div style={{ textAlign: 'center', padding: '60px 20px' }}>
            <p style={{ marginBottom: '24px', fontSize: '16px', color: '#ff4d4f' }}>获取用户信息失败: {user.error}</p>
            <Button 
              type="primary" 
              icon={<ReloadOutlined />} 
              onClick={handleRetryFetchUserInfo}
              size="large"
              shape="round"
            >
              重新获取
            </Button>
          </div>
        ) : (
          <div className="user-info-container">
            {/* 顶部背景区域 */}
            <div style={{ 
              height: '200px', 
              background: 'linear-gradient(135deg, #1890ff 0%, #722ed1 100%)',
              borderTopLeftRadius: '8px',
              borderTopRightRadius: '8px',
              position: 'relative'
            }}>
              <div style={{ 
                position: 'absolute', 
                top: '70px', 
                left: '50%', 
                transform: 'translateX(-50%)',
                zIndex: 1
              }}>
                <Avatar 
                  src={user.avatar} 
                  icon={!user.avatar ? <UserOutlined /> : undefined} 
                  size={100}
                  style={{ 
                    border: '4px solid #fff',
                    boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center'
                  }}
                />
              </div>
              <Button 
                type="text" 
                onClick={handleCancel} 
                style={{ 
                  position: 'absolute', 
                  top: '16px', 
                  right: '16px',
                  color: '#fff',
                  fontSize: '16px'
                }}
              >
                ×
              </Button>
            </div>
            
            {/* 用户信息内容区 */}
            <div style={{ padding: '60px 0 40px' }}>
              <div style={{ textAlign: 'center', marginBottom: '24px' }}>
                <Typography.Title level={3} style={{ margin: '8px 0 4px', fontWeight: 600 }}>
                  {user.name || '未知用户'}
                </Typography.Title>
                <Tag color="blue" style={{ margin: '8px 0', fontSize: '14px', padding: '2px 10px' }}>
                  {user.role || '未知角色'}
                </Tag>
              </div>
              
              <Divider style={{ margin: '16px 0 24px' }} />
              
              <Row gutter={[24, 24]}>
                <Col span={8}>
                  <Card
                    bordered={false}
                    className="info-card"
                    style={{ 
                      borderRadius: '8px',
                      boxShadow: '0 2px 8px rgba(0,0,0,0.06)',
                      height: '100%'
                    }}
                  >
                    <Space direction="vertical" size={4} style={{ width: '100%' }}>
                      <div style={{ color: '#8c8c8c', fontSize: '14px', marginBottom: '4px' }}>用户ID</div>
                      <Space>
                        <IdcardOutlined style={{ color: '#1890ff' }} />
                        <Typography.Text strong>{user.id || '未知'}</Typography.Text>
                      </Space>
                    </Space>
                  </Card>
                </Col>
                
                <Col span={8}>
                  <Card
                    variant="borderless"
                    className="info-card"
                    style={{ 
                      borderRadius: '8px',
                      boxShadow: '0 2px 8px rgba(0,0,0,0.06)',
                      height: '100%'
                    }}
                  >
                    <Space direction="vertical" size={4} style={{ width: '100%' }}>
                      <div style={{ color: '#8c8c8c', fontSize: '14px', marginBottom: '4px' }}>邮箱</div>
                      <Space>
                        <MailOutlined style={{ color: '#1890ff' }} />
                        <Typography.Text strong>{user.email || '未设置'}</Typography.Text>
                      </Space>
                    </Space>
                  </Card>
                </Col>
                
                <Col span={8}>
                  <Card
                    variant="borderless"
                    className="info-card"
                    style={{ 
                      borderRadius: '8px',
                      boxShadow: '0 2px 8px rgba(0,0,0,0.06)',
                      height: '100%'
                    }}
                  >
                    <Space direction="vertical" size={4} style={{ width: '100%' }}>
                      <div style={{ color: '#8c8c8c', fontSize: '14px', marginBottom: '4px' }}>最后登录</div>
                      <Space>
                        <ClockCircleOutlined style={{ color: '#1890ff' }} />
                        <Typography.Text strong>{formatDate(user.lastLoginAt)}</Typography.Text>
                      </Space>
                    </Space>
                  </Card>
                </Col>
              </Row>
            </div>
          </div>
        )}
      </Modal>
    </>
  );
};

export default UserProfile;
