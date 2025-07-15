import React, { memo, useState, useRef, useEffect } from 'react';
import * as monaco from 'monaco-editor';
import { Spin } from 'antd';
import { EditorSettings } from '../types';
import styles from './QueryEditor.module.less';

import { initMonacoEditorLocally } from '../utils/monacoLocalInit';

interface QueryEditorProps {
  sqlQuery: string;
  onChange: (value: string | undefined) => void;
  onEditorMount?: (editor: monaco.editor.IStandaloneCodeEditor, monaco: typeof import('monaco-editor')) => void;
  editorSettings: EditorSettings;
  collapsed?: boolean;
}

/**
 * SQL查询编辑器组件 - 完全本地版本，简化高度管理
 * 使用Monaco编辑器提供语法高亮和自动完成功能
 * 支持收起/展开功能
 * 🚀 无CDN依赖，100%本地资源，简单的100%高度
 */
const QueryEditor: React.FC<QueryEditorProps> = ({
  sqlQuery,
  onChange,
  onEditorMount,
  editorSettings,
  collapsed = false,
}) => {
  const [isCollapsed, setIsCollapsed] = useState(collapsed);
  const [loading, setLoading] = useState(true);
  const [monacoInitialized, setMonacoInitialized] = useState(false);
  const editorRef = useRef<monaco.editor.IStandaloneCodeEditor | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const resizeObserverRef = useRef<ResizeObserver | null>(null);

  useEffect(() => {
    setIsCollapsed(collapsed);
  }, [collapsed]);

  // 监听容器大小变化并更新编辑器布局
  useEffect(() => {
    if (!containerRef.current || !editorRef.current) return;

    // 创建ResizeObserver来监听容器大小变化
    const resizeObserver = new ResizeObserver((entries) => {
      for (const entry of entries) {
        if (editorRef.current && entry.target === containerRef.current) {
          // 使用requestAnimationFrame确保DOM更新完成后再调用layout
          requestAnimationFrame(() => {
            if (editorRef.current) {
              editorRef.current.layout();
              // 额外触发一次layout，确保在容器收缩时也能正确更新
              setTimeout(() => {
                if (editorRef.current) {
                  editorRef.current.layout();
                }
              }, 50);
            }
          });
        }
      }
    });

    resizeObserver.observe(containerRef.current);
    resizeObserverRef.current = resizeObserver;

    return () => {
      if (resizeObserverRef.current) {
        resizeObserverRef.current.disconnect();
        resizeObserverRef.current = null;
      }
    };
  }, [editorRef.current, containerRef.current]);

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
          automaticLayout: true, // 保持自动布局
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
          // 添加滚动条配置，确保编辑器能正确处理容器大小变化
          scrollbar: {
            verticalScrollbarSize: 8,
            horizontalScrollbarSize: 8,
            alwaysConsumeMouseWheel: false,
          },
          // 优化性能
          renderLineHighlight: 'line',
          renderControlCharacters: false,
          disableLayerHinting: false,
          disableMonospaceOptimizations: false,
        });

        if (!isMounted) {
          editor.dispose();
          return;
        }

        editorRef.current = editor;

        // 监听内容变化 - 添加防抖处理并避免循环更新
        let changeTimeout: NodeJS.Timeout;

        const handleContentChange = () => {
          // 如果是程序更新内容或正在从外部状态更新，跳过onChange回调
          if (isUpdatingFromState.current) {
            return;
          }

          if (changeTimeout) {
            clearTimeout(changeTimeout);
          }
          changeTimeout = setTimeout(() => {
            if (isMounted && editorRef.current && !isUpdatingFromState.current) {
              try {
                const value = editor.getValue();
                // 避免不必要的状态更新
                if (value !== lastExternalUpdate.current) {
                  onChange(value);
                }
              } catch (error) {
                console.warn('获取编辑器内容失败:', error);
              }
            }
          }, 150); // 增加防抖时间，减少频繁更新
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
      // 清理ResizeObserver
      if (resizeObserverRef.current) {
        resizeObserverRef.current.disconnect();
        resizeObserverRef.current = null;
      }
    };
  }, [isCollapsed, monacoInitialized]); // 添加monacoInitialized依赖

  // 更新编辑器内容 - 使用标志位避免循环更新，保持光标位置
  const isUpdatingFromState = useRef(false);
  const lastExternalUpdate = useRef<string>('');

  // 监听sqlQuery变化，同步更新编辑器内容
  useEffect(() => {
    if (editorRef.current && sqlQuery !== lastExternalUpdate.current) {
      // 设置标志位，避免onChange回调触发
      isUpdatingFromState.current = true;
      lastExternalUpdate.current = sqlQuery;

      try {
        // 获取当前编辑器内容
        const currentValue = editorRef.current.getValue();

        // 只有当内容真正不同时才更新
        if (currentValue !== sqlQuery) {
          // 更新编辑器内容，但不保存或恢复光标位置
          // 让光标保持在最自然的位置（通常是内容的末尾）
          editorRef.current.setValue(sqlQuery);

          // 将光标移动到内容末尾，这是最自然的行为
          if (sqlQuery.length > 0) {
            const model = editorRef.current.getModel();
            if (model) {
              const lastLine = model.getLineCount();
              const lastColumn = model.getLineMaxColumn(lastLine);
              editorRef.current.setPosition({
                lineNumber: lastLine,
                column: lastColumn,
              });
            }
          }
        }
      } catch (error) {
        console.warn('更新编辑器内容失败:', error);
      } finally {
        // 重置标志位
        setTimeout(() => {
          isUpdatingFromState.current = false;
        }, 50); // 减少延迟时间
      }
    }
  }, [sqlQuery]);

  // 如果折叠，返回简单视图
  if (isCollapsed) {
    return (
      <div className={`${styles.editorContainer} ${styles.collapsed}`}>
        <div className={`${styles.editorWrapper} ${styles.collapsed}`} />
      </div>
    );
  }

  return (
    <div className={styles.editorContainer}>
      <div className={styles.editorWrapper}>
        {loading && (
          <div className={styles.editorLoading}>
            <Spin size="large" tip="正在加载编辑器..." />
          </div>
        )}
        <div ref={containerRef} className={`${styles.monacoEditorContainer} ${loading ? styles.loading : ''}`} />
      </div>
    </div>
  );
};

export default memo(QueryEditor);
