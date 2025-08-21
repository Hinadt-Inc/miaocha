import { ISSEData } from './AISSEService';

/**
 * 模拟 AI SSE 服务，用于测试和开发
 */
export class MockAISSEService {
  private isActive = false;

  private readonly mockData: ISSEData[] = [
    {
      type: 'message',
      data: {
        content: '我正在分析您的请求',
        isComplete: false,
        messageId: 'mock_msg_1',
      },
      timestamp: Date.now(),
    },
    {
      type: 'message',
      data: {
        content: '，请稍等...',
        isComplete: false,
        messageId: 'mock_msg_1',
      },
      timestamp: Date.now() + 500,
    },
    {
      type: 'action',
      data: {
        type: 'search',
        description: '执行日志搜索',
        params: {
          query: '您的查询内容',
          results: 42,
        },
      },
      timestamp: Date.now() + 1000,
    },
    {
      type: 'message',
      data: {
        content: '已为您找到相关日志信息，共42条记录。',
        isComplete: true,
        messageId: 'mock_msg_1',
      },
      timestamp: Date.now() + 1500,
    },
    {
      type: 'done',
      data: {
        messageId: 'mock_msg_1',
        totalTokens: 25,
      },
      timestamp: Date.now() + 2000,
    },
  ];

  async startSession(
    message: string,
    context: any,
    onData: (data: ISSEData) => void,
    onError?: (error: Error) => void,
    onComplete?: () => void,
  ): Promise<void> {
    console.log('MockAISSEService: 模拟 SSE 会话开始', { message, context });

    this.isActive = true;

    try {
      // 模拟网络延迟
      await new Promise((resolve) => setTimeout(resolve, 200));

      // 依次发送模拟数据
      for (const data of this.mockData) {
        if (!this.isActive) break; // 检查是否已停止

        // 模拟流式响应的延迟
        await new Promise((resolve) => setTimeout(resolve, 800));

        if (!this.isActive) break; // 再次检查是否已停止

        console.log('MockAISSEService: 发送模拟数据', data);
        onData(data);

        // 如果是完成事件，调用完成回调
        if (data.type === 'done') {
          this.isActive = false;
          onComplete?.();
          break;
        }
      }
    } catch (error) {
      console.error('MockAISSEService: 模拟会话错误', error);
      this.isActive = false;
      onError?.(error as Error);
    }
  }

  closeConnection(): void {
    console.log('MockAISSEService: 关闭模拟连接');
    this.isActive = false;
  }

  isConnected(): boolean {
    return this.isActive;
  }

  destroy(): void {
    console.log('MockAISSEService: 销毁模拟服务');
    this.isActive = false;
  }
}
