import React, { useState, useRef, useEffect, useCallback, useImperativeHandle, forwardRef } from 'react';
import { Table } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import styles from './VirtualList.module.less';

interface VirtualListProps<T> {
  data: T[];
  columns?: ColumnsType<T>;
  itemHeight: number;
  renderItem?: (item: T) => React.ReactNode;
  expandedRowKeys?: React.Key[];
  onRowClick?: (record: T) => void;
  activeRowKey?: string | null;
  expandRowRender?: (record: T) => React.ReactNode;
}

export interface VirtualListRef {
  scrollToBottom: () => void;
}

export const VirtualList = forwardRef(
  <T extends Record<string, any>>(
    {
      data,
      columns,
      itemHeight,
      renderItem,
      expandedRowKeys,
      onRowClick,
      activeRowKey,
      expandRowRender,
    }: VirtualListProps<T>,
    ref: React.Ref<VirtualListRef>,
  ) => {
    const containerRef = useRef<HTMLDivElement>(null);
    const [visibleRange, setVisibleRange] = useState<{ start: number; end: number }>({ start: 0, end: 20 }); // 初始显示前20条
    const [containerHeight, setContainerHeight] = useState<number>(0);

    // 计算当前应该渲染哪些数据
    const calculateVisibleRange = useCallback(() => {
      if (!containerRef.current) return;

      // 查找实际的滚动容器
      const scrollContainer = containerRef.current.parentElement?.classList.contains('scrollableWrapper')
        ? containerRef.current.parentElement
        : containerRef.current;

      const containerScrollTop = scrollContainer?.scrollTop || 0;
      // setScrollTop(containerScrollTop); // 移除未使用的状态更新

      // 重新计算当前的可视数量
      const currentVisibleCount = containerHeight > 0 ? Math.ceil(containerHeight / itemHeight) + 10 : 50;

      console.log('当前滚动位置:', containerScrollTop, '容器高度:', containerHeight, '可视数量:', currentVisibleCount);

      const start = Math.max(0, Math.floor(containerScrollTop / itemHeight) - 5);
      const end = Math.min(data.length, start + currentVisibleCount);

      console.log('可视范围:', { start, end, total: data.length });
      setVisibleRange({ start, end });
    }, [data.length, itemHeight, containerHeight]); // 移除 visibleCount 依赖

    // 监听容器的滚动事件
    useEffect(() => {
      const container = containerRef.current;
      if (!container) return;

      // 查找实际的滚动容器
      const scrollContainer = container.parentElement?.classList.contains('scrollableWrapper')
        ? container.parentElement
        : container;

      const handleScroll = () => {
        requestAnimationFrame(calculateVisibleRange);
      };

      if (scrollContainer) {
        scrollContainer.addEventListener('scroll', handleScroll);
        calculateVisibleRange(); // 初始计算一次

        return () => {
          scrollContainer.removeEventListener('scroll', handleScroll);
        };
      }
    }, [calculateVisibleRange]);

    // 监听容器大小变化
    useEffect(() => {
      const container = containerRef.current;
      if (!container) return;

      // 延迟查找滚动容器，确保DOM已完全渲染
      const findScrollContainerAndSetup = () => {
        const scrollContainer = container.parentElement?.classList.contains('scrollableWrapper')
          ? container.parentElement
          : container;

        const updateContainerHeight = () => {
          if (scrollContainer) {
            const height = scrollContainer.clientHeight;
            console.log('容器高度更新:', height);
            setContainerHeight(height);

            // 立即重新计算可视范围
            if (height > 0) {
              const currentVisibleCount = Math.ceil(height / itemHeight) + 10;
              const newEnd = Math.min(data.length, currentVisibleCount);
              setVisibleRange((prev) => ({ start: prev.start, end: Math.max(prev.end, newEnd) }));
            }

            calculateVisibleRange();
          }
        };

        updateContainerHeight();

        const resizeObserver = new ResizeObserver((entries) => {
          for (const entry of entries) {
            if (entry.target === scrollContainer) {
              const height = entry.contentRect.height;
              console.log('ResizeObserver 容器高度:', height);
              setContainerHeight(height);
              calculateVisibleRange();
            }
          }
        });

        if (scrollContainer) {
          resizeObserver.observe(scrollContainer);
        }

        return () => {
          resizeObserver.disconnect();
        };
      };

      // 稍微延迟确保组件完全挂载
      const timer = setTimeout(findScrollContainerAndSetup, 100);

      return () => {
        clearTimeout(timer);
      };
    }, [calculateVisibleRange, itemHeight, data.length]);

    // 监听数据变化，重新计算可视范围
    useEffect(() => {
      // 确保有数据时重新计算
      if (data.length > 0) {
        // 如果还没有设置正确的可视范围，立即设置
        if (visibleRange.end === 0 || visibleRange.end < 20) {
          const defaultEnd = Math.min(data.length, 50);
          setVisibleRange({ start: 0, end: defaultEnd });
          console.log('初始设置可视范围:', { start: 0, end: defaultEnd });
        }

        setTimeout(() => {
          calculateVisibleRange();
        }, 50); // 稍微延迟确保DOM已更新
      }
    }, [data, calculateVisibleRange, visibleRange.end]);

    // 初始化时确保计算一次
    useEffect(() => {
      const timer = setTimeout(() => {
        calculateVisibleRange();
      }, 100);

      return () => clearTimeout(timer);
    }, [calculateVisibleRange]);

    // 根据是否使用表格样式，返回不同的渲染结果
    const visibleData = data.slice(visibleRange.start, visibleRange.end);

    console.log('渲染数据:', {
      total: data.length,
      visibleRange,
      visibleDataLength: visibleData.length,
      containerHeight,
      itemHeight,
    });

    useImperativeHandle(ref, () => ({
      scrollToBottom: () => {
        // 查找父级滚动容器
        const container = containerRef.current?.parentElement;
        if (container?.classList.contains('scrollableWrapper')) {
          container.scrollTop = container.scrollHeight;
        } else if (containerRef.current) {
          // 回退到原来的方式
          containerRef.current.scrollTop = containerRef.current.scrollHeight;
        }
        // 强制刷新可视区域
        setTimeout(() => {
          calculateVisibleRange();
        }, 0);
      },
    }));

    return (
      <div ref={containerRef} className={styles.virtualListContainer}>
        {/* 占位容器，撑开正确的滚动高度 */}
        <div className={styles.virtualListSpacer} style={{ height: data.length * itemHeight }}>
          <div
            className={styles.virtualListVisible}
            style={{ transform: `translateY(${visibleRange.start * itemHeight}px)` }}
          >
            {columns ? (
              // 表格模式
              <Table
                dataSource={visibleData}
                columns={columns}
                pagination={false}
                size="middle"
                scroll={{ x: 'max-content' }}
                rowKey="key"
                expandable={
                  expandRowRender
                    ? {
                        expandedRowKeys: expandedRowKeys,
                        expandedRowRender: expandRowRender,
                        expandRowByClick: false,
                      }
                    : undefined
                }
                className="virtual-table"
                rowClassName={(record) => (record.key === activeRowKey ? 'active-table-row' : '')}
                onRow={(record) => ({
                  onClick: () => onRowClick && onRowClick(record),
                  className: record.key === activeRowKey ? 'active-table-row' : '',
                })}
              />
            ) : (
              // 自定义渲染模式
              <div>
                {visibleData.length > 0
                  ? visibleData.map((item, index) => (
                      <div key={`${item.key ?? index}-${visibleRange.start + index}`}>
                        {renderItem ? renderItem(item) : JSON.stringify(item)}
                      </div>
                    ))
                  : // 如果可视数据为空但总数据不为空，显示调试信息
                    data.length > 0 && (
                      <div style={{ color: 'red', padding: '10px' }}>
                        调试信息: 总数据 {data.length} 条，可视范围 {visibleRange.start}-{visibleRange.end}，容器高度{' '}
                        {containerHeight}
                      </div>
                    )}
              </div>
            )}
          </div>
        </div>
      </div>
    );
  },
);

// 添加类型导出，帮助 TypeScript 正确识别泛型
VirtualList.displayName = 'VirtualList';
