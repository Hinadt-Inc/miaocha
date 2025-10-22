/**
 * SearchBar 组件相关的类型定义
 */

import { ILogSearchParams, ILogColumnsResponse, ITimeOption } from '../types';

export interface SearchBarProps {
  // 数据与配置
  searchParams: ILogSearchParams; // 搜索参数
  totalCount?: number; // 记录总数
  columns?: ILogColumnsResponse[]; // 字段列表数据
  activeColumns?: string[]; // 激活的字段列表
  commonColumns?: string[]; // 普通字段列表（不含有.的字段）
  sortConfig?: any[]; // 排序配置

  // 查询状态
  keywords: string[]; // 关键词列表
  sqls: string[]; // SQL条件列表

  // 加载态
  loading?: boolean; // 加载状态

  // 分享相关
  sharedParams?: any; // 分享参数
  hasAppliedSharedParams?: boolean; // 是否已应用分享参数

  // 回调（动作）
  onSearch: (params: ILogSearchParams) => void; // 搜索回调函数
  onRefresh?: () => void; // 刷新回调函数
  onSqlsChange?: (sqls: string[]) => void; // SQL列表变化回调函数
  setKeywords: (k: string[]) => void; // 设置关键词列表
  setSqls: (s: string[]) => void; // 设置SQL条件列表
  setWhereSqlsFromSider: any; // 设置whereSqlsFromSider
  onRemoveSql?: (sql: string) => void; // 删除SQL条件回调函数
  refreshFieldDistributions?: () => void; // 获取字段分布回调函数
}

export interface SearchBarRef {
  addSql: (sql: string) => void; // 添加SQL条件
  removeSql: (sql: string) => void; // 移除SQL条件
  setTimeOption: (option: ITimeOption) => void; // 设置时间选项
  setTimeGroup: (group: string) => void; // 设置时间分组
  autoRefresh: () => void; // 自动刷新
}

// 时间提交参数接口
export interface ILogTimeSubmitParams {
  value?: string;
  range?: string[];
  label?: string;
  type?: 'quick' | 'absolute' | 'relative';
  startOption?: any;
  endOption?: any;
}

// 过滤标签数据接口
export interface IFilterTag {
  type: 'keyword' | 'sql' | 'time';
  value: string;
  color: string;
  closable?: boolean;
  onClick?: () => void;
  onClose?: () => void;
}

// 搜索输入状态接口
export interface ISearchInputState {
  keyword: string;
  sql: string;
}

// 时间状态接口
export interface ITimeState {
  timeOption: ITimeOption;
  timeGroup: string;
  openTimeRange: boolean;
  openTimeGroup: boolean;
  activeTab: string;
}
