import React, { memo, useState, useEffect, useRef, useCallback } from 'react';
import { Table, Spin, Empty, Alert, Typography, Button, Tooltip, message } from 'antd';
import { QueryResult } from '../types';
import ResizeObserver from 'rc-resize-observer';
import dayjs from 'dayjs';
import styles from '../SQLEditorPage.module.less';
import Loading from '@/components/Loading';

const { Text } = Typography;

// 选中单元格的接口
interface SelectedCell {
  rowIndex: number;
  columnKey: string;
  value: any;
}

interface ResultsViewerProps {
  queryResults: QueryResult | null;
  loading: boolean;
  formatTableCell: (value: any) => React.ReactNode;
  downloadResults: () => void;
}

// 常量
const TABLE_HEIGHT = '100%'; // 表格高度
const PAGE_SIZE_OPTIONS = ['10', '20', '50', '100'];
const MIN_COLUMN_WIDTH = 80; // 最小列宽
const MAX_COLUMN_WIDTH = 300; // 最大列宽，保证表格整齐紧凑  // 可调整宽度的表头组件
interface ResizableTitleProps extends React.HTMLAttributes<HTMLElement> {
  onResize: (width: number) => void;
  width?: number;
  columnKey?: string;
}

const ResizableTitle: React.FC<ResizableTitleProps> = (props) => {
  const { onResize, width, columnKey, ...restProps } = props;

  if (!width) {
    return <th {...restProps} />;
  }

  // 使用引用跟踪拖拽状态，避免闭包问题
  const isDraggingRef = useRef(false);
  const startXRef = useRef(0);
  const startWidthRef = useRef(0);
  const [innerDragging, setInnerDragging] = useState<boolean>(false);

  // 拖拽处理函数
  const handleMouseDown = (e: React.MouseEvent<HTMLDivElement>) => {
    e.stopPropagation();
    e.preventDefault();

    setInnerDragging(true);
    isDraggingRef.current = true;

    // 获取初始位置
    startXRef.current = e.clientX;
    startWidthRef.current = width;

    // 处理拖拽过程
    const handleMouseMove = (e: MouseEvent) => {
      if (isDraggingRef.current) {
        const newWidth = startWidthRef.current + e.clientX - startXRef.current;
        if (newWidth >= MIN_COLUMN_WIDTH) {
          onResize(newWidth);
        }
      }
    };

    // 处理拖拽结束
    const handleMouseUp = () => {
      setInnerDragging(false);
      isDraggingRef.current = false;
      document.removeEventListener('mousemove', handleMouseMove);
      document.removeEventListener('mouseup', handleMouseUp);
    };

    // 添加全局事件监听
    document.addEventListener('mousemove', handleMouseMove);
    document.addEventListener('mouseup', handleMouseUp);
  };

  return (
    <th {...restProps} className={innerDragging ? 'dragging' : ''}>
      {restProps.children}
      <div
        className={`column-resizer ${innerDragging ? 'dragging' : ''}`}
        onMouseDown={handleMouseDown}
        onClick={(e) => e.stopPropagation()}
        title="拖拽调整列宽"
      />
    </th>
  );
};

// 比较值的工具函数，用于排序
const compareValues = (a: unknown, b: unknown): number => {
  // 处理 null、undefined 等空值
  if (a === null || a === undefined) {
    if (b === null || b === undefined) return 0;
    return -1; // 空值排在前面
  }
  if (b === null || b === undefined) {
    return 1;
  }

  // 如果都是数字类型，按数字比较
  if (typeof a === 'number' && typeof b === 'number') {
    return a - b;
  }

  // 尝试将值转换为数字进行比较（适用于数字字符串）
  const numA = Number(a);
  const numB = Number(b);
  if (!isNaN(numA) && !isNaN(numB)) {
    return numA - numB;
  }

  // 字符串比较
  if (typeof a === 'string' && typeof b === 'string') {
    return a.localeCompare(b);
  }

  // 其他类型转换为字符串比较
  return String(a).localeCompare(String(b));
};

