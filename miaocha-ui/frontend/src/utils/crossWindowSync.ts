/**
 * 跨窗口同步工具
 * 使用 BroadcastChannel 或 localStorage 事件实现多窗口通信
 */

// 事件类型
export enum CrossWindowEventType {
  LOGOUT = 'LOGOUT', // 退出登录
  LOGIN = 'LOGIN', // 登录成功
}

// 事件数据接口
export interface CrossWindowEvent {
  type: CrossWindowEventType;
  timestamp: number;
  data?: any;
}

// 频道名称
const CHANNEL_NAME = 'miaocha_auth_sync';

// 使用 BroadcastChannel（现代浏览器）或 localStorage（兼容方案）
class CrossWindowSyncManager {
  private broadcastChannel: BroadcastChannel | null = null;
  private storageEventHandler: ((event: StorageEvent) => void) | null = null;
  private listeners = new Map<CrossWindowEventType, Set<(data?: any) => void>>();

  constructor() {
    this.init();
  }

  /**
   * 初始化通信通道
   */
  private init() {
    // 优先使用 BroadcastChannel（性能更好，不依赖 localStorage）
    if (typeof BroadcastChannel !== 'undefined') {
      try {
        this.broadcastChannel = new BroadcastChannel(CHANNEL_NAME);
        this.broadcastChannel.onmessage = (event) => {
          this.handleMessage(event.data);
        };
        console.log('[CrossWindowSync] 使用 BroadcastChannel');
      } catch (error) {
        console.warn('[CrossWindowSync] BroadcastChannel 初始化失败，降级使用 localStorage:', error);
        this.initStorageFallback();
      }
    } else {
      // 降级使用 localStorage 事件
      this.initStorageFallback();
    }
  }

  /**
   * 使用 localStorage 作为降级方案
   */
  private initStorageFallback() {
    this.storageEventHandler = (event: StorageEvent) => {
      if (event.key === CHANNEL_NAME && event.newValue) {
        try {
          const message: CrossWindowEvent = JSON.parse(event.newValue);
          this.handleMessage(message);
        } catch (error) {
          console.error('[CrossWindowSync] 解析 localStorage 消息失败:', error);
        }
      }
    };
    window.addEventListener('storage', this.storageEventHandler);
    console.log('[CrossWindowSync] 使用 localStorage 事件');
  }

  /**
   * 处理接收到的消息
   */
  private handleMessage(message: CrossWindowEvent) {
    const { type, data, timestamp } = message;

    // 防止处理过期的消息（超过5秒）
    if (Date.now() - timestamp > 5000) {
      return;
    }

    console.log('[CrossWindowSync] 接收到消息:', { type, data, timestamp });

    // 触发对应的监听器
    const callbacks = this.listeners.get(type);
    if (callbacks) {
      callbacks.forEach((callback) => {
        try {
          callback(data);
        } catch (error) {
          console.error('[CrossWindowSync] 执行回调失败:', error);
        }
      });
    }
  }

  /**
   * 广播消息到其他窗口
   */
  broadcast(type: CrossWindowEventType, data?: any) {
    const message: CrossWindowEvent = {
      type,
      timestamp: Date.now(),
      data,
    };

    console.log('[CrossWindowSync] 广播消息:', message);

    if (this.broadcastChannel) {
      // 使用 BroadcastChannel
      this.broadcastChannel.postMessage(message);
    } else {
      // 使用 localStorage 事件（需要先写入再删除，触发 storage 事件）
      try {
        const key = CHANNEL_NAME;
        localStorage.setItem(key, JSON.stringify(message));
        // 立即删除，避免污染 localStorage
        setTimeout(() => {
          localStorage.removeItem(key);
        }, 100);
      } catch (error) {
        console.error('[CrossWindowSync] localStorage 写入失败:', error);
      }
    }
  }

  /**
   * 监听特定类型的事件
   */
  on(type: CrossWindowEventType, callback: (data?: any) => void) {
    if (!this.listeners.has(type)) {
      this.listeners.set(type, new Set());
    }
    const callbacks = this.listeners.get(type);
    if (callbacks) {
      callbacks.add(callback);
    }
  }

  /**
   * 移除监听器
   */
  off(type: CrossWindowEventType, callback: (data?: any) => void) {
    const callbacks = this.listeners.get(type);
    if (callbacks) {
      callbacks.delete(callback);
    }
  }

  /**
   * 销毁管理器
   */
  destroy() {
    if (this.broadcastChannel) {
      this.broadcastChannel.close();
      this.broadcastChannel = null;
    }

    if (this.storageEventHandler) {
      window.removeEventListener('storage', this.storageEventHandler);
      this.storageEventHandler = null;
    }

    this.listeners.clear();
  }
}

// 单例实例
export const crossWindowSync = new CrossWindowSyncManager();

/**
 * 通知所有窗口执行退出登录
 */
export const broadcastLogout = () => {
  crossWindowSync.broadcast(CrossWindowEventType.LOGOUT);
};

/**
 * 通知所有窗口登录成功
 */
export const broadcastLogin = () => {
  crossWindowSync.broadcast(CrossWindowEventType.LOGIN);
};

/**
 * 监听退出登录事件
 */
export const onLogout = (callback: () => void) => {
  crossWindowSync.on(CrossWindowEventType.LOGOUT, callback);
};

/**
 * 监听登录成功事件
 */
export const onLogin = (callback: () => void) => {
  crossWindowSync.on(CrossWindowEventType.LOGIN, callback);
};

/**
 * 移除退出登录监听
 */
export const offLogout = (callback: () => void) => {
  crossWindowSync.off(CrossWindowEventType.LOGOUT, callback);
};

/**
 * 移除登录成功监听
 */
export const offLogin = (callback: () => void) => {
  crossWindowSync.off(CrossWindowEventType.LOGIN, callback);
};
