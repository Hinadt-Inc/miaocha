import React, { memo, useState, useRef, useEffect, useCallback } from 'react';
import Editor, { OnMount } from '@monaco-editor/react';
import * as monaco from 'monaco-editor';
import { Spin, Button, Tooltip } from 'antd';
import { UpOutlined, DownOutlined } from '@ant-design/icons';
import { EditorSettings } from '../types';
import './QueryEditor.less';

interface QueryEditorProps {
  sqlQuery: string;
  onChange: (value: string | undefined) => void;
  onEditorMount: OnMount;
  editorSettings: EditorSettings;
  height?: number;
  minHeight?: number;
  maxHeight?: number;
  collapsed?: boolean;
  onCollapsedChange?: (collapsed: boolean) => void;
  onHeightChange?: (height: number) => void;
}

/**
 * SQL查询编辑器组件
 * 使用Monaco编辑器提供语法高亮和自动完成功能
 * 支持调整高度和收起/展开功能
 */
const QueryEditor: React.FC<QueryEditorProps> = ({
  sqlQuery,
  onChange,
  onEditorMount,
  editorSettings,
  height = 200,
  minHeight = 100,
  maxHeight = 800,
  collapsed = false,
  onCollapsedChange,
  onHeightChange,
}) => {
  // 状态 - 确保初始高度不小于最小高度
  const initialHeight = Math.max(height, minHeight);
  const [currentHeight, setCurrentHeight] = useState(initialHeight);
  const [isCollapsed, setIsCollapsed] = useState(collapsed);
  const [isDragging, setIsDragging] = useState(false);

  // refs
  const editorRef = useRef<monaco.editor.IStandaloneCodeEditor | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const editorContainerRef = useRef<HTMLDivElement>(null);
  // 使用ref保存状态，避免拖拽过程中的状态更新导致的重渲染
  const heightRef = useRef(initialHeight);

  // 更新ref中保存的高度值
  useEffect(() => {
    heightRef.current = currentHeight;
  }, [currentHeight]);

  // 组件挂载时，强制设置初始高度
  useEffect(() => {
    if (!isCollapsed && containerRef.current) {
      // 安全检查：确保高度不为0
      const safeHeight = Math.max(initialHeight, minHeight, 100);
      setCurrentHeight(safeHeight);
      heightRef.current = safeHeight;

      // 延迟执行布局更新，确保DOM已经渲染
      setTimeout(() => {
        if (editorRef.current) {
          editorRef.current.layout();
        }
      }, 100);
    }
  }, []);

  // 同步外部传入的高度和折叠状态
  useEffect(() => {
    if (!isCollapsed && !isDragging && height !== currentHeight) {
      const newHeight = Math.max(height, minHeight);
      setCurrentHeight(newHeight);
      heightRef.current = newHeight;
    }
  }, [height, isCollapsed, currentHeight, minHeight, isDragging]);

  useEffect(() => {
    setIsCollapsed(collapsed);
  }, [collapsed]);

  // 更新编辑器布局 - 使用useCallback减少函数创建
  const updateEditorLayout = useCallback(() => {
    if (editorRef.current && !isCollapsed) {
      try {
        editorRef.current.layout();
      } catch (error) {
        console.error('编辑器布局更新失败', error);
      }
    }
  }, [isCollapsed]);

  // 处理收起/展开
  const handleToggleCollapse = useCallback(() => {
    const newCollapsedState = !isCollapsed;
    setIsCollapsed(newCollapsedState);

    if (!newCollapsedState) {
      // 确保从收起状态恢复时有一个合理的高度
      const restoreHeight = Math.max(heightRef.current, minHeight);
      setCurrentHeight(restoreHeight);
      heightRef.current = restoreHeight;

      // 从收起状态恢复时，确保在下一个事件循环中更新布局
      setTimeout(updateEditorLayout, 100);
    }

    if (onCollapsedChange) {
      onCollapsedChange(newCollapsedState);
    }
  }, [isCollapsed, minHeight, onCollapsedChange, updateEditorLayout]);

  // 处理编辑器挂载
  const handleEditorDidMount: OnMount = useCallback(
    (editor, monacoInstance) => {
      editorRef.current = editor;

      // 确保编辑器在挂载后正确布局
      setTimeout(() => {
        editor.layout();
      }, 100);

      if (onEditorMount) {
        onEditorMount(editor, monacoInstance);
      }
    },
    [onEditorMount],
  );

  // 拖动开始
  const handleDragStart = useCallback(
    (e: React.MouseEvent) => {
      e.preventDefault();

      // 防止重复触发
      if (isDragging) return;

      // 开始拖动状态
      setIsDragging(true);
      document.body.style.cursor = 'row-resize';

      // 获取初始位置
      const startY = e.clientY;
      const startHeight = heightRef.current;

      // 添加阻止默认选中文本的样式
      document.body.classList.add('no-select');

      // 拖动中处理函数
      const handleDragMove = (moveEvent: MouseEvent) => {
        moveEvent.preventDefault();

        // 计算高度变化
        const deltaY = moveEvent.clientY - startY;
        const newHeight = Math.max(minHeight, Math.min(maxHeight, startHeight + deltaY));

        // 直接更新DOM元素样式，避免React重渲染
        if (editorContainerRef.current) {
          editorContainerRef.current.style.height = `${newHeight}px`;
        }

        // 更新ref中的高度值，但不触发状态更新
        heightRef.current = newHeight;

        // 实时更新编辑器布局
        if (editorRef.current) {
          editorRef.current.layout();
        }
      };

      // 拖动结束处理函数
      const handleDragEnd = () => {
        // 移除事件监听
        document.removeEventListener('mousemove', handleDragMove);
        document.removeEventListener('mouseup', handleDragEnd);

        // 恢复默认鼠标样式和文本选择
        document.body.style.cursor = '';
        document.body.classList.remove('no-select');

        // 结束拖动状态
        setIsDragging(false);

        // 使用setTimeout确保状态更新在拖动结束后进行
        setTimeout(() => {
          // 更新React状态
          setCurrentHeight(heightRef.current);

          // 通知父组件
          if (onHeightChange) {
            onHeightChange(heightRef.current);
          }

          // 再次确保编辑器布局更新
          updateEditorLayout();
        }, 10);
      };

      // 添加全局事件监听
      document.addEventListener('mousemove', handleDragMove);
      document.addEventListener('mouseup', handleDragEnd);
    },
    [isDragging, minHeight, maxHeight, onHeightChange, updateEditorLayout],
  );

  // 如果折叠，则返回一个简单的视图
  if (isCollapsed) {
    return (
      <div className="editor-container collapsed">
        <div className="editor-wrapper collapsed" />

        {/* 收起/展开按钮 */}
        <div className="editor-collapse-button">
          <Tooltip title="展开编辑器">
            <Button type="text" icon={<DownOutlined />} onClick={handleToggleCollapse} size="small" />
          </Tooltip>
        </div>
      </div>
    );
  }

  return (
    <div className="editor-container" ref={containerRef}>
      {/* 直接使用固定高度的容器，避免高度计算问题 */}
      <div
        ref={editorContainerRef}
        style={{
          position: 'relative',
          height: `${currentHeight}px`,
          minHeight: `${minHeight}px`,
          border: '1px solid #e8e8e8',
          borderRadius: '2px',
          overflow: 'hidden',
        }}
      >
        <Editor
          height="100%"
          language="sql"
          value={sqlQuery}
          onChange={onChange}
          onMount={handleEditorDidMount}
          loading={<Spin tip="加载编辑器..." />}
          theme={editorSettings.theme}
          options={{
            minimap: { enabled: editorSettings.minimap },
            scrollBeyondLastLine: false,
            folding: true,
            lineNumbers: 'on',
            wordWrap: editorSettings.wordWrap ? 'on' : 'off',
            automaticLayout: true,
            fontFamily: '"SFMono-Regular", Consolas, "Liberation Mono", Menlo, monospace',
            fontSize: editorSettings.fontSize,
            tabSize: editorSettings.tabSize,
            quickSuggestions: editorSettings.autoComplete,
            suggestOnTriggerCharacters: editorSettings.autoComplete,
          }}
        />
      </div>

      {/* 拖动调整大小区域 */}
      <div className={`editor-resize-handle ${isDragging ? 'dragging' : ''}`} onMouseDown={handleDragStart}>
        <span className="resize-handle-text">拖动调整高度</span>
      </div>

      {/* 收起/展开按钮 */}
      <div className="editor-collapse-button">
        <Tooltip title="收起编辑器">
          <Button type="text" icon={<UpOutlined />} onClick={handleToggleCollapse} size="small" />
        </Tooltip>
      </div>
    </div>
  );
};

export default memo(QueryEditor);
