import { useState, useEffect, useCallback, useMemo, memo } from 'react';
import { Form, Input, Modal } from 'antd';
import { changeMyPassword, ChangePasswordParams } from '@/api/user';
import { getOAuthProviders, logoutToOAuthProvider } from '@/api/auth';
import { useSelector, useDispatch } from 'react-redux';
import { useNavigate } from 'react-router-dom';
import { Avatar, Dropdown, Button, Typography, Space, Spin, Tooltip, Card, Row, Col, Divider, Tag, App } from 'antd';
import type { MenuProps } from 'antd';
import { UserOutlined, LogoutOutlined, ReloadOutlined, LockOutlined } from '@ant-design/icons';
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
  const { modal, message } = App.useApp();
  const [form] = Form.useForm();
  const [changingPassword, setChangingPassword] = useState(false);
  const [passwordModalVisible, setPasswordModalVisible] = useState(false);

  const confirmLogout = useCallback(() => {
    modal.confirm({
      title: '确认退出登录',
      content: '您确定要退出当前账号吗？',
      okText: '确认',
      cancelText: '取消',
      onOk: async () => {
        try {
          // 先调用后端退出接口
          await dispatch(logoutUser());
          
          // 判断登录类型，决定退出方式
          if (user.loginType === 'mandao') {
            console.log('mandao用户退出，跳转到第三方退出页面');
            // mandao用户需要跳转到第三方退出
            try {
              const providers = await getOAuthProviders();
              const mandaoProvider = providers?.find(p => p.providerId === 'mandao');
              if (mandaoProvider?.revocationEndpoint) {
                logoutToOAuthProvider(mandaoProvider.revocationEndpoint);
                return; // 不执行navigate('/login')，因为会跳转到第三方
              }
            } catch (error) {
              console.error('获取OAuth提供者信息失败:', error);
            }
          }
          
          // system用户或mandao用户获取provider失败时的处理
          navigate('/login');
        } catch (error) {
          navigate('/login');
          console.error('退出登录失败:', error);
        }
      },
    });
  }, [dispatch, modal, navigate, user.loginType]);

  const handleRetryFetchUserInfo = useCallback(() => {
    dispatch(fetchUserInfo());
  }, [dispatch]);

  const showModal = useCallback(() => {
    if (user.isLoggedIn && !user.email) {
      dispatch(fetchUserInfo());
    }
    setIsModalVisible(true);
  }, [dispatch, user.isLoggedIn, user.email]);

  const handleCancel = useCallback(() => {
    setIsModalVisible(false);
  }, []);

  const items = useMemo<MenuProps['items']>(
    () => [
      {
        key: '1',
        label: '个人资料',
        icon: <UserOutlined />,
        onClick: showModal,
      },
      {
        key: '2',
        label: '修改密码',
        icon: <LockOutlined />,
        onClick: () => {
          setPasswordModalVisible(true);
          form.resetFields();
        },
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
    ],
    [showModal, confirmLogout, form],
  );

  const formatDate = useCallback((dateString?: string) => {
    if (!dateString) return '未知';
    return new Date(dateString).toLocaleString('zh-CN');
  }, []);

  useEffect(() => {
    if (user.isLoggedIn && user.sessionChecked && (!user.name || !user.email)) {
      dispatch(fetchUserInfo());
    }
  }, [dispatch, user.isLoggedIn, user.sessionChecked, user.name, user.email]);

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
      return <Avatar src={user.avatar} icon={!user.avatar ? <UserOutlined /> : undefined} size="small" />;
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

  const handleChangePassword = useCallback(
    async (values: ChangePasswordParams) => {
      try {
        setChangingPassword(true);
        await changeMyPassword({
          oldPassword: values.oldPassword,
          newPassword: values.newPassword,
        });
        message.success('密码修改成功');
        form.resetFields();
        setPasswordModalVisible(false);
      } catch (error) {
        message.error('密码修改失败: ' + (error as Error).message);
      } finally {
        setChangingPassword(false);
      }
    },
    [form, message],
  );

  const modalContent = useMemo(() => {
    if (user.loading) {
      return (
        <div className={styles.loading}>
          <Spin size="large" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }} />
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

      <Modal
        title="修改密码"
        open={passwordModalVisible}
        onCancel={() => setPasswordModalVisible(false)}
        footer={null}
        width={500}
        centered
      >
        <Form form={form} onFinish={handleChangePassword} layout="vertical">
          <Form.Item name="oldPassword" label="旧密码" rules={[{ required: true, message: '请输入旧密码' }]}>
            <Input.Password />
          </Form.Item>
          <Form.Item
            name="newPassword"
            label="新密码"
            rules={[
              { required: true, message: '请输入新密码' },
              { min: 8, message: '密码长度至少8位' },
            ]}
          >
            <Input.Password />
          </Form.Item>
          <Form.Item
            name="confirmPassword"
            label="确认新密码"
            dependencies={['newPassword']}
            rules={[
              { required: true, message: '请确认新密码' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue('newPassword') === value) {
                    return Promise.resolve();
                  }
                  return Promise.reject(new Error('两次输入的密码不一致'));
                },
              }),
            ]}
          >
            <Input.Password />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" loading={changingPassword}>
              修改密码
            </Button>
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
};

export default memo(Profile);
