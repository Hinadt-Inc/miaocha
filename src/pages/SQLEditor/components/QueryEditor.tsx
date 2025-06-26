import React, { memo, useState, useRef, useEffect } from 'react';
import * as monaco from 'monaco-editor';
import { Spin } from 'antd';
import { EditorSettings } from '../types';
import './QueryEditor.less';

import { initMonacoEditorLocally } from '../utils/monacoLocalInit';

interface QueryEditorProps {
  sqlQuery: string;
  onChange: (value: string | undefined) => void;
  onEditorMount?: (editor: monaco.editor.IStandaloneCodeEditor, monaco: typeof import('monaco-editor')) => void;
  editorSettings: EditorSettings;
  collapsed?: boolean;
  height?: number | string;
  resizable?: boolean; // 是否支持拖拽调整高度
  minHeight?: number; // 最小高度
  maxHeight?: number; // 最大高度
  onHeightChange?: (height: number) => void; // 高度变化回调
}

/**
 * SQL查询编辑器组件 - 完全本地版本
 * 使用Monaco编辑器提供语法高亮和自动完成功能
 * 支持收起/展开功能
 * 🚀 无CDN依赖，100%本地资源
 */
const QueryEditor: React.FC<QueryEditorProps> = ({
  sqlQuery,
  onChange,
  onEditorMount,
  editorSettings,
  collapsed = false,
  height = 300,
  resizable = true,
  minHeight = 200,
  maxHeight = 800,
  onHeightChange,
}) => {
  const [isCollapsed, setIsCollapsed] = useState(collapsed);
  const [loading, setLoading] = useState(true);
  const [monacoInitialized, setMonacoInitialized] = useState(false);
  const [currentHeight, setCurrentHeight] = useState<number>(typeof height === 'number' ? height : 300);
  const [isResizing, setIsResizing] = useState(false);
  const editorRef = useRef<monaco.editor.IStandaloneCodeEditor | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const resizeRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    setIsCollapsed(collapsed);
  }, [collapsed]);

  // 处理拖拽调整高度
  useEffect(() => {
    if (!resizable || !resizeRef.current) return;

    const resizeHandle = resizeRef.current;
    let startY: number;
    let startHeight: number;

    const handleMouseDown = (e: MouseEvent) => {
      setIsResizing(true);
      startY = e.clientY;
      startHeight = currentHeight;

      document.addEventListener('mousemove', handleMouseMove);
      document.addEventListener('mouseup', handleMouseUp);
      document.body.style.cursor = 'row-resize';
      document.body.style.userSelect = 'none';

      e.preventDefault();
    };

    const handleMouseMove = (e: MouseEvent) => {
      const deltaY = e.clientY - startY;
      const newHeight = Math.max(minHeight, Math.min(maxHeight, startHeight + deltaY));

      setCurrentHeight(newHeight);
      onHeightChange?.(newHeight);

      // 实时调整编辑器布局
      if (editorRef.current) {
        setTimeout(() => {
          editorRef.current?.layout();
        }, 0);
      }
    };

    const handleMouseUp = () => {
      setIsResizing(false);
      document.removeEventListener('mousemove', handleMouseMove);
      document.removeEventListener('mouseup', handleMouseUp);
      document.body.style.cursor = '';
      document.body.style.userSelect = '';
    };

    resizeHandle.addEventListener('mousedown', handleMouseDown);

    return () => {
      resizeHandle.removeEventListener('mousedown', handleMouseDown);
      document.removeEventListener('mousemove', handleMouseMove);
      document.removeEventListener('mouseup', handleMouseUp);
    };
  }, [resizable, currentHeight, minHeight, maxHeight, onHeightChange]);

  // 监听外部高度变化
  useEffect(() => {
    if (typeof height === 'number' && height !== currentHeight) {
      setCurrentHeight(height);
    }
  }, [height]);

  // 一次性初始化Monaco Editor
  useEffect(() => {
    if (!monacoInitialized) {
      // 🎯 使用完全本地化的Monaco初始化 - 只初始化一次
      initMonacoEditorLocally();
      setMonacoInitialized(true);
    }
  }, []); // 空依赖数组，只在组件挂载时执行一次

  // 初始化编辑器 - 完全本地化
  useEffect(() => {
    if (isCollapsed || !containerRef.current || !monacoInitialized) return;

    let isMounted = true;
    let disposables: monaco.IDisposable[] = [];

    const initEditor = () => {
      try {
        setLoading(true);

        // 🎯 直接使用已初始化的Monaco实例
        if (!window.monaco) {
          console.error('Monaco Editor 未初始化');
          setLoading(false);
          return;
        }

        // 确保容器存在且组件未卸载
        if (!containerRef.current || !isMounted) {
          console.log('容器不存在或组件已卸载，跳过编辑器创建');
          setLoading(false);
          return;
        }

        // 创建编辑器实例 - 恢复功能但保持稳定性
        const editor = window.monaco.editor.create(containerRef.current, {
          value: sqlQuery,
          language: 'sql',
          theme: editorSettings.theme || 'sqlTheme',
          automaticLayout: true,
          minimap: { enabled: editorSettings?.minimap ?? false },
          wordWrap: editorSettings?.wordWrap ? 'on' : 'off',
          fontSize: editorSettings?.fontSize ?? 14,
          tabSize: editorSettings?.tabSize ?? 2,
          // SQL编辑器专用配置
          scrollBeyondLastLine: false,
          renderWhitespace: 'none',
          contextmenu: true,
          selectOnLineNumbers: true,
          roundedSelection: false,
          readOnly: false,
          cursorStyle: 'line',
          folding: true,
          lineNumbers: 'on',
          fontFamily: '"SFMono-Regular", Consolas, "Liberation Mono", Menlo, monospace',
          // 智能提示配置 - 恢复但使用更保守的设置
          quickSuggestions: editorSettings?.autoComplete ?? true,
          suggestOnTriggerCharacters: editorSettings?.autoComplete ?? true,
          acceptSuggestionOnCommitCharacter: true,
          acceptSuggestionOnEnter: 'on',
          // 自动格式化 - 恢复但减少频率
          formatOnPaste: false, // 粘贴时不格式化，避免大文本问题
          formatOnType: false, // 输入时不格式化，减少Promise rejection
          // Hover提示 - 恢复但设置合理的延迟
          hover: {
            enabled: true,
            delay: 500, // 增加hover延迟，减少频繁触发
          },
          // 参数提示 - 恢复
          parameterHints: { enabled: true },
          // 其他有用的功能
          suggest: {
            showKeywords: true,
            showSnippets: true,
            showFunctions: true,
          },
        });

        if (!isMounted) {
          editor.dispose();
          return;
        }

        editorRef.current = editor;

        // 监听内容变化 - 添加防抖处理
        let changeTimeout: NodeJS.Timeout;

        const handleContentChange = () => {
          if (changeTimeout) {
            clearTimeout(changeTimeout);
          }
          changeTimeout = setTimeout(() => {
            if (isMounted && editorRef.current) {
              try {
                const value = editor.getValue();
                onChange(value);
              } catch (error) {
                console.warn('获取编辑器内容失败:', error);
              }
            }
          }, 100); // 减少防抖时间到100ms，提高响应性
        };

        const changeDisposable = editor.onDidChangeModelContent(handleContentChange);

        disposables.push(changeDisposable);

        // 调用挂载回调
        if (onEditorMount && isMounted) {
          try {
            onEditorMount(editor, window.monaco);
          } catch (error) {
            console.warn('编辑器挂载回调失败:', error);
          }
        }

        if (isMounted) {
          setLoading(false);
          console.log('✅ QueryEditor 完全本地初始化成功，无CDN依赖！');
        }

        // 返回清理函数
        return () => {
          if (changeTimeout) {
            clearTimeout(changeTimeout);
          }
          disposables.forEach((d) => {
            try {
              d.dispose();
            } catch (error) {
              console.warn('清理disposable失败:', error);
            }
          });
          try {
            editor.dispose();
          } catch (error) {
            console.warn('清理编辑器失败:', error);
          }
        };
      } catch (error) {
        console.error('❌ QueryEditor 初始化失败:', error);
        if (isMounted) {
          setLoading(false);
        }
      }
    };

    const cleanup = initEditor();

    // 清理函数
    return () => {
      isMounted = false;
      if (typeof cleanup === 'function') {
        cleanup();
      }
      if (editorRef.current) {
        try {
          editorRef.current.dispose();
        } catch (error) {
          console.warn('清理编辑器实例失败:', error);
        }
        editorRef.current = null;
      }
    };
  }, [isCollapsed, monacoInitialized]); // 添加monacoInitialized依赖

  // 更新编辑器内容
  useEffect(() => {
    if (editorRef.current && editorRef.current.getValue() !== sqlQuery) {
      editorRef.current.setValue(sqlQuery);
    }
  }, [sqlQuery]);

  // 设置CSS变量
  useEffect(() => {
    if (containerRef.current?.parentElement) {
      const wrapper = containerRef.current.parentElement.parentElement;
      if (wrapper) {
        wrapper.style.setProperty('--editor-height', `${currentHeight}px`);
      }
    }
  }, [currentHeight]);

  // 如果折叠，返回简单视图
  if (isCollapsed) {
    return (
      <div className="editor-container collapsed">
        <div className="editor-wrapper collapsed" />
      </div>
    );
  }

  return (
    <div className={`editor-container ${isResizing ? 'resizing' : ''}`}>
      <div className="editor-wrapper">
        {loading && (
          <div className="editor-loading">
            <Spin size="large" tip="正在加载编辑器..." />
          </div>
        )}
        <div
          ref={containerRef}
          className={`monaco-editor-container ${loading ? 'loading' : ''}`}
          data-height={currentHeight}
        />
      </div>
    </div>
  );
};

export default memo(QueryEditor);
