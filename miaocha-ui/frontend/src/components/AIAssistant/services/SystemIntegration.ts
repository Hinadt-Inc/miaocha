import { IAction } from '../context/AIAssistantContext';

// 系统集成服务，用于执行AI助手生成的操作指令

interface ISystemIntegrationConfig {
  onLogSearch?: (params: any) => void;
  onFieldSelect?: (fields: string[]) => void;
  onTimeRangeChange?: (timeRange: any) => void;
  currentSearchParams?: any;
  logData?: any;
  moduleOptions?: any[];
}

export class SystemIntegration {
  private config: ISystemIntegrationConfig;

  constructor(config: ISystemIntegrationConfig) {
    this.config = config;
  }

  /**
   * 执行AI返回的操作指令
   */
  async executeActions(actions: IAction[]): Promise<void> {
    for (const action of actions) {
      try {
        await this.executeAction(action);
        // 添加延迟，让用户能看到操作过程
        await this.delay(500);
      } catch (error) {
        console.error(`执行操作失败: ${action.type}`, error);
      }
    }
  }

  /**
   * 执行单个操作
   */
  private async executeAction(action: IAction): Promise<void> {
    switch (action.type) {
      case 'search':
        await this.executeSearch(action.params);
        break;
      case 'filter':
        await this.executeFilter(action.params);
        break;
      case 'timeRange':
        await this.executeTimeRange(action.params);
        break;
      case 'fields':
        await this.executeFieldSelect(action.params);
        break;
      case 'analyze':
        await this.executeAnalyze(action.params);
        break;
      default:
        console.warn(`未知操作类型: ${action.type}`);
    }
  }

  /**
   * 执行搜索操作
   */
  private async executeSearch(params: any): Promise<void> {
    if (!this.config.onLogSearch) return;

    const searchParams = {
      ...this.config.currentSearchParams,
      ...params,
    };

    // 模拟UI操作，显示搜索过程
    this.simulateUIOperation('正在执行日志搜索...');

    this.config.onLogSearch(searchParams);
  }

  /**
   * 执行过滤操作
   */
  private async executeFilter(params: any): Promise<void> {
    if (!this.config.onLogSearch) return;

    // 构建过滤条件
    const filterCondition = this.buildFilterCondition(params);

    const searchParams = {
      ...this.config.currentSearchParams,
      filters: [...(this.config.currentSearchParams?.filters || []), filterCondition],
    };

    this.simulateUIOperation(`正在添加过滤条件: ${params.field} ${params.operator || '='} ${params.value}`);

    this.config.onLogSearch(searchParams);
  }

  /**
   * 执行时间范围设置
   */
  private async executeTimeRange(params: any): Promise<void> {
    if (!this.config.onTimeRangeChange) return;

    const timeRange = this.convertTimeRange(params.range);

    this.simulateUIOperation(`正在设置时间范围: ${params.range}`);

    this.config.onTimeRangeChange(timeRange);
  }

  /**
   * 执行字段选择
   */
  private async executeFieldSelect(params: any): Promise<void> {
    if (!this.config.onFieldSelect) return;

    const fields = params.fields || [];

    this.simulateUIOperation(`正在选择显示字段: ${fields.join(', ')}`);

    this.config.onFieldSelect(fields);
  }

  /**
   * 执行分析操作
   */
  private async executeAnalyze(params: any): Promise<void> {
    // 分析操作通常需要特殊处理，这里可以触发分析界面的显示
    this.simulateUIOperation(`正在执行${params.type}分析...`);

    // 可以在这里调用专门的分析功能
    if (this.config.onLogSearch) {
      const analyzeParams = {
        ...this.config.currentSearchParams,
        analyze: true,
        analyzeType: params.type,
        metrics: params.metrics,
      };

      this.config.onLogSearch(analyzeParams);
    }
  }

  /**
   * 构建过滤条件
   */
  private buildFilterCondition(params: any): any {
    return {
      field: params.field,
      operator: params.operator || '=',
      value: params.value,
      label: `${params.field} ${params.operator || '='} ${params.value}`,
    };
  }

  /**
   * 转换时间范围
   */
  private convertTimeRange(range: string): any {
    const timeRangeMap: Record<string, any> = {
      last_1_hour: {
        type: 'relative',
        value: 1,
        unit: 'hour',
      },
      last_24_hours: {
        type: 'relative',
        value: 24,
        unit: 'hour',
      },
      today: {
        type: 'absolute',
        start: new Date().setHours(0, 0, 0, 0),
        end: new Date().setHours(23, 59, 59, 999),
      },
      yesterday: {
        type: 'absolute',
        start: new Date(Date.now() - 24 * 60 * 60 * 1000).setHours(0, 0, 0, 0),
        end: new Date(Date.now() - 24 * 60 * 60 * 1000).setHours(23, 59, 59, 999),
      },
      this_week: {
        type: 'relative',
        value: 7,
        unit: 'day',
      },
      last_week: {
        type: 'relative',
        value: 14,
        unit: 'day',
      },
    };

    return (
      timeRangeMap[range] || {
        type: 'relative',
        value: 1,
        unit: 'hour',
      }
    );
  }

  /**
   * 模拟UI操作过程
   */
  private simulateUIOperation(message: string): void {
    // 这里可以添加UI反馈，比如显示Toast消息
    console.log(`[AI助手] ${message}`);

    // 可以通过事件系统通知UI组件显示操作过程
    window.dispatchEvent(
      new CustomEvent('ai-assistant-operation', {
        detail: { message },
      }),
    );
  }

  /**
   * 延迟函数
   */
  private delay(ms: number): Promise<void> {
    return new Promise((resolve) => setTimeout(resolve, ms));
  }

  /**
   * 获取当前系统状态
   */
  getCurrentState(): any {
    return {
      searchParams: this.config.currentSearchParams,
      logData: this.config.logData
        ? {
            total: this.config.logData.total,
            hasData: this.config.logData.data && this.config.logData.data.length > 0,
          }
        : null,
      moduleOptions: this.config.moduleOptions?.map((option) => ({
        label: option.label,
        value: option.value,
      })),
    };
  }

  /**
   * 验证操作是否可执行
   */
  canExecuteAction(action: IAction): boolean {
    switch (action.type) {
      case 'search':
        return !!this.config.onLogSearch;
      case 'filter':
        return !!this.config.onLogSearch;
      case 'timeRange':
        return !!this.config.onTimeRangeChange;
      case 'fields':
        return !!this.config.onFieldSelect;
      case 'analyze':
        return !!this.config.onLogSearch;
      default:
        return false;
    }
  }
}
