import { useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { login } from '@/store/userSlice';
import { oAuthCallback } from '@/api/auth';
import { STORAGE_KEYS, URL_PARAMS } from '../constants';

/**
 * OAuth回调处理的hook
 */
export const useOAuthCallback = () => {
  const dispatch = useDispatch();
  const [urlSearchParams] = useSearchParams();
  console.log(22, urlSearchParams);

  // 处理CAS回调
  useEffect(() => {
    const handleCASCallback = async () => {
      const ticket = urlSearchParams.get(URL_PARAMS.TICKET);
      console.log(333, ticket);

      if (ticket) {
        try {
          // 构造回调URL
          const redirectUri = `${window.location.origin}`;

          // 从sessionStorage获取provider信息
          const providerId = sessionStorage.getItem(STORAGE_KEYS.OAUTH_PROVIDER) || 'mandao';

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
            sessionStorage.removeItem(STORAGE_KEYS.OAUTH_PROVIDER);

            // 移除URL中的ticket参数，但保留分享参数
            const newUrl = new URL(window.location.href);
            newUrl.searchParams.delete(URL_PARAMS.TICKET);
            window.history.replaceState({}, '', newUrl.toString());
          }
        } catch (error) {
          console.error('CAS回调处理失败:', error);
        }
      }
    };

    handleCASCallback();
  }, [urlSearchParams, dispatch]);
};
