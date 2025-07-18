import { Form, Input, Modal, Select, Checkbox, message, Divider, Typography } from 'antd';
import { useEffect, useState } from 'react';
import type { LogstashProcess } from '@/types/logstashTypes';
import type { Machine } from '@/types/machineTypes';
import { getMachines } from '@/api/machine';

const { Title, Text } = Typography;
const { Option } = Select;

interface LogstashScaleModalProps {
  visible: boolean;
  onCancel: () => void;
  onOk: (params: { addMachineIds: number[]; customDeployPath: string; forceScale: boolean }) => Promise<void>;
  currentProcess?: LogstashProcess | null;
  initialParams: {
    addMachineIds: number[];
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

  // 当前进程使用的机器数量
  const currentMachineCount = currentProcess?.logstashMachineStatusInfo?.length || 0;

  // 获取当前进程中每个机器的实例数量
  const getCurrentProcessMachineInstanceCount = (machineIp: string): number => {
    if (!currentProcess?.logstashMachineStatusInfo) return 0;
    return currentProcess.logstashMachineStatusInfo.filter((info) => info.machineIp === machineIp).length;
  };

  useEffect(() => {
    const fetchMachines = async () => {
      if (visible) {
        setLoading(true);
        try {
          const machineList = await getMachines();
          setMachines(machineList);

          // 重置表单
          form.resetFields();
          form.setFieldsValue({
            addMachineIds: [],
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

      // 验证机器选择
      if (!values.addMachineIds || values.addMachineIds.length === 0) {
        message.warning('请选择要添加的机器');
        return;
      }

      setConfirmLoading(true);
      await onOk({
        addMachineIds: values.addMachineIds || [],
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
      title={`扩容 - ${currentProcess?.name || ''}`}
      open={visible}
      onOk={handleOk}
      onCancel={handleCancel}
      confirmLoading={confirmLoading}
      width={600}
      maskClosable={false}
    >
      <Form form={form} layout="vertical" initialValues={initialParams}>
        <Title level={5}>当前进程信息</Title>
        <div style={{ marginBottom: 16, padding: 12, backgroundColor: '#f5f5f5', borderRadius: 4 }}>
          <Text strong>进程名称: </Text>
          <Text>{currentProcess?.name}</Text>
          <br />
          <Text strong>模块: </Text>
          <Text>{currentProcess?.moduleName}</Text>
          <br />
          <Text strong>当前实例数量: </Text>
          <Text>{currentMachineCount}</Text>
          <br />
          <Text strong>部署路径: </Text>
          <Text>{currentProcess?.customDeployPath || '默认路径'}</Text>
        </div>

        <Divider />

        <Title level={5}>扩容操作</Title>
        <Form.Item name="addMachineIds" label="添加机器" help={`共 ${machines.length} 台机器可选`}>
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
            {machines.map((machine) => {
              const instanceCount = getCurrentProcessMachineInstanceCount(machine.ip);
              return (
                <Option key={machine.id} value={machine.logstashMachineId}>
                  {machine.name} ({machine.ip}) {instanceCount > 0 ? `- 当前 ${instanceCount} 个实例` : ''}
                </Option>
              );
            })}
          </Select>
        </Form.Item>

        <Divider />

        <Title level={5}>部署配置</Title>
        <Form.Item name="customDeployPath" label="自定义部署路径" help="可选，留空则使用默认路径">
          <Input placeholder="例如: /opt/logstash" />
        </Form.Item>

        <Form.Item name="forceScale" valuePropName="checked" help="强制执行扩容操作，即使某些检查失败">
          <Checkbox>强制执行</Checkbox>
        </Form.Item>
      </Form>
    </Modal>
  );
}
