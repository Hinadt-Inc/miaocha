import { Modal, Form, Input } from 'antd';
import { useEffect, memo } from 'react';
import type { FormInstance } from 'antd/es/form';
import type { UserListItem } from '../types';
import * as v from '@/utils/validate';

interface PasswordModalProps {
  visible: boolean;
  selectedRecord: UserListItem | null;
  onSubmit: () => Promise<void>;
  onCancel: () => void;
  form: FormInstance;
  confirmLoading?: boolean;
}

const PasswordModal: React.FC<PasswordModalProps> = ({
  visible,
  selectedRecord,
  onSubmit,
  onCancel,
  form,
  confirmLoading,
}) => {
  console.log('渲染：监听PasswordModal组件');
  useEffect(() => {
    if (visible) {
      form.resetFields();
    }
  }, [visible, form]);

  return (
    <Modal
      confirmLoading={confirmLoading}
      maskClosable={false}
      open={visible}
      title="修改密码"
      onCancel={onCancel}
      onOk={() => form.submit()}
    >
      <Form
        autoComplete="off"
        form={form}
        labelCol={{ span: 4 }}
        wrapperCol={{ span: 20 }}
        onFinish={() => void onSubmit()}
      >
        <Form.Item label="昵称">
          <Input disabled value={selectedRecord?.nickname ?? selectedRecord?.username} variant="borderless" />
        </Form.Item>
        {selectedRecord?.role === 'SUPER_ADMIN' && (
          <Form.Item
            label="旧密码"
            name="oldPassword"
            preserve={false}
            rules={[v.required('旧密码'), v.passwordPolicy()]}
          >
            <Input.Password allowClear autoComplete="new-password" placeholder="请输入旧密码" />
          </Form.Item>
        )}
        <Form.Item
          label="新密码"
          name="newPassword"
          preserve={false}
          rules={[v.required('新密码'), v.passwordPolicy()]}
        >
          <Input.Password allowClear autoComplete="new-password" placeholder="请输入新密码" />
        </Form.Item>
        <Form.Item
          dependencies={['newPassword']}
          label="确认密码"
          name="confirmPassword"
          preserve={false}
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
          <Input.Password allowClear autoComplete="new-password" placeholder="请再次输入新密码" />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default memo(PasswordModal);