// 时间格式化函数
const formatLogTime = (value: any): React.ReactNode => {
  if (!value) return value;

  try {
    // 尝试解析时间值
    let date = dayjs(value);

    // 如果第一次解析失败，尝试其他常见格式
    if (!date.isValid()) {
      // 尝试作为时间戳解析（毫秒）
      if (typeof value === 'number' || /^\d+$/.test(String(value))) {
        const timestamp = Number(value);
        // 判断是秒还是毫秒时间戳
        if (timestamp < 10000000000) {
          // 秒级时间戳
          date = dayjs.unix(timestamp);
        } else {
          // 毫秒级时间戳
          date = dayjs(timestamp);
        }
      }
    }

    if (date.isValid()) {
      // 要求到毫秒级
      return date.format('YYYY-MM-DD HH:mm:ss.SSS');
    }
  } catch (error) {
    console.warn('Failed to format log_time:', value, error);
  }

  // 如果解析失败，返回原值
  return value;
};

// 估算列宽的实用函数
const estimateColumnWidth = (col: string, rows: any[], maxWidth: number = MAX_COLUMN_WIDTH): number => {
  // 统一使用最大宽度限制，保证表格整齐
  let columnMaxWidth = maxWidth;

  // 列名长度本身也是重要参考
  let estimatedWidth = Math.max(col.length * 12, MIN_COLUMN_WIDTH);

  // 抽样检查实际数据
  const sampleSize = Math.min(20, rows.length);
  const samples = rows.slice(0, sampleSize);

  samples.forEach((row) => {
    const value = row[col];
    if (value === null || value === undefined) return;

    // 根据数据类型估算宽度
    let valueWidth = 0;

    if (typeof value === 'number') {
      // 数字类型，根据位数估算
      valueWidth = String(value).length * 10;
    } else if (typeof value === 'boolean') {
      // 布尔值固定宽度
      valueWidth = 60;
    } else if (typeof value === 'string') {
      // 字符串根据长度估算，但都限制在最大宽度内
      const strValue = String(value);

      if (/^\d{4}-\d{2}-\d{2}/.test(strValue)) {
        // 日期类型
        valueWidth = 150;
      } else if (/^\d{2}:\d{2}:\d{2}/.test(strValue)) {
        // 时间类型
        valueWidth = 100;
      } else if (strValue.length <= 10) {
        // 短字符串
        valueWidth = strValue.length * 10 + 20;
      } else {
        // 普通字符串，按平均字符宽度计算，但限制最大宽度
        valueWidth = Math.min(strValue.length * 8, columnMaxWidth);
      }
    } else {
      // 对象或其他复杂类型
      valueWidth = 200;
    }

    // 更新最大估算宽度
    if (valueWidth > estimatedWidth) {
      estimatedWidth = valueWidth;
    }
  });

  // 增加一些边距
  estimatedWidth += 24;

  // 确保列宽在合理范围内，统一限制最大宽度
  return Math.min(Math.max(estimatedWidth, MIN_COLUMN_WIDTH), columnMaxWidth);
};

/**
 * 查询结果显示组件
 * 以表格形式展示 SQL 查询结果
 */
