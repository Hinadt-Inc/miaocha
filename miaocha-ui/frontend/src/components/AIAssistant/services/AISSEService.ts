import { IMessage, IAction } from '../context/AIAssistantContext';

// SSE 响应数据类型
export interface ISSEData {
  type: 'action' | 'message' | 'error' | 'done';
  data: any;
  timestamp: number;
}

// Action 类型的 SSE 数据
export interface ISSEActionData extends ISSEData {
  type: 'action';
  data: IAction;
}

// Message 类型的 SSE 数据
export interface ISSEMessageData extends ISSEData {
  type: 'message';
  data: {
    content: string;
    isComplete: boolean;
    messageId: string;
  };
}

// 错误类型的 SSE 数据
export interface ISSEErrorData extends ISSEData {
  type: 'error';
  data: {
    error: string;
    code?: string;
  };
}

// 完成类型的 SSE 数据
export interface ISSEDoneData extends ISSEData {
  type: 'done';
  data: {
    messageId: string;
    totalTokens?: number;
  };
}

// AI SSE 服务请求上下文
interface IAISSEContext {
  conversationHistory: IMessage[];
  currentSearchParams?: any;
  logData?: any;
  moduleOptions?: any[];
  analysis?: any;
}

// SSE 事件监听器类型
type SSEEventListener = (data: ISSEData) => void;

export class AISSEService {
  private apiEndpoint = '/api/ai/session';
  private eventSource: EventSource | null = null;
  private listeners = new Map<string, SSEEventListener[]>();

  /**
   * 开始 AI 会话流
   */
  async startSession(
    message: string,
    context: IAISSEContext,
    onData: (data: ISSEData) => void,
    onError?: (error: Error) => void,
    onComplete?: () => void,
  ): Promise<void> {
    try {
      console.log('AISSEService: 开始会话', { message, context });

      // 构建查询参数
      const params = new URLSearchParams({
        message: encodeURIComponent(message),
        context: encodeURIComponent(
          JSON.stringify({
            conversationHistory: context.conversationHistory.slice(-10),
            currentSearchParams: context.currentSearchParams,
            logData: context.logData ? this.sanitizeLogData(context.logData) : null,
            moduleOptions: context.moduleOptions,
            analysis: context.analysis,
          }),
        ),
        timestamp: Date.now().toString(),
      });

      // 构建 SSE URL
      const sseUrl = `${this.apiEndpoint}?${params.toString()}`;
      console.log('AISSEService: SSE URL:', sseUrl);

      // 创建 EventSource
      this.eventSource = new EventSource(sseUrl);
      console.log('AISSEService: EventSource 已创建');

      // 设置事件监听器
      this.eventSource.onopen = () => {
        console.log('AI SSE 连接已建立');
      };

      this.eventSource.onmessage = (event) => {
        console.log('AISSEService: 收到消息', event.data);
        try {
          const data: ISSEData = JSON.parse(event.data);
          onData(data);

          // 如果是完成事件，关闭连接
          if (data.type === 'done') {
            this.closeConnection();
            onComplete?.();
          }
        } catch (error) {
          console.error('解析 SSE 数据失败:', error);
          onError?.(new Error('解析 SSE 数据失败'));
        }
      };

      this.eventSource.onerror = (event) => {
        console.error('AI SSE 连接错误:', event);
        onError?.(new Error('SSE 连接错误'));
        this.closeConnection();
      };
    } catch (error) {
      console.error('启动 AI SSE 会话失败:', error);
      onError?.(error as Error);
    }
  }

  /**
   * 关闭 SSE 连接
   */
  closeConnection(): void {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }
  }

  /**
   * 检查连接状态
   */
  isConnected(): boolean {
    return this.eventSource?.readyState === EventSource.OPEN;
  }

  /**
   * 添加事件监听器
   */
  addEventListener(type: string, listener: SSEEventListener): void {
    if (!this.listeners.has(type)) {
      this.listeners.set(type, []);
    }
    this.listeners.get(type)!.push(listener);
  }

  /**
   * 移除事件监听器
   */
  removeEventListener(type: string, listener: SSEEventListener): void {
    const listeners = this.listeners.get(type);
    if (listeners) {
      const index = listeners.indexOf(listener);
      if (index > -1) {
        listeners.splice(index, 1);
      }
    }
  }

  /**
   * 触发事件
   */
  private emit(type: string, data: ISSEData): void {
    const listeners = this.listeners.get(type);
    if (listeners) {
      listeners.forEach((listener) => listener(data));
    }
  }

  /**
   * 清理日志数据，避免发送过大的数据
   */
  private sanitizeLogData(logData: any): any {
    if (!logData) return null;

    // 如果是数组，只取前100条
    if (Array.isArray(logData)) {
      return logData.slice(0, 100).map((item) => ({
        timestamp: item.timestamp,
        level: item.level,
        message: item.message?.substring(0, 500), // 限制消息长度
        module: item.module,
      }));
    }

    // 如果是对象，提取关键信息
    return {
      total: logData.total,
      fields: logData.fields,
      sampleData: Array.isArray(logData.data) ? logData.data.slice(0, 10) : null,
    };
  }

  /**
   * 获取认证 token
   */
  private getAuthToken(): string {
    return localStorage.getItem('auth_token') || '';
  }

  /**
   * 析构函数，确保连接被关闭
   */
  destroy(): void {
    this.closeConnection();
    this.listeners.clear();
  }
}

// 工具函数：解析不同类型的 SSE 数据
export const parseSSEData = {
  /**
   * 解析 Action 数据
   */
  action: (data: ISSEData): ISSEActionData | null => {
    if (data.type === 'action') {
      return data as ISSEActionData;
    }
    return null;
  },

  /**
   * 解析 Message 数据
   */
  message: (data: ISSEData): ISSEMessageData | null => {
    if (data.type === 'message') {
      return data as ISSEMessageData;
    }
    return null;
  },

  /**
   * 解析 Error 数据
   */
  error: (data: ISSEData): ISSEErrorData | null => {
    if (data.type === 'error') {
      return data as ISSEErrorData;
    }
    return null;
  },

  /**
   * 解析 Done 数据
   */
  done: (data: ISSEData): ISSEDoneData | null => {
    if (data.type === 'done') {
      return data as ISSEDoneData;
    }
    return null;
  },
};

export default AISSEService;
