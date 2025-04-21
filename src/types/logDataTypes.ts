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
  | 'is_between' // 在...之间

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
  key: number;
  timestamp: string;
  message: string;
  host: string;
  source: string;
  status: number;
  bytes: number;
  response_time: number;
  ip: string;
  method: string;
  path: string;
  user_agent: string;
  referer: string;
  'geo.country': string;
  'geo.city': string;
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
