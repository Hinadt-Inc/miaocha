import React from 'react';
import { Modal, Button, Space, Alert } from 'antd';
import { PlayCircleOutlined } from '@ant-design/icons';
import MonacoEditor from '@monaco-editor/react';

interface ExecuteConfirmationModalProps {
  visible: boolean;
  sql: string;
  onConfirm: () => void;
  onCancel: () => void;
  loading?: boolean;
  title?: React.ReactNode;
}

const ExecuteConfirmationModal: React.FC<ExecuteConfirmationModalProps> = ({
  visible,
  sql,
  onConfirm,
  onCancel,
  loading,
  title,
}) => {
  return (
    <Modal
      title={title || '确认执行SQL'}
      open={visible}
      width={800}
      onCancel={onCancel}
      footer={
        <Space>
          <Button onClick={onCancel}>取消</Button>
          <Button type="primary" icon={<PlayCircleOutlined />} onClick={onConfirm} loading={loading}>
            确认执行
          </Button>
        </Space>
      }
    >
      <MonacoEditor
        height="300px"
        language="sql"
        theme="vs-dark"
        value={sql}
        options={{
          readOnly: true,
          minimap: { enabled: false },
          scrollBeyondLastLine: false,
        }}
      />
    </Modal>
  );
};

export default ExecuteConfirmationModal;
