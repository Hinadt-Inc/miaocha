import { Modal, Form, Input, Select, Row, Col } from 'antd';
import { useEffect } from 'react';

export interface UserData {
  key: string;
  nickname: string;
  email: string;
  role: string;
  status: number;
  username?: string;
  createTime?: string;
  updateTime?: string;
  modulePermissions?: any[];
}

interface UserFormModalProps {
  visible: boolean;
  selectedRecord: UserData | null;
  onSubmit: () => Promise<void>;
  onCancel: () => void;
  form: any;
}

const roleOptions = [
  { value: 'ADMIN', label: '管理员' },
  { value: 'USER', label: '普通用户' },
];

const UserFormModal: React.FC<UserFormModalProps> = ({ visible, selectedRecord, onSubmit, onCancel, form }) => {
  useEffect(() => {
    if (visible) {
      form.resetFields();
      if (selectedRecord) {
        form.setFieldsValue({
          ...selectedRecord,
        });
      }
    }
  }, [visible, selectedRecord, form]);

  return (
    <Modal
      title={selectedRecord ? '编辑用户' : '添加用户'}
      open={visible}
      onOk={onSubmit}
      onCancel={onCancel}
      width={600}
      maskClosable={false}
    >
      <Form form={form} layout="vertical">
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item name="nickname" label="昵称" rules={[{ required: true, message: '请输入昵称' }]}>
              <Input placeholder="请输入昵称" />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item
              name="email"
              label="邮箱"
              rules={[
                { required: true, message: '请输入邮箱' },
                { type: 'email', message: '请输入有效的邮箱地址' },
              ]}
            >
              <Input placeholder="请输入邮箱" />
            </Form.Item>
          </Col>
        </Row>

        <Row gutter={16}>
          <Col span={12}>
            <Form.Item name="role" label="角色" rules={[{ required: true, message: '请选择角色' }]}>
              <Select options={roleOptions} placeholder="请选择角色" />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item name="status" label="状态" initialValue={1} rules={[{ required: true, message: '请选择状态' }]}>
              <Select
                options={[
                  { value: 1, label: '启用' },
                  { value: 0, label: '禁用' },
                ]}
                placeholder="请选择状态"
              />
            </Form.Item>
          </Col>
        </Row>

        {!selectedRecord && (
          <Row gutter={16}>
            <Col span={24}>
              <Form.Item
                name="password"
                label="密码"
                rules={[
                  { required: true, message: '请输入密码' },
                  { min: 6, message: '密码长度不能少于6个字符' },
                ]}
              >
                <Input.Password placeholder="请输入密码" />
              </Form.Item>
            </Col>
          </Row>
        )}
      </Form>
    </Modal>
  );
};

export default UserFormModal;
