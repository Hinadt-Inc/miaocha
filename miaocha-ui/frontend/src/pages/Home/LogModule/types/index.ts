/**
 * LogModule 模块的类型定义
 */

import { ILogSearchParams, IStatus, ILogDetailsResponse, ILogColumnsResponse, ILogHistogramData } from '../../types';

// Log 组件的 Props 接口
export interface ILogProps {
  /** 直方图数据 - 可以是单个对象或数组 */
  histogramData: ILogHistogramData[] | ILogHistogramData | null;
  /** 直方图数据是否正在加载 */
  histogramDataLoading: boolean;
  /** 加载日志数据的函数 */
  getDetailData: any;
  /** 日志数据 */
  detailData: ILogDetailsResponse;
  /** 搜索参数 */
  searchParams: ILogSearchParams;
  /** 动态列配置 */
  dynamicColumns?: ILogColumnsResponse[];
  /** 侧边栏的where条件 */
  whereSqlsFromSider: IStatus[];
  /** SQL语句列表 */
  sqls?: string[];
  /** 搜索回调函数 */
  onSearch: (params: ILogSearchParams) => void;
  /** 列变化回调函数 - 传递单个列对象 */
  onChangeColumns: (col: any) => void;
  /** 来自表格的搜索回调 */
  onSearchFromTable?: (params: ILogSearchParams) => void;
  /** 模块查询配置 */
  moduleQueryConfig?: any;
  /** 排序变化回调函数 */
  onSortChange?: (sortConfig: any[]) => void;
}

// LogChart 组件的 Props 接口
export interface ILogChartProps {
  /** 直方图数据 - 可以是单个对象或数组 */
  data: ILogHistogramData[] | ILogHistogramData | null;
  /** 搜索参数 */
  searchParams: ILogSearchParams;
  /** 搜索回调函数 */
  onSearch: (params: ILogSearchParams) => void;
}

// LogTable 组件的 Props 接口
export interface ILogTableProps {
  /** 侧边栏的where条件 */
  whereSqlsFromSider?: IStatus[];
  /** 列变化回调函数 */
  onChangeColumns?: (col: any) => void;
  /** 表格数据 */
  data: any[];
  /** 搜索参数 */
  searchParams?: ILogSearchParams;
  /** 加载状态 */
  loading: boolean;
  /** 加载更多回调函数 */
  onLoadMore: () => void;
  /** 是否还有更多数据 */
  hasMore: boolean;
  /** 总数据条数 */
  totalCount?: number;
  /** 动态列配置 */
  dynamicColumns?: ILogColumnsResponse[];
  /** SQL语句列表 */
  sqls?: string[];
  /** 搜索回调函数 */
  onSearch?: (params: ILogSearchParams) => void;
  /** 模块查询配置 */
  moduleQueryConfig?: any;
  /** 排序变化回调函数 */
  onSortChange?: (sortConfig: any[]) => void;
}
