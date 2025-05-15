import { useState, useEffect, useCallback, useMemo, memo } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { useNavigate } from 'react-router-dom';
import {
  Avatar,
  Modal,
  Dropdown,
  Button,
  Typography,
  Space,
  Spin,
  Tooltip,
  Card,
  Row,
  Col,
  Divider,
  Tag,
  App,
} from 'antd';
import type { MenuProps } from 'antd';
import { UserOutlined, LogoutOutlined, SettingOutlined, ReloadOutlined } from '@ant-design/icons';
import { fetchUserInfo, logoutUser } from '@/store/userSlice';
import type { AppDispatch } from '@/store/store';
import styles from './index.module.less';

const { Text } = Typography;

interface IProps {
  collapsed?: boolean;
}

const Profile: React.FC<IProps> = ({ collapsed = false }) => {
  const dispatch = useDispatch<AppDispatch>();
  const navigate = useNavigate();
  const user = useSelector((state: { user: IStoreUser }) => state.user);
  const [isModalVisible, setIsModalVisible] = useState(false);
  const { modal } = App.useApp();

  // 使用 useCallback 缓存函数
  const confirmLogout = useCallback(() => {
    modal.confirm({
      title: '确认退出登录',
      content: '您确定要退出当前账号吗？',
      okText: '确认',
      cancelText: '取消',
      onOk: async () => {
        try {
          await dispatch(logoutUser());
          navigate('/login');
        } catch (error) {
          navigate('/login');
          console.error('退出登录失败:', error);
        }
      },
    });
  }, [dispatch, modal, navigate]);

  // 使用 useCallback 缓存函数
  const handleRetryFetchUserInfo = useCallback(() => {
    dispatch(fetchUserInfo());
  }, [dispatch]);

  // 使用 useCallback 缓存函数
  const showModal = useCallback(() => {
    if (user.isLoggedIn && !user.email) {
      dispatch(fetchUserInfo());
    }
    setIsModalVisible(true);
  }, [dispatch, user.isLoggedIn, user.email]);

  // 使用 useCallback 缓存函数
  const handleCancel = useCallback(() => {
    setIsModalVisible(false);
  }, []);

  // 使用 useMemo 缓存菜单项
  const items = useMemo<MenuProps['items']>(
    () => [
      {
        key: '1',
        label: '个人资料',
        icon: <UserOutlined />,
        onClick: showModal,
      },
      // {
      //   key: '2',
      //   label: '设置',
      //   icon: <SettingOutlined />,
      // },
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
    ],
    [showModal, confirmLogout],
  );

  // 使用 useMemo 缓存日期格式化函数
  const formatDate = useCallback((dateString?: string) => {
    if (!dateString) return '未知';
    return new Date(dateString).toLocaleString('zh-CN');
  }, []);

  // 只在必要时获取用户信息
  useEffect(() => {
    if (user.isLoggedIn && user.sessionChecked && (!user.name || !user.email)) {
      dispatch(fetchUserInfo());
    }
  }, [dispatch, user.isLoggedIn, user.sessionChecked, user.name, user.email]);

  // 使用 useMemo 缓存用户信息渲染
  const renderUserInfo = useMemo(() => {
    if (!user.sessionChecked) {
      return (
        <Button type="text" className={styles.profileButton}>
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

    if (collapsed) {
      return (
        <Avatar src={user.avatar} icon={!user.avatar ? <UserOutlined /> : undefined} size="small" />
      );
    }

    return (
      <Space>
        <Avatar src={user.avatar} icon={!user.avatar ? <UserOutlined /> : undefined} size="small" />
        {user.loading ? (
          <Spin size="small" />
        ) : (
          <Space>
            <Text ellipsis style={{ maxWidth: 150, verticalAlign: 'middle' }}>
              {user.name || '用户'}
            </Text>
            {user.error && (
              <Tooltip placement="left" title="重新获取用户信息">
                <ReloadOutlined onClick={handleRetryFetchUserInfo} style={{ cursor: 'pointer' }} />
              </Tooltip>
            )}
          </Space>
        )}
      </Space>
    );
  }, [user, collapsed, handleRetryFetchUserInfo]);

  // 使用 useMemo 缓存模态框内容
  const modalContent = useMemo(() => {
    if (user.loading) {
      return (
        <div className={styles.loading}>
          <Spin
            size="large"
            style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}
          />
          <p className={styles.loadingText}>正在获取用户信息...</p>
        </div>
      );
    }

    if (user.error) {
      return (
        <div className={styles.error}>
          <p className={styles.errorText}>获取用户信息失败: {user.error}</p>
          <Button
            type="primary"
            icon={<ReloadOutlined />}
            onClick={handleRetryFetchUserInfo}
            size="large"
            shape="round"
            className={styles.retryButton}
          >
            重新获取
          </Button>
        </div>
      );
    }

    return (
      <div className="user-info-container">
        <div className={styles.header}>
          <div className={styles.avatarWrapper}>
            <Avatar
              src={user.avatar}
              icon={!user.avatar ? <UserOutlined /> : undefined}
              size={100}
              className={styles.avatar}
            />
          </div>
          <Button type="text" onClick={handleCancel} className={styles.closeButton}>
            ×
          </Button>
        </div>

        <div className={styles.content}>
          <div className={styles.title}>
            <Typography.Title level={3}>{user.name || '未知用户'}</Typography.Title>
            <Tag color="blue" className={styles.tag}>
              {user.role || '未知角色'}
            </Tag>
          </div>

          <Divider className={styles.divider} />

          <Row gutter={[24, 24]}>
            <Col span={8}>
              <Card bordered={false} className={styles.card}>
                <Space direction="vertical" size={4} style={{ width: '100%' }}>
                  <div className={styles.cardTitle}>用户ID</div>
                  <Space>
                    <Typography.Text strong>{user.userId || '未知'}</Typography.Text>
                  </Space>
                </Space>
              </Card>
            </Col>

            <Col span={8}>
              <Card variant="borderless" className={styles.card}>
                <Space direction="vertical" size={4} style={{ width: '100%' }}>
                  <div className={styles.cardTitle}>邮箱</div>
                  <Space>
                    <Typography.Text strong>{user.email || '未设置'}</Typography.Text>
                  </Space>
                </Space>
              </Card>
            </Col>

            <Col span={8}>
              <Card variant="borderless" className={styles.card}>
                <Space direction="vertical" size={4} style={{ width: '100%' }}>
                  <div className={styles.cardTitle}>创建时间</div>
                  <Space>
                    <Typography.Text strong>{formatDate(user.createTime)}</Typography.Text>
                  </Space>
                </Space>
              </Card>
            </Col>
          </Row>
        </div>
      </div>
    );
  }, [user, handleRetryFetchUserInfo, handleCancel, formatDate]);

  return (
    <>
      <Dropdown menu={{ items }} arrow>
        {renderUserInfo}
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
        {modalContent}
      </Modal>
    </>
  );
};

export default memo(Profile);
