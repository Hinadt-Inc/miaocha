import React from 'react';
import { Card, Space, Tooltip, Button } from 'antd';
import { CopyOutlined } from '@ant-design/icons';
import QueryEditor from './QueryEditor';
import { SQLSnippetSelector } from './SQLSnippetSelector';
import * as monaco from 'monaco-editor';
import styles from '../SQLEditorPage.module.less';

export interface SQLQueryPanelProps {
  sqlQuery: string;
  onChange: (value: string | undefined) => void;
  onEditorMount: (editor: monaco.editor.IStandaloneCodeEditor, monaco: typeof import('monaco-editor')) => void;
  editorSettings: any;
  height?: number | string; // 改为可选，因为编辑器现在使用100%
  onHeightChange?: (height: number) => void; // 保持可选，向后兼容
  onInsertSnippet: (snippet: string) => void;
  onCopyToClipboard: () => void;
  header: React.ReactNode;
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
  onInsertSnippet,
  onCopyToClipboard,
  header,
}) => {
  return (
    <Card
      className={styles.editorCard}
      hoverable={false}
      title={
        <div className={styles.editorHeaderContainer}>
          <Space>
            <span>SQL 查询</span>
            <Tooltip title="复制 SQL">
              <Button
                aria-label="复制SQL语句"
                disabled={!sqlQuery.trim()}
                icon={<CopyOutlined />}
                size="small"
                type="text"
                onClick={onCopyToClipboard}
              />
            </Tooltip>
            <SQLSnippetSelector onSelect={onInsertSnippet} />
          </Space>
          {header}
        </div>
      }
    >
      <QueryEditor
        editorSettings={editorSettings}
        sqlQuery={sqlQuery}
        onChange={onChange}
        onEditorMount={onEditorMount}
      />
    </Card>
  );
};
