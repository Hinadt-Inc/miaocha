import { Form, Input, Modal, Select, Spin, Button, Space, Tooltip, message } from 'antd';
import { safeCopy } from '@/utils/clipboard';
import { FileTextOutlined as IconTemplate, CopyOutlined } from '@ant-design/icons';
import {
  LOGSTASH_CONFIG_TEMPLATE,
  JVM_CONFIG_TEMPLATE,
  LOGSTASH_BASE_CONFIG_TEMPLATE,
} from '@/utils/logstashTemplates';
import { useEffect, useState } from 'react';
import type { LogstashProcess } from '@/types/logstashTypes';
import { getModules } from '@/api/modules';
import { getMachines } from '@/api/machine';
import type { Machine } from '@/types/machineTypes';
import type { Module } from '@/api/modules';

interface LogstashEditModalProps {
  readonly visible: boolean;
  readonly onCancel: () => void;
  readonly onOk: (values: Partial<LogstashProcess>) => Promise<void>;
  readonly initialValues?: LogstashProcess | null;
}

export default function LogstashEditModal({ visible, onCancel, onOk, initialValues }: LogstashEditModalProps) {
  const [form] = Form.useForm();
  const [confirmLoading, setConfirmLoading] = useState(false);
  const [loading, setLoading] = useState(false);
  const [moduleData, setModuleData] = useState<Module[]>([]);
  const [machines, setMachines] = useState<Machine[]>([]);
  const [messageApi, contextHolder] = message.useMessage();

  useEffect(() => {
    const fetchData = async () => {
      if (visible) {
        setLoading(true);
        try {
          const [dsRes, machineRes] = await Promise.all([getModules(), getMachines()]);
          setModuleData(dsRes);
          setMachines(machineRes);

          form.resetFields();
          if (initialValues) {
            console.log('Initial values for edit:', initialValues);
            const machineIds = initialValues.logstashMachineStatusInfo?.map((m) => m.machineId) || [];
            console.log('Calculated machineIds:', machineIds);

            form.setFieldsValue({
              ...initialValues,
              machineIds,
            });
          }
        } finally {
          setLoading(false);
        }
      }
    };

    fetchData();
  }, [visible, initialValues, form]);

  const handleOk = async () => {
    try {
      setConfirmLoading(true);
      const values = await form.validateFields();

      if (initialValues) {
        // 编辑模式 - 传递配置字段但不包含 machineIds（部署机器不可编辑）和 customDeployPath（编辑时不显示）
        await onOk({
          id: initialValues.id,
          name: values.name,
          moduleId: values.moduleId,
          configContent: values.configContent,
          jvmOptions: values.jvmOptions,
          logstashYml: values.logstashYml,
          // 注意：不传递 machineIds、customDeployPath 和 description，因为这些字段在编辑模式下不可更改
          updateUser: 'admin', // 这里应该从用户上下文获取实际用户
        });
      } else {
        // 创建模式 - 使用原有逻辑
        await onOk(values);
      }
    } catch (error) {
      console.error('表单验证失败:', error);
    } finally {
      setConfirmLoading(false);
    }
  };

  const applyTemplate = async (type: 'config' | 'jvm' | 'base') => {
    if (initialValues) {
      // 如果是编辑模式，则为复制功能
      const textToCopy = (() => {
        switch (type) {
          case 'config':
            return form.getFieldValue('configContent');
          case 'jvm':
            return form.getFieldValue('jvmOptions');
          case 'base':
            return form.getFieldValue('logstashYml');
        }
      })();

      const success = await safeCopy(textToCopy);
      if (!success) {
        messageApi.error('复制失败，请手动选择文本后复制');
      }
      messageApi.success('内容已复制到剪贴板');
      return;
    }
    switch (type) {
      case 'config':
        form.setFieldsValue({ configContent: LOGSTASH_CONFIG_TEMPLATE });
        break;
      case 'jvm':
        form.setFieldsValue({ jvmOptions: JVM_CONFIG_TEMPLATE });
        break;
      case 'base':
        form.setFieldsValue({ logstashYml: LOGSTASH_BASE_CONFIG_TEMPLATE });
        break;
    }
  };

  return (
    <Spin spinning={loading}>
      {contextHolder}
      <Modal
        confirmLoading={confirmLoading}
        maskClosable={false}
        open={visible}
        title={initialValues ? '编辑Logstash进程' : '新增Logstash进程'}
        width={800}
        onCancel={onCancel}
        onOk={handleOk}
      >
        <Form form={form} layout="vertical">
          <div>
            <div>
              <div>
                <Form.Item label="名称" name="name" rules={[{ required: true, message: '请输入名称' }]}>
                  <Input maxLength={128} placeholder="请输入Logstash进程名称，例如：订单处理服务" />
                </Form.Item>
                <Form.Item label="模块" name="moduleId" rules={[{ required: true, message: '请选择模块' }]}>
                  <Select loading={loading}>
                    {moduleData.map((ds) => (
                      <Select.Option key={ds.id} value={ds.id}>
                        {ds.name}
                      </Select.Option>
                    ))}
                  </Select>
                </Form.Item>
              </div>
              <Form.Item label="部署机器" name="machineIds" rules={[{ required: true, message: '请选择部署机器' }]}>
                <Select disabled={!!initialValues} loading={loading} mode="multiple">
                  {machines.map((m) => (
                    <Select.Option key={m.id} value={m.id}>
                      {m.name} ({m.ip})-当前{m.logstashMachineCount}个实例
                    </Select.Option>
                  ))}
                </Select>
              </Form.Item>
              {!initialValues && (
                <Form.Item label="自定义部署路径" name="customDeployPath">
                  <Input placeholder="例如：/opt/custom/logstash" />
                </Form.Item>
              )}
            </div>
          </div>

          <Form.Item
            extra="模板包含Kafka输入和Doris输出的标准配置"
            label={
              <Space>
                <span>配置内容</span>
                <Tooltip title={initialValues ? '复制' : '应用标准配置模板'}>
                  <Button
                    icon={initialValues ? <CopyOutlined /> : <IconTemplate />}
                    size="small"
                    onClick={() => applyTemplate('config')}
                  />
                </Tooltip>
              </Space>
            }
            name="configContent"
            rules={[{ required: true, message: '请输入配置内容' }]}
          >
            <Input.TextArea
              placeholder="请输入Logstash配置文件内容，例如：input { beats { port => 5044 } }"
              rows={6}
              style={{ width: '100%' }}
            />
          </Form.Item>

          <Form.Item
            extra="模板包含基础JVM参数和专家级配置选项"
            label={
              <Space>
                <span>JVM参数</span>
                <Tooltip title={initialValues ? '复制' : '应用JVM参数模板'}>
                  <Button
                    icon={initialValues ? <CopyOutlined /> : <IconTemplate />}
                    size="small"
                    onClick={() => applyTemplate('jvm')}
                  />
                </Tooltip>
              </Space>
            }
            name="jvmOptions"
          >
            <Input.TextArea
              placeholder="请输入JVM参数，例如：-Xms1g -Xmx1g -XX:+HeapDumpOnOutOfMemoryError"
              rows={4}
              style={{ width: '100%' }}
            />
          </Form.Item>

          <Form.Item
            extra="模板包含基础Logstash配置参数"
            label={
              <Space>
                <span>Logstash配置</span>
                <Tooltip title={initialValues ? '复制' : '应用基础配置模板'}>
                  <Button
                    icon={initialValues ? <CopyOutlined /> : <IconTemplate />}
                    size="small"
                    onClick={() => applyTemplate('base')}
                  />
                </Tooltip>
              </Space>
            }
            name="logstashYml"
          >
            <Input.TextArea
              placeholder="请输入logstash.yml配置内容，例如：http.host: 0.0.0.0"
              rows={4}
              style={{ width: '100%' }}
            />
          </Form.Item>
        </Form>
      </Modal>
    </Spin>
  );
}
