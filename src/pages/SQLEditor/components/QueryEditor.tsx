import React, { memo, useState, useRef, useEffect, useCallback } from 'react';
import Editor, { OnMount } from '@monaco-editor/react';
import * as monaco from 'monaco-editor';
import { Spin } from 'antd';
import { EditorSettings } from '../types';
import './QueryEditor.less';

import initMonacoEditor, { THEME_CONFIG } from '../utils/monacoInit';

interface QueryEditorProps {
  sqlQuery: string;
  onChange: (value: string | undefined) => void;
  onEditorMount: OnMount;
  editorSettings: EditorSettings;
  collapsed?: boolean;
  height?: number | string;
}

/**
 * SQL查询编辑器组件
 * 使用Monaco编辑器提供语法高亮和自动完成功能
 * 支持收起/展开功能
 */
const QueryEditor: React.FC<QueryEditorProps> = ({
  sqlQuery,
  onChange,
  onEditorMount,
  editorSettings,
  collapsed = false,
  height,
}) => {
  const [isCollapsed, setIsCollapsed] = useState(collapsed);
  const editorRef = useRef<monaco.editor.IStandaloneCodeEditor | null>(null);

  useEffect(() => {
    setIsCollapsed(collapsed);
    // 初始化monaco编辑器
    initMonacoEditor().catch((error: Error) => {
      console.error('Monaco编辑器初始化失败:', error);
    });
  }, [collapsed]);

  // 处理编辑器挂载
  const handleEditorDidMount: OnMount = useCallback(
    (editor, monacoInstance) => {
      monacoInstance.editor.defineTheme('sqlTheme', THEME_CONFIG);
      monacoInstance.editor.setTheme('sqlTheme');
      editorRef.current = editor;
      if (onEditorMount) {
        onEditorMount(editor, monacoInstance);
      }
    },
    [onEditorMount],
  );

  // 如果折叠，则返回一个简单的视图
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
        <Editor
          height={height || 'auto'}
          language="sql"
          value={sqlQuery}
          onChange={onChange}
          onMount={handleEditorDidMount}
          loading={
            <div className="editor-loading">
              <Spin />
            </div>
          }
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
    </div>
  );
};

export default memo(QueryEditor);
