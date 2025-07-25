import { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { Spin } from 'antd';
import { login } from '@/store/userSlice';
import { oAuthCallback } from '@/api/auth';
import styles from './Callback.module.less';

const OAuthCallback = () => {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const [searchParams] = useSearchParams();

  useEffect(() => {
    const handleCallback = async () => {
      const ticket = searchParams.get('ticket'); // CAS使用ticket参数
      const error = searchParams.get('error');
      const errorDescription = searchParams.get('error_description');

      // 检查是否有错误参数
      if (error) {
        console.error('OAuth错误:', error, errorDescription);
        navigate('/login', { 
          state: { 
            error: errorDescription || error || '第三方登录失败' 
          } 
        });
        return;
      }

      // 检查必要参数 - CAS只需要ticket
      if (!ticket) {
        console.error('缺少必要的CAS票据参数');
        navigate('/login', { 
          state: { 
            error: '登录参数不完整，请重试' 
          } 
        });
        return;
      }

      try {
        // 构造回调URL
        const redirectUri = `${window.location.origin}/login`;
        
        // 从sessionStorage获取provider信息
        const providerId = sessionStorage.getItem('oauthProvider') || 'mandao';
        
        // 调用后端回调接口，对于CAS，我们需要传递ticket
        const response = await oAuthCallback({
          provider: providerId,
          code: ticket, // 将ticket作为code参数传递
          redirect_uri: redirectUri,
        });

        if (!response) {
          navigate('/login', { 
            state: { 
              error: '登录失败，请重试' 
            } 
          });
          return;
        }

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
      } catch (error) {
        console.error('CAS回调处理失败:', error);
        navigate('/login', { 
          state: { 
            error: '登录处理失败，请重试' 
          } 
        });
      }
    };

    handleCallback();
  }, [searchParams, navigate, dispatch]);

  return (
    <div className={styles.callbackPage}>
      <div className={styles.content}>
        <Spin size="large" />
        <div className={styles.message}>正在处理登录信息...</div>
      </div>
    </div>
  );
};

export default OAuthCallback;
