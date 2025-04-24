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

export default function LogstashEditModal({
  visible,
  onCancel,
  onOk,
  initialValues
}: LogstashEditModalProps) {
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
          const [dsRes, machineRes] = await Promise.all([
            getAllDataSources(),
            getMachines()
          ]);
          setDatasources(dsRes);
          setMachines(machineRes);
          
          form.resetFields();
          if (initialValues) {
            form.setFieldsValue({
              ...initialValues,
              machineIds: initialValues.machines?.map(m => m.id)
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
      await onOk(values);
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
        width={600}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="name"
            label="名称"
            rules={[{ required: true, message: '请输入名称' }]}
          >
            <Input />
          </Form.Item>
          <Form.Item
            name="module"
            label="模块"
            rules={[{ required: true, message: '请输入模块' }]}
          >
            <Input />
          </Form.Item>
          <Form.Item
            name="datasourceId"
            label="数据源"
            rules={[{ required: true, message: '请选择数据源' }]}
          >
            <Select loading={loading}>
              {datasources.map(ds => (
                <Select.Option key={ds.id} value={ds.id}>
                  {ds.name}
                </Select.Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item
            name="machineIds"
            label="部署机器"
            rules={[{ required: true, message: '请选择部署机器' }]}
          >
            <Select mode="multiple" loading={loading}>
              {machines.map(m => (
                <Select.Option key={m.id} value={m.id}>
                  {m.name} ({m.ip})
                </Select.Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item
            name="configJson"
            label="配置JSON"
            rules={[{ required: true, message: '请输入配置JSON' }]}
          >
            <Input.TextArea rows={4} />
          </Form.Item>
          <Form.Item
            name="dorisSql"
            label="Doris SQL"
            rules={[{ required: true, message: '请输入Doris SQL' }]}
          >
            <Input.TextArea rows={4} />
          </Form.Item>
        </Form>
      </Modal>
    </Spin>
  );
}
