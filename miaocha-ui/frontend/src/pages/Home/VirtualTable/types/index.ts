/**
 * VirtualTable 组件相关类型定义
 */

export interface VirtualTableProps {
  data: any[]; // 数据
  loading?: boolean; // 加载状态
  searchParams: ILogSearchParams; // 搜索参数
  onLoadMore: () => void; // 加载更多数据的回调函数
  hasMore?: boolean; // 是否还有更多数据
  dynamicColumns?: ILogColumnsResponse[]; // 动态列配置
  whereSqlsFromSider: IStatus[];
  onChangeColumns: (col: any) => void; // 列变化回调函数 - 传递单个列对象
  sqls?: string[]; // SQL语句列表
  onSearch?: (params: ILogSearchParams) => void; // 搜索回调函数
  moduleQueryConfig?: any; // 模块查询配置
  onSortChange?: (sortConfig: any[]) => void; // 排序变化回调函数
}

export interface ColumnHeaderProps {
  title: React.ReactNode;
  colIndex: number;
  onDelete: (colIndex: number) => void;
  onMoveLeft: (colIndex: number) => void;
  onMoveRight: (colIndex: number) => void;
  showActions: boolean;
  columns: any[];
}

export interface ResizableTitleProps {
  onResize?: (width: number) => void;
  width?: number;
  [key: string]: any;
}

export interface SortConfig {
  fieldName: string;
  direction: 'ASC' | 'DESC';
}
