/**
 * Sider模块相关的类型定义
 */

// Sider组件的Props接口
export interface SiderProps {
  searchParams: ILogSearchParams; // 搜索参数
  modules: IStatus[]; // 模块名称列表
  onSearch: (params: ILogSearchParams) => void; // 搜索回调函数
  onChangeColumns?: (params: ILogColumnsResponse[]) => void; // 列变化回调函数
  setWhereSqlsFromSider: any; // 设置where条件
  onActiveColumnsChange?: (activeColumns: string[]) => void; // 激活字段变化回调函数
  onSelectedModuleChange?: (selectedModule: string, datasourceId?: number) => void; // 选中模块变化回调函数
  moduleQueryConfig?: any; // 模块查询配置
  onCommonColumnsChange?: (commonColumns: string[]) => void; // 普通字段变化回调函数
  selectedModule?: string; // 外部传入的选中模块，用于同步状态
  activeColumns?: string[]; // 外部传入的活跃字段，用于同步状态
  setActiveColumns?: (activeColumns: string[]) => void; // 设置活跃字段
  onColumnsLoaded?: (loaded: boolean) => void; // columns加载完成回调
}

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
