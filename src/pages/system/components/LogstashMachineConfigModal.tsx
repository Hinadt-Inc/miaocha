import { Form, Input, Modal, message, Switch, Button, Tooltip } from 'antd';
import { LOGSTASH_CONFIG_TEMPLATE, JVM_CONFIG_TEMPLATE } from '../../../utils/logstashTemplates';
import { CopyOutlined } from '@ant-design/icons';
import { useEffect, useState } from 'react';
import { getLogstashMachineDetail, updateLogstashMachineConfig } from '../../../api/logstash';

interface LogstashMachineConfigModalProps {
  visible: boolean;
  onCancel: () => void;
  logstashMachineId: number;
  processId: number;
  initialConfig?: {
    configContent?: string;
    jvmOptions?: string;
    logstashYml?: string;
  };
}

export default function LogstashMachineConfigModal({
  visible,
  onCancel,
  initialConfig,
  logstashMachineId,
  processId,
}: LogstashMachineConfigModalProps) {
  const [form] = Form.useForm();
  const [confirmLoading, setConfirmLoading] = useState(false);
  const [enableEdit, setEnableEdit] = useState({
    configContent: false,
    jvmOptions: false,
    logstashYml: false,
  });
  const [messageApi, contextHolder] = message.useMessage();

  useEffect(() => {
    if (visible) {
      console.log('LogstashMachineConfigModal visible:', logstashMachineId);
    }
  }, [visible, form]);

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
      const data: any = {};
      if (enableEdit.configContent) {
        data.configContent = values.configContent;
      }
      if (enableEdit.jvmOptions) {
        data.jvmOptions = values.jvmOptions;
      }
      if (enableEdit.logstashYml) {
        data.logstashYml = values.logstashYml;
      }
      if (Object.keys(data).length === 0) {
        messageApi.warning('请至少修改一项配置');
        return;
      }
      await updateLogstashMachineConfig(processId, logstashMachineId, data);
      messageApi.success('机器配置更新成功');
      onCancel();
    } finally {
      setConfirmLoading(false);
    }
  };

  return (
    <Modal
      title={<span>编辑机器配置 (实例ID: {logstashMachineId})</span>}
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
              <Switch
                checked={enableEdit.configContent}
                onChange={(checked) => setEnableEdit({ ...enableEdit, configContent: checked })}
                checkedChildren="可编辑"
                unCheckedChildren="锁定"
                size="small"
              />
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
            disabled={!enableEdit.configContent}
          />
        </Form.Item>

        <Form.Item
          name="jvmOptions"
          label={
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <span>JVM参数</span>
              <Switch
                checked={enableEdit.jvmOptions}
                onChange={(checked) => setEnableEdit({ ...enableEdit, jvmOptions: checked })}
                checkedChildren="可编辑"
                unCheckedChildren="锁定"
                size="small"
              />
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
            disabled={!enableEdit.jvmOptions}
          />
        </Form.Item>

        <Form.Item
          name="logstashYml"
          label={
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <span>Logstash配置</span>
              <Switch
                checked={enableEdit.logstashYml}
                onChange={(checked) => setEnableEdit({ ...enableEdit, logstashYml: checked })}
                checkedChildren="可编辑"
                unCheckedChildren="锁定"
                size="small"
              />
            </div>
          }
        >
          <Input.TextArea
            rows={4}
            style={{ width: '100%' }}
            placeholder="请输入logstash.yml配置内容，例如：http.host: 0.0.0.0"
            disabled={!enableEdit.logstashYml}
          />
        </Form.Item>
      </Form>
    </Modal>
  );
}
