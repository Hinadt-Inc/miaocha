import { Form, Input, Modal, Select, Spin } from 'antd';
import { useEffect, useState } from 'react';
import type { LogstashProcess } from '../../../types/logstashTypes';
import { getAllDataSources } from '../../../api/datasource';
import { getMachines } from '../../../api/machine';
import type { DataSource } from '../../../types/datasourceTypes';
import type { Machine } from '../../../types/machineTypes';

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
  const [datasources, setDatasources] = useState<DataSource[]>([]);
  const [machines, setMachines] = useState<Machine[]>([]);

  useEffect(() => {
    const fetchData = async () => {
      if (visible) {
        setLoading(true);
        try {
          const [dsRes, machineRes] = await Promise.all([getAllDataSources(), getMachines()]);
          setDatasources(dsRes);
          setMachines(machineRes);

          form.resetFields();
          if (initialValues) {
            console.log('Initial values for edit:', initialValues);
            const machineIds =
              initialValues.machines?.map((m) => m.id) || initialValues.machineStatuses?.map((m) => m.machineId);
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
      const submitValues = initialValues
        ? {
            id: initialValues.id,
            name: values.name,
            module: values.module,
          }
        : values;
      await onOk(submitValues);
    } catch (error) {
      console.error('表单验证失败:', error);
    } finally {
      setConfirmLoading(false);
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
                <Form.Item name="module" label="模块" rules={[{ required: true, message: '请输入模块' }]}>
                  <Input placeholder="请输入模块名称，例如：order-service" />
                </Form.Item>
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px' }}>
                <Form.Item name="datasourceId" label="数据源" rules={[{ required: true, message: '请选择数据源' }]}>
                  <Select loading={loading} disabled={!!initialValues}>
                    {datasources.map((ds) => (
                      <Select.Option key={ds.id} value={ds.id}>
                        {ds.name}
                      </Select.Option>
                    ))}
                  </Select>
                </Form.Item>
                <Form.Item name="tableName" label="表名" rules={[{ required: false, message: '请输入表名' }]}>
                  <Input placeholder="请输入表名，例如：order_logs" disabled={!!initialValues} />
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
            </div>
          </div>

          <Form.Item name="configContent" label="配置内容" rules={[{ required: true, message: '请输入配置内容' }]}>
            <Input.TextArea
              disabled={!!initialValues}
              rows={6}
              style={{ width: '100%' }}
              placeholder="请输入Logstash配置文件内容，例如：input { beats { port => 5044 } }"
            />
          </Form.Item>

          <Form.Item name="jvmOptions" label="JVM参数">
            <Input.TextArea
              disabled={!!initialValues}
              rows={4}
              style={{ width: '100%' }}
              placeholder="请输入JVM参数，例如：-Xms1g -Xmx1g -XX:+HeapDumpOnOutOfMemoryError"
            />
          </Form.Item>

          <Form.Item name="logstashYml" label="Logstash配置">
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
