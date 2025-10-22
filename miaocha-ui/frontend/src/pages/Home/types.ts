// 搜索参数接口
export interface ILogSearchParams {
  offset: number;
  pageSize: number;
  datasourceId: number | null;
  module: string | null;
  startTime: string;
  endTime: string;
  timeRange: string;
  timeGrouping: string;
  whereSqls?: string[];
  keywords?: string[];
  fields?: string[];
  sortFields?: any[];
  timeType?: 'relative' | 'absolute' | 'quick';
  relativeStartOption?: IRelativeTime;
  relativeEndOption?: IRelativeTime;
}

// 模块选项接口
// export interface IStatus {
//   label: string;
//   value: string;
//   datasourceId: string | number;
//   datasourceName: string;
//   module: string;
// }

// 日志详情响应接口
export interface ILogDetailsResponse {
  totalCount: number;
  rows: any[];
}

// 日志列配置响应接口
export interface ILogColumnsResponse {
  columnName?: string;
  selected: boolean;
  _createTime?: number;
}

// 日志时间分布数据接口
export interface ILogHistogramData {
  distributionData: any[];
  timeUnit: string;
  timeInterval: number;
}

// 日志时间分布响应接口（API返回）
export interface ILogHistogramResponse {
  distributionData: ILogHistogramData[];
}

// 模块响应接口
export interface IMyModulesResponse {
  datasourceId: string | number;
  datasourceName: string;
  module: string;
}

// 分享参数接口
export interface ISharedParams {
  keywords?: string[];
  whereSqls?: string[];
  timeRange?: string;
  startTime?: string;
  endTime?: string;
  module?: string;
  timeGrouping?: string;
  fields?: string[];
  timeType?: 'relative' | 'absolute' | 'quick';
  relativeStartOption?: IRelativeTime;
  relativeEndOption?: IRelativeTime;
}

// 时间选项接口
export interface ITimeOption {
  value: string;
  range: [string, string];
  label: string;
  type: 'quick' | 'absolute' | 'relative';
  startOption?: IRelativeTime;
  endOption?: IRelativeTime;
  // 强制更新标识，用于触发重新搜索
  _forceUpdate?: number;
  // 搜索来源标识，用于区分是否来自搜索按钮
  _fromSearch?: boolean;
}

// 搜索栏Ref接口
export interface SearchBarRef {
  setTimeGroup?: (timeGrouping: string) => void;
  setTimeOption?: (timeOption: ITimeOption) => void;
  removeSql?: (sql: string) => void;
  addSql?: (sql: string) => void;
  autoRefresh?: () => void;
}

// Sider组件Ref接口
export interface SiderRef {
  refreshFieldDistributions?: () => void;
}

// 排序配置类型
export type SortConfig = any[];

// 模块查询配置接口
export interface IModuleQueryConfig {
  module: string;
  timeField: string;
  excludeFields?: string[];
  keywordFields?: any[];
  [key: string]: any;
}
