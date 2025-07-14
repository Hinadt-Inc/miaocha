import { Button, Form, Input } from 'antd';
import { UserOutlined, LockOutlined } from '@ant-design/icons';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { login } from '@/store/userSlice';
import { login as apiLogin } from '@/api/auth';
import login_bg_poster from '@/assets/login/banner-bg.png';
import login_bg_video from '@/assets/login/banner.mp4';
import styles from './index.module.less';

const LoginPage = () => {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const [loading, setLoading] = useState(false);
  const [form] = Form.useForm();

  const onFinish = async (values: { username: string; password: string }) => {
    setLoading(true);

    try {
      const response = await apiLogin({
        email: values.username,
        password: values.password,
      });
      if (!response) return;

      dispatch(
        login({
          userId: response.userId,
          name: response.nickname,
          role: response.role,
          tokens: {
            accessToken: response.token,
            refreshToken: response.refreshToken,
            expiresAt: response.expiresAt,
            refreshExpiresAt: response.refreshExpiresAt,
          },
        }),
      );
      navigate('/');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={styles.loginPage}>
      <video autoPlay muted loop playsInline className={styles.videoBackground} poster={login_bg_poster}>
        <source src={login_bg_video} type="video/mp4" />
      </video>

      <div className={styles.contentWrapper}>
        <div className={styles.brandSection}>
          <div className={styles.logoContainer}>
            <img src="/logo.png" alt="Logo" className={styles.logo} />
            <span className={styles.brandName}>秒查</span>
          </div>
          <div className={styles.slogan}>一站式日志采集、日志查询、日志分析</div>
        </div>

        <div className={styles.loginCard}>
          <div className={styles.cardHeader}>欢迎登录</div>
          <Form form={form} onFinish={onFinish} size="large" layout="vertical" className={styles.loginForm}>
            <Form.Item
              name="username"
              rules={[{ required: true, message: '请输入用户名' }]}
              normalize={(value) => value.trim()}
            >
              <Input prefix={<UserOutlined />} placeholder="用户名" allowClear maxLength={30} />
            </Form.Item>

            <Form.Item
              name="password"
              rules={[{ required: true, message: '请输入密码' }]}
              normalize={(value) => value.trim()}
            >
              <Input.Password prefix={<LockOutlined />} placeholder="密码" allowClear maxLength={30} />
            </Form.Item>

            <Form.Item>
              <Button type="primary" htmlType="submit" block size="large" loading={loading}>
                登录
              </Button>
            </Form.Item>
          </Form>
        </div>
      </div>

      <div className={styles.footer}>
        Copyright © {new Date().getFullYear()} 秒查 All Rights Reserved. 海纳数科 版权所有
      </div>
    </div>
  );
};

export default LoginPage;
