/**
 * ExpandedRow组件相关的常量定义
 */

// 默认时间字段名称
export const DEFAULT_TIME_FIELD = 'log_time';

// Tab键名常量
export const TAB_KEYS = {
  TABLE: 'Table',
  JSON: 'JSON',
} as const;

// Tab标签文本
export const TAB_LABELS = {
  TABLE: 'Table',
  JSON: 'JSON',
} as const;

// 表格列宽配置
export const COLUMN_WIDTHS = {
  FIELD: 150,
} as const;

// 样式类名
export const CSS_CLASSES = {
  FIELD_TITLE: 'field-title',
} as const;

// ReactJson配置
export const REACT_JSON_CONFIG = {
  COLLAPSED_LEVEL: 2,
  ENABLE_CLIPBOARD: true,
  DISPLAY_DATA_TYPES: false,
  SHOW_NAME: false,
} as const;
