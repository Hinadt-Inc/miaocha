import { IMessage, IAction } from '../context/AIAssistantContext';

// AI服务响应接口
interface IAIResponse {
  content: string;
  actions?: IAction[];
  searchResults?: {
    total: number;
    data: any[];
  };
  searchParams?: any;
}

// AI服务请求上下文
interface IAIContext {
  conversationHistory: IMessage[];
  currentSearchParams?: any;
  logData?: any;
  moduleOptions?: any[];
  analysis?: any;
}

export class AIService {
  private apiEndpoint = '/api/ai/session'; // 这里需要根据实际后端API调整

  /**
   * 发送消息到AI服务
   */
  async sendMessage(message: string, context: IAIContext): Promise<IAIResponse> {
    try {
      // 构建请求负载
      const payload = {
        message,
        context: {
          conversationHistory: context.conversationHistory.slice(-10), // 只发送最近10条消息
          currentSearchParams: context.currentSearchParams,
          logData: context.logData ? this.sanitizeLogData(context.logData) : null,
          moduleOptions: context.moduleOptions,
          analysis: context.analysis,
        },
        timestamp: Date.now(),
      };

      // 发送请求到后端AI服务
      const response = await fetch(this.apiEndpoint, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${this.getAuthToken()}`,
        },
        body: JSON.stringify(payload),
      });

      if (!response.ok) {
        throw new Error(`AI服务请求失败: ${response.status}`);
      }

      const result = await response.json();
      return this.processAIResponse(result);
    } catch (error) {
      console.error('AI服务调用错误:', error);
      // 如果后端服务不可用，返回模拟响应
      return this.getMockResponse(message, context);
    }
  }

  /**
   * 处理AI响应
   */
  private processAIResponse(response: any): IAIResponse {
    return {
      content: response.content || '抱歉，我无法理解您的请求。',
      actions: response.actions || [],
      searchResults: response.searchResults,
      searchParams: response.searchParams,
    };
  }

  /**
   * 获取模拟响应（用于开发阶段或后端不可用时）
   */
  private getMockResponse(message: string, context: IAIContext): IAIResponse {
    const lowerMessage = message.toLowerCase();

    // 错误日志查询
    if (lowerMessage.includes('错误') || lowerMessage.includes('error') || lowerMessage.includes('异常')) {
      return {
        content:
          '我已经为您准备了错误日志的查询方案：\n\n1. 设置时间范围为最近1小时\n2. 添加日志级别过滤条件：ERROR\n3. 按时间倒序排列\n4. 显示关键字段：时间戳、模块名、错误信息\n\n正在为您执行搜索...',
        actions: [
          {
            type: 'timeRange',
            description: '设置时间范围为最近1小时',
            params: { range: 'last_1_hour' },
          },
          {
            type: 'filter',
            description: '添加错误级别过滤',
            params: { field: 'level', value: 'ERROR' },
          },
          {
            type: 'search',
            description: '执行日志搜索',
            params: { query: 'level:ERROR', sort: 'timestamp desc' },
          },
        ],
        searchResults: {
          total: 156,
          data: [],
        },
      };
    }

    // 性能分析
    if (lowerMessage.includes('性能') || lowerMessage.includes('响应时间') || lowerMessage.includes('慢')) {
      return {
        content:
          '我将为您分析性能相关的日志：\n\n1. 查询响应时间大于2秒的请求\n2. 统计各个接口的平均响应时间\n3. 识别性能瓶颈\n\n开始分析...',
        actions: [
          {
            type: 'filter',
            description: '过滤响应时间大于2秒的请求',
            params: { field: 'response_time', operator: '>', value: 2000 },
          },
          {
            type: 'analyze',
            description: '性能分析',
            params: { type: 'performance', metrics: ['response_time', 'request_count'] },
          },
        ],
      };
    }

    // 用户行为追踪
    if (lowerMessage.includes('用户') && (lowerMessage.includes('追踪') || lowerMessage.includes('行为'))) {
      const userIdMatch = message.match(/用户ID?[：:]?\s*(\w+)/i) || message.match(/用户\s*(\w+)/i);
      const userId = userIdMatch ? userIdMatch[1] : 'unknown';

      return {
        content: `我将为您追踪用户${userId}的行为记录：\n\n1. 查找该用户的所有操作日志\n2. 按时间排序显示操作轨迹\n3. 统计操作频率和模式\n\n正在搜索...`,
        actions: [
          {
            type: 'filter',
            description: `过滤用户${userId}的日志`,
            params: { field: 'user_id', value: userId },
          },
          {
            type: 'search',
            description: '搜索用户行为日志',
            params: { query: `user_id:${userId}`, sort: 'timestamp desc' },
          },
        ],
      };
    }

    // 时间范围查询
    if (lowerMessage.includes('最近') || lowerMessage.includes('今天') || lowerMessage.includes('昨天')) {
      let timeRange = 'last_1_hour';
      let description = '最近1小时';

      if (lowerMessage.includes('今天')) {
        timeRange = 'today';
        description = '今天';
      } else if (lowerMessage.includes('昨天')) {
        timeRange = 'yesterday';
        description = '昨天';
      } else if (lowerMessage.includes('24小时')) {
        timeRange = 'last_24_hours';
        description = '最近24小时';
      }

      return {
        content: `我将为您查询${description}的日志：\n\n1. 设置时间范围为${description}\n2. 按时间倒序排列\n3. 显示主要字段\n\n正在搜索...`,
        actions: [
          {
            type: 'timeRange',
            description: `设置时间范围为${description}`,
            params: { range: timeRange },
          },
          {
            type: 'search',
            description: '执行时间范围搜索',
            params: { sort: 'timestamp desc' },
          },
        ],
      };
    }

    // 默认响应
    return {
      content: `我理解您想要：${message}\n\n我会帮您分析这个需求并生成相应的日志查询方案。请稍等...`,
      actions: [
        {
          type: 'analyze',
          description: '分析查询需求',
          params: { query: message },
        },
      ],
    };
  }

  /**
   * 清理日志数据，避免发送过多数据到AI服务
   */
  private sanitizeLogData(logData: any): any {
    if (!logData) return null;

    return {
      total: logData.total || 0,
      fields: logData.fields?.slice(0, 10) || [], // 只发送前10个字段
      sample: logData.data?.slice(0, 5) || [], // 只发送前5条数据作为样本
    };
  }

  /**
   * 获取认证token
   */
  private getAuthToken(): string {
    // 这里需要根据实际的认证机制获取token
    return localStorage.getItem('accessToken') || '';
  }
}

export type { IAIResponse, IAIContext };
