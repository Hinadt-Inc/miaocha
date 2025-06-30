import { Modal, Form, Row, Col, Input, Select } from 'antd';
import type { DataSource } from '@/types/datasourceTypes';
import type { ModuleData, ModuleFormData } from '../types';

interface ModuleFormModalProps {
  visible: boolean;
  selectedRecord: ModuleData | null;
  dataSources: DataSource[];
  onSubmit: (values: ModuleFormData) => Promise<void>;
  onCancel: () => void;
}

const ModuleFormModal: React.FC<ModuleFormModalProps> = ({
  visible,
  selectedRecord,
  dataSources,
  onSubmit,
  onCancel,
}) => {
  const [form] = Form.useForm();

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      await onSubmit(values);
    } catch (error) {
      console.error('表单验证失败:', error);
    }
  };

  return (
    <Modal
      title={selectedRecord ? '编辑模块' : '添加模块'}
      open={visible}
      onOk={handleSubmit}
      onCancel={onCancel}
      width={600}
      maskClosable={false}
    >
      <Form form={form} layout="vertical" initialValues={selectedRecord || {}}>
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item name="name" label="模块名称" rules={[{ required: true, message: '请输入模块名称' }]}>
              <Input placeholder="请输入模块名称" maxLength={128} />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item name="datasourceId" label="数据源" rules={[{ required: true, message: '请选择数据源' }]}>
              <Select
                placeholder="请选择数据源"
                options={dataSources.map((ds) => ({
                  value: ds.id,
                  label: ds.name,
                }))}
              />
            </Form.Item>
          </Col>
        </Row>

        <Row gutter={16}>
          <Col span={12}>
            <Form.Item name="tableName" label="表名" rules={[{ required: true, message: '请输入表名' }]}>
              <Input placeholder="请输入表名" maxLength={128} />
            </Form.Item>
          </Col>
        </Row>
      </Form>
    </Modal>
  );
};

export default ModuleFormModal;
