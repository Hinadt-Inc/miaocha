import { useState, useCallback } from 'react';

/**
 * 编辑器布局管理Hook
 * 管理侧边栏、分割器等布局相关状态
 */
export const useEditorLayout = () => {
  // 侧边栏状态
  const [siderCollapsed, setSiderCollapsed] = useState(false);
  const siderWidth = siderCollapsed ? 80 : 250;

  // 分割器拖动事件处理
  const handleSplitterDrag = useCallback((sizes: number[]) => {
    // 这里可以添加分割器拖动的逻辑
    console.log('Splitter resized:', sizes);
  }, []);

  // 切换侧边栏
  const toggleSider = useCallback(() => {
    setSiderCollapsed((prev) => !prev);
  }, []);

  return {
    siderWidth,
    siderCollapsed,
    setSiderCollapsed,
    handleSplitterDrag,
    toggleSider,
  };
};
