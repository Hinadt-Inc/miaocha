import React, { useState, useRef, useEffect, useCallback } from 'react';
import { Table } from 'antd';
import type { ColumnsType } from 'antd/es/table';

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

export const VirtualList = <T extends Record<string, any>>({
  data,
  columns,
  itemHeight,
  renderItem,
  expandedRowKeys,
  onRowClick,
  activeRowKey,
  expandRowRender
}: VirtualListProps<T>) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const [visibleRange, setVisibleRange] = useState<{ start: number; end: number }>({ start: 0, end: 0 });
  const [containerHeight, setContainerHeight] = useState<number>(0);
  const [scrollTop, setScrollTop] = useState<number>(0);

  // 估计一共会渲染多少条数据（含可视区域上下的缓冲区）
  const visibleCount = Math.ceil(containerHeight / itemHeight) + 10; // 额外渲染上下各 5 条作为缓冲

  // 计算当前应该渲染哪些数据
  const calculateVisibleRange = useCallback(() => {
    if (!containerRef.current) return;
    
    const containerScrollTop = containerRef.current.scrollTop;
    setScrollTop(containerScrollTop);
    console.log('当前滚动位置:', scrollTop);
    
    const start = Math.max(0, Math.floor(containerScrollTop / itemHeight) - 5);
    const end = Math.min(data.length, start + visibleCount);
    
    setVisibleRange({ start, end });
  }, [data.length, itemHeight, visibleCount]);

  // 监听容器的滚动事件
  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;
    
    const handleScroll = () => {
      requestAnimationFrame(calculateVisibleRange);
    };
    
    container.addEventListener('scroll', handleScroll);
    calculateVisibleRange(); // 初始计算一次
    
    return () => {
      container.removeEventListener('scroll', handleScroll);
    };
  }, [calculateVisibleRange]);

  // 监听容器大小变化
  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;
    
    setContainerHeight(container.clientHeight);
    calculateVisibleRange();
    
    const resizeObserver = new ResizeObserver((entries) => {
      for (const entry of entries) {
        if (entry.target === container) {
          setContainerHeight(entry.contentRect.height);
          calculateVisibleRange();
        }
      }
    });
    
    resizeObserver.observe(container);
    
    return () => {
      resizeObserver.disconnect();
    };
  }, [calculateVisibleRange]);

  // 监听数据变化，重新计算可视范围
  useEffect(() => {
    calculateVisibleRange();
  }, [data, calculateVisibleRange]);

  // 根据是否使用表格样式，返回不同的渲染结果
  const visibleData = data.slice(visibleRange.start, visibleRange.end);
  
  return (
    <div
      ref={containerRef}
      style={{
        height: '100%',
        overflow: 'auto',
        position: 'relative',
      }}
    >
      <div
        style={{
          height: `${data.length * itemHeight}px`,
          position: 'relative'
        }}
      >
        <div
          style={{
            position: 'absolute',
            top: 0,
            left: 0,
            right: 0,
            transform: `translateY(${visibleRange.start * itemHeight}px)`,
          }}
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
              expandable={expandRowRender ? {
                expandedRowKeys: expandedRowKeys,
                expandedRowRender: expandRowRender,
                expandRowByClick: false,
              } : undefined}
              className="virtual-table"
              rowClassName={(record) => record.key === activeRowKey ? 'active-table-row' : ''}
              onRow={(record) => ({
                onClick: () => onRowClick && onRowClick(record),
                className: record.key === activeRowKey ? 'active-table-row' : ''
              })}
            />
          ) : (
            // 自定义渲染模式
            <div>
              {visibleData.map((item, index) => (
                <div
                  key={`${item.key?? index}-${visibleRange.start + index}`}
                  style={{ height: `${itemHeight}px` }}
                >
                  {renderItem ? renderItem(item) : JSON.stringify(item)}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

// 添加类型导出，帮助 TypeScript 正确识别泛型
VirtualList.displayName = 'VirtualList';
