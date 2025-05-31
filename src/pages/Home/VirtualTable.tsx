import { useEffect, useRef, useState, Fragment, useMemo } from 'react';
import { Table, Button, Tooltip } from 'antd';
import ExpandedRow from './ExpandedRow';
import styles from './VirtualTable.module.less';
import { highlightText } from '@/utils/highlightText';
import { CloseOutlined, DoubleLeftOutlined, DoubleRightOutlined } from '@ant-design/icons';

interface IProps {
  data: any[]; // 数据
  loading?: boolean; // 加载状态
  searchParams: ILogSearchParams; // 搜索参数
  onLoadMore: () => void; // 加载更多数据的回调函数
  hasMore?: boolean; // 是否还有更多数据
  dynamicColumns?: ILogColumnsResponse[]; // 动态列配置
  onChangeColumns: (params: ILogColumnsResponse[]) => void; // 列变化回调函数
}

interface ColumnHeaderProps {
  title: React.ReactNode;
  colIndex: number;
  onDelete: (colIndex: number) => void;
  onMoveLeft: (colIndex: number) => void;
  onMoveRight: (colIndex: number) => void;
  showActions: boolean;
  columns: any[];
}

const ResizableTitle = (props: any) => {
  const { onResize, width, ...restProps } = props;

  if (!width) {
    return <th {...restProps} />;
  }

  return (
    <th {...restProps} style={{ width, position: 'relative' }}>
      {restProps.children}
      <div
        className={styles.resizeHandle}
        onMouseDown={(e) => {
          e.preventDefault();
          e.stopPropagation();
          const startX = e.pageX;
          const startWidth = width;

          const handleMouseMove = (e: MouseEvent) => {
            const newWidth = startWidth + (e.pageX - startX);
            if (newWidth >= 50 && newWidth <= 500) {
              onResize(newWidth);
            }
          };

          const handleMouseUp = () => {
            document.removeEventListener('mousemove', handleMouseMove);
            document.removeEventListener('mouseup', handleMouseUp);
          };

          document.addEventListener('mousemove', handleMouseMove);
          document.addEventListener('mouseup', handleMouseUp);
        }}
      />
    </th>
  );
};

const ColumnHeader: React.FC<ColumnHeaderProps> = ({
  title,
  colIndex,
  onDelete,
  onMoveLeft,
  onMoveRight,
  showActions,
  columns,
}) => {
  const [hovered, setHovered] = useState(false);
  const isLeftLogTime = colIndex > 0 && columns[colIndex - 1]?.dataIndex === '_source';
  const isLast = colIndex === columns.length - 1;
  return (
    <div
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      style={{ display: 'flex', alignItems: 'center' }}
    >
      {title}
      {showActions && hovered && (
        <div className={styles.headerActions}>
          <Tooltip title="移除该列">
            <Button color="primary" variant="link" size="small" onClick={() => onDelete(colIndex)}>
              <CloseOutlined />
            </Button>
          </Tooltip>
          {colIndex > 0 && !isLeftLogTime && (
            <Tooltip title="将列左移​">
              <Button color="primary" variant="link" size="small" onClick={() => onMoveLeft(colIndex)}>
                <DoubleLeftOutlined />
              </Button>
            </Tooltip>
          )}
          {!isLast && (
            <Tooltip title="将列右移​">
              <Button color="primary" variant="link" size="small" onClick={() => onMoveRight(colIndex)}>
                <DoubleRightOutlined />
              </Button>
            </Tooltip>
          )}
        </div>
      )}
    </div>
  );
};

