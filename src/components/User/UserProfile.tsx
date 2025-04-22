import React, { useState, useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { Avatar, Modal, Dropdown, Button, Typography, Space, Spin, Descriptions, Tooltip } from 'antd';
import type { PopconfirmProps, MenuProps } from 'antd';
import { 
  UserOutlined, 
  LogoutOutlined, 
  SettingOutlined,
  MailOutlined,
  ReloadOutlined
} from '@ant-design/icons';
import { fetchUserInfo } from '../../store/userSlice';
import type { AppDispatch } from '../../store/store';

const { Text } = Typography;

const confirm: PopconfirmProps['onConfirm'] = (e) => {
  console.log(e);
};

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
  const user = useSelector((state: { user: UserStateType }) => state.user);
  const [isModalVisible, setIsModalVisible] = useState(false);
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
    // 在显示模态框前确保用户信息已加载
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
        confirm();
      },
    },
  ];

  const formatDate = (dateString?: string) => {
    if (!dateString) return '未知';
    return new Date(dateString).toLocaleString('zh-CN');
  };

  // 会话检查中显示加载状态
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

  // 未登录则不显示
  if (!user.isLoggedIn) {
    return null;
  }

  // 即使没有用户名，也显示基本信息
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
