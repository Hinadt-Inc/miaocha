/**
 * 安全的存储工具类，处理跨域和存储失败的情况
 */

const STORAGE_KEYS = {
  RETURN_URL: 'oauth_return_url',
  OAUTH_PROVIDER: 'oauth_provider',
  LOGIN_TYPE: 'login_type',
} as const;

/**
 * 安全地设置sessionStorage值
 */
export const setSecureStorage = (key: string, value: string): boolean => {
  try {
    // 检查sessionStorage是否可用
    if (typeof Storage !== 'undefined' && sessionStorage) {
      sessionStorage.setItem(key, value);
      return true;
    }
  } catch (error) {
    console.warn('SessionStorage不可用，使用内存存储:', error);
    // 使用内存存储作为备选方案
    window._tempStorage = window._tempStorage || {};
    window._tempStorage[key] = value;
    return true;
  }
  return false;
};

/**
 * 安全地获取sessionStorage值
 */
export const getSecureStorage = (key: string): string | null => {
  try {
    if (typeof Storage !== 'undefined' && sessionStorage) {
      return sessionStorage.getItem(key);
    }
  } catch (error) {
    console.warn('SessionStorage不可用，尝试从内存存储获取:', error);
    // 从内存存储获取
    const tempStorage = window._tempStorage;
    return tempStorage?.[key] || null;
  }
  return null;
};

/**
 * 安全地移除sessionStorage值
 */
export const removeSecureStorage = (key: string): boolean => {
  try {
    if (typeof Storage !== 'undefined' && sessionStorage) {
      sessionStorage.removeItem(key);
      return true;
    }
  } catch (error) {
    console.warn('SessionStorage不可用，从内存存储移除:', error);
    // 从内存存储移除
    const tempStorage = window._tempStorage;
    if (tempStorage && tempStorage[key]) {
      delete tempStorage[key];
    }
    return true;
  }
  return false;
};

/**
 * OAuth流程安全存储助手
 * 处理OAuth过程中的状态存储，支持跨域场景
 */
export const OAuthStorageHelper = {
  // Return URL管理
  setReturnUrl: (url: string): boolean => {
    return setSecureStorage(STORAGE_KEYS.RETURN_URL, url);
  },

  getReturnUrl: (): string | null => {
    return getSecureStorage(STORAGE_KEYS.RETURN_URL);
  },

  removeReturnUrl: (): boolean => {
    return removeSecureStorage(STORAGE_KEYS.RETURN_URL);
  },

  // Provider ID管理
  setProvider: (providerId: string): boolean => {
    return setSecureStorage(STORAGE_KEYS.OAUTH_PROVIDER, providerId);
  },

  getProvider: (): string | null => {
    return getSecureStorage(STORAGE_KEYS.OAUTH_PROVIDER);
  },

  removeProvider: (): boolean => {
    return removeSecureStorage(STORAGE_KEYS.OAUTH_PROVIDER);
  },

  // 清理所有OAuth相关存储
  clearAll: (): void => {
    removeSecureStorage(STORAGE_KEYS.RETURN_URL);
    removeSecureStorage(STORAGE_KEYS.OAUTH_PROVIDER);
  }
};

// 类型声明
declare global {
  interface Window {
    _tempStorage?: Record<string, string>;
  }
}
