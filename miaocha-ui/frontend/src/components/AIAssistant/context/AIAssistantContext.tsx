import React, { createContext, useContext, useReducer, ReactNode } from 'react';

// 定义状态类型
interface IMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: number;
  actions?: IAction[];
}

interface IAction {
  type: 'search' | 'filter' | 'timeRange' | 'fields' | 'analyze';
  description: string;
  params: any;
}

interface ISuggestion {
  id: string;
  title: string;
  description: string;
  query: string;
  category: 'search' | 'analysis' | 'filter';
}

interface ISearchHistory {
  id: string;
  query: string;
  timestamp: string;
  results: number;
  params: any;
}

interface IAIAssistantState {
  messages: IMessage[];
  loading: boolean;
  suggestions: ISuggestion[];
  searchHistory: ISearchHistory[];
  currentConversationId: string;
}

// 定义行为类型
type AIAssistantAction =
  | { type: 'ADD_MESSAGE'; payload: IMessage }
  | { type: 'SET_LOADING'; payload: boolean }
  | { type: 'SET_SUGGESTIONS'; payload: ISuggestion[] }
  | { type: 'ADD_SEARCH_HISTORY'; payload: ISearchHistory }
  | { type: 'CLEAR_MESSAGES' }
  | { type: 'UPDATE_LAST_MESSAGE'; payload: Partial<IMessage> };

// 初始状态
const initialState: IAIAssistantState = {
  messages: [],
  loading: false,
  suggestions: [
    {
      id: '1',
      title: '查找错误日志',
      description: '快速查找最近的错误和异常日志',
      query: '查找最近1小时内的错误日志',
      category: 'search',
    },
    {
      id: '2',
      title: '性能分析',
      description: '分析响应时间和性能指标',
      query: '分析最近24小时的性能指标',
      category: 'analysis',
    },
    {
      id: '3',
      title: '用户行为追踪',
      description: '追踪特定用户的操作记录',
      query: '查找用户ID为12345的所有操作记录',
      category: 'filter',
    },
    {
      id: '4',
      title: '异常模式识别',
      description: '识别异常访问模式和可疑活动',
      query: '识别异常的访问模式',
      category: 'analysis',
    },
  ],
  searchHistory: [],
  currentConversationId: '',
};

// Reducer
const aiAssistantReducer = (state: IAIAssistantState, action: AIAssistantAction): IAIAssistantState => {
  switch (action.type) {
    case 'ADD_MESSAGE':
      return {
        ...state,
        messages: [...state.messages, action.payload],
      };

    case 'SET_LOADING':
      return {
        ...state,
        loading: action.payload,
      };

    case 'SET_SUGGESTIONS':
      return {
        ...state,
        suggestions: action.payload,
      };

    case 'ADD_SEARCH_HISTORY':
      return {
        ...state,
        searchHistory: [action.payload, ...state.searchHistory.slice(0, 9)], // 保持最新的10条记录
      };

    case 'CLEAR_MESSAGES':
      return {
        ...state,
        messages: [],
      };

    case 'UPDATE_LAST_MESSAGE':
      return {
        ...state,
        messages: state.messages.map((msg, index) =>
          index === state.messages.length - 1 ? { ...msg, ...action.payload } : msg,
        ),
      };

    default:
      return state;
  }
};

// Context类型
interface IAIAssistantContext {
  state: IAIAssistantState;
  dispatch: React.Dispatch<AIAssistantAction>;
}

// 创建Context
const AIAssistantContext = createContext<IAIAssistantContext | undefined>(undefined);

// Provider组件
interface IAIAssistantProviderProps {
  children: ReactNode;
}

export const AIAssistantProvider: React.FC<IAIAssistantProviderProps> = ({ children }) => {
  const [state, dispatch] = useReducer(aiAssistantReducer, initialState);

  return <AIAssistantContext.Provider value={{ state, dispatch }}>{children}</AIAssistantContext.Provider>;
};

// 自定义Hook
export const useAIAssistantContext = () => {
  const context = useContext(AIAssistantContext);
  if (context === undefined) {
    throw new Error('useAIAssistantContext must be used within an AIAssistantProvider');
  }
  return context;
};

// 导出类型
export type { IMessage, IAction, ISuggestion, ISearchHistory, IAIAssistantState };
