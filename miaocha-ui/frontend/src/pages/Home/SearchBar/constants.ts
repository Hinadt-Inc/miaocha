/**
 * SearchBar 组件的常量配置
 */

// 搜索框占位符文本
export const PLACEHOLDERS = {
  KEYWORD: '输入关键词搜索',
  SQL: "WHERE子句，例如: level = 'ERROR' AND marker.reqType = 'EXECUTE'",
} as const;

// 搜索按钮配置
export const SEARCH_BUTTON = {
  TEXT: '搜索',
  SIZE: 'small' as const,
  TYPE: 'primary' as const,
} as const;

// 标签颜色配置
export const TAG_COLORS = {
  KEYWORD: 'orange',
  SQL: 'success',
  TIME: 'blue',
} as const;

// 统计信息配置
export const STATISTICS = {
  COUNT_UP_DURATION: 1,
  SEPARATOR: ',',
} as const;

// 样式配置
export const STYLES = {
  SPACE_SIZE: 8,
  TAG_MAX_WIDTH: 300,
} as const;
