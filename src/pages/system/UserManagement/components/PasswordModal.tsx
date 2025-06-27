import { Modal, Form, Input } from 'antd';
import { useEffect } from 'react';
import type { UserData } from './UserFormModal';

interface PasswordModalProps {
  visible: boolean;
  selectedRecord: UserData | null;
  onSubmit: () => Promise<void>;
  onCancel: () => void;
  form: any;
}

const PasswordModal: React.FC<PasswordModalProps> = ({ visible, selectedRecord, onSubmit, onCancel, form }) => {
  useEffect(() => {
    if (visible) {
      form.resetFields();
    }
  }, [visible, form]);

  return (
    <Modal title="修改密码" open={visible} onOk={onSubmit} onCancel={onCancel} width={400} maskClosable={false}>
      <Form form={form} layout="vertical">
        <Form.Item label="用户名">
          <Input value={selectedRecord?.nickname || selectedRecord?.username} readOnly disabled />
        </Form.Item>
        <Form.Item
          name="newPassword"
          label="新密码"
          rules={[
            { required: true, message: '请输入新密码' },
            {
              min: 6,
              max: 20,
              message: '密码长度需在6~20个字符之间',
            },
            {
              pattern: /^(?=.*[a-zA-Z])(?=.*\d).+$/,
              message: '密码必须包含字母和数字',
            },
          ]}
        >
          <Input.Password placeholder="请输入新密码" />
        </Form.Item>
        <Form.Item
          name="confirmPassword"
          label="确认新密码"
          dependencies={['newPassword']}
          rules={[
            { required: true, message: '请再次输入新密码' },
            ({ getFieldValue }) => ({
              validator(_, value) {
                if (!value || getFieldValue('newPassword') === value) {
                  return Promise.resolve();
                }
                return Promise.reject(new Error('两次输入的密码不一致'));
              },
            }),
          ]}
        >
          <Input.Password placeholder="请再次输入新密码" />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default PasswordModal;
