import { DEFAULT_TIME_FIELD } from './constants';
import type { ITableDataSource, IModuleQueryConfig } from './types';

/**
 * ExpandedRow组件相关的工具函数
 */

/**
 * 处理时间字段的值，将T替换为空格
 * @param value 原始值
 * @returns 处理后的值
 */
export const formatTimeValue = (value: any): string => {
  return String(value ?? '').replace('T', ' ');
};

/**
 * 格式化字段值
 * @param key 字段名
 * @param value 字段值
 * @param timeField 时间字段名
 * @returns 格式化后的值
 */
export const formatFieldValue = (key: string, value: any, timeField: string): string => {
  if (key === timeField) {
    return formatTimeValue(value);
  }
  return String(value ?? '');
};

/**
 * 将数据对象转换为表格数据源
 * @param data 原始数据对象
 * @param moduleQueryConfig 模块查询配置
 * @returns 表格数据源数组
 */
export const transformDataToTableSource = (
  data: Record<string, any>,
  moduleQueryConfig?: IModuleQueryConfig,
  showKey?: string[],
): ITableDataSource[] => {
  const timeField = moduleQueryConfig?.timeField || DEFAULT_TIME_FIELD;
  return Object.entries(data)
    .filter(([key]) => (showKey || []).includes(key)) // 过滤掉内部key
    .map(([key, value], index) => ({
      key: `${key}_${index}`,
      field: key,
      value: formatFieldValue(key, value, timeField),
    }));
};

/**
 * 检查字段是否应该被排除
 * @param fieldName 字段名
 * @param excludeFields 要排除的字段列表
 * @returns 是否应该排除
 */
export const shouldExcludeField = (fieldName: string, excludeFields?: string[]): boolean => {
  if (!excludeFields || excludeFields.length === 0) {
    return false;
  }
  return excludeFields.includes(fieldName);
};

/**
 * 过滤数据对象，移除应该排除的字段
 * @param data 原始数据对象
 * @param moduleQueryConfig 模块查询配置
 * @returns 过滤后的数据对象
 */
export const filterDataByConfig = (
  data: Record<string, any>,
  moduleQueryConfig: IModuleQueryConfig,
): Record<string, any> => {
  if (!moduleQueryConfig?.excludeFields) {
    return data;
  }
  const filteredData: Record<string, any> = {};
  Object.entries(data).forEach(([key, value]) => {
    if (!shouldExcludeField(key, moduleQueryConfig.excludeFields)) {
      filteredData[key] = value;
    }
  });

  return filteredData;
};
