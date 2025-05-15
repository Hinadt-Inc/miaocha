// 过滤器操作符类型
export type FilterOperator =
  | 'is' // 等于
  | 'is_not' // 不等于
  | 'contains' // 包含
  | 'does_not_contain' // 不包含
  | 'exists' // 存在
  | 'does_not_exist' // 不存在
  | 'is_one_of' // 是其中之一
  | 'is_not_one_of' // 不是其中之一
  | 'greater_than' // 大于
  | 'less_than' // 小于
  | 'is_between'; // 在...之间

// 过滤器类型定义
export interface Filter {
  id: string; // 唯一标识符
  field: string; // 字段名
  operator: FilterOperator; // 操作符
  value: string | string[] | [number, number] | null; // 值
  color: string; // 标签颜色
}

// 日志数据类型
export interface LogData {
  key: string;
  timestamp: string;
  message: string;
  host: string;
  source: string;
  level: string;
  distributionData?: Array<{
    timePoint: string;
    count: number;
  }>;
  [key: string]: unknown;
}

// 表格类型定义
export interface TableDefinition {
  name: string;
  fields: string[];
}

// 字段类型定义
export interface FieldDefinition {
  name: string;
  type: string;
}

// useFilters 返回值类型
export interface UseFiltersReturn {
  filters: Filter[];
  showFilterModal: boolean;
  setShowFilterModal: (show: boolean) => void;
  selectedFilterField: string;
  selectedFilterOperator: FilterOperator;
  openFilterModal: () => void;
  handleFilterFieldChange: (fieldName: string) => void;
  addFilter: (values: {
    field: string;
    operator: FilterOperator;
    value: string | string[] | [number, number] | null;
  }) => void;
  removeFilter: (filterId: string) => void;
}

// API返回的日志数据类型
export interface APILogData {
  [key: string]: unknown;
}

// 日志分布点类型
export interface SearchLogsParams {
  datasourceId: number;
  module: string;
  tableName: string; // 新增表名字段
  keyword?: string;
  whereSql?: string;
  startTime?: string;
  endTime?: string;
  timeRange?: string;
  timeGrouping?: string;
  pageSize?: number;
  offset?: number;
  fields?: string[];
}

export interface SearchLogsResult {
  success: boolean;
  errorMessage?: string;
  executionTimeMs: number;
  columns: string[];
  rows: Record<string, unknown>[];
  totalCount: number;
  distributionData?: DistributionPoint[];
  fieldDistributions?: FieldDistribution[]; // 字段分布数据（后端暂未实现）
  records?: never; // 该字段已废弃，请使用rows
  distribution?: DistributionPoint[]; // 修正类型定义
}

// 字段分布类型
export interface FieldDistribution {
  fieldName: string;
  valueDistributions: ValueDistribution[];
  totalCount: number;
  nonNullCount: number;
  nullCount: number;
  uniqueValueCount: number;
}

// 值分布类型
export interface ValueDistribution {
  value: string;
  count: number;
  percentage: number;
}

export type DistributionPoint = {
  timePoint: string;
  count: number;
};
