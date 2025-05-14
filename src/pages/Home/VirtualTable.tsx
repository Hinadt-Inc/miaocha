import { useEffect, useRef, useMemo } from 'react';
import { useVirtualizer } from '@tanstack/react-virtual';
import {
  createColumnHelper,
  flexRender,
  getCoreRowModel,
  useReactTable,
} from '@tanstack/react-table';
import { Spin } from 'antd';
import styles from './VirtualTable.module.less';

interface IProps {
  data: any[];
  loading?: boolean;
  onLoadMore: () => void;
  hasMore?: boolean;
}

const VirtualTable = (props: IProps) => {
  const { data, loading = false, onLoadMore, hasMore = false } = props;
  const containerRef = useRef<HTMLDivElement>(null);
  const columnHelper = createColumnHelper<any>();

  // 定义表格列
  const columns = useMemo(
    () => [
      columnHelper.accessor('log_time', {
        header: 'log_time',
        cell: (info) => info.getValue()?.replace('T', ' '),
      }),
      columnHelper.accessor('message', {
        header: 'message',
        cell: (info) => info.getValue(),
      }),
    ],
    [],
  );

  // 初始化表格实例
  const table = useReactTable({
    data,
    columns,
    getCoreRowModel: getCoreRowModel(),
  });

  // 设置虚拟滚动
  const { rows } = table.getRowModel();
  const rowVirtualizer = useVirtualizer({
    count: rows.length,
    getScrollElement: () => containerRef.current,
    estimateSize: () => 35, // 预估的行高
    overscan: 10, // 预加载的行数
  });

  // 处理滚动加载
  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;

    const handleScroll = () => {
      if (loading || !hasMore) return;

      const { scrollTop, scrollHeight, clientHeight } = container;
      // 当滚动到距离底部50px时加载更多
      if (scrollHeight - scrollTop - clientHeight < 50) {
        onLoadMore();
      }
    };

    container.addEventListener('scroll', handleScroll);
    return () => container.removeEventListener('scroll', handleScroll);
  }, [loading, hasMore, onLoadMore]);

  return (
    <div ref={containerRef} className={styles.virtualTable}>
      <table className={styles.table}>
        <thead className={styles.tableHeader}>
          {table.getHeaderGroups().map((headerGroup) => (
            <tr key={headerGroup.id}>
              {headerGroup.headers.map((header) => (
                <th key={header.id} className={styles.headerCell}>
                  {flexRender(header.column.columnDef.header, header.getContext())}
                </th>
              ))}
            </tr>
          ))}
        </thead>
        <tbody>
          <tr>
            <td colSpan={table.getAllColumns().length}>
              <div
                style={{
                  height: `${rowVirtualizer.getTotalSize()}px`,
                  width: '100%',
                  position: 'relative',
                }}
              >
                {rowVirtualizer.getVirtualItems().map((virtualRow) => {
                  const row = rows[virtualRow.index];
                  return (
                    <div
                      key={virtualRow.index}
                      data-index={virtualRow.index}
                      ref={rowVirtualizer.measureElement}
                      className={styles.tableRow}
                      style={{
                        position: 'absolute',
                        top: 0,
                        left: 0,
                        width: '100%',
                        transform: `translateY(${virtualRow.start}px)`,
                      }}
                    >
                      {row.getVisibleCells().map((cell) => (
                        <div key={cell.id} className={styles.tableCell}>
                          {flexRender(cell.column.columnDef.cell, cell.getContext())}
                        </div>
                      ))}
                    </div>
                  );
                })}
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      {loading && (
        <div className={styles.loadingContainer}>
          <Spin />
        </div>
      )}
    </div>
  );
};

export default VirtualTable;
