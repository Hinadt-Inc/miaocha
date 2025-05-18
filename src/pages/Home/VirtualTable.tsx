import { useEffect, useRef, useMemo } from 'react';
import { useVirtualizer } from '@tanstack/react-virtual';
import { createColumnHelper, flexRender, getCoreRowModel, useReactTable } from '@tanstack/react-table';
import { Spin } from 'antd';
import styles from './VirtualTable.module.less';

interface IProps {
  data: any[]; // 数据
  loading?: boolean; // 加载状态
  onLoadMore: () => void; // 加载更多数据的回调函数
  hasMore?: boolean; // 是否还有更多数据
  dynamicColumns?: { columnName: string; selected: boolean }[]; // 动态列配置
}

const VirtualTable = (props: IProps) => {
  const { data, loading = false, onLoadMore, hasMore = false, dynamicColumns = [] } = props;
  const containerRef = useRef<HTMLDivElement>(null); // 滚动容器的ref
  const columnHelper = createColumnHelper<any>(); // 列辅助函数

  // 定义表格列
  const columns = useMemo(
    () => {
      // 始终显示log_time列作为第一列
      const logTimeColumn = columnHelper.accessor('log_time', {
        header: 'log_time', // 表头
        cell: (info) => info.getValue()?.replace('T', ' '), // 单元格渲染函数
      });

      // 添加动态列（除了log_time，因为它已经作为固定列添加）
      const additionalColumns = dynamicColumns
        .filter((col) => col.selected && col.columnName !== 'log_time' && col.columnName !== '_source')
        .map((col) =>
          columnHelper.accessor(col.columnName, {
            header: col.columnName,
            cell: (info) => info.getValue(),
          }),
        );

      // 检查是否只有log_time列被选中
      const hasOtherSelected = dynamicColumns.some(
        (col) => col.selected && col.columnName !== 'log_time' && col.columnName !== '_source',
      );
      const columns = [logTimeColumn];

      // 如果没有其他列被选中，添加_source列显示整行数据
      if (!hasOtherSelected && dynamicColumns.length > 0) {
        columns.push(
          columnHelper.accessor((row) => row, {
            header: '_source',
            cell: (info) => {
              const rowData = info.row.original;
              return rowData ? JSON.stringify(rowData) : '';
            },
          }) as any,
        );
      }

      return [...columns, ...additionalColumns];
    },
    [dynamicColumns], // 依赖项添加dynamicColumns，当动态列变化时重新计算
  );

  // 初始化表格实例
  const table = useReactTable({
    data, // 数据
    columns, // 列
    getCoreRowModel: getCoreRowModel(), // 负责把原始数据(raw data)转换成表格能直接使用的格式
  });

  // 设置虚拟滚动
  const { rows } = table.getRowModel(); // 将原始数据转换为表格可用的行结构
  const rowVirtualizer = useVirtualizer({
    count: rows.length, // 总行数
    getScrollElement: () => containerRef.current, // 滚动容器
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
    <div ref={containerRef} className={styles.virtualLayout}>
      <Spin spinning={loading}>
        <table className={styles.table}>
          <thead className={styles.tableHeader}>
            {/* 获取表格的表 */}
            {table.getHeaderGroups().map((item) => (
              <tr key={item.id}>
                {item.headers.map((sub) => (
                  <th key={sub.id} className={styles.headerCell}>
                    {flexRender(sub.column.columnDef.header, sub.getContext())}
                  </th>
                ))}
              </tr>
            ))}
          </thead>
          <tbody
            style={{
              height: `${rowVirtualizer.getTotalSize()}px`,
              position: 'relative',
            }}
          >
            {rowVirtualizer.getVirtualItems().map((item) => {
              const row = rows[item.index];
              return (
                <tr
                  key={item.index}
                  data-index={item.index}
                  ref={rowVirtualizer.measureElement}
                  className={styles.tableRow}
                  style={{
                    position: 'absolute',
                    top: 0,
                    left: 0,
                    width: '100%',
                    transform: `translateY(${item.start}px)`,
                  }}
                >
                  {row.getVisibleCells().map((cell) => (
                    <td key={`${item.index}_${cell.id}`} className={styles.tableCell}>
                      {flexRender(cell.column.columnDef.cell, cell.getContext())}
                    </td>
                  ))}
                </tr>
              );
            })}
          </tbody>
        </table>
      </Spin>
    </div>
  );
};

export default VirtualTable;
