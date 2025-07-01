import { useEffect, useRef, useState, useCallback } from 'react';

interface UseSplitterSizeOptions {
  /**
   * 编辑器头部和操作区域的总高度
   * 包括Card标题、工具栏等元素的高度
   */
  reservedHeight?: number;
  /**
   * 最小高度
   */
  minHeight?: number;
  /**
   * 最大高度
   */
  maxHeight?: number;
  /**
   * 初始高度
   */
  initialHeight?: number;
}

/**
 * 用于监听Splitter面板尺寸变化并自动调整编辑器高度的Hook
 *
 * @param options 配置选项
 * @returns { containerRef, editorHeight, updateHeight }
 */
export const useSplitterSize = (options: UseSplitterSizeOptions = {}) => {
  const {
    reservedHeight = 120, // Card头部 + 工具栏 + 边距等
    minHeight = 200,
    maxHeight = 800,
    initialHeight = 300,
  } = options;

  const containerRef = useRef<HTMLDivElement>(null);
  const [editorHeight, setEditorHeight] = useState<number>(initialHeight);
  const resizeObserverRef = useRef<ResizeObserver | null>(null);

  /**
   * 计算编辑器合适的高度
   */
  const calculateEditorHeight = useCallback(
    (containerHeight: number): number => {
      const availableHeight = containerHeight - reservedHeight;
      return Math.max(minHeight, Math.min(maxHeight, availableHeight));
    },
    [reservedHeight, minHeight, maxHeight],
  );

  /**
   * 手动更新高度（用于外部调用）
   */
  const updateHeight = useCallback(() => {
    if (!containerRef.current) return;

    const containerHeight = containerRef.current.clientHeight;
    const newHeight = calculateEditorHeight(containerHeight);

    if (newHeight !== editorHeight) {
      setEditorHeight(newHeight);
    }
  }, [calculateEditorHeight, editorHeight]);

  /**
   * 设置ResizeObserver监听容器尺寸变化
   */
  useEffect(() => {
    if (!containerRef.current) return;

    // 创建ResizeObserver实例
    resizeObserverRef.current = new ResizeObserver((entries) => {
      for (const entry of entries) {
        const { height } = entry.contentRect;
        const newHeight = calculateEditorHeight(height);

        // 只有高度真正变化时才更新状态
        setEditorHeight((prevHeight) => {
          if (Math.abs(prevHeight - newHeight) > 5) {
            // 5px的阈值，避免微小变化
            return newHeight;
          }
          return prevHeight;
        });
      }
    });

    // 开始观察容器
    resizeObserverRef.current.observe(containerRef.current);

    // 初始化时计算一次高度
    const initialContainerHeight = containerRef.current.clientHeight;
    if (initialContainerHeight > 0) {
      const newHeight = calculateEditorHeight(initialContainerHeight);
      setEditorHeight(newHeight);
    }

    return () => {
      if (resizeObserverRef.current) {
        resizeObserverRef.current.disconnect();
        resizeObserverRef.current = null;
      }
    };
  }, [calculateEditorHeight]);

  /**
   * 监听窗口尺寸变化（作为ResizeObserver的补充）
   */
  useEffect(() => {
    const handleResize = () => {
      // 延迟执行，确保布局已经更新
      setTimeout(updateHeight, 100);
    };

    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, [updateHeight]);

  return {
    /**
     * 需要绑定到Splitter.Panel或其直接子容器的ref
     */
    containerRef,
    /**
     * 计算得出的编辑器高度
     */
    editorHeight,
    /**
     * 手动触发高度重新计算
     */
    updateHeight,
  };
};
