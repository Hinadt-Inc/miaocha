import React, { memo, useState, useRef, useEffect, useCallback } from 'react';
import Editor, { OnMount } from '@monaco-editor/react';
import * as monaco from 'monaco-editor';
import { Spin, Button, Tooltip } from 'antd';
import { UpOutlined, DownOutlined, DragOutlined } from '@ant-design/icons';
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
  height = 300,
  minHeight = 100,
  maxHeight = 800,
  collapsed = false,
  onCollapsedChange,
  onHeightChange
}) => {
  const [currentHeight, setCurrentHeight] = useState(height);
  const [isCollapsed, setIsCollapsed] = useState(collapsed);
  const [isDragging, setIsDragging] = useState(false);
  const editorContainerRef = useRef<HTMLDivElement>(null);
  const editorWrapperRef = useRef<HTMLDivElement>(null);
  const resizeRef = useRef<HTMLDivElement>(null);
  const startYRef = useRef(0);
  const startHeightRef = useRef(0);
  const editorInstanceRef = useRef<monaco.editor.IStandaloneCodeEditor | null>(null);

  // 同步外部传入的高度和collapsed状态
  useEffect(() => {
    if (!isCollapsed) {
      updateEditorHeight(height);
    }
  }, [height, isCollapsed]);
  
  useEffect(() => {
    setIsCollapsed(collapsed);
  }, [collapsed]);

  // 直接更新DOM元素高度，避免React渲染周期的干扰
  const updateEditorHeight = useCallback((newHeight: number) => {
    setCurrentHeight(newHeight);
    
    // 直接设置DOM元素高度，避免CSS变量可能的问题
    if (editorWrapperRef.current) {
      editorWrapperRef.current.style.height = `${newHeight}px`;
    }
    
    // 通知Monaco编辑器布局变化
    if (editorInstanceRef.current) {
      try {
        // 使用setTimeout确保高度变化后再调整布局
        setTimeout(() => {
          editorInstanceRef.current?.layout();
        }, 10);
      } catch (e) {
        console.error('布局更新失败:', e);
      }
    }
    
    // 通知父组件高度变化
    if (onHeightChange) {
      onHeightChange(newHeight);
    }
  }, [onHeightChange]);

  // 处理拖拽开始 - 使用mousedown原生事件而非React合成事件
  const setupResizeHandle = useCallback(() => {
    const resizeHandle = resizeRef.current;
    if (!resizeHandle) return;
    
    const handleMouseDown = (e: MouseEvent) => {
      console.log('Native mousedown triggered');
      e.preventDefault();
      e.stopPropagation();
      
      document.body.style.cursor = 'row-resize';
      setIsDragging(true);
      startYRef.current = e.clientY;
      startHeightRef.current = currentHeight;
      
      // 添加事件监听 - 在document上监听，确保不会丢失事件
      document.addEventListener('mousemove', handleMouseMove);
      document.addEventListener('mouseup', handleMouseUp);
    };
    
    // 将监听器直接添加到DOM元素，避免React合成事件系统
    resizeHandle.addEventListener('mousedown', handleMouseDown as EventListener);
    
    return () => {
      resizeHandle.removeEventListener('mousedown', handleMouseDown as EventListener);
    };
  }, [currentHeight]); // 只依赖currentHeight，避免频繁重建
  
  // 在组件挂载后设置拖拽处理
  useEffect(() => {
    const cleanup = setupResizeHandle();
    return cleanup;
  }, [setupResizeHandle]);
  
  // 处理拖拽移动 - 定义在组件外部，避免重建
  const handleMouseMove = (e: MouseEvent) => {
    if (!isDragging) return;
    
    // 计算鼠标移动的垂直距离
    const deltaY = e.clientY - startYRef.current;
    // 根据拖拽移动距离计算新的高度，并确保在最小和最大高度范围内
    const newHeight = Math.max(minHeight, Math.min(maxHeight, startHeightRef.current + deltaY));
    
    // 直接更新DOM元素高度
    updateEditorHeight(newHeight);
    
    // 阻止事件进一步传播
    e.preventDefault();
    e.stopPropagation();
  };
  
  // 处理拖拽结束
  const handleMouseUp = (e: MouseEvent) => {
    document.body.style.cursor = '';
    setIsDragging(false);
    document.removeEventListener('mousemove', handleMouseMove);
    document.removeEventListener('mouseup', handleMouseUp);
    
    // 阻止事件进一步传播
    e.preventDefault();
    e.stopPropagation();
  };
  
  // 处理收起/展开
  const toggleCollapse = () => {
    const newCollapsedState = !isCollapsed;
    setIsCollapsed(newCollapsedState);
    
    if (!newCollapsedState && editorWrapperRef.current) {
      // 从收起状态恢复时，确保高度正确设置
      setTimeout(() => updateEditorHeight(currentHeight), 50);
    }
    
    if (onCollapsedChange) {
      onCollapsedChange(newCollapsedState);
    }
  };

  // 保存Monaco编辑器实例
  const handleEditorDidMount: OnMount = (editor, monaco) => {
    editorInstanceRef.current = editor;
    
    // 调用原始的onEditorMount
    if (onEditorMount) {
      onEditorMount(editor, monaco);
    }
  };

  return (
    <div className={`editor-container ${isCollapsed ? 'collapsed' : ''}`} ref={editorContainerRef}>
      <div 
        className={`editor-wrapper ${isCollapsed ? 'collapsed' : ''}`}
        ref={editorWrapperRef}
      >
        <Editor
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
            automaticLayout: true, // 保留automaticLayout以支持宽度变化
            fontFamily: '"SFMono-Regular", Consolas, "Liberation Mono", Menlo, monospace',
            fontSize: editorSettings.fontSize,
            tabSize: editorSettings.tabSize,
            quickSuggestions: editorSettings.autoComplete,
            suggestOnTriggerCharacters: editorSettings.autoComplete
          }}
        />
      </div>
      
      {/* 调整大小控制条 */}
      <div 
        className={`editor-resize-handle ${isDragging ? 'dragging' : ''} ${isCollapsed ? 'hidden' : ''}`}
        ref={resizeRef}
      >
        <DragOutlined />
        <span className="resize-handle-text">拖动调整高度</span>
      </div>
      
      {/* 收起/展开按钮 */}
      <div className="editor-collapse-button">
        <Tooltip title={isCollapsed ? "展开编辑器" : "收起编辑器"}>
          <Button 
            type="text" 
            icon={isCollapsed ? <DownOutlined /> : <UpOutlined />} 
            onClick={toggleCollapse}
            size="small"
          />
        </Tooltip>
      </div>
    </div>
  );
};

// 使用 memo 避免不必要的重渲染
export default memo(QueryEditor);
