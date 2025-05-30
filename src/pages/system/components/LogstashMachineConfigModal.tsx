import { Form, Input, Modal, message, Switch, Space, Button, Tooltip } from 'antd';
import {
  LOGSTASH_CONFIG_TEMPLATE,
  JVM_CONFIG_TEMPLATE,
  LOGSTASH_BASE_CONFIG_TEMPLATE,
} from '../../../utils/logstashTemplates';
import { CopyOutlined } from '@ant-design/icons';
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

      const currentValues = form.getFieldsValue();
      const hasChanges = (Object.keys(currentValues) as Array<keyof typeof initialConfig>).some(
        (key) => currentValues[key] !== initialConfig?.[key],
      );

      if (!hasChanges) {
        messageApi.info('配置内容未修改');
        return;
      }

      const values = await form.validateFields();
      let data = {
        configContent: values.configContent,
      };
      if (enableEdit) {
        data = {
          ...data,
          ...values,
        };
      }
      await updateLogstashMachineConfig(processId, machineId, data);
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
        <Form.Item
          name="configContent"
          label={
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <span>配置内容</span>
              <Tooltip title="复制配置内容模板">
                <Button
                  size="small"
                  icon={<CopyOutlined />}
                  onClick={() => {
                    const template = `${LOGSTASH_CONFIG_TEMPLATE}`;
                    navigator.clipboard
                      .writeText(template)
                      .then(() => messageApi.success('配置已复制到剪贴板'))
                      .catch(() => messageApi.error('复制失败'));
                  }}
                  title="复制配置模板"
                />
              </Tooltip>
            </div>
          }
        >
          <Input.TextArea
            rows={6}
            style={{ width: '100%' }}
            placeholder="请输入Logstash配置文件内容，例如：input { beats { port => 5044 } }"
          />
        </Form.Item>

        <Form.Item
          name="jvmOptions"
          label={
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <span>JVM参数</span>
              <Tooltip title="复制JVM参数模板">
                <Button
                  size="small"
                  icon={<CopyOutlined />}
                  onClick={() => {
                    navigator.clipboard
                      .writeText(JVM_CONFIG_TEMPLATE)
                      .then(() => messageApi.success('JVM参数已复制到剪贴板'))
                      .catch(() => messageApi.error('复制失败'));
                  }}
                  title="复制JVM参数模板"
                />
              </Tooltip>
            </div>
          }
        >
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