const VirtualTable = (props: IProps) => {
  const { data, loading = false, onLoadMore, hasMore = false, dynamicColumns, searchParams, onChangeColumns } = props;
  const containerRef = useRef<HTMLDivElement>(null);
  const tblRef: Parameters<typeof Table>[0]['ref'] = useRef(null);
  const [containerHeight, setContainerHeight] = useState<number>(0);
  const [headerHeight, setHeaderHeight] = useState<number>(0);
  const [columns, setColumns] = useState<any[]>([]);
  const [columnWidths, setColumnWidths] = useState<Record<string, number>>({});
  const [scrollX, setScrollX] = useState(1300);

  const handleResize = (index: number) => (width: number) => {
    const column = columns[index];
    if (!column?.dataIndex) return;

    setColumnWidths((prev) => ({
      ...prev,
      [column.dataIndex]: width,
    }));
  };

  const getBaseColumns = useMemo(() => {
    const otherColumns = dynamicColumns?.filter((item) => item.selected && item.columnName !== 'log_time');
    const _columns: any[] = [];
    if (otherColumns && otherColumns.length > 0) {
      otherColumns.forEach((item: ILogColumnsResponse) => {
        const { columnName = '' } = item;
        _columns.push({
          title: columnName,
          dataIndex: columnName,
          width: columnWidths[columnName] ?? 150,
          render: (text: string) => highlightText(text, searchParams?.keywords || []),
        });
      });
    }

    return [
      {
        title: 'log_time',
        dataIndex: 'log_time',
        width: 190,
        resizable: false,
        sorter: (a: any, b: any) => {
          const dateA = new Date(a.log_time).getTime();
          const dateB = new Date(b.log_time).getTime();
          return dateA - dateB;
        },
        render: (text: string) => text?.replace('T', ' '),
      },
      {
        title: '_source',
        dataIndex: '_source',
        width: undefined,
        ellipsis: false,
        hidden: _columns.length > 0,
        render: (_: any, record: ILogColumnsResponse) => {
          const { keywords = [] } = searchParams;
          const highlight = (text: string) => highlightText(text, keywords);
          return (
            <dl className={styles.source}>
              {Object.entries(record).map(([key, value]) => (
                <Fragment key={key}>
                  <dt>{key}</dt>
                  <dd>{highlight(value)}</dd>
                </Fragment>
              ))}
            </dl>
          );
        },
      },
      ..._columns.map((column) => ({
        ...column,
        sorter: (a: any, b: any) => {
          const valueA = a[column.dataIndex];
          const valueB = b[column.dataIndex];
          if (typeof valueA === 'string' && typeof valueB === 'string') {
            return valueA.localeCompare(valueB);
          }
          return (valueA || '').toString().localeCompare((valueB || '').toString());
        },
      })),
    ];
  }, [dynamicColumns, searchParams.keywords, columnWidths]);

  useEffect(() => {
    const resizableColumns = getBaseColumns.map((col, index) => ({
      ...col,
      onHeaderCell: (column: any) => ({
        width: column.width,
        onResize: handleResize(index),
      }),
    }));

    setColumns(resizableColumns);
  }, [getBaseColumns]);

  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;
    // 初始设置高度
    setContainerHeight(container.clientHeight);

    // 监听容器大小变化
    const resizeObserver = new ResizeObserver((entries) => {
      for (const entry of entries) {
        if (entry.target === container) {
          setContainerHeight(entry.contentRect.height);
        }
      }
    });

    resizeObserver.observe(container);

    // 获取表头高度
    const tableNode = tblRef.current?.nativeElement;
    if (tableNode) {
      const header = tableNode.querySelector('.ant-table-thead');
      if (header) {
        setHeaderHeight(header.clientHeight);
      }
    }

    // 添加滚动事件监听
    const handleScroll = () => {
      if (!hasMore || loading) return;

      const scrollElement = tableNode?.querySelector('.ant-table-tbody-virtual-holder');
      if (scrollElement) {
        const { scrollHeight, scrollTop, clientHeight } = scrollElement;
        const distanceToBottom = scrollHeight - scrollTop - clientHeight;
        if (distanceToBottom > 0 && distanceToBottom < 600) {
          onLoadMore();
        }
      }
    };

    if (tableNode) {
      const scrollElement = tableNode.querySelector('.ant-table-tbody-virtual-holder');
      if (scrollElement) {
        scrollElement.addEventListener('scroll', handleScroll);
      }
    }

    // 清理事件监听器和ResizeObserver
    return () => {
      resizeObserver.disconnect();
      if (tableNode) {
        const scrollElement = tableNode.querySelector('.ant-table-tbody-virtual-holder');
        if (scrollElement) {
          scrollElement.removeEventListener('scroll', handleScroll);
        }
      }
    };
  }, [containerRef.current, tblRef.current, hasMore, loading, onLoadMore]);

  // 动态计算scroll.x
  useEffect(() => {
    // 只统计动态列（不含log_time/_source）
    const dynamicCols = columns.filter((col: any) => col.dataIndex !== 'log_time' && col.dataIndex !== '_source');
    let extra = 0;
    dynamicCols.forEach((col: any) => {
      const titleStr = typeof col.title === 'string' ? col.title : col.dataIndex || '';
      extra += (titleStr.length || 0) * 15;
    });
    setScrollX(Math.max(1300, 1300 + extra));
  }, [columns]);

  // 列顺序操作
  const hasSourceColumn = columns.some((col) => col.dataIndex === '_source') && columns.length === 2;

  // 删除列
  const handleDeleteColumn = (colIndex: number) => {
    const col = columns[colIndex];
    const newCols = columns.filter((_, idx) => idx !== colIndex);
    setColumns(newCols);
    onChangeColumns(col);
    // 这里如果有 onChangeColumns 也要同步
  };
  // 左移
  const handleMoveLeft = (colIndex: number) => {
    if (colIndex <= 0) return;
    const newCols = [...columns];
    [newCols[colIndex - 1], newCols[colIndex]] = [newCols[colIndex], newCols[colIndex - 1]];
    setColumns(newCols);
  };
  // 右移
  const handleMoveRight = (colIndex: number) => {
    if (colIndex >= columns.length - 1) return;
    const newCols = [...columns];
    [newCols[colIndex], newCols[colIndex + 1]] = [newCols[colIndex + 1], newCols[colIndex]];
    setColumns(newCols);
  };

  // 包装列头
  const enhancedColumns = !hasSourceColumn
    ? columns.map((col, idx) => {
        if (col.dataIndex === 'log_time') {
          return col;
        }
        return {
          ...col,
          title: (
            <ColumnHeader
              title={col.title}
              colIndex={idx}
              onDelete={handleDeleteColumn}
              onMoveLeft={handleMoveLeft}
              onMoveRight={handleMoveRight}
              showActions={true}
              columns={columns}
            />
          ),
        };
      })
    : columns;

  return (
    <div className={styles.virtualLayout} ref={containerRef}>
      <Table
        virtual
        size="small"
        ref={tblRef}
        rowKey="_key"
        dataSource={data}
        pagination={false}
        columns={enhancedColumns}
        loading={{ spinning: loading, size: 'small' }}
        scroll={{ x: data.length > 0 ? scrollX : 0, y: containerHeight - headerHeight - 1 }}
        expandable={{
          columnWidth: 26,
          expandedRowRender: (record) => <ExpandedRow data={record} keywords={searchParams?.keywords || []} />,
        }}
        components={{
          header: {
            cell: ResizableTitle,
          },
        }}
      />
    </div>
  );
};

export default VirtualTable;
