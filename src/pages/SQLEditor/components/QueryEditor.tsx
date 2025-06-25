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
}) => {
  const [isCollapsed, setIsCollapsed] = useState(collapsed);
  const [loading, setLoading] = useState(true);
  const editorRef = useRef<monaco.editor.IStandaloneCodeEditor | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    setIsCollapsed(collapsed);
  }, [collapsed]);

  // 初始化编辑器 - 完全本地化
  useEffect(() => {
    if (isCollapsed || !containerRef.current) return;

    const initEditor = async () => {
      try {
        setLoading(true);

        // 🎯 使用完全本地化的Monaco初始化
        const monacoInstance = initMonacoEditorLocally();

        // 创建编辑器实例
        const editor = monacoInstance.editor.create(containerRef.current!, {
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
          // 智能提示配置
          quickSuggestions: editorSettings?.autoComplete ?? true,
          suggestOnTriggerCharacters: editorSettings?.autoComplete ?? true,
          acceptSuggestionOnCommitCharacter: true,
          acceptSuggestionOnEnter: 'on',
          // 自动格式化
          formatOnPaste: true,
          formatOnType: true,
        });

        editorRef.current = editor;

        // 监听内容变化
        const disposable = editor.onDidChangeModelContent(() => {
          const value = editor.getValue();
          onChange(value);
        });

        // 调用挂载回调
        if (onEditorMount) {
          onEditorMount(editor, monacoInstance);
        }

        setLoading(false);
        console.log('✅ QueryEditor 完全本地初始化成功，无CDN依赖！');

        // 返回清理函数
        return () => {
          disposable.dispose();
          editor.dispose();
        };
      } catch (error) {
        console.error('❌ QueryEditor 初始化失败:', error);
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

export default memo(QueryEditor);
