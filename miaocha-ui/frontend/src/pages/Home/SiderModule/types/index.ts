// 字段列表项数据接口
export interface IFieldData {
  activeColumns: string[]; // 选中的列
  searchParams: ILogSearchParams; // 搜索参数
  distributions: Record<string, IFieldDistributions>; // 字段分布
  distributionLoading: Record<string, boolean>; // 字段分布加载状态
  onToggle: (column: ILogColumnsResponse) => void; // 切换选中状态
  onDistribution: (columnName: string, newActiveColumns: string[], sql: string) => void; // 分布
  onActiveColumns: (params: string[]) => void; // 选中的列
  setWhereSqlsFromSider: any; // 设置where条件
}

// 字段列表项Props接口
export interface FieldListItemProps {
  key: string;
  isSelected: boolean; // 是否选中
  columnIndex: number; // 字段索引
  column: ILogColumnsResponse; // 字段数据
}

// 虚拟列表Props接口
export interface IVirtualFieldListProps {
  data: any[];
  itemHeight: number;
  containerHeight: number;
  renderItem: (item: any, index: number) => React.ReactNode;
}

// Sider Ref接口
export interface SiderRef {
  refreshFieldDistributions: () => void;
}

// 模块选择器选项接口
export interface ModuleSelectOption {
  value: string;
  label: React.ReactNode;
  title: string;
  module: string;
  datasourceId: number;
}
