/**
 * ExpandedRow组件模块导出
 */

// 主组件
export { default } from './ExpandedRow';

// 类型定义
export type {
  IExpandedRowProps,
  IModuleQueryConfig,
  ITableDataSource,
  ITableColumn,
  ITabItem,
} from './types';

// 工具函数
export {
  formatTimeValue,
  formatFieldValue,
  transformDataToTableSource,
  shouldExcludeField,
  filterDataByConfig,
} from './utils';

// Hooks
export {
  useTableColumns,
  useTableDataSource,
  useFilteredJsonData,
  useTabItems,
} from './hooks';

// 常量
export {
  DEFAULT_TIME_FIELD,
  TAB_KEYS,
  TAB_LABELS,
  COLUMN_WIDTHS,
  CSS_CLASSES,
  REACT_JSON_CONFIG,
} from './constants';
