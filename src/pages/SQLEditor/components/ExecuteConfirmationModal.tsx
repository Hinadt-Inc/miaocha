import React from 'react';
import { Modal, Button, Space, Alert } from 'antd';
import { PlayCircleOutlined } from '@ant-design/icons';
import MonacoEditor from '@monaco-editor/react';

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
  loading,
  title,
  readonly = false,
}) => {
  return (
    <Modal
      title={title || (readonly ? '查看SQL' : '确认执行SQL')}
      open={visible}
      width={800}
      onCancel={onCancel}
      footer={
        <Space>
          <Button onClick={onCancel}>{readonly ? '关闭' : '取消'}</Button>
          {!readonly && (
            <Button type="primary" icon={<PlayCircleOutlined />} onClick={onConfirm} loading={loading}>
              确认执行
            </Button>
          )}
        </Space>
      }
    >
      {readonly && <Alert message="此模块已有SQL语句，无法编辑" type="info" style={{ marginBottom: 16 }} showIcon />}
      <MonacoEditor
        height="300px"
        language="sql"
        theme="vs-dark"
        value={sql}
        onChange={(value) => !readonly && onSqlChange?.(value || '')}
        options={{
          minimap: { enabled: false },
          scrollBeyondLastLine: false,
          readOnly: readonly,
        }}
      />
    </Modal>
  );
};

export default ExecuteConfirmationModal;
