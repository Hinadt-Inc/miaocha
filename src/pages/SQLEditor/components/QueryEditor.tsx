import { OnMount } from '@monaco-editor/react';
import Editor from '@monaco-editor/react';
import { EditorSettings } from '../types';
import './QueryEditor.less';

interface QueryEditorProps {
  sqlQuery: string;
  onChange: (value: string | undefined) => void;
  onEditorMount: OnMount;
  editorSettings: EditorSettings;
}

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

export default QueryEditor;