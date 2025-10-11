import { QUICK_RANGES, DATE_FORMAT_THOUSOND } from './utils';
import type { ILogSearchParams } from './types';

/**
 * Home页面相关的常量定义
 */

// 默认的搜索参数
export const DEFAULT_SEARCH_PARAMS: ILogSearchParams = {
  offset: 0,
  pageSize: 50, // 改为每页50条数据
  datasourceId: null,
  module: null,
  startTime: QUICK_RANGES.last_15m.from().format(DATE_FORMAT_THOUSOND),
  endTime: QUICK_RANGES.last_15m.to().format(DATE_FORMAT_THOUSOND),
  timeRange: 'last_15m',
  timeGrouping: 'auto',
};

// 请求防抖延迟时间（毫秒）
export const REQUEST_DEBOUNCE_DELAY = 300;

// 滚动加载相关常量
export const SCROLL_LOAD_THRESHOLD = 200; // 距离底部200px时触发加载
export const PAGE_SIZE = 50; // 每页数据条数

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
  URL_PARAMS.KEYWORDS,
  URL_PARAMS.WHERE_SQLS,
  URL_PARAMS.TIME_RANGE,
  URL_PARAMS.START_TIME,
  URL_PARAMS.END_TIME,
  URL_PARAMS.MODULE,
  URL_PARAMS.TIME_GROUPING,
] as const;
