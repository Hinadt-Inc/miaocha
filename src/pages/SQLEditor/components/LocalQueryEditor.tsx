import React, { memo, useState, useRef, useEffect } from 'react';
import * as monaco from 'monaco-editor';
import { Spin } from 'antd';
import { EditorSettings } from '../types';
import './QueryEditor.less';

import { initMonacoEditorLocally } from '../utils/monacoLocalInit';

interface QueryEditorProps {
  sqlQuery: string;
  onChange: (value: string) => void;
  onEditorMount?: (editor: monaco.editor.IStandaloneCodeEditor, monaco: typeof import('monaco-editor')) => void;
  editorSettings?: EditorSettings;
  collapsed?: boolean;
  height?: string | number;
}

/**
 * SQL查询编辑器组件 - 完全本地版本
 * 使用Monaco编辑器提供语法高亮和自动完成功能
 * 无CDN依赖，100%本地资源
 */
const LocalQueryEditor: React.FC<QueryEditorProps> = ({
  sqlQuery,
  onChange,
  onEditorMount,
  editorSettings,
  collapsed = false,
  height = 300,
}) => {
  const [isCollapsed, setIsCollapsed] = useState(collapsed);
  const [loading, setLoading] = useState(true);
  const editorRef = useRef<monaco.editor.IStandaloneCodeEditor | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    setIsCollapsed(collapsed);
  }, [collapsed]);

  // 初始化编辑器
  useEffect(() => {
    if (isCollapsed || !containerRef.current) return;

    const initEditor = async () => {
      try {
        setLoading(true);

        // 初始化Monaco (100%本地)
        const monacoInstance = initMonacoEditorLocally();

        // 创建编辑器实例
        const editor = monacoInstance.editor.create(containerRef.current!, {
          value: sqlQuery,
          language: 'sql',
          theme: 'sqlTheme',
          automaticLayout: true,
          minimap: { enabled: editorSettings?.minimap ?? false },
          wordWrap: editorSettings?.wordWrap ? 'on' : 'off',
          fontSize: editorSettings?.fontSize ?? 14,
          // 其他配置
          scrollBeyondLastLine: false,
          renderWhitespace: 'none',
          contextmenu: true,
          selectOnLineNumbers: true,
          roundedSelection: false,
          readOnly: false,
          cursorStyle: 'line',
        });

        editorRef.current = editor;

        // 监听内容变化
        editor.onDidChangeModelContent(() => {
          const value = editor.getValue();
          onChange(value);
        });

        // 调用挂载回调
        if (onEditorMount) {
          onEditorMount(editor, monacoInstance);
        }

        setLoading(false);
        console.log('LocalQueryEditor 初始化成功');
      } catch (error) {
        console.error('LocalQueryEditor 初始化失败:', error);
        setLoading(false);
      }
    };

    initEditor();

    // 清理函数
    return () => {
      if (editorRef.current) {
        editorRef.current.dispose();
        editorRef.current = null;
      }
    };
  }, [isCollapsed, onEditorMount, editorSettings]);

  // 更新编辑器内容
  useEffect(() => {
    if (editorRef.current && editorRef.current.getValue() !== sqlQuery) {
      editorRef.current.setValue(sqlQuery);
    }
  }, [sqlQuery]);

  // 如果折叠，返回简单视图
  if (isCollapsed) {
    return (
      <div className="editor-container collapsed">
        <div className="editor-wrapper collapsed" />
      </div>
    );
  }

  return (
    <div className="editor-container">
      <div className="editor-wrapper">
        {loading && (
          <div className="editor-loading">
            <Spin size="large" tip="正在加载编辑器..." />
          </div>
        )}
        <div
          ref={containerRef}
          className={`monaco-editor-container ${loading ? 'loading' : ''}`}
          data-height={height}
        />
      </div>
    </div>
  );
};

export default memo(LocalQueryEditor);
