import { Form, Input, Modal, message, Switch, Space } from 'antd';
import { useEffect, useState } from 'react';
import { getLogstashMachineDetail, updateLogstashMachineConfig } from '../../../api/logstash';

interface LogstashMachineConfigModalProps {
  visible: boolean;
  onCancel: () => void;
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
  processId,
  machineId,
  initialConfig,
}: LogstashMachineConfigModalProps) {
  const [form] = Form.useForm();
  const [confirmLoading, setConfirmLoading] = useState(false);
  const [enableEdit, setEnableEdit] = useState(false);
  const [messageApi, contextHolder] = message.useMessage();

  useEffect(() => {
    if (visible) {
      const fetchDetail = async () => {
        try {
          const detail = await getLogstashMachineDetail(processId, machineId);
          form.setFieldsValue({
            configContent: detail.configContent,
            jvmOptions: detail.jvmOptions,
            logstashYml: detail.logstashYml,
          });
        } catch (error) {
          messageApi.error('获取机器配置详情失败');
        }
      };
      fetchDetail();
    }
  }, [visible, processId, machineId, form]);

  const handleOk = async () => {
    try {
      setConfirmLoading(true);

      if (!enableEdit) {
        messageApi.warning('请开启编辑模式后再提交修改');
        return;
      }

      const currentValues = form.getFieldsValue();
      const hasChanges = (Object.keys(currentValues) as Array<keyof typeof initialConfig>).some(
        (key) => currentValues[key] !== initialConfig?.[key],
      );

      if (!hasChanges) {
        messageApi.info('配置内容未修改');
        return;
      }

      const values = await form.validateFields();
      await updateLogstashMachineConfig(processId, machineId, values);
      messageApi.success('机器配置更新成功');
      onCancel();
    } finally {
      setConfirmLoading(false);
    }
  };

  return (
    <Modal
      title={
        <Space>
          <span>编辑机器配置 (机器ID: {machineId})</span>
          <Switch checked={enableEdit} onChange={setEnableEdit} checkedChildren="编辑中" unCheckedChildren="仅查看" />
        </Space>
      }
      open={visible}
      onOk={handleOk}
      confirmLoading={confirmLoading}
      onCancel={onCancel}
      width={800}
    >
      {contextHolder}
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
            disabled={!enableEdit}
          />
        </Form.Item>

        <Form.Item name="logstashYml" label="Logstash配置">
          <Input.TextArea
            rows={4}
            style={{ width: '100%' }}
            placeholder="请输入logstash.yml配置内容，例如：http.host: 0.0.0.0"
            disabled={!enableEdit}
          />
        </Form.Item>
      </Form>
    </Modal>
  );
}
