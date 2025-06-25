import React, { useRef, useEffect, useState } from 'react';
import { Modal, Button, Space, Alert } from 'antd';
import { PlayCircleOutlined } from '@ant-design/icons';
import * as monaco from 'monaco-editor';

import { initMonacoEditorLocally } from '../utils/monacoLocalInit';
import './QueryEditor.less';

interface ExecuteConfirmationModalProps {
  visible: boolean;
  sql: string;
  onConfirm: () => void;
  onCancel: () => void;
  onSqlChange?: (value: string) => void; // 新增SQL变化回调
  loading?: boolean;
  title?: React.ReactNode;
  readonly?: boolean; // 新增只读模式属性
}

const ExecuteConfirmationModal: React.FC<ExecuteConfirmationModalProps> = ({
  visible,
  sql,
  onConfirm,
  onCancel,
  onSqlChange,
  loading = false,
  title,
  readonly = false,
}) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const editorRef = useRef<monaco.editor.IStandaloneCodeEditor | null>(null);
  const [editorLoading, setEditorLoading] = useState(true);

  // 初始化编辑器
  useEffect(() => {
    if (!visible || !containerRef.current) return;

    const initEditor = async () => {
      try {
        setEditorLoading(true);

        // 🎯 使用完全本地化的Monaco初始化
        const monacoInstance = initMonacoEditorLocally();

        // 创建编辑器实例
        const editor = monacoInstance.editor.create(containerRef.current!, {
          value: sql,
          language: 'sql',
          theme: 'vs-dark',
          automaticLayout: true,
          minimap: { enabled: false },
          scrollBeyondLastLine: false,
          readOnly: readonly,
          contextmenu: true,
          selectOnLineNumbers: true,
          lineNumbers: 'on',
          fontSize: 14,
          fontFamily: '"SFMono-Regular", Consolas, "Liberation Mono", Menlo, monospace',
        });

        editorRef.current = editor;

        // 监听内容变化
        if (!readonly && onSqlChange) {
          editor.onDidChangeModelContent(() => {
            const value = editor.getValue();
            onSqlChange(value);
          });
        }

        setEditorLoading(false);
        console.log('✅ ExecuteConfirmationModal 编辑器完全本地初始化成功！');
      } catch (error) {
        console.error('❌ ExecuteConfirmationModal 编辑器初始化失败:', error);
        setEditorLoading(false);
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
  }, [visible, readonly, onSqlChange]);

  // 更新编辑器内容
  useEffect(() => {
    if (editorRef.current && editorRef.current.getValue() !== sql) {
      editorRef.current.setValue(sql);
    }
  }, [sql]);

  return (
    <Modal
      title={title || '确认执行'}
      open={visible}
      onCancel={onCancel}
      width={800}
      footer={
        <Space>
          <Button onClick={onCancel}>取消</Button>
          <Button type="primary" icon={<PlayCircleOutlined />} loading={loading} onClick={onConfirm}>
            执行
          </Button>
        </Space>
      }
    >
      {readonly && <Alert message="此模块已有SQL语句，无法编辑" type="info" style={{ marginBottom: 16 }} showIcon />}
      <div ref={containerRef} className="execute-modal-editor-container" />
      {editorLoading && (
        <div className="execute-modal-loading">
          <div>正在加载编辑器...</div>
        </div>
      )}
    </Modal>
  );
};

export default ExecuteConfirmationModal;
