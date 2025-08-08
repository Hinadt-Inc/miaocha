import React from 'react';
import { ResizableTitleProps } from '../types';
import styles from '../VirtualTable.module.less';

/**
 * 可调整大小的表格标题组件
 */
const ResizableTitle: React.FC<ResizableTitleProps> = (props) => {
  const { onResize, width, ...restProps } = props;
  
  if (!width) {
    return <th {...restProps} />;
  }

  return (
    <th {...restProps} className={styles.resizableHeader} data-width={width}>
      {restProps.children}
      <div
        className={styles.resizeHandle}
        onMouseDown={(e) => {
          // 阻止事件冒泡，防止触发排序
          e.preventDefault();
          e.stopPropagation();

          const startX = e.pageX;
          const startWidth = width;

          const handleMouseMove = (e: MouseEvent) => {
            // 计算新宽度
            // 鼠标往右拖，e.pageX - startX 为正，宽度变大。
            // 鼠标往左拖，e.pageX - startX 为负，宽度变小。
            const newWidth = startWidth + (e.pageX - startX);
            // 限制宽度
            if (newWidth >= 80 && newWidth <= 800) {
              onResize?.(newWidth);
            }
          };

          const handleMouseUp = (e: MouseEvent) => {
            // 拖拽结束时也阻止事件冒泡
            e.preventDefault();
            e.stopPropagation();
            document.removeEventListener('mousemove', handleMouseMove);
            document.removeEventListener('mouseup', handleMouseUp);
          };

          document.addEventListener('mousemove', handleMouseMove);
          document.addEventListener('mouseup', handleMouseUp);
        }}
        onClick={(e) => {
          // 点击调整手柄时阻止冒泡，防止触发排序
          e.preventDefault();
          e.stopPropagation();
        }}
      />
    </th>
  );
};

export default ResizableTitle;
