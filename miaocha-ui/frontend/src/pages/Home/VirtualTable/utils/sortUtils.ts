import { SortConfig } from '../types';

/**
 * 排序相关工具函数
 */

// 不支持排序的字段类型
export const UNSORTABLE_FIELD_TYPES = [
  'LONGTEXT',
  'MEDIUMTEXT',
  'TINYTEXT',
  'JSON',
  'BLOB',
  'BITMAP',
  'ARRAY',
  'MAP',
  'STRUCT',
  'JSONB',
  'VARIANT',
];

/**
 * 检查字段是否可以排序
 * @param dataType 数据类型
 * @returns 是否可排序
 */
export const isFieldSortable = (dataType: string): boolean => {
  return !UNSORTABLE_FIELD_TYPES.includes(dataType.toUpperCase());
};

/**
 * 处理表格排序变化，转换为标准的排序配置
 * @param sorter Antd Table 的排序器参数
 * @returns 标准化的排序配置数组
 */
export const processSorterChange = (sorter: any): SortConfig[] => {
  let resultSorter: SortConfig[] = [];

  if (sorter) {
    // 判断是单个排序还是多个排序
    if (Array.isArray(sorter)) {
      // 多列排序
      const activeSorts = sorter.filter((sort: any) => sort.order);
      resultSorter = activeSorts.map((sort: any) => ({
        fieldName: sort.field || sort.columnKey,
        direction: sort.order === 'ascend' ? 'ASC' : 'DESC',
      }));
    } else {
      // 单列排序
      if (sorter.order) {
        resultSorter.push({
          fieldName: sorter.field || sorter.columnKey,
          direction: sorter.order === 'ascend' ? 'ASC' : 'DESC',
        });
      }
    }
  }

  return resultSorter;
};

/**
 * 创建列排序比较函数
 * @param columnName 列名
 * @param dataType 数据类型
 * @returns 排序比较函数
 */
export const createColumnSorter = (columnName: string, dataType: string) => {
  return (a: any, b: any) => {
    const valueA = a[columnName];
    const valueB = b[columnName];

    // 处理 null/undefined 值
    if (valueA === null || valueA === undefined) {
      if (valueB === null || valueB === undefined) return 0;
      return -1;
    }
    if (valueB === null || valueB === undefined) {
      return 1;
    }

    // 判断是否为数值类型
    const upperDataType = dataType?.toUpperCase();
    const isNumericType = [
      'INT',
      'INTEGER',
      'BIGINT',
      'TINYINT',
      'SMALLINT',
      'FLOAT',
      'DOUBLE',
      'DECIMAL',
      'NUMERIC',
    ].includes(upperDataType);

    if (isNumericType) {
      // 数值类型排序
      const numA = parseFloat(valueA);
      const numB = parseFloat(valueB);

      if (!isNaN(numA) && !isNaN(numB)) {
        return numA - numB;
      }

      if (isNaN(numA) && !isNaN(numB)) return 1;
      if (!isNaN(numA) && isNaN(numB)) return -1;

      return String(valueA).localeCompare(String(valueB));
    } else {
      // 字符串类型排序
      if (typeof valueA === 'string' && typeof valueB === 'string') {
        return valueA.localeCompare(valueB);
      }
      return (valueA || '').toString().localeCompare((valueB || '').toString());
    }
  };
};

export const createTimeSorter = (timeField: string) => {
  return (a: any, b: any) => {
    const timeA = a[timeField];
    const timeB = b[timeField];

    if (!timeA && !timeB) return 0;
    if (!timeA) return 1;
    if (!timeB) return -1;

    const parseTime = (timeStr: any) => {
      if (!timeStr) return 0;
      const str = String(timeStr);
      if (str.match(/^\d{4}-\d{2}-\d{2}\s\d{2}:\d{2}:\d{2}/)) {
        return str;
      }
      const date = new Date(str);
      if (!isNaN(date.getTime())) {
        return date.getTime();
      }
      return str;
    };

    const parsedA = parseTime(timeA);
    const parsedB = parseTime(timeB);

    if (typeof parsedA === 'number' && typeof parsedB === 'number') {
      return parsedA - parsedB;
    }

    return String(parsedA).localeCompare(String(parsedB));
  };
};
