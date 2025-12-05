import React, { useState, useRef, useCallback, useMemo } from 'react';

import { IVirtualFieldListProps } from '../types';

import styles from './VirtualFieldList.module.less';

/**
 * 简化的虚拟滚动组件
 * 用于提高大量字段列表的渲染性能
 */
const VirtualFieldList: React.FC<IVirtualFieldListProps> = ({ data, itemHeight, containerHeight, renderItem }) => {
  const [scrollTop, setScrollTop] = useState(0);
  const containerRef = useRef<HTMLDivElement>(null);

  const handleScroll = useCallback((e: React.UIEvent<HTMLDivElement>) => {
    setScrollTop(e.currentTarget.scrollTop);
  }, []);

  // 计算可视范围，增大缓冲区
  const visibleRange = useMemo(() => {
    const actualHeight = containerRef.current?.clientHeight || containerHeight;
    const visibleCount = Math.ceil(actualHeight / itemHeight);
    const bufferSize = Math.max(5, Math.floor(visibleCount / 2)); // 动态缓冲区，至少5个元素

    const start = Math.floor(scrollTop / itemHeight);
    const end = Math.min(data.length, start + visibleCount + bufferSize);

    return {
      start: Math.max(0, start - bufferSize),
      end,
    };
  }, [scrollTop, itemHeight, containerHeight, data.length]);

  const visibleData = data.slice(visibleRange.start, visibleRange.end);

  return (
    <div ref={containerRef} className={styles.virtualContainer} onScroll={handleScroll}>
      {/* 总高度占位符 */}
      <div className={styles.placeholder} style={{ height: `${data.length * itemHeight}px` }}>
        {/* 可视区域内容 */}
        <div
          className={styles.visibleContent}
          style={{ transform: `translateY(${visibleRange.start * itemHeight}px)` }}
        >
          {visibleData.map((item, index) => renderItem(item, visibleRange.start + index))}
        </div>
      </div>
    </div>
  );
};

export default VirtualFieldList;
