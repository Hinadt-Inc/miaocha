import { Form, Input, Modal, Select, Spin, Button, Space, Tooltip } from 'antd';
import { FileTextOutlined as IconTemplate, CopyOutlined } from '@ant-design/icons';
import {
  LOGSTASH_CONFIG_TEMPLATE,
  JVM_CONFIG_TEMPLATE,
  LOGSTASH_BASE_CONFIG_TEMPLATE,
} from '../../../utils/logstashTemplates';
import { useEffect, useState } from 'react';
import type { LogstashProcess } from '../../../types/logstashTypes';
import { getModules } from '../../../api/modules';
import { getMachines } from '../../../api/machine';
import type { Machine } from '../../../types/machineTypes';
import type { Module } from '../../../api/modules';

interface LogstashEditModalProps {
  visible: boolean;
  onCancel: () => void;
  onOk: (values: Partial<LogstashProcess>) => Promise<void>;
  initialValues?: LogstashProcess | null;
}

export default function LogstashEditModal({ visible, onCancel, onOk, initialValues }: LogstashEditModalProps) {
  const [form] = Form.useForm();
  const [confirmLoading, setConfirmLoading] = useState(false);
  const [loading, setLoading] = useState(false);
  const [moduleData, setModuleData] = useState<Module[]>([]);
  const [machines, setMachines] = useState<Machine[]>([]);

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
        // 编辑模式 - 使用metadata接口更新
        await onOk({
          id: initialValues.id,
          name: values.name,
          moduleId: values.moduleId,
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

  const applyTemplate = (type: 'config' | 'jvm' | 'base') => {
    if (initialValues) {
      // 如果是编辑模式，则为复制功能
      switch (type) {
        case 'config':
          navigator.clipboard.writeText(form.getFieldValue('configContent'));
          break;
        case 'jvm':
          navigator.clipboard.writeText(form.getFieldValue('jvmOptions'));
          break;
        case 'base':
          navigator.clipboard.writeText(form.getFieldValue('logstashYml'));
          break;
      }
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
      <Modal
        title={initialValues ? '编辑Logstash进程' : '新增Logstash进程'}
        open={visible}
        onOk={handleOk}
        confirmLoading={confirmLoading}
        onCancel={onCancel}
        width={800}
      >
        <Form form={form} layout="vertical">
          <div>
            <div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px' }}>
                <Form.Item name="name" label="名称" rules={[{ required: true, message: '请输入名称' }]}>
                  <Input placeholder="请输入Logstash进程名称，例如：订单处理服务" />
                </Form.Item>
                <Form.Item name="moduleId" label="模块" rules={[{ required: true, message: '请输入模块' }]}>
                  <Select loading={loading}>
                    {moduleData.map((ds) => (
                      <Select.Option key={ds.id} value={ds.id}>
                        {ds.name}
                      </Select.Option>
                    ))}
                  </Select>
                </Form.Item>
              </div>
              <Form.Item name="machineIds" label="部署机器" rules={[{ required: true, message: '请选择部署机器' }]}>
                <Select mode="multiple" loading={loading} disabled={!!initialValues}>
                  {machines.map((m) => (
                    <Select.Option key={m.id} value={m.id}>
                      {m.name} ({m.ip})
                    </Select.Option>
                  ))}
                </Select>
              </Form.Item>
              <Form.Item name="description" label="描述">
                <Input.TextArea rows={2} placeholder="请输入Logstash进程描述信息" disabled={!!initialValues} />
              </Form.Item>
              <Form.Item name="customDeployPath" label="自定义部署路径">
                <Input placeholder="例如：/opt/custom/logstash" disabled={!!initialValues} />
              </Form.Item>
            </div>
          </div>

          <Form.Item
            name="configContent"
            label={
              <Space>
                <span>配置内容</span>
                <Tooltip title={initialValues ? '复制' : '应用标准配置模板'}>
                  <Button
                    size="small"
                    onClick={() => applyTemplate('config')}
                    icon={initialValues ? <CopyOutlined /> : <IconTemplate />}
                  />
                </Tooltip>
              </Space>
            }
            rules={[{ required: true, message: '请输入配置内容' }]}
            extra="模板包含Kafka输入和Doris输出的标准配置"
          >
            <Input.TextArea
              disabled={!!initialValues}
              rows={6}
              style={{ width: '100%' }}
              placeholder="请输入Logstash配置文件内容，例如：input { beats { port => 5044 } }"
            />
          </Form.Item>

          <Form.Item
            name="jvmOptions"
            label={
              <Space>
                <span>JVM参数</span>
                <Tooltip title="应用JVM参数模板">
                  <Button
                    size="small"
                    onClick={() => applyTemplate('jvm')}
                    icon={initialValues ? <CopyOutlined /> : <IconTemplate />}
                  />
                </Tooltip>
              </Space>
            }
            extra="模板包含基础JVM参数和专家级配置选项"
          >
            <Input.TextArea
              disabled={!!initialValues}
              rows={4}
              style={{ width: '100%' }}
              placeholder="请输入JVM参数，例如：-Xms1g -Xmx1g -XX:+HeapDumpOnOutOfMemoryError"
            />
          </Form.Item>

          <Form.Item
            name="logstashYml"
            label={
              <Space>
                <span>Logstash配置</span>
                <Tooltip title="应用基础配置模板">
                  <Button
                    size="small"
                    onClick={() => applyTemplate('base')}
                    icon={initialValues ? <CopyOutlined /> : <IconTemplate />}
                  />
                </Tooltip>
              </Space>
            }
            extra="模板包含基础Logstash配置参数"
          >
            <Input.TextArea
              disabled={!!initialValues}
              rows={4}
              style={{ width: '100%' }}
              placeholder="请输入logstash.yml配置内容，例如：http.host: 0.0.0.0"
            />
          </Form.Item>
        </Form>
      </Modal>
    </Spin>
  );
}
