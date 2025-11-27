/**
 * SiderModule 导出文件
// 工具函数
export {
  getFavoriteModule,
  setFavoriteModuleStorage,
  toggleFavoriteModule,
  getLocalActiveColumns,
  setLocalActiveColumns,
  removeLocalActiveColumns,
  updateSearchParamsInStorage,
  clearSearchConditionsKeepFields,
  sumArrayCount,
  generateQueryConditionsKey,
  hasDistributionData,
  cleanInvalidFieldsFromStorage
} from './utils';
 */

// 主组件
export { default } from './index';

// 类型定义
export type { SiderProps, SiderRef, FieldListItemProps, ModuleSelectOption } from './types';

// 子组件
export { VirtualFieldList, ModuleSelector, FieldListItem } from './components';

// 自定义Hooks
export { useFavoriteModule, useColumns, useModuleSelection, useDistributions } from './hooks';

// 工具函数
export {
  getFavoriteModule,
  setFavoriteModuleStorage,
  toggleFavoriteModule,
  getLocalActiveColumns,
  setLocalActiveColumns,
  updateSearchParamsInStorage,
  clearSearchConditionsKeepFields,
  sumArrayCount,
  generateQueryConditionsKey,
  hasDistributionData,
} from './utils';
