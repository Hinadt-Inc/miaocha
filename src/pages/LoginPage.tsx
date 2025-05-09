import { Button, Form, Input, Alert, message, Typography } from 'antd';
import { UserOutlined, LockOutlined } from '@ant-design/icons';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { login } from '../store/userSlice';
import { login as apiLogin } from '../api/auth';
import login_bg_poster from '@/assets/login/banner-bg.png';
import login_bg_video from '@/assets/login/banner.mp4';
import styles from './LoginPage.module.less';

const { Title, Text } = Typography;

const LoginPage = () => {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [form] = Form.useForm();

  const onFinish = async (values: { username: string; password: string }) => {
    setLoading(true);
    setError(null);
    
    try {
      const response = await apiLogin({
        email: values.username,
        password: values.password,
      });
      
      dispatch(login({
        userId: response.userId,
        name: response.nickname,
        role: response.role,
        tokens: {
          accessToken: response.token,
          refreshToken: response.refreshToken,
          expiresAt: response.expiresAt,
          refreshExpiresAt: response.refreshExpiresAt
        },
      }));
      
      message.success('登录成功！');
      navigate('/'); 
    } catch {
      setError('登录时发生错误，请稍后再试！');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={styles.loginPage}>
      <video
        autoPlay
        muted
        loop
        playsInline
        className={styles.videoBackground}
        poster={login_bg_poster}
      >
        <source src={login_bg_video} type="video/mp4" />
      </video>
      <div className={styles.overlay} />

      <div className={styles.contentWrapper}>
        <div className={styles.brandSection}>
          <div className={styles.logoContainer}>
            <img src="/logo.png" alt="Logo" className={styles.logo} />
            <Title level={2} className={styles.brandName}>秒查</Title>
          </div>
          <div className={styles.slogan}>
            <Text>一站式日志采集、日志查询、日志分析</Text>
          </div>
        </div>

        <div className={styles.loginCard}>
          <div className={styles.cardHeader}>
            <Title level={3}>欢迎使用秒查</Title>
            <Text type="secondary">请登录您的账号</Text>
          </div>

          {error && (
            <Alert 
              message={error} 
              type="error" 
              showIcon 
              closable 
              className={styles.errorAlert}
            />
          )}

          <Form
            form={form}
            name="login"
            onFinish={onFinish}
            size="large"
            layout="vertical"
            className={styles.loginForm}
          >
            <Form.Item
              name="username"
              rules={[{ required: true, message: '请输入用户名!' }]}
            >
              <Input 
                prefix={<UserOutlined className={styles.inputIcon} />} 
                placeholder="用户名" 
                allowClear
                className={styles.formInput}
              />
            </Form.Item>

            <Form.Item
              name="password"
              rules={[
                { required: true, message: '请输入密码!' },
                { min: 6, message: '密码长度不能少于6个字符' }
              ]}
            >
              <Input.Password 
                prefix={<LockOutlined className={styles.inputIcon} />} 
                placeholder="密码"
                className={styles.formInput}
              />
            </Form.Item>

            <Form.Item>
              <Button
                type="primary" 
                htmlType="submit" 
                block 
                className={styles.loginButton} 
                loading={loading}
              >
                登录
              </Button>
            </Form.Item>
          </Form>
        </div>
      </div>

      <div className={styles.footer}>
        <Text type="secondary">© 2025 秒查 - 版权所有</Text>
      </div>
    </div>
  );
};

export default LoginPage;
