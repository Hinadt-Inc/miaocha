// 日志搜索分析器，用于分析用户意图并生成搜索建议

interface IUserIntent {
  type: 'search' | 'filter' | 'analyze' | 'timeRange' | 'export';
  confidence: number;
  parameters: any;
  suggestions: string[];
}

interface IAnalysisResult {
  intent: IUserIntent;
  keywords: string[];
  entities: IExtractedEntity[];
  searchQuery: string;
  filters: ISearchFilter[];
}

interface IExtractedEntity {
  type: 'time' | 'user' | 'module' | 'level' | 'ip' | 'status' | 'field';
  value: string;
  confidence: number;
}

interface ISearchFilter {
  field: string;
  operator: '=' | '!=' | '>' | '<' | '>=' | '<=' | 'contains' | 'not_contains';
  value: any;
}

export class LogSearchAnalyzer {
  private moduleOptions: any[];

  // 预定义的关键词映射
  private keywordMappings = {
    // 时间相关
    time: [
      '时间',
      '最近',
      '今天',
      '昨天',
      '小时',
      '分钟',
      '天',
      '周',
      '月',
      'time',
      'recent',
      'hour',
      'day',
      'week',
      'month',
    ],

    // 日志级别
    level: ['错误', '警告', '信息', '调试', 'error', 'warn', 'info', 'debug', 'trace', 'fatal'],

    // 用户相关
    user: ['用户', '用户ID', '用户名', 'user', 'userid', 'username', 'uid'],

    // 状态码
    status: ['状态码', '状态', '成功', '失败', 'status', 'code', 'success', 'fail', '200', '404', '500'],

    // IP地址
    ip: ['IP', 'ip', '地址', 'address', '来源'],

    // 模块相关
    module: ['模块', '服务', '组件', 'module', 'service', 'component'],

    // 操作类型
    action: ['操作', '请求', '响应', '登录', '注册', 'action', 'request', 'response', 'login', 'register'],

    // 性能相关
    performance: ['性能', '响应时间', '耗时', '慢', '快', 'performance', 'response_time', 'duration', 'slow', 'fast'],
  };

  // 时间表达式模式
  private timePatterns = [
    { pattern: /最近(\d+)小时/g, unit: 'hour' },
    { pattern: /最近(\d+)分钟/g, unit: 'minute' },
    { pattern: /最近(\d+)天/g, unit: 'day' },
    { pattern: /今天/g, value: 'today' },
    { pattern: /昨天/g, value: 'yesterday' },
    { pattern: /本周/g, value: 'this_week' },
    { pattern: /上周/g, value: 'last_week' },
  ];

  constructor(moduleOptions: any[] = []) {
    this.moduleOptions = moduleOptions;
  }

  /**
   * 分析用户意图
   */
  async analyzeUserIntent(message: string): Promise<IAnalysisResult> {
    const normalizedMessage = message.toLowerCase().trim();

    // 提取实体
    const entities = this.extractEntities(normalizedMessage);

    // 分析意图
    const intent = this.determineIntent(normalizedMessage, entities);

    // 提取关键词
    const keywords = this.extractKeywords(normalizedMessage);

    // 生成搜索查询
    const searchQuery = this.generateSearchQuery(normalizedMessage, entities);

    // 生成过滤条件
    const filters = this.generateFilters(entities);

    return {
      intent,
      keywords,
      entities,
      searchQuery,
      filters,
    };
  }

  /**
   * 提取实体信息
   */
  private extractEntities(message: string): IExtractedEntity[] {
    const entities: IExtractedEntity[] = [];

    // 提取时间实体
    this.timePatterns.forEach(({ pattern, unit, value }) => {
      const matches = message.match(pattern);
      if (matches) {
        matches.forEach((match) => {
          entities.push({
            type: 'time',
            value: value || `${match}_${unit}`,
            confidence: 0.9,
          });
        });
      }
    });

    // 提取用户ID
    const userIdPattern = /用户ID?[：:]?\s*(\w+)/gi;
    const userMatches = message.match(userIdPattern);
    if (userMatches) {
      userMatches.forEach((match) => {
        const userId = match.replace(/用户ID?[：:]?\s*/gi, '');
        entities.push({
          type: 'user',
          value: userId,
          confidence: 0.8,
        });
      });
    }

    // 提取IP地址
    const ipPattern = /\b(?:\d{1,3}\.){3}\d{1,3}\b/g;
    const ipMatches = message.match(ipPattern);
    if (ipMatches) {
      ipMatches.forEach((ip) => {
        entities.push({
          type: 'ip',
          value: ip,
          confidence: 0.9,
        });
      });
    }

    // 提取状态码
    const statusPattern = /\b[1-5]\d{2}\b/g;
    const statusMatches = message.match(statusPattern);
    if (statusMatches) {
      statusMatches.forEach((status) => {
        entities.push({
          type: 'status',
          value: status,
          confidence: 0.8,
        });
      });
    }

    // 提取日志级别
    const levelKeywords = ['error', 'warn', 'info', 'debug', '错误', '警告', '信息', '调试'];
    levelKeywords.forEach((keyword) => {
      if (message.includes(keyword)) {
        entities.push({
          type: 'level',
          value: keyword,
          confidence: 0.9,
        });
      }
    });

    // 提取模块名
    this.moduleOptions.forEach((module) => {
      if (message.includes(module.value) || message.includes(module.label)) {
        entities.push({
          type: 'module',
          value: module.value,
          confidence: 0.8,
        });
      }
    });

    return entities;
  }

