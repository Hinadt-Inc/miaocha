import { Modal, Form, Row, Col, Input, Select } from 'antd';
import { useEffect } from 'react';
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

  // 处理表单初始化和数据回显
  useEffect(() => {
    if (visible) {
      if (selectedRecord) {
        // 编辑模式：回显数据
        form.setFieldsValue({
          name: selectedRecord.name,
          datasourceId: selectedRecord.datasourceId,
          tableName: selectedRecord.tableName,
        });
      } else {
        // 添加模式：清空表单
        form.resetFields();
      }
    }
  }, [visible, selectedRecord, form]);

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
      maskClosable={false}
      open={visible}
      title={selectedRecord ? '编辑模块' : '添加模块'}
      width={600}
      onCancel={onCancel}
      onOk={handleSubmit}
    >
      <Form form={form} layout="vertical">
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item label="模块名称" name="name" rules={[{ required: true, message: '请输入模块名称' }]}>
              <Input maxLength={128} placeholder="请输入模块名称" />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item label="数据源" name="datasourceId" rules={[{ required: true, message: '请选择数据源' }]}>
              <Select
                options={dataSources.map((ds) => ({
                  value: ds.id,
                  label: ds.name,
                }))}
                placeholder="请选择数据源"
              />
            </Form.Item>
          </Col>
        </Row>

        <Row gutter={16}>
          <Col span={12}>
            <Form.Item label="表名" name="tableName" rules={[{ required: true, message: '请输入表名' }]}>
              <Input maxLength={128} placeholder="请输入表名" />
            </Form.Item>
          </Col>
        </Row>
      </Form>
    </Modal>
  );
};

export default ModuleFormModal;
