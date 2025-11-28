import { QUICK_RANGES, DATE_FORMAT_THOUSOND } from './utils';

/**
 * Home页面相关的常量定义
 */

// 默认的搜索参数（使用全局类型）
export const DEFAULT_SEARCH_PARAMS: ILogSearchParams = {
  offset: 0,
  pageSize: 50,
  datasourceId: null,
  module: null,
  startTime: QUICK_RANGES.last_15m.from().format(DATE_FORMAT_THOUSOND),
  endTime: QUICK_RANGES.last_15m.to().format(DATE_FORMAT_THOUSOND),
  timeRange: 'last_15m',
  timeGrouping: 'auto',
  range: [],
  sortFields: [],
};

// 请求防抖延迟时间（毫秒）
export const REQUEST_DEBOUNCE_DELAY = 300;

// 分享参数应用延迟时间（毫秒）
export const SHARED_PARAMS_APPLY_DELAY = 200;

// SessionStorage键名
export const STORAGE_KEYS = {
  SHARED_PARAMS: 'miaocha_shared_params',
  OAUTH_PROVIDER: 'oauthProvider',
  SEARCH_BAR_PARAMS: 'searchBarParams',
} as const;

// URL参数键名
export const URL_PARAMS = {
  TICKET: 'ticket',
  KEYWORDS: 'keywords',
  WHERE_SQLS: 'whereSqls',
  TIME_RANGE: 'timeRange',
  START_TIME: 'startTime',
  END_TIME: 'endTime',
  MODULE: 'module',
  TIME_GROUPING: 'timeGrouping',
} as const;

// 清理URL时需要删除的参数列表
export const URL_PARAMS_TO_CLEAN = [
  URL_PARAMS.KEYWORDS, // 关键字
  URL_PARAMS.WHERE_SQLS, // 自定义SQL
  URL_PARAMS.TIME_RANGE, // 时间范围
  URL_PARAMS.START_TIME, // 开始时间
  URL_PARAMS.END_TIME, // 结束时间
  URL_PARAMS.MODULE, // 模块
  URL_PARAMS.TIME_GROUPING, // 时间分组
] as const;
