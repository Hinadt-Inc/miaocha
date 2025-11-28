export {};
declare global {
  /**
   * 时间分组类型
   * - second: 按秒分组
   * - minute: 按分钟分组
   * - hour: 按小时分组
   * - day: 按天分组
   * - auto: 自动分组
   */
  type TimeGrouping = 'second' | 'minute' | 'hour' | 'day' | 'auto';

  /**
   * 模块信息接口
   */
  interface IModules {
    /** 模块名称 */
    moduleName: string;
    /** 权限ID */
    permissionId: number | null;
  }
  /**
   * 我的模块响应接口
   */
  interface IMyModulesResponse {
    /** ID */
    id: number | null;
    /** 用户ID */
    userId: number;
    /** 数据源ID */
    datasourceId: number;
    /** 数据源名称 */
    datasourceName: string;
    /** 数据库名称 */
    databaseName: string;
    /** 模块名称 */
    module: string;
    /** 创建时间 */
    createTime: string | null;
    /** 更新时间 */
    updateTime: string | null;
    /** 创建用户 */
    createUser: number | null;
    /** 创建用户名 */
    createUserName: string | null;
    /** 更新用户 */
    updateUser: number | null;
    /** 更新用户名 */
    updateUserName: string | null;
  }

  /**
   * 日志查询参数接口
   */
  interface ILogSearchParams {
    /**
     * 数据源ID
     */
    datasourceId?: number | null;
    /**
     * 模块名称
     */
    module: string | null;
    /**
     * 关键词列表
     */
    keywords?: string[];
    /**
     * 自定义SQL条件列表
     */
    whereSqls?: string[];
    /**
     * 开始时间，格式为YYYY-MM-DD HH:mm:ss
     */
    startTime?: string;
    /**
     * 结束时间，格式为YYYY-MM-DD HH:mm:ss
     */
    endTime?: string;
    range: string[];
    /**
     * 时间范围，支持以下值：
     * "last_5m" - 最近5分钟
     * "last_15m" - 最近15分钟
     * "last_30m" - 最近30分钟
     * "last_1h" - 最近1小时
     * "last_8h" - 最近8小时
     * "last_24h" - 最近24小时
     * "today" - 今天
     * "yesterday" - 昨天
     * "last_week" - 上周
     */
    timeRange?:
      | 'last_5m'
      | 'last_15m'
      | 'last_30m'
      | 'last_1h'
      | 'last_8h'
      | 'last_24h'
      | 'today'
      | 'yesterday'
      | 'last_week';
    /**
     * 时间分组
     */
    timeGrouping?: TimeGrouping;
    timeType?: string;
    /**
     * 每页记录数
     */
    pageSize?: number;
    /**
     * 偏移量，用于分页
     */
    offset?: number;
    /**
     * 需要查询的字段列表
     */
    fields?: string[];
    /**
     * 排序字段列表
     */
    sortFields?: {
      fieldName: string;
      direction: 'ASC' | 'DESC';
    }[];
    relativeStartOption?: IRelativeTime;
    relativeEndOption?: IRelativeTime;
    /**
     * 查询配置
     */
    queryConfig?: {
      /**
       * 时间字段名
       */
      timeField?: string;
      /**
       * 关键词字段配置列表
       */
      keywordFields?: {
        fieldName: string;
        searchMethod: string;
      }[];
    };
  }

  /**
   * 日志查询响应接口
   */
  interface ILogDetailsResponse {
    columns: string[]; // 列名列表
    errorMessage?: string;
    executionTimeMs: number; // 查询耗时（毫秒）
    rows: Record<string, unknown>[]; // 日志数据明细列表
    success: boolean;
    totalCount: number; // 日志总数
  }

  interface ILogHistogram {
    timePoint: string; // 时间
    count: number; // 数量
  }

  interface ILogHistogramData {
    distributionData: ILogHistogram[];
    timeUnit: string; // 时间单位
    timeInterval: number; // 时间间隔
  }

  interface ILogHistogramResponse {
    distributionData: ILogHistogramData[]; // 数据分布
  }

  // interface IFieldDistributions {
  //   /** 字段名称 */
  //   fieldName: string;

  //   /** 字段值分布列表，按数量降序排序 */
  //   valueDistributions: {
  //     /** 字段值 */
  //     value: string;
  //     /** 该值出现的次数 */
  //     count: number;
  //     /** 该值占比 */
  //     percentage: number;
  //   }[];
  //   /** 该字段的总记录数 */
  //   totalCount?: number;
  //   /** 该字段的非空记录数 */
  //   nonNullCount?: number;
  //   /** 该字段的空记录数 */
  //   nullCount?: number;
  //   /** 该字段的唯一值数 */
  //   uniqueCount?: number;
  //   sampleSize?: number; // 样本大小
  // }

  /**
   * 日志字段分布响应接口
   */
  interface IDistributionsResponse {
    /** 字段数据分布统计信息，用于展示各字段的Top5值及占比 */
    fieldDistributions: IFieldDistributions[];
    sampleSize: number; // 样本大小
    /** 查询是否成功 */
    success: boolean;
    /** 错误信息 */
    errorMessage?: string;
    /** 查询耗时(毫秒) */
    executionTimeMs: number;
  }

  // 日志表字段的参数
  interface ILogColumnsParams {
    datasourceId?: number; // 数据源ID
    module: string; // 表名
  }

  // 日志表字段的响应
  interface ILogColumnsResponse {
    columnName?: string;
    dataType: string;
    selected?: boolean;
    columnComment?: string;
    isPrimaryKey?: boolean;
    isNullable?: boolean;
    isFixed?: boolean;
    _createTime?: number;
  }

  // 保存的搜索条件接口
  interface ISavedSearchCondition {
    /** ID */
    id: string;
    /** 用户ID */
    userId: number;
    /** 搜索条件名称 */
    name: string;
    /** 搜索条件描述 */
    description?: string;
    /** 搜索参数 */
    searchParams: ILogSearchParams;
    /** 创建时间 */
    createTime: string;
    /** 更新时间 */
    updateTime: string;
  }

  // 保存搜索条件的参数
  interface ISaveSearchConditionParams {
    /** 搜索条件名称 */
    name: string;
    /** 搜索条件描述 */
    description?: string;
    /** 搜索参数 */
    searchParams: ILogSearchParams;
  }

  // 保存搜索条件并返回缓存键的参数
  interface ISaveSearchConditionWithCacheParams {
    /** 模块名称 */
    module: string;
    /** 关键词列表 */
    keywords?: string[];
    /** SQL条件列表 */
    whereSqls?: string[];
    /** 开始时间（可选） */
    startTime?: string;
    /** 结束时间（可选） */
    endTime?: string;
    /** 时间范围（可选） */
    timeRange?: string;
    /** 时间分组（可选） */
    timeGrouping?: string;
    /** 目标桶数量 */
    targetBuckets?: number;
    /** 页面大小 */
    pageSize?: number;
    /** 偏移量 */
    offset?: number;
    /** 字段列表 */
    fields?: string[];
    /** 排序字段列表（可选） */
    sortFields?: {
      fieldName: string;
      direction: 'ASC' | 'DESC';
    }[];
    /** 搜索条件名称 */
    name: string;
    /** 搜索条件描述（可选） */
    description?: string;
  }

  // 保存搜索条件并返回缓存键的响应
  interface ISaveSearchConditionCacheResponse {
    /** 生成的缓存键 */
    cacheKey: string;
    /** 搜索条件ID */
    id?: string;
    /** 消息 */
    message?: string;
  }

  // 缓存条件数据结构
  interface ICacheSearchConditionData {
    /** 模块名称 */
    module: string;
    /** 关键词列表 */
    keywords?: string[];
    /** SQL条件列表 */
    whereSqls?: string[];
    /** 开始时间 */
    startTime?: string;
    /** 结束时间 */
    endTime?: string;
    /** 时间范围 */
    timeRange?: string;
    /** 时间分组 */
    timeGrouping?: string;
    /** 目标桶数量 */
    targetBuckets?: number;
    /** 页面大小 */
    pageSize?: number;
    /** 偏移量 */
    offset?: number;
    /** 字段列表 */
    fields?: string[];
    /** 排序字段列表 */
    sortFields?: {
      fieldName: string;
      direction: 'ASC' | 'DESC';
    }[];
    /** 搜索条件名称 */
    name: string;
    /** 搜索条件描述 */
    description?: string;
  }

  // 缓存搜索条件完整结构
  interface ICachedSearchCondition {
    /** 缓存ID */
    id: number;
    /** 缓存分组 */
    cacheGroup: string;
    /** 缓存键 */
    cacheKey: string;
    /** 搜索条件数据 */
    data: ICacheSearchConditionData;
    /** 创建时间 */
    createTime: string;
    /** 创建用户 */
    createUser: string;
  }
}
