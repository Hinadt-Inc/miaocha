import { useState, useEffect } from 'react';

import { UserOutlined, LockOutlined } from '@ant-design/icons';
import { Button, Form, Input, Alert, message } from 'antd';
import { useDispatch } from 'react-redux';
import { useNavigate, useSearchParams } from 'react-router-dom';

import { login as apiLogin, oAuthCallback } from '@/api/auth';
import login_bg_poster from '@/assets/login/banner-bg.png';
import login_bg_video from '@/assets/login/banner.mp4';
import OAuthButtons from '@/components/OAuthButtons/index';
import { getOAuthRedirectUri } from '@/constants/env';
import { login } from '@/store/userSlice';
import { broadcastLogin, onLogin, offLogin } from '@/utils/crossWindowSync';
import { OAuthStorageHelper } from '@/utils/secureStorage';

import styles from './index.module.less';

const LoginPage = () => {
  const [messageApi, contextHolder] = message.useMessage();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const dispatch = useDispatch();
  const [loading, setLoading] = useState(false);
  const [casLoading, setCasLoading] = useState(false);
  const [form] = Form.useForm();

  // 监听跨窗口登录/登出事件
  useEffect(() => {
    const handleLogin = () => {
      console.log('[LoginPage] 接收到跨窗口登录成功事件');
      // 刷新页面，跳转到首页
      window.location.href = window.location.origin;
    };

    // 注册监听器
    onLogin(handleLogin);

    // 清理监听器
    return () => {
      offLogin(handleLogin);
    };
  }, [form]);

  // 处理OAuth回调（包括CAS、OAuth2等）
  useEffect(() => {
    const handleOAuthCallback = async () => {
      // 检查URL参数中是否有授权码或票据
      const ticket = searchParams.get('ticket'); // CAS协议
      const code = searchParams.get('code'); // OAuth2协议
      const state = searchParams.get('state'); // OAuth2 state参数
      const error = searchParams.get('error'); // 错误信息

      // 如果有错误参数，直接显示错误
      if (error) {
        messageApi.error(`第三方登录失败: ${searchParams.get('error_description') || error}`);
        return;
      }

      // 获取授权码（CAS使用ticket，OAuth2使用code）
      const authCode = ticket || code;

      if (authCode) {
        setCasLoading(true);
        try {
          // 构造回调URL
          const redirectUri = getOAuthRedirectUri();

          // 从安全存储或state参数获取provider信息
          let providerId = OAuthStorageHelper.getProvider();
          if (!providerId && state) {
            providerId = state; // OAuth2可能通过state参数传递providerId
          }
          providerId = providerId || 'mandao'; // 默认provider

          console.log('处理OAuth回调:', { providerId, authCode: authCode.substring(0, 10) + '...' });

          // 调用后端回调接口
          const response = await oAuthCallback({
            provider: providerId,
            code: authCode,
            redirect_uri: redirectUri,
          });

          if (response) {
            console.log('OAuth登录成功:', { userId: response.userId, nickname: response.nickname });

            // 登录成功，更新用户状态
            dispatch(
              login({
                userId: response.userId,
                name: response.nickname,
                role: response.role,
                loginType: response.loginType,
                tokens: {
                  accessToken: response.token,
                  refreshToken: response.refreshToken,
                  expiresAt: response.expiresAt,
                  refreshExpiresAt: response.refreshExpiresAt,
                },
              }),
            );

            // 清理安全存储中的provider信息
            OAuthStorageHelper.removeProvider();

            // 跳转到首页或用户原本要访问的页面
            let returnUrl = OAuthStorageHelper.getReturnUrl() || '/';
            OAuthStorageHelper.removeReturnUrl();

            // 确保不会跳转回登录页面
            if (returnUrl === '/login') {
              returnUrl = '/';
            }

            console.log('OAuth登录完成，即将跳转到:', returnUrl);

            // 使用setTimeout确保Redux状态更新完成后再跳转
            setTimeout(() => {
              navigate(returnUrl, { replace: true });

              // 广播登录成功事件到所有窗口
              broadcastLogin();
            }, 100);
          } else {
            throw new Error('OAuth回调返回空响应');
          }
        } catch (oauthError) {
          console.error('OAuth回调处理失败:', oauthError);
          throw new Error('第三方登录失败，请重试');
        } finally {
          setCasLoading(false);
          // 清理URL参数，避免重复处理
          const newUrl = new URL(window.location.href);
          newUrl.searchParams.delete('ticket');
          newUrl.searchParams.delete('code');
          newUrl.searchParams.delete('state');
          newUrl.searchParams.delete('error');
          newUrl.searchParams.delete('error_description');
          window.history.replaceState({}, '', newUrl.toString());
        }
      }
    };

    handleOAuthCallback();
  }, [searchParams, dispatch, navigate, messageApi]);

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
          loginType: response.loginType,
          tokens: {
            accessToken: response.token,
            refreshToken: response.refreshToken,
            expiresAt: response.expiresAt,
            refreshExpiresAt: response.refreshExpiresAt,
          },
        }),
      );

      // 跳转到首页
      console.log('普通登录成功，跳转到首页');
      navigate('/', { replace: true });

      // 广播登录成功事件到所有窗口
      broadcastLogin();
    } catch (loginError) {
      console.error('登录失败:', loginError);
      throw new Error('登录失败，请检查用户名和密码');
    } finally {
      setLoading(false);
    }
  };

  const handleOAuthError = (errorMessage: string) => {
    message.error(errorMessage);
  };

  return (
    <div className={styles.loginPage}>
      {contextHolder}
      <video autoPlay className={styles.videoBackground} loop muted playsInline poster={login_bg_poster}>
        <source src={login_bg_video} type="video/mp4" />
      </video>

      <div className={styles.contentWrapper}>
        <div className={styles.brandSection}>
          <div className={styles.logoContainer}>
            <img alt="Logo" className={styles.logo} src="/logo.png" />
            <span className={styles.brandName}>秒查</span>
          </div>
          <div className={styles.slogan}>一站式日志采集、日志查询、日志分析</div>
        </div>

        <div className={styles.loginCard}>
          <div className={styles.cardHeader}>欢迎登录</div>

          {/* OAuth回调处理中的提示 */}
          {casLoading && (
            <Alert message="正在处理第三方登录，请稍候..." showIcon style={{ marginBottom: 16 }} type="info" />
          )}

          <Form className={styles.loginForm} form={form} layout="vertical" size="large" onFinish={onFinish}>
            <Form.Item
              name="username"
              normalize={(value) => value.trim()}
              rules={[{ required: true, message: '请输入用户名' }]}
            >
              <Input allowClear maxLength={30} placeholder="用户名" prefix={<UserOutlined />} />
            </Form.Item>

            <Form.Item
              name="password"
              normalize={(value) => value.trim()}
              rules={[{ required: true, message: '请输入密码' }]}
            >
              <Input.Password allowClear maxLength={30} placeholder="密码" prefix={<LockOutlined />} />
            </Form.Item>

            <Form.Item>
              <Button
                block
                disabled={loading || casLoading}
                htmlType="submit"
                loading={loading || casLoading}
                size="large"
                type="primary"
              >
                {(() => {
                  if (casLoading) return '正在处理第三方登录...';
                  if (loading) return '登录中...';
                  return '登录';
                })()}
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
