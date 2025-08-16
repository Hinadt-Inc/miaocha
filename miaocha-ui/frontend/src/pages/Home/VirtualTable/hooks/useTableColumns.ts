import { useMemo } from 'react';
import { highlightText } from '@/utils/highlightText';
import { getAutoColumnWidth } from '../utils/columnUtils';
import { isFieldSortable } from '../utils/sortUtils';

/**
 * 管理表格列配置的Hook
 */
export const useTableColumns = (
  dynamicColumns: ILogColumnsResponse[] | undefined,
  columnWidths: Record<string, number>,
  screenWidth: number,
  moduleQueryConfig: any,
  keyWordsFormat: string[],
  whereSqlsFromSider: IStatus[]
) => {
  return useMemo(() => {
    const timeField = moduleQueryConfig?.timeField || 'log_time';
    const otherColumns = dynamicColumns?.filter((item) => item.selected && item.columnName !== timeField);
    const _columns: any[] = [];

    if (otherColumns && otherColumns.length > 0) {
      // 在小屏幕上，计算每列的最大允许宽度
      const isSmallScreen = screenWidth < 1200;
      
      otherColumns.forEach((item: ILogColumnsResponse) => {
        const { columnName = '' } = item;

        // 优先使用用户手动调整的宽度，其次根据屏幕大小动态计算
        let columnWidth;
        if (columnWidths[columnName]) {
          columnWidth = columnWidths[columnName];
        } else {
          if (isSmallScreen) {
            const availableWidth = screenWidth - 250;
            const maxWidthPerColumn = Math.floor(availableWidth / Math.max(otherColumns.length, 2));
            columnWidth = Math.min(getAutoColumnWidth(columnName, screenWidth), maxWidthPerColumn);
          } else {
            columnWidth = getAutoColumnWidth(columnName, screenWidth);
          }
        }

        _columns.push({
          title: columnName,
          dataIndex: columnName,
          width: columnWidth,
          render: (text: string) => highlightText(text, keyWordsFormat || []),
          // 只有可排序的字段才添加排序器
          ...(isFieldSortable(item.dataType) ? {
            sorter: {
              compare: (a: any, b: any) => {
                const valueA = a[columnName];
                const valueB = b[columnName];
                
                // 处理 null、undefined 等空值
                if (valueA === null || valueA === undefined) {
                  if (valueB === null || valueB === undefined) return 0;
                  return -1;
                }
                if (valueB === null || valueB === undefined) {
                  return 1;
                }

                // 检查数据类型，针对数字类型进行特殊处理
                const dataType = item.dataType?.toUpperCase();
                const isNumericType = ['INT', 'INTEGER', 'BIGINT', 'TINYINT', 'SMALLINT', 
                                     'FLOAT', 'DOUBLE', 'DECIMAL', 'NUMERIC'].includes(dataType);

                if (isNumericType) {
                  const numA = parseFloat(valueA);
                  const numB = parseFloat(valueB);
                  
                  if (!isNaN(numA) && !isNaN(numB)) {
                    return numA - numB;
                  }
                  
                  if (isNaN(numA) && !isNaN(numB)) return 1;
                  if (!isNaN(numA) && isNaN(numB)) return -1;
                  
                  return String(valueA).localeCompare(String(valueB));
                } else {
                  if (typeof valueA === 'string' && typeof valueB === 'string') {
                    return valueA.localeCompare(valueB);
                  }
                  return (valueA || '').toString().localeCompare((valueB || '').toString());
                }
              },
              multiple: otherColumns.findIndex(col => col.columnName === columnName) + 2,
            }
          } : {}),
        });
      });
    }

    return {
      timeField,
      dynamicColumns: _columns,
      otherColumnsLength: otherColumns?.length || 0,
    };
  }, [
    dynamicColumns, 
    keyWordsFormat, 
    columnWidths, 
    whereSqlsFromSider, 
    screenWidth, 
    moduleQueryConfig
  ]);
};
