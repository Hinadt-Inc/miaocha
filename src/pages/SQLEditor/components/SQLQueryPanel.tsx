import React from 'react';
import { Card, Space, Tooltip, Button, Splitter } from 'antd';
import { CopyOutlined } from '@ant-design/icons';
import QueryEditor from './QueryEditor';
import { SQLSnippetSelector } from './SQLSnippetSelector';
import * as monaco from 'monaco-editor';

export interface SQLQueryPanelProps {
  sqlQuery: string;
  onChange: (value: string | undefined) => void;
  onEditorMount: (editor: monaco.editor.IStandaloneCodeEditor, monaco: typeof import('monaco-editor')) => void;
  editorSettings: any;
  height: number;
  onHeightChange: (height: number) => void;
  onInsertSnippet: (snippet: string) => void;
  onCopyToClipboard: () => void;
  header: React.ReactNode;
  onSplitterResize: (sizes: number[]) => void;
}

/**
 * SQL查询面板组件
 * 包含查询编辑器和相关操作
 */
export const SQLQueryPanel: React.FC<SQLQueryPanelProps> = ({
  sqlQuery,
  onChange,
  onEditorMount,
  editorSettings,
  height,
  onHeightChange,
  onInsertSnippet,
  onCopyToClipboard,
  header,
  onSplitterResize,
}) => {
  return (
    <Splitter layout="vertical" onResize={onSplitterResize}>
      <Splitter.Panel>
        <Card
          hoverable={false}
          title={
            <div className="editor-header-container">
              <Space>
                <span>SQL 查询</span>
                <Tooltip title="复制 SQL">
                  <Button
                    type="text"
                    size="small"
                    icon={<CopyOutlined />}
                    onClick={onCopyToClipboard}
                    disabled={!sqlQuery.trim()}
                    aria-label="复制SQL语句"
                  />
                </Tooltip>
                <SQLSnippetSelector onSelect={onInsertSnippet} />
              </Space>
              {header}
            </div>
          }
          className="editor-card"
        >
          <QueryEditor
            sqlQuery={sqlQuery}
            onChange={onChange}
            onEditorMount={onEditorMount}
            editorSettings={editorSettings}
            height={height}
            resizable={true}
            minHeight={200}
            maxHeight={800}
            onHeightChange={onHeightChange}
          />
        </Card>
      </Splitter.Panel>
    </Splitter>
  );
};
