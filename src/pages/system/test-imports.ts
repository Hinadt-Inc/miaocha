// 测试重构后的数据源管理模块是否能正确导入和使用

// 测试主模块导入
import DataSourceManagementPage from './DataSourceManagement';

// 测试子组件导入
import { DataSourcePageHeader, DataSourceFormModal } from './DataSourceManagement/components';

// 测试hooks导入
import { useDataSourceData, useTableConfig, useDataSourceActions } from './DataSourceManagement/hooks';

// 测试类型导入
import type { DataSourceItem } from './DataSourceManagement/hooks';
import type { DataSourceFormData } from './DataSourceManagement/components';

console.log('所有导入测试通过！');

// 导出供其他地方使用
export {
  DataSourceManagementPage,
  DataSourcePageHeader,
  DataSourceFormModal,
  useDataSourceData,
  useTableConfig,
  useDataSourceActions,
};

export type { DataSourceItem, DataSourceFormData };
