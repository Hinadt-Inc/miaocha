/**
 * SearchBar 组件相关的类型定义
 */

import { ILogSearchParams, ILogColumnsResponse, ITimeOption } from '../types';

// SearchBar 组件的 Props 接口
export interface ISearchBarProps {
  searchParams: ILogSearchParams; // 搜索参数
  totalCount?: number; // 记录总数
  onSearch: (params: ILogSearchParams) => void; // 搜索回调函数
  onRefresh?: () => void; // 刷新回调函数
  setWhereSqlsFromSider: any; // 设置whereSqlsFromSider
  onRemoveSql?: (sql: string) => void; // 删除SQL条件回调函数
  columns?: ILogColumnsResponse[]; // 字段列表数据
  onSqlsChange?: (sqls: string[]) => void; // SQL列表变化回调函数
  activeColumns?: string[]; // 激活的字段列表
  getDistributionWithSearchBar?: () => void; // 获取字段分布回调函数
  sortConfig?: any[]; // 排序配置
  commonColumns?: string[]; // 普通字段列表（不含有.的字段）
  loading?: boolean; // 加载状态
  keywords: string[]; // 关键词列表
  setKeywords: (k: string[]) => void; // 设置关键词列表
  sqls: string[]; // SQL条件列表
  setSqls: (s: string[]) => void; // 设置SQL条件列表
  sharedParams?: any; // 分享参数
  hasAppliedSharedParams?: boolean; // 是否已应用分享参数
}

// SearchBar 组件 Ref 接口
export interface ISearchBarRef {
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