  /**
   * 确定用户意图
   */
  private determineIntent(message: string, entities: IExtractedEntity[]): IUserIntent {
    const searchKeywords = ['查找', '搜索', '查询', 'search', 'find', 'query'];
    const filterKeywords = ['过滤', '筛选', 'filter', 'where'];
    const analyzeKeywords = ['分析', '统计', '汇总', 'analyze', 'statistics', 'summary'];
    const exportKeywords = ['导出', '下载', 'export', 'download'];

    let type: 'search' | 'filter' | 'analyze' | 'timeRange' | 'export' = 'search';
    let confidence = 0.5;

    if (searchKeywords.some((keyword) => message.includes(keyword))) {
      type = 'search';
      confidence = 0.8;
    } else if (filterKeywords.some((keyword) => message.includes(keyword))) {
      type = 'filter';
      confidence = 0.8;
    } else if (analyzeKeywords.some((keyword) => message.includes(keyword))) {
      type = 'analyze';
      confidence = 0.8;
    } else if (exportKeywords.some((keyword) => message.includes(keyword))) {
      type = 'export';
      confidence = 0.8;
    } else if (entities.some((entity) => entity.type === 'time')) {
      type = 'timeRange';
      confidence = 0.7;
    }

    // 生成建议
    const suggestions = this.generateSuggestions(type, entities);

    return {
      type,
      confidence,
      parameters: this.extractParameters(message, entities),
      suggestions,
    };
  }

  /**
   * 提取关键词
   */
  private extractKeywords(message: string): string[] {
    const keywords: string[] = [];

    Object.entries(this.keywordMappings).forEach(([, words]) => {
      words.forEach((word) => {
        if (message.includes(word)) {
          keywords.push(word);
        }
      });
    });

    return [...new Set(keywords)]; // 去重
  }

  /**
   * 生成搜索查询
   */
  private generateSearchQuery(_message: string, entities: IExtractedEntity[]): string {
    const queryParts: string[] = [];

    entities.forEach((entity) => {
      switch (entity.type) {
        case 'level':
          queryParts.push(`level:${entity.value.toUpperCase()}`);
          break;
        case 'user':
          queryParts.push(`user_id:${entity.value}`);
          break;
        case 'module':
          queryParts.push(`module:${entity.value}`);
          break;
        case 'status':
          queryParts.push(`status:${entity.value}`);
          break;
        case 'ip':
          queryParts.push(`ip:${entity.value}`);
          break;
      }
    });

    return queryParts.join(' AND ');
  }

  /**
   * 生成过滤条件
   */
  private generateFilters(entities: IExtractedEntity[]): ISearchFilter[] {
    const filters: ISearchFilter[] = [];

    entities.forEach((entity) => {
      switch (entity.type) {
        case 'level':
          filters.push({
            field: 'level',
            operator: '=',
            value: entity.value.toUpperCase(),
          });
          break;
        case 'user':
          filters.push({
            field: 'user_id',
            operator: '=',
            value: entity.value,
          });
          break;
        case 'module':
          filters.push({
            field: 'module',
            operator: '=',
            value: entity.value,
          });
          break;
        case 'status':
          filters.push({
            field: 'status',
            operator: '=',
            value: parseInt(entity.value),
          });
          break;
        case 'ip':
          filters.push({
            field: 'ip',
            operator: '=',
            value: entity.value,
          });
          break;
      }
    });

    return filters;
  }

  /**
   * 提取参数
   */
  private extractParameters(_message: string, entities: IExtractedEntity[]): any {
    const parameters: any = {};

    entities.forEach((entity) => {
      if (entity.type === 'time') {
        parameters.timeRange = entity.value;
      }
    });

    return parameters;
  }

  /**
   * 生成建议
   */
  private generateSuggestions(type: string, _entities: IExtractedEntity[]): string[] {
    const suggestions: string[] = [];

    switch (type) {
      case 'search':
        suggestions.push('添加时间范围限制', '指定日志级别', '选择特定模块');
        break;
      case 'filter':
        suggestions.push('按状态码过滤', '按用户ID过滤', '按IP地址过滤');
        break;
      case 'analyze':
        suggestions.push('统计错误分布', '分析访问趋势', '性能指标汇总');
        break;
      case 'export':
        suggestions.push('导出为CSV', '导出为JSON', '导出到文件');
        break;
      default:
        suggestions.push('优化查询条件', '设置时间范围', '添加过滤器');
    }

    return suggestions;
  }
}

export type { IUserIntent, IAnalysisResult, IExtractedEntity, ISearchFilter };
