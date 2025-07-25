import { Button, Divider } from 'antd';
import { useState, useEffect } from 'react';
import { getOAuthProviders } from '../../api/auth';

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

  useEffect(() => {
    const fetchProviders = async () => {
      try {
        const response = await getOAuthProviders();
        if (response) {
          // 只显示启用的提供者，并按排序顺序排列
          const enabledProviders = response
            .filter(provider => provider.enabled)
            .sort((a, b) => a.sortOrder - b.sortOrder);
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
      // 保存当前页面URL，登录成功后跳转回来
      sessionStorage.setItem('returnUrl', window.location.pathname + window.location.search);
      // 同时保存provider信息，方便回调时使用
      sessionStorage.setItem('oauthProvider', provider.providerId);
      
      // 构造回调URL - 回调到登录页面
      const redirectUri = `${window.location.origin}/login`;
      
      // 构造CAS授权URL参数
      const authParams = new URLSearchParams({
        service: redirectUri, // CAS使用service参数
      });
      
      // 直接跳转到CAS登录页面
      const authUrl = `${provider.authorizationEndpoint}?${authParams.toString()}`;
      window.location.href = authUrl;
    } catch (error) {
      console.error('启动第三方登录失败:', error);
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
    <div>
      <Divider>或</Divider>
      
      <div>
        {providers.map((provider) => (
          <Button
            key={provider.providerId}
            size="large"
            block
            onClick={() => handleOAuthLogin(provider)}
            style={{ marginBottom: 8 }}
          >
            {provider.iconUrl && (
              <img 
                src={provider.iconUrl} 
                alt={provider.displayName}
                width={20}
                height={20}
              />
            )}
            使用 {provider.displayName} 登录
          </Button>
        ))}
      </div>
      
      {providers.length > 0 && (
        <div className="oauth-description">
          点击上方按钮将跳转到第三方登录页面
        </div>
      )}
    </div>
  );
};

export default OAuthButtons;
