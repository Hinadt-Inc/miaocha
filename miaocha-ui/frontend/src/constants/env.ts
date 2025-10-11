// 环境配置文件
export const ENV_CONFIG = {
  // 应用基础URL
  APP_BASE_URL: window.location.origin,
};

// 获取OAuth回调URL
export const getOAuthRedirectUri = () => {
  return `${ENV_CONFIG.APP_BASE_URL}/login`;
};

// 获取第三方登出回调URL
export const getOAuthLogoutCallbackUrl = () => {
  return `${ENV_CONFIG.APP_BASE_URL}/login`;
};
