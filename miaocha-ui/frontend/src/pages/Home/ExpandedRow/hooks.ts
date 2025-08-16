import { useMemo } from 'react';
import type { ITableColumn, ITableDataSource, ITabItem, IModuleQueryConfig } from './types';
import { transformDataToTableSource, filterDataByConfig } from './utils';
import { CSS_CLASSES, COLUMN_WIDTHS, TAB_KEYS, TAB_LABELS } from './constants';
import { highlightText } from '@/utils/highlightText';

/**
 * ExpandedRow组件相关的hooks
 */

/**
 * 生成表格列配置的hook
 * @param keywords 搜索关键词
 * @returns 表格列配置
 */
export const useTableColumns = (keywords: string[]): ITableColumn[] => {
  return useMemo(() => [
    {
      title: '字段',
      className: CSS_CLASSES.FIELD_TITLE,
      dataIndex: 'field',
      width: COLUMN_WIDTHS.FIELD,
    },
    {
      title: '值',
      dataIndex: 'value',
      render: (text: string) => highlightText(text, keywords),
    },
  ], [keywords]);
};

/**
 * 生成表格数据源的hook
 * @param data 原始数据
 * @param moduleQueryConfig 模块查询配置
 * @returns 表格数据源
 */
export const useTableDataSource = (
  data: Record<string, any>,
  moduleQueryConfig?: IModuleQueryConfig
): ITableDataSource[] => {
  return useMemo(() => {
    // 先过滤数据，再转换为表格数据源
    const filteredData = filterDataByConfig(data, moduleQueryConfig);
    return transformDataToTableSource(filteredData, moduleQueryConfig);
  }, [data, moduleQueryConfig]);
};

/**
 * 生成过滤后的JSON数据的hook
 * @param data 原始数据
 * @param moduleQueryConfig 模块查询配置
 * @returns 过滤后的JSON数据
 */
export const useFilteredJsonData = (
  data: Record<string, any>,
  moduleQueryConfig?: IModuleQueryConfig
): Record<string, any> => {
  return useMemo(() => {
    return filterDataByConfig(data, moduleQueryConfig);
  }, [data, moduleQueryConfig]);
};

/**
 * 生成Tabs配置的hook
 * @param tableComponent 表格组件
 * @param jsonComponent JSON组件
 * @returns Tabs配置
 */
export const useTabItems = (
  tableComponent: React.ReactNode,
  jsonComponent: React.ReactNode
): ITabItem[] => {
  return useMemo(() => [
    {
      key: TAB_KEYS.TABLE,
      label: TAB_LABELS.TABLE,
      children: tableComponent,
    },
    {
      key: TAB_KEYS.JSON,
      label: TAB_LABELS.JSON,
      children: jsonComponent,
    },
  ], [tableComponent, jsonComponent]);
};
