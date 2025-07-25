import { Button, Form, Input, Alert } from 'antd';
import { UserOutlined, LockOutlined } from '@ant-design/icons';
import { useState, useEffect } from 'react';
import { useNavigate, useLocation, useSearchParams } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { login } from '@/store/userSlice';
import { login as apiLogin, oAuthCallback } from '@/api/auth';
import OAuthButtons from '@/components/OAuthButtons/index';
import login_bg_poster from '@/assets/login/banner-bg.png';
import login_bg_video from '@/assets/login/banner.mp4';
import styles from './index.module.less';

const LoginPage = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams] = useSearchParams();
  const dispatch = useDispatch();
  const [loading, setLoading] = useState(false);
  const [casLoading, setCasLoading] = useState(false);
  const [error, setError] = useState<string>('');
  const [form] = Form.useForm();

  // 检查是否有来自OAuth回调的错误信息
  useEffect(() => {
    if (location.state?.error) {
      setError(location.state.error);
      // 清除location state中的错误信息
      window.history.replaceState({}, document.title);
    }
  }, [location.state]);

  // 处理CAS回调
  useEffect(() => {
    const handleCASCallback = async () => {
      const ticket = searchParams.get('ticket');
      
      if (ticket) {
        setCasLoading(true);
        try {
          // 构造回调URL
          const redirectUri = `${window.location.origin}/login`;
          
          // 从sessionStorage获取provider信息
          const providerId = sessionStorage.getItem('oauthProvider') || 'mandao';
          
          // 调用后端回调接口
          const response = await oAuthCallback({
            provider: providerId,
            code: ticket,
            redirect_uri: redirectUri,
          });

          if (response) {
            // 登录成功，更新用户状态
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

            // 清理sessionStorage中的provider信息
            sessionStorage.removeItem('oauthProvider');
            
            // 跳转到首页或用户原本要访问的页面
            const returnUrl = sessionStorage.getItem('returnUrl') || '/';
            sessionStorage.removeItem('returnUrl');
            navigate(returnUrl);
          }
        } catch (casError) {
          console.error('CAS回调处理失败:', casError);
          setError('第三方登录失败，请重试');
          
          // 移除URL中的ticket参数
          const newUrl = new URL(window.location.href);
          newUrl.searchParams.delete('ticket');
          window.history.replaceState({}, '', newUrl.toString());
        } finally {
          setCasLoading(false);
        }
      }
    };

    handleCASCallback();
  }, [searchParams, dispatch, navigate]);

  const onFinish = async (values: { username: string; password: string }) => {
    setLoading(true);
    setError(''); // 清除之前的错误信息

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
    } catch (loginError) {
      console.error('登录失败:', loginError);
      setError('登录失败，请检查用户名和密码');
    } finally {
      setLoading(false);
    }
  };

  const handleOAuthError = (errorMessage: string) => {
    setError(errorMessage);
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
          
          {/* 错误信息显示 */}
          {error && (
            <Alert 
              message={error} 
              type="error" 
              showIcon 
              closable 
              onClose={() => setError('')}
              style={{ marginBottom: 16 }}
            />
          )}
          
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
              <Button type="primary" htmlType="submit" block size="large" loading={loading || casLoading}>
                登录
              </Button>
            </Form.Item>
          </Form>
          
          {/* 第三方登录按钮 */}
          <OAuthButtons onError={handleOAuthError} />
        </div>
      </div>

      <div className={styles.footer}>
        Copyright © {new Date().getFullYear()} 秒查 All Rights Reserved. 海纳数科 版权所有
      </div>
    </div>
  );
};

export default LoginPage;
