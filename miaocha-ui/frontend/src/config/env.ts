// 环境配置文件
export const ENV_CONFIG = {
  // API基础URL
  API_BASE_URL: import.meta.env.VITE_API_BASE_URL || '',
  
  // 应用基础URL 
  APP_BASE_URL: import.meta.env.VITE_APP_BASE_URL || window.location.origin,
  
  // 是否为生产环境
  IS_PRODUCTION: import.meta.env.PROD,
  
  // 是否为开发环境
  IS_DEVELOPMENT: import.meta.env.DEV,
};

// 获取完整的API URL
export const getApiUrl = (path: string) => {
  if (ENV_CONFIG.API_BASE_URL) {
    return `${ENV_CONFIG.API_BASE_URL}${path}`;
  }
  return path; // 开发环境使用代理
};

// 获取OAuth回调URL
export const getOAuthRedirectUri = () => {
  return `${ENV_CONFIG.APP_BASE_URL}/login`;
};

// 获取第三方登出回调URL
export const getOAuthLogoutCallbackUrl = () => {
  return `${ENV_CONFIG.APP_BASE_URL}/login`;
};
