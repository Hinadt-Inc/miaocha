import { Button, Divider } from 'antd';
import { useState, useEffect } from 'react';
import { getOAuthProviders } from '../../api/auth';
import { getOAuthRedirectUri } from '../../config/env';
import { OAuthStorageHelper } from '../../utils/secureStorage';
import styles from './styles.module.less';

interface OAuthButtonsProps {
  onError?: (error: string) => void;
}

// 临时类型定义
interface Provider {
  providerId: string;
  displayName: string;
  description: string;
  version: string;
  authorizationEndpoint: string;
  tokenEndpoint: string;
  userinfoEndpoint: string | null;
  revocationEndpoint: string;
  iconUrl: string | null;
  enabled: boolean;
  sortOrder: number;
  supportedScopes: string;
  supportedResponseTypes: string;
  supportedGrantTypes: string;
}

const OAuthButtons = ({ onError }: OAuthButtonsProps) => {
  const [providers, setProviders] = useState<Provider[]>([]);
  const [loading, setLoading] = useState(true);
  const [initiatingLogin, setInitiatingLogin] = useState<string | null>(null);

  useEffect(() => {
    const fetchProviders = async () => {
      try {
        console.log('开始获取OAuth提供者列表...');
        const response = await getOAuthProviders();
        if (response) {
          // 只显示启用的提供者，并按排序顺序排列
          const enabledProviders = response
            .filter(provider => provider.enabled)
            .sort((a, b) => a.sortOrder - b.sortOrder);
          console.log('获取到OAuth提供者:', enabledProviders.map(p => p.displayName));
          setProviders(enabledProviders);
        }
      } catch (error) {
        console.error('获取第三方登录提供者失败:', error);
        onError?.('无法加载第三方登录选项');
      } finally {
        setLoading(false);
      }
    };

    fetchProviders();
  }, [onError]);

  const handleOAuthLogin = (provider: Provider) => {
    try {
      setInitiatingLogin(provider.providerId);
      
      // 保存当前页面URL，登录成功后跳转回来
      // 如果当前在登录页面，则保存首页路径，否则保存当前路径
      const currentPath = window.location.pathname + window.location.search;
      const returnUrl = currentPath === '/login' ? '/' : currentPath;
      OAuthStorageHelper.setReturnUrl(returnUrl);
      // 同时保存provider信息，方便回调时使用
      OAuthStorageHelper.setProvider(provider.providerId);
      
      // 构造回调URL - 使用环境配置的URL
      const redirectUri = getOAuthRedirectUri();
      
      // 根据提供者类型构造不同的授权URL参数
      let authParams: URLSearchParams;
      
      if (provider.providerId.toLowerCase().includes('cas') || provider.providerId === 'mandao') {
        // CAS协议使用service参数
        authParams = new URLSearchParams({
          service: redirectUri,
        });
      } else {
        // 标准OAuth2协议使用response_type、client_id、redirect_uri等参数
        authParams = new URLSearchParams({
          response_type: 'code',
          redirect_uri: redirectUri,
          state: provider.providerId, // 使用providerId作为state参数
        });
      }
      
      // 构造完整的授权URL并跳转
      const authUrl = `${provider.authorizationEndpoint}?${authParams.toString()}`;
      console.log('正在跳转到第三方登录:', authUrl);
      
      // 短暂延迟后跳转，让用户看到loading状态
      setTimeout(() => {
        window.location.href = authUrl;
      }, 100);
    } catch (error) {
      console.error('启动第三方登录失败:', error);
      setInitiatingLogin(null);
      onError?.('启动第三方登录失败，请重试');
    }
  };

  if (loading) {
    return null; // 或者显示加载状态
  }

  if (providers.length === 0) {
    return null; // 没有可用的第三方登录提供者
  }

  return (
    <div className={styles.oauthContainer}>
      <Divider className={styles.divider}>或</Divider>
      
      <div className={styles.providersContainer}>
        {providers.map((provider) => (
          <Button
            key={provider.providerId}
            size="large"
            block
            className={styles.oauthButton}
            onClick={() => handleOAuthLogin(provider)}
            loading={initiatingLogin === provider.providerId}
            disabled={!!initiatingLogin}
          >
            {provider.iconUrl && !initiatingLogin && (
              <img 
                src={provider.iconUrl} 
                alt={provider.displayName}
                className={styles.providerIcon}
              />
            )}
            {initiatingLogin === provider.providerId 
              ? `正在跳转到 ${provider.displayName}...` 
              : `使用 ${provider.displayName} 登录`
            }
          </Button>
        ))}
      </div>
      
      {providers.length > 0 && (
        <div className={styles.description}>
          点击上方按钮将跳转到第三方登录页面
        </div>
      )}
    </div>
  );
};

export default OAuthButtons;
