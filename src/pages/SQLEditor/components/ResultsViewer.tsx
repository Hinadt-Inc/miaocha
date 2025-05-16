import React, { memo, useState, useEffect, useRef } from 'react';
import { Table, Spin, Empty, Alert, Typography, Button } from 'antd';
import { QueryResult } from '../types';
import ResizeObserver from 'rc-resize-observer';
import './ResultsViewer.less';

const { Text } = Typography;

// 列单元格自定义属性
interface ColumnCellProps {
  width?: number;
  onResize: (width: number) => void;
  columnKey?: string;
}

interface ResultsViewerProps {
  queryResults: QueryResult | null;
  loading: boolean;
  formatTableCell: (value: any) => React.ReactNode;
  downloadResults: () => void;
}

// 常量
const TABLE_HEIGHT = '800px'; // 表格高度
const PAGE_SIZE_OPTIONS = ['10', '20', '50', '100'];
const MIN_COLUMN_WIDTH = 100; // 最小列宽  // 可调整宽度的表头组件
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
  if (typeof a === 'number' && typeof b === 'number') {
    return a - b;
  }
  if (typeof a === 'string' && typeof b === 'string') {
    return a.localeCompare(b);
  }
  return 0;
};

// 估算列宽的实用函数
const estimateColumnWidth = (col: string, rows: any[], maxWidth: number = 400): number => {
  // 根据列名类型调整最大宽度和初始值
  const lowerCol = col.toLowerCase();
  const isMessageColumn = lowerCol.includes('message');
  const isPathColumn =
    lowerCol.includes('path') || lowerCol.includes('url') || lowerCol.includes('file');
  const isTimestampColumn = lowerCol.includes('time') || lowerCol.includes('date');
  const isIdColumn = lowerCol === 'id' || lowerCol.endsWith('_id');
  const isStatusColumn = lowerCol.includes('status') || lowerCol.includes('state');

  // 为不同类型的列提供不同的默认宽度
  let columnMaxWidth = maxWidth;
  if (isMessageColumn) {
    columnMaxWidth = 800; // message列宽度更大
  } else if (isPathColumn) {
    columnMaxWidth = 600; // path列也需要更大宽度
  } else if (isTimestampColumn) {
    columnMaxWidth = 180; // 时间列适中宽度
  } else if (isIdColumn) {
    columnMaxWidth = 120; // ID列通常较窄
  } else if (isStatusColumn) {
    columnMaxWidth = 100; // 状态列通常较窄
  }

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
      // 字符串根据长度和类型估算
      const strValue = String(value);

      // 对不同类型的字符串进行不同处理
      if (/^\d{4}-\d{2}-\d{2}/.test(strValue)) {
        // 日期类型
        valueWidth = 150;
      } else if (/^\d{2}:\d{2}:\d{2}/.test(strValue)) {
        // 时间类型
        valueWidth = 100;
      } else if (isMessageColumn) {
        // message列特殊处理，给予足够的空间但仍限制上限
        // 每个字符估计8个像素，计算预估宽度
        const baseWidth = Math.min(strValue.length * 8, columnMaxWidth);

        // 针对较长的消息提供足够的最小宽度
        if (strValue.length > 50) {
          valueWidth = Math.max(baseWidth, 300);
        } else {
          valueWidth = baseWidth;
        }
      } else if (isPathColumn) {
        // path列特殊处理，给予足够空间显示完整路径
        // 路径通常有特殊字符和斜杠，需要更多空间
        const baseWidth = Math.min(strValue.length * 7, columnMaxWidth);
        valueWidth = Math.max(baseWidth, 200);
      } else if (strValue.length <= 10) {
        // 短字符串固定宽度
        valueWidth = strValue.length * 10 + 20;
      } else {
        // 普通字符串，按平均字符宽度计算
        valueWidth = Math.min(strValue.length * 8, 300);
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

  // 确保列宽在合理范围内
  return Math.min(Math.max(estimatedWidth, MIN_COLUMN_WIDTH), columnMaxWidth);
};

/**
 * 查询结果显示组件
 * 以表格形式展示 SQL 查询结果
 */
const ResultsViewer: React.FC<ResultsViewerProps> = ({
  queryResults,
  loading,
  formatTableCell,
  downloadResults,
}) => {
  // 组件状态
  const [tableWidth, setTableWidth] = useState<number>(0);
  const [columnWidths, setColumnWidths] = useState<Record<string, number>>({});
  const [draggingColumn, setDraggingColumn] = useState<string | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);

  // 当查询结果变化时重新计算列宽
  useEffect(() => {
    if (!queryResults?.rows?.length || !queryResults.columns?.length) return;

    const widths: Record<string, number> = {};

    // 计算每列的初始宽度
    queryResults.columns.forEach((col) => {
      widths[col] = estimateColumnWidth(col, queryResults.rows);
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

    // 检查是否是特殊列
    const lowerKey = columnKey.toLowerCase();
    const isMessageColumn = lowerKey.includes('message');
    const isPathColumn =
      lowerKey.includes('path') || lowerKey.includes('url') || lowerKey.includes('file');

    // 重新计算该列的最佳宽度，对于特殊列使用更大的最大宽度
    let maxWidth = 500;
    if (isMessageColumn) {
      maxWidth = 800;
    } else if (isPathColumn) {
      maxWidth = 600;
    }

    const optimalWidth = estimateColumnWidth(columnKey, queryResults.rows, maxWidth);

    // 更新列宽
    setColumnWidths((prev) => ({
      ...prev,
      [columnKey]: optimalWidth,
    }));
  };
  // 加载中或无结果的早期返回
  if (loading) return <Spin tip="执行查询中..." />;
  if (!queryResults) return <Empty description="请执行查询以查看结果" />;

  // 错误状态
  if (queryResults.status === 'error') {
    return (
      <Alert
        type="error"
        message="查询执行错误"
        description={queryResults.message ?? '未知错误'}
        showIcon
      />
    );
  }

  // 无数据返回
  if (!queryResults.rows?.length) {
    return <Empty description="查询没有返回数据" />;
  }

  // 构建表格列
  const columns = queryResults.columns?.map((col) => {
    // 检查是否为特殊类型的列
    const lowerCol = col.toLowerCase();
    const isMessageColumn = lowerCol.includes('message');
    const isPathColumn =
      lowerCol.includes('path') || lowerCol.includes('url') || lowerCol.includes('file');
    const isTimeColumn = lowerCol === 'log_time' || lowerCol.includes('timestamp');
    const shouldNotEllipsis = isMessageColumn || isPathColumn;

    return {
      title: col,
      dataIndex: col,
      key: col,
      width: columnWidths[col] || (isMessageColumn ? 400 : isPathColumn ? 300 : isTimeColumn ? 180 : 150), // 为特殊列提供更宽的默认宽度
      render: (value: any) => formatTableCell(value, col),
      // 对于特殊类型的列，禁用文本省略
      ellipsis: !shouldNotEllipsis,
      sorter: (a: Record<string, unknown>, b: Record<string, unknown>) =>
        compareValues(a[col], b[col]),
      onHeaderCell: () => ({
        width: columnWidths[col] || (isMessageColumn ? 400 : isPathColumn ? 300 : isTimeColumn ? 180 : 150),
        onResize: (width: number) => {
          setColumnWidths((prev) => ({
            ...prev,
            [col]: Math.max(width, MIN_COLUMN_WIDTH),
          }));
        },
        columnKey: col, // 用于识别列
      }),
      // 为特殊类型的列添加特殊样式
      onCell: () => ({
        className: isMessageColumn 
          ? 'message-column' 
          : isPathColumn 
            ? 'path-column' 
            : isTimeColumn 
              ? 'time-column' 
              : '',
      }),
    };
  });

  // 查询执行信息
  const executionInfo = (
    <div className="execution-info">
      {queryResults.executionTimeMs && (
        <Text type="secondary" style={{ display: 'block', marginBottom: 8 }}>
          查询执行时间: {queryResults.executionTimeMs}ms | 返回行数: {queryResults.rows.length}
          {queryResults.affectedRows !== undefined && ` | 影响行数: ${queryResults.affectedRows}`}
        </Text>
      )}
      <div className="toolbar">
        <Button
          type="link"
          size="small"
          onClick={() => {
            if (!queryResults?.rows?.length || !queryResults.columns?.length) return;

            const widths: Record<string, number> = {};
            queryResults.columns.forEach((col) => {
              widths[col] = estimateColumnWidth(col, queryResults.rows);
            });

            setColumnWidths(widths);
          }}
        >
          重置所有列宽
        </Button>
      </div>
    </div>
  );

  return (
    <div className="results-viewer-container" ref={containerRef}>
      {executionInfo}
      <ResizeObserver
        onResize={({ width }) => {
          setTableWidth(width);
        }}
      >
        <div className="table-wrapper">
          <Table
            dataSource={queryResults.rows.map((row, index) => ({ ...row, key: index }))}
            columns={columns}
            scroll={{ x: 'max-content', y: TABLE_HEIGHT }}
            size="small"
            bordered={false}
            pagination={{
              showSizeChanger: true,
              showQuickJumper: true,
              pageSizeOptions: PAGE_SIZE_OPTIONS,
              showTotal: (total) => `共 ${total} 条记录`,
            }}
            className="resizable-table"
            components={{
              header: {
                cell: ResizableTitle,
              },
            }}
            onHeaderRow={(column) => ({
              onMouseEnter: () => {
                // 鼠标悬停在列头上时的效果
                if (column.key) {
                  document
                    .querySelector(`th[data-column-key="${column.key}"]`)
                    ?.classList.add('hover');
                }
              },
              onMouseLeave: () => {
                // 鼠标离开列头时的效果
                if (column.key) {
                  document
                    .querySelector(`th[data-column-key="${column.key}"]`)
                    ?.classList.remove('hover');
                }
              },
              onDoubleClick: () => {
                // 双击时自动调整列宽
                if (column.key) {
                  handleDoubleClickHeader(column.key as string);
                }
              },
              'data-column-key': column.key,
              title: '双击自动调整列宽',
            })}
          />
        </div>
      </ResizeObserver>
    </div>
  );
};

export default memo(ResultsViewer);
