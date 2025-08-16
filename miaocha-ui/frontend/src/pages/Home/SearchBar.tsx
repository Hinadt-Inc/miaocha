/**
 * SearchBar 组件 - 兼容性导出
 *
 * 注意：此文件已重构为模块化结构，原有功能已迁移到 ./SearchBar/ 目录
 * 为了保持向后兼容性，此文件保留并导出新的模块化组件
 */

import SearchBarComponent from './SearchBar/index';
export type { ISearchBarProps, ISearchBarRef } from './SearchBar/types';

export default SearchBarComponent;
