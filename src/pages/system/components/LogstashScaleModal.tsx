import { Form, Input, Modal, Select, Checkbox, message, Divider, Typography } from 'antd';
import { useEffect, useState } from 'react';
import type { LogstashProcess } from '../../../types/logstashTypes';
import type { Machine } from '../../../types/machineTypes';
import { getMachines } from '../../../api/machine';

const { Title, Text } = Typography;
const { Option } = Select;

interface LogstashScaleModalProps {
  visible: boolean;
  onCancel: () => void;
  onOk: (params: {
    addMachineIds: number[];
    removeMachineIds: number[];
    customDeployPath: string;
    forceScale: boolean;
  }) => Promise<void>;
  currentProcess?: LogstashProcess | null;
  initialParams: {
    addMachineIds: number[];
    removeMachineIds: number[];
    customDeployPath: string;
    forceScale: boolean;
  };
}

export default function LogstashScaleModal({
  visible,
  onCancel,
  onOk,
  currentProcess,
  initialParams,
}: Readonly<LogstashScaleModalProps>) {
  const [form] = Form.useForm();
  const [confirmLoading, setConfirmLoading] = useState(false);
  const [machines, setMachines] = useState<Machine[]>([]);
  const [loading, setLoading] = useState(false);

  // 获取当前进程已使用的机器ID
  const currentMachineIds = currentProcess?.machineStatuses?.map((m) => m.machineId) || [];

  // 可添加的机器（排除当前已使用的机器）
  const availableMachines = machines.filter((machine) => !currentMachineIds.includes(machine.id));

  // 可移除的机器（当前进程使用的机器）
  const removableMachines = machines.filter((machine) => currentMachineIds.includes(machine.id));

  useEffect(() => {
    const fetchMachines = async () => {
      if (visible) {
        setLoading(true);
        try {
          const machineList = await getMachines();
          setMachines(machineList);

          // 设置表单初始值
          form.setFieldsValue({
            addMachineIds: initialParams.addMachineIds,
            removeMachineIds: initialParams.removeMachineIds,
            customDeployPath: initialParams.customDeployPath,
            forceScale: initialParams.forceScale,
          });
        } catch (err) {
          message.error('获取机器列表失败');
          console.error('获取机器列表失败:', err);
        } finally {
          setLoading(false);
        }
      }
    };

    fetchMachines();
  }, [visible, initialParams, form]);

  const handleOk = async () => {
    try {
      const values = await form.validateFields();

      // 验证至少选择一种操作
      if (
        (!values.addMachineIds || values.addMachineIds.length === 0) &&
        (!values.removeMachineIds || values.removeMachineIds.length === 0)
      ) {
        message.warning('请至少选择一种扩容或缩容操作');
        return;
      }

      setConfirmLoading(true);
      await onOk({
        addMachineIds: values.addMachineIds || [],
        removeMachineIds: values.removeMachineIds || [],
        customDeployPath: values.customDeployPath || '',
        forceScale: values.forceScale || false,
      });
    } catch (err) {
      console.error('表单验证失败:', err);
    } finally {
      setConfirmLoading(false);
    }
  };

  const handleCancel = () => {
    form.resetFields();
    onCancel();
  };

  return (
    <Modal
      title={`扩容/缩容 - ${currentProcess?.name || ''}`}
      open={visible}
      onOk={handleOk}
      onCancel={handleCancel}
      confirmLoading={confirmLoading}
      width={600}
    >
      <Form form={form} layout="vertical" initialValues={initialParams}>
        <Title level={5}>当前进程信息</Title>
        <div style={{ marginBottom: 16, padding: 12, backgroundColor: '#f5f5f5', borderRadius: 4 }}>
          <Text strong>进程名称: </Text>
          <Text>{currentProcess?.name}</Text>
          <br />
          <Text strong>模块: </Text>
          <Text>{currentProcess?.module}</Text>
          <br />
          <Text strong>当前机器数量: </Text>
          <Text>{currentMachineIds.length}</Text>
          <br />
          <Text strong>部署路径: </Text>
          <Text>{currentProcess?.customDeployPath || '默认路径'}</Text>
        </div>

        <Divider />

        <Title level={5}>扩容操作</Title>
        <Form.Item name="addMachineIds" label="添加机器" help={`可添加 ${availableMachines.length} 台机器`}>
          <Select
            mode="multiple"
            placeholder="选择要添加的机器"
            loading={loading}
            showSearch
            filterOption={(input, option) => {
              const label = option?.label as string;
              return label?.toLowerCase().includes(input.toLowerCase());
            }}
          >
            {availableMachines.map((machine) => (
              <Option key={machine.id} value={machine.id}>
                {machine.name} ({machine.ip})
              </Option>
            ))}
          </Select>
        </Form.Item>

        <Divider />

        <Title level={5}>缩容操作</Title>
        <Form.Item name="removeMachineIds" label="移除机器" help={`当前有 ${removableMachines.length} 台机器可移除`}>
          <Select
            mode="multiple"
            placeholder="选择要移除的机器"
            loading={loading}
            showSearch
            filterOption={(input, option) => {
              const label = option?.label as string;
              return label?.toLowerCase().includes(input.toLowerCase());
            }}
          >
            {removableMachines.map((machine) => (
              <Option key={machine.id} value={machine.id}>
                {machine.name} ({machine.ip})
              </Option>
            ))}
          </Select>
        </Form.Item>

        <Divider />

        <Title level={5}>部署配置</Title>
        <Form.Item name="customDeployPath" label="自定义部署路径" help="可选，留空则使用默认路径">
          <Input placeholder="例如: /opt/logstash" />
        </Form.Item>

        <Form.Item name="forceScale" valuePropName="checked" help="强制执行扩容/缩容操作，即使某些检查失败">
          <Checkbox>强制执行</Checkbox>
        </Form.Item>
      </Form>
    </Modal>
  );
}
