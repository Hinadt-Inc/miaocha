import React, { memo } from 'react';
import Editor, { OnMount } from '@monaco-editor/react';
import { Spin } from 'antd';
import { EditorSettings } from '../types';
import './QueryEditor.less';

interface QueryEditorProps {
  sqlQuery: string;
  onChange: (value: string | undefined) => void;
  onEditorMount: OnMount;
  editorSettings: EditorSettings;
}

/**
 * SQL查询编辑器组件
 * 使用Monaco编辑器提供语法高亮和自动完成功能
 */
const QueryEditor: React.FC<QueryEditorProps> = ({
  sqlQuery,
  onChange,
  onEditorMount,
  editorSettings
}) => {
  return (
    <div className="editor-wrapper">
      <Editor
        language="sql"
        value={sqlQuery}
        onChange={onChange}
        onMount={onEditorMount}
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
          suggestOnTriggerCharacters: editorSettings.autoComplete
        }}
      />
    </div>
  );
};

// 使用 memo 避免不必要的重渲染
export default memo(QueryEditor);