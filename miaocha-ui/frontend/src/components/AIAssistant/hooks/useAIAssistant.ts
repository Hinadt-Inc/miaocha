import { useCallback } from 'react';

import { useAIAssistantContext, IMessage, ISearchHistory } from '../context/AIAssistantContext';
import { AIService } from '../services/AIService';

interface ISendMessageOptions {
  context?: {
    currentSearchParams?: any;
    logData?: any;
    moduleOptions?: any[];
    analysis?: any;
  };
}

export const useAIAssistant = () => {
  const { state, dispatch } = useAIAssistantContext();
  const aiService = new AIService();

  // 发送消息
  const sendMessage = useCallback(
    async (content: string, options?: ISendMessageOptions) => {
      const userMessage: IMessage = {
        id: Date.now().toString(),
        role: 'user',
        content,
        timestamp: Date.now(),
      };

      // 添加用户消息
      dispatch({ type: 'ADD_MESSAGE', payload: userMessage });
      dispatch({ type: 'SET_LOADING', payload: true });

      try {
        // 调用AI服务获取响应
        const response = await aiService.sendMessage(content, {
          conversationHistory: state.messages,
          ...options?.context,
        });

        const assistantMessage: IMessage = {
          id: (Date.now() + 1).toString(),
          role: 'assistant',
          content: response.content,
          timestamp: Date.now(),
          actions: response.actions,
        };

        // 添加AI响应
        dispatch({ type: 'ADD_MESSAGE', payload: assistantMessage });

        // 如果有搜索操作，添加到历史记录
        if (response.actions?.some((action: any) => action.type === 'search')) {
          const historyItem: ISearchHistory = {
            id: Date.now().toString(),
            query: content,
            timestamp: new Date().toLocaleString(),
            results: response.searchResults?.total || 0,
            params: response.searchParams,
          };
          dispatch({ type: 'ADD_SEARCH_HISTORY', payload: historyItem });
        }

        return response;
      } catch (error) {
        console.error('AI服务调用失败:', error);

        const errorMessage: IMessage = {
          id: (Date.now() + 1).toString(),
          role: 'assistant',
          content: '抱歉，我遇到了一些问题。请稍后再试或重新描述您的需求。',
          timestamp: Date.now(),
        };

        dispatch({ type: 'ADD_MESSAGE', payload: errorMessage });
        throw error;
      } finally {
        dispatch({ type: 'SET_LOADING', payload: false });
      }
    },
    [state.messages, dispatch, aiService],
  );

  // 清空消息
  const clearMessages = useCallback(() => {
    dispatch({ type: 'CLEAR_MESSAGES' });
  }, [dispatch]);

  // 重新生成最后一条回复
  const regenerateLastResponse = useCallback(async () => {
    if (state.messages.length < 2) return;

    const lastUserMessage = [...state.messages].reverse().find((msg) => msg.role === 'user');
    if (!lastUserMessage) return;

    // 移除最后一条AI消息
    const newMessages = state.messages.slice(0, -1);
    dispatch({ type: 'CLEAR_MESSAGES' });
    newMessages.forEach((msg) => {
      dispatch({ type: 'ADD_MESSAGE', payload: msg });
    });

    // 重新发送最后一条用户消息
    await sendMessage(lastUserMessage.content);
  }, [state.messages, dispatch, sendMessage]);

  // 更新建议
  const updateSuggestions = useCallback(
    (suggestions: any[]) => {
      dispatch({ type: 'SET_SUGGESTIONS', payload: suggestions });
    },
    [dispatch],
  );

  return {
    messages: state.messages,
    loading: state.loading,
    suggestions: state.suggestions,
    searchHistory: state.searchHistory,
    sendMessage,
    clearMessages,
    regenerateLastResponse,
    updateSuggestions,
  };
};
