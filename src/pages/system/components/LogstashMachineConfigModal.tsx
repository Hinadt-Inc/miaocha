import { Form, Input, Modal } from 'antd';
import { useState } from 'react';
import { updateLogstashMachineConfig } from '../../../api/logstash';

interface LogstashMachineConfigModalProps {
  visible: boolean;
  onCancel: () => void;
  onOk: (values: { configContent: string; jvmOptions: string; logstashYml: string }) => void;
  processId: number;
  machineId: number;
  initialConfig?: {
    configContent?: string;
    jvmOptions?: string;
    logstashYml?: string;
  };
}

export default function LogstashMachineConfigModal({
  visible,
  onCancel,
  onOk,
  processId,
  machineId,
  initialConfig,
}: LogstashMachineConfigModalProps) {
  const [form] = Form.useForm();
  const [confirmLoading, setConfirmLoading] = useState(false);

  const handleOk = async () => {
    try {
      setConfirmLoading(true);
      const values = await form.validateFields();
      await updateLogstashMachineConfig(processId, machineId, values);
      onOk(values);
    } finally {
      setConfirmLoading(false);
    }
  };

  return (
    <Modal
      title={`编辑机器配置 (机器ID: ${machineId})`}
      open={visible}
      onOk={handleOk}
      confirmLoading={confirmLoading}
      onCancel={onCancel}
      width={800}
    >
      <Form form={form} layout="vertical" initialValues={initialConfig}>
        <Form.Item name="configContent" label="配置内容">
          <Input.TextArea
            rows={6}
            style={{ width: '100%' }}
            placeholder="请输入Logstash配置文件内容，例如：input { beats { port => 5044 } }"
          />
        </Form.Item>

        <Form.Item name="jvmOptions" label="JVM参数">
          <Input.TextArea
            rows={4}
            style={{ width: '100%' }}
            placeholder="请输入JVM参数，例如：-Xms1g -Xmx1g -XX:+HeapDumpOnOutOfMemoryError"
          />
        </Form.Item>

        <Form.Item name="logstashYml" label="Logstash配置">
          <Input.TextArea
            rows={4}
            style={{ width: '100%' }}
            placeholder="请输入logstash.yml配置内容，例如：http.host: 0.0.0.0"
          />
        </Form.Item>
      </Form>
    </Modal>
  );
}