const ResultsViewer: React.FC<ResultsViewerProps> = ({ queryResults, loading, formatTableCell }) => {
  // 组件状态
  const [columnWidths, setColumnWidths] = useState<Record<string, number>>({});
  const [draggingColumn, setDraggingColumn] = useState<string | null>(null);
  const [selectedCell, setSelectedCell] = useState<SelectedCell | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);

  // 复制选中单元格内容到剪贴板
  const copySelectedCell = useCallback(async () => {
    if (!selectedCell) return;

    try {
      const textValue = String(selectedCell.value || '');
      await navigator.clipboard.writeText(textValue);
      message.success('已复制到剪贴板');
    } catch (error) {
      console.error('复制失败:', error);
      message.error('复制失败');
    }
  }, [selectedCell]);

  // 键盘事件监听
  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      // 检查是否按下了 Ctrl+C (或在 Mac 上是 Cmd+C)
      if ((event.ctrlKey || event.metaKey) && event.key === 'c') {
        if (selectedCell) {
          event.preventDefault();
          copySelectedCell();
        }
      }
      // ESC 键取消选中
      else if (event.key === 'Escape') {
        setSelectedCell(null);
      }
    };

    // 添加键盘事件监听
    document.addEventListener('keydown', handleKeyDown);

    return () => {
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [selectedCell, copySelectedCell]);

  // 处理单元格点击
  const handleCellClick = useCallback((record: any, rowIndex: number, columnKey: string) => {
    const value = record[columnKey];
    setSelectedCell({
      rowIndex,
      columnKey,
      value,
    });
  }, []);

  // 当查询结果变化时重新计算列宽
  useEffect(() => {
    if (!queryResults?.rows?.length || !queryResults.columns?.length) return;

    const widths: Record<string, number> = {};

    // 计算每列的初始宽度
    queryResults.columns.forEach((col) => {
      if (queryResults.rows) {
        widths[col] = estimateColumnWidth(col, queryResults.rows);
      }
    });

    setColumnWidths(widths);
  }, [queryResults?.rows, queryResults?.columns]);

  // 在组件外部点击时取消拖拽
  useEffect(() => {
    const handleClickOutside = () => {
      if (draggingColumn) {
        setDraggingColumn(null);
      }
    };

    document.addEventListener('click', handleClickOutside);
    return () => {
      document.removeEventListener('click', handleClickOutside);
    };
  }, [draggingColumn]);

  // 双击自动调整列宽
  const handleDoubleClickHeader = (columnKey: string) => {
    if (!queryResults?.rows?.length) return;

    // 重新计算该列的最佳宽度，统一使用最大宽度限制
    const optimalWidth = estimateColumnWidth(columnKey, queryResults.rows, MAX_COLUMN_WIDTH);

    // 更新列宽
    setColumnWidths((prev) => ({
      ...prev,
      [columnKey]: optimalWidth,
    }));
  };
  // 加载中或无结果的早期返回
  if (loading)
    return (
      <div className={styles.resultsViewerContainer}>
        <div className={`${styles.tableWrapper} ${styles.centeredContent}`}>
          <Spin size="large" />
        </div>
      </div>
    );

  if (!queryResults) {
    return (
      <div className={styles.resultsViewerContainer}>
        <div className={`${styles.tableWrapper} ${styles.centeredContent}`}>
          <Empty description="请执行查询以查看结果" />
        </div>
      </div>
    );
  }

  console.log('QueryResults available, checking status and data...', {
    status: queryResults.status,
    hasRows: !!queryResults.rows?.length,
    hasColumns: !!queryResults.columns?.length,
  });

  // 错误状态
  if (queryResults.status === 'error') {
    console.log('Showing error state:', queryResults.message);
    return <Alert type="error" message="查询执行错误" description={queryResults.message ?? '未知错误'} showIcon />;
  }

  // 检查数据可用性 - 放宽条件
  if (!queryResults.rows || queryResults.rows.length === 0) {
    console.log('No rows in query results - showing empty state');
    return (
      <div className={styles.resultsViewerContainer}>
        <div className={`${styles.tableWrapper} ${styles.centeredContent}`}>
          <Empty description="查询没有返回数据" />
        </div>
      </div>
    );
  }

  console.log('Proceeding to render table with data:', {
    rowCount: queryResults.rows.length,
    columnCount: queryResults.columns?.length,
  });

  // 构建表格列 - 如果没有columns字段，从第一行数据推导
  let columnList = queryResults.columns;
  if (!columnList || columnList.length === 0) {
    // 从第一行数据推导列名
    if (queryResults.rows && queryResults.rows.length > 0) {
      columnList = Object.keys(queryResults.rows[0]);
      console.log('Columns derived from first row:', columnList);
    } else {
      console.log('No columns and no rows to derive from');
      columnList = [];
    }
  } else {
    console.log('Using provided columns:', columnList);
  }

  if (columnList.length === 0) {
    console.log('No columns available - cannot render table');
    return (
      <div className={styles.resultsViewerContainer}>
        <div className={`${styles.tableWrapper} ${styles.centeredContent}`}>
          <Empty description="查询结果格式异常，无法显示表格" />
        </div>
      </div>
    );
  }

  // 重置所有列宽的函数
  const resetColumnWidths = () => {
    if (!queryResults?.rows?.length || !columnList?.length) return;

    const widths: Record<string, number> = {};
    columnList.forEach((col) => {
      if (queryResults.rows) {
        widths[col] = estimateColumnWidth(col, queryResults.rows, MAX_COLUMN_WIDTH);
      }
    });

    setColumnWidths(widths);
  };

  // 创建列配置的辅助函数
  const createHeaderCellConfig = (col: string, defaultWidth: number) => {
    const handleResize = (width: number) => {
      const newWidth = Math.min(Math.max(width, MIN_COLUMN_WIDTH), MAX_COLUMN_WIDTH);
      setColumnWidths((prev) => ({ ...prev, [col]: newWidth }));
    };

    return {
      width: defaultWidth,
      onResize: handleResize,
      columnKey: col,
    };
  };

  const columns = columnList.map((col) => {
    // 检查是否为特殊类型的列
    const lowerCol = col.toLowerCase();
    const isTimeColumn = lowerCol === 'log_time' || lowerCol.includes('timestamp');

    // 统一使用最大宽度限制，保证表格整齐
    const defaultWidth = Math.min(columnWidths[col] || 150, MAX_COLUMN_WIDTH);

    return {
      title: col,
      dataIndex: col,
      key: col,
      width: defaultWidth,
      render: (value: any, record: any, index: number) => {
        // 检查当前单元格是否被选中
        const isSelected = selectedCell && selectedCell.rowIndex === index && selectedCell.columnKey === col;

        // 对 log_time 列进行特殊时间格式化处理
        let displayValue;
        if (isTimeColumn && (col.toLowerCase() === 'log_time' || col.toLowerCase().includes('timestamp'))) {
          displayValue = formatLogTime(value);
        } else {
          displayValue = formatTableCell(value);
        }

        const cellClassName = `table-cell ${isSelected ? 'selected-cell' : ''}`;
        const stringValue = String(value || '');

        // 渲染单元格内容
        const cellContent = (
          <button
            type="button"
            className={cellClassName}
            onClick={(e) => {
              e.stopPropagation();
              handleCellClick(record, index, col);
            }}
            aria-label={`选择单元格: ${col}, 值: ${stringValue}`}
            title={stringValue}
          >
            {stringValue.length <= 100 ? (
              displayValue
            ) : (
              <Tooltip title={stringValue} placement="topLeft">
                <span>{displayValue}</span>
              </Tooltip>
            )}
          </button>
        );

        return cellContent;
      },
      // 禁用ellipsis，因为我们使用自定义渲染
      ellipsis: false,
      sorter: (a: Record<string, unknown>, b: Record<string, unknown>) => compareValues(a[col], b[col]),
      onHeaderCell: () => createHeaderCellConfig(col, defaultWidth),
    };
  });

  // 查询执行信息
  const executionInfo = (
    <div className={styles.executionInfo}>
      <div className={styles.toolbar}>
        {queryResults.executionTimeMs && (
          <Text type="secondary">
            查询执行时间: {queryResults.executionTimeMs}ms | 返回行数: {queryResults.rows.length}
            {selectedCell && ` | 已选中: ${selectedCell.columnKey}`}
          </Text>
        )}
        <div className={styles.toolbarActions}>
          {selectedCell && (
            <Text type="secondary" className={styles.copyHint}>
              按 Ctrl+C 复制选中内容
            </Text>
          )}
          <Button type="link" size="small" onClick={resetColumnWidths}>
            重置所有列宽
          </Button>
        </div>
      </div>
    </div>
  );

  return (
    <div className={styles.resultsViewerContainer} ref={containerRef}>
      {executionInfo}
      <ResizeObserver>
        <div className={styles.tableWrapper}>
          <div className={styles.tableContainer}>
            <Table
              dataSource={queryResults.rows.map((row, index) => ({ ...row, key: index }))}
              columns={columns}
              scroll={{ x: 'max-content', y: TABLE_HEIGHT }}
              size="small" // 使用小尺寸，更紧凑
              bordered={true}
              pagination={{
                showSizeChanger: true,
                showQuickJumper: true,
                pageSizeOptions: PAGE_SIZE_OPTIONS,
                showTotal: (total) => `共 ${total} 条记录`,
                size: 'small', // 分页器也使用小尺寸
              }}
              className={`${styles.resizableTable} compact-table`}
              components={{
                header: {
                  cell: ResizableTitle,
                },
              }}
              // 设置表格行高更紧凑
              rowClassName={() => 'compact-row'}
              onHeaderRow={(columns) => ({
                onMouseEnter: () => {
                  const columnKey = columns[0]?.key;
                  if (columnKey) {
                    document.querySelector(`th[data-column-key="${columnKey}"]`)?.classList.add('hover');
                  }
                },
                onMouseLeave: () => {
                  const columnKey = columns[0]?.key;
                  if (columnKey) {
                    document.querySelector(`th[data-column-key="${columnKey}"]`)?.classList.remove('hover');
                  }
                },
                onDoubleClick: () => {
                  const columnKey = columns[0]?.key;
                  if (columnKey) {
                    handleDoubleClickHeader(columnKey as string);
                  }
                },
                'data-column-key': columns[0]?.key,
                title: '双击自动调整列宽',
              })}
            />
            {loading && (
              <Loading fullScreen={false} size="large" tip="查询执行中..." className={styles.tableLoadingOverlay} />
            )}
          </div>
        </div>
      </ResizeObserver>
    </div>
  );
};

export default memo(ResultsViewer);
