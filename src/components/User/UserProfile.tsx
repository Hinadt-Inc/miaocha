import React, { useState, useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { useNavigate } from 'react-router-dom';
import { Avatar, Modal, Dropdown, Button, Typography, Space, Spin, Descriptions, Tooltip } from 'antd';
import type { MenuProps } from 'antd';
import { 
  UserOutlined, 
  LogoutOutlined, 
  SettingOutlined,
  MailOutlined,
  ReloadOutlined
} from '@ant-design/icons';
import { fetchUserInfo, logoutUser } from '../../store/userSlice';
import type { AppDispatch } from '../../store/store';

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

  const confirmLogout = () => {
    Modal.confirm({
      title: '确认退出登录',
      content: '您确定要退出当前账号吗？',
      okText: '确认',
      cancelText: '取消',
      onOk: async () => {
        await dispatch(logoutUser());
        navigate('/login');
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
        <Space>
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
      <Dropdown menu={{ items }} placement="bottomRight" arrow>
        <Button type="text" className="user-profile-button">
          <Space>
            <Avatar 
              src={user.avatar} 
              icon={!user.avatar ? <UserOutlined /> : undefined} 
              size="small" 
            />
            {user.loading ? (
              <Spin size="small" />
            ) : (
              <Space>
                <Text ellipsis style={{ maxWidth: 120 }}>
                  {user.name || '用户'}
                </Text>
                {user.error && (
                  <Tooltip title="重新获取用户信息">
                    <ReloadOutlined onClick={handleRetryFetchUserInfo} style={{ cursor: 'pointer' }} />
                  </Tooltip>
                )}
              </Space>
            )}
          </Space>
        </Button>
      </Dropdown>

      <Modal
        title="个人资料"
        open={isModalVisible}
        onCancel={handleCancel}
        footer={null}
        width={600}
        centered
      >
        {user.loading ? (
          <div style={{ textAlign: 'center', padding: '20px' }}>
            <Spin size="large" />
            <p style={{ marginTop: '10px' }}>正在获取用户信息...</p>
          </div>
        ) : user.error ? (
          <div style={{ textAlign: 'center', padding: '20px' }}>
            <p style={{ marginBottom: '20px' }}>获取用户信息失败: {user.error}</p>
            <Button 
              type="primary" 
              icon={<ReloadOutlined />} 
              onClick={handleRetryFetchUserInfo}
            >
              重新获取
            </Button>
          </div>
        ) : (
          <div className="user-info-container" style={{ display: 'flex', alignItems: 'center', marginBottom: '20px' }}>
            <div className="user-details-section" style={{ flex: 1 }}>
              <Descriptions column={1} bordered>
                <Descriptions.Item label="用户名">{user.name || '未知'}</Descriptions.Item>
                <Descriptions.Item label="用户ID">{user.id || '未知'}</Descriptions.Item>
                <Descriptions.Item label="角色">{user.role || '未知'}</Descriptions.Item>
                <Descriptions.Item label="邮箱">
                  <Space>
                    <MailOutlined />
                    {user.email || '未设置'}
                  </Space>
                </Descriptions.Item>
                <Descriptions.Item label="最后登录时间">
                  {formatDate(user.lastLoginAt)}
                </Descriptions.Item>
              </Descriptions>
            </div>
          </div>
        )}
      </Modal>
    </>
  );
};

export default UserProfile;
