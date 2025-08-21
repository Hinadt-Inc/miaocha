import { useCallback, useRef, useEffect } from 'react';
import { useAIAssistantContext, IMessage, ISearchHistory } from '../context/AIAssistantContext';
import { AISSEService, ISSEData, parseSSEData } from '../services/AISSEService';
import { MockAISSEService } from '../services/MockAISSEService';
import { SystemIntegration } from '../services/SystemIntegration';

// 开发模式开关：是否使用模拟服务
const USE_MOCK_SERVICE = process.env.NODE_ENV === 'development';

interface ISendMessageOptions {
  context?: {
    currentSearchParams?: any;
    logData?: any;
    moduleOptions?: any[];
    analysis?: any;
  };
}

interface ISystemIntegrationCallbacks {
  onLogSearch?: (params: any) => void;
  onFieldSelect?: (fields: string[]) => void;
  onTimeRangeChange?: (timeRange: any) => void;
  currentSearchParams?: any;
  logData?: any;
  moduleOptions?: any[];
}

export const useAIAssistantSSE = (callbacks?: ISystemIntegrationCallbacks) => {
  const { state, dispatch } = useAIAssistantContext();
  const sseServiceRef = useRef<AISSEService | MockAISSEService | null>(null);
  const currentMessageRef = useRef<string>('');
  const systemIntegrationRef = useRef<SystemIntegration | null>(null);

  // 初始化 SSE 服务和系统集成
  useEffect(() => {
    // 根据环境选择服务实现
    if (USE_MOCK_SERVICE) {
      console.log('useAIAssistantSSE: 使用模拟 AI SSE 服务');
      sseServiceRef.current = new MockAISSEService();
    } else {
      console.log('useAIAssistantSSE: 使用真实 AI SSE 服务');
      sseServiceRef.current = new AISSEService();
    }

    // 如果有回调函数，创建系统集成实例
    if (callbacks) {
      systemIntegrationRef.current = new SystemIntegration(callbacks);
    }

    // 清理函数
    return () => {
      sseServiceRef.current?.destroy();
    };
  }, [callbacks]);

  // 发送消息（使用 SSE）
  const sendMessage = useCallback(
    async (content: string, options?: ISendMessageOptions) => {
      console.log('useAIAssistantSSE: sendMessage 被调用', { content, options });

      if (!sseServiceRef.current) {
        console.error('useAIAssistantSSE: SSE 服务未初始化');
        return;
      }

      const userMessage: IMessage = {
        id: Date.now().toString(),
        role: 'user',
        content,
        timestamp: Date.now(),
      };

      console.log('useAIAssistantSSE: 添加用户消息', userMessage);
      // 添加用户消息
      dispatch({ type: 'ADD_MESSAGE', payload: userMessage });
      dispatch({ type: 'SET_LOADING', payload: true });

      // 重置当前消息内容
      currentMessageRef.current = '';

      try {
        console.log('useAIAssistantSSE: 开始 SSE 会话');
        await sseServiceRef.current.startSession(
          content,
          {
            conversationHistory: state.messages,
            ...options?.context,
          },
          // onData: 处理 SSE 数据
          (data: ISSEData) => {
            console.log('useAIAssistantSSE: 收到 SSE 数据:', data);

            switch (data.type) {
              case 'action':
                handleActionData(data);
                break;
              case 'message':
                handleMessageData(data);
                break;
              case 'error':
                handleErrorData(data);
                break;
              case 'done':
                handleDoneData(data);
                break;
              default:
                console.warn('未知的 SSE 数据类型:', data.type);
            }
          },
          // onError: 处理错误
          (error: Error) => {
            console.error('AI SSE 会话错误:', error);
            dispatch({ type: 'SET_LOADING', payload: false });

            // 添加错误消息
            const errorMessage: IMessage = {
              id: Date.now().toString(),
              role: 'assistant',
              content: `抱歉，AI 服务出现错误：${error.message}`,
              timestamp: Date.now(),
            };
            dispatch({ type: 'ADD_MESSAGE', payload: errorMessage });
          },
          // onComplete: 处理完成
          () => {
            console.log('AI SSE 会话完成');
            dispatch({ type: 'SET_LOADING', payload: false });
          },
        );
      } catch (error) {
        console.error('启动 AI SSE 会话失败:', error);
        dispatch({ type: 'SET_LOADING', payload: false });

        // 添加错误消息
        const errorMessage: IMessage = {
          id: Date.now().toString(),
          role: 'assistant',
          content: '抱歉，无法连接到 AI 服务，请稍后重试。',
          timestamp: Date.now(),
        };
        dispatch({ type: 'ADD_MESSAGE', payload: errorMessage });
      }
    },
    [state.messages, dispatch],
  );

  // 处理 Action 数据
  const handleActionData = useCallback(
    async (data: ISSEData) => {
      const actionData = parseSSEData.action(data);
      if (!actionData) return;

      console.log('收到 Action 数据:', actionData.data);

      // 使用系统集成执行动作
      if (systemIntegrationRef.current) {
        try {
          await systemIntegrationRef.current.executeActions([actionData.data]);
        } catch (error) {
          console.error('执行系统动作失败:', error);
        }
      }

      // 如果是搜索动作，更新搜索历史
      if (actionData.data.type === 'search') {
        const searchHistory: ISearchHistory = {
          id: Date.now().toString(),
          query: actionData.data.params.query || '',
          timestamp: new Date().toLocaleString(),
          results: actionData.data.params.results || 0,
          params: actionData.data.params,
        };
        dispatch({ type: 'ADD_SEARCH_HISTORY', payload: searchHistory });
      }
    },
    [dispatch],
  );

  // 处理 Message 数据
  const handleMessageData = useCallback(
    (data: ISSEData) => {
      const messageData = parseSSEData.message(data);
      if (!messageData) return;

      // 累积消息内容
      currentMessageRef.current += messageData.data.content;

      // 如果消息完整，添加到消息列表
      if (messageData.data.isComplete) {
        const assistantMessage: IMessage = {
          id: messageData.data.messageId,
          role: 'assistant',
          content: currentMessageRef.current,
          timestamp: Date.now(),
        };

        dispatch({ type: 'ADD_MESSAGE', payload: assistantMessage });
        currentMessageRef.current = '';
      } else {
        // 如果消息未完整，更新最后一条消息（实现打字机效果）
        dispatch({
          type: 'UPDATE_LAST_MESSAGE',
          payload: {
            content: currentMessageRef.current,
            id: messageData.data.messageId,
          },
        });
      }
    },
    [dispatch],
  );

  // 处理 Error 数据
  const handleErrorData = useCallback(
    (data: ISSEData) => {
      const errorData = parseSSEData.error(data);
      if (!errorData) return;

      console.error('收到 Error 数据:', errorData.data);

      const errorMessage: IMessage = {
        id: Date.now().toString(),
        role: 'assistant',
        content: `错误：${errorData.data.error}`,
        timestamp: Date.now(),
      };

      dispatch({ type: 'ADD_MESSAGE', payload: errorMessage });
      dispatch({ type: 'SET_LOADING', payload: false });
    },
    [dispatch],
  );

  // 处理 Done 数据
  const handleDoneData = useCallback(
    (data: ISSEData) => {
      const doneData = parseSSEData.done(data);
      if (!doneData) return;

      console.log('收到 Done 数据:', doneData.data);
      dispatch({ type: 'SET_LOADING', payload: false });
    },
    [dispatch],
  );

  // 清空消息
  const clearMessages = useCallback(() => {
    dispatch({ type: 'CLEAR_MESSAGES' });
  }, [dispatch]);

  // 关闭 SSE 连接
  const closeConnection = useCallback(() => {
    sseServiceRef.current?.closeConnection();
  }, []);

  // 检查连接状态
  const isConnected = useCallback(() => {
    return sseServiceRef.current?.isConnected() || false;
  }, []);

  return {
    messages: state.messages,
    loading: state.loading,
    suggestions: state.suggestions,
    searchHistory: state.searchHistory,
    sendMessage,
    clearMessages,
    closeConnection,
    isConnected,
  };
};

export default useAIAssistantSSE;
