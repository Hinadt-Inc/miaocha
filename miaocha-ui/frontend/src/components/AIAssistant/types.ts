// 全局类型定义文件，用于AI助手相关的类型声明

declare global {
  interface Window {
    dispatchEvent(event: CustomEvent): boolean;
  }
}

// AI助手相关接口
export interface ILogDetailsResponse {
  total: number;
  data: any[];
  fields?: string[];
  [key: string]: any;
}

export interface ILogColumnsResponse {
  field: string;
  type: string;
  label?: string;
  [key: string]: any;
}

export interface ILogHistogramData {
  buckets: Array<{
    timestamp: number;
    count: number;
  }>;
  total: number;
}

export interface IStatus {
  label: string;
  value: string;
  [key: string]: any;
}

// AI助手回调函数类型
export type AILogSearchCallback = (params: {
  module?: string;
  keywords?: string[];
  sqls?: string[];
  activeColumns?: string[];
  sortConfig?: any[];
  whereSqls?: any[];
  filters?: any[];
  query?: string;
  sort?: string;
  analyze?: boolean;
  analyzeType?: string;
  metrics?: string[];
}) => void;

export type AIFieldSelectCallback = (fields: string[]) => void;

export type AITimeRangeCallback = (timeRange: {
  type: 'relative' | 'absolute';
  value?: number;
  unit?: 'hour' | 'day' | 'week' | 'month';
  start?: number;
  end?: number;
}) => void;

export {};
