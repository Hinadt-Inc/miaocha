/**
 * ExpandedRow组件相关的类型定义
 */

// 组件Props接口
export interface IExpandedRowProps {
  /** 展示的数据对象 */
  data: Record<string, any>;
  /** 搜索关键词列表，用于高亮显示 */
  keywords: string[];
  /** 模块查询配置，包含时间字段等信息 */
  enhancedColumns: any[];
}

// 模块查询配置接口（从父级类型继承）
export interface IModuleQueryConfig {
  timeField: string;
  excludeFields?: string[];
  keywordFields?: any[];
  [key: string]: any;
}

// 表格数据源接口
export interface ITableDataSource {
  key: string;
  field: string;
  value: string;
}

// 表格列配置接口
export interface ITableColumn {
  title: string;
  className?: string;
  dataIndex: string;
  width?: number;
  render?: (text: string) => React.ReactNode;
}

// Tabs配置接口
export interface ITabItem {
  key: string;
  label: string;
  children: React.ReactNode;
}
