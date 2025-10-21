import { Modal, Form, Input, Select, Switch } from 'antd';
import { useEffect, memo } from 'react';
import { useSelector } from 'react-redux';
import type { FormInstance } from 'antd/es/form';
import * as v from '@/utils/validate';
import type { UserListItem } from '../types';

interface Props {
  visible: boolean;
  selectedRecord: UserListItem | null;
  onSubmit: () => Promise<void>;
  onCancel: () => void;
  form: FormInstance;
  confirmLoading?: boolean;
}

const UserFormModal: React.FC<Props> = ({ visible, selectedRecord, onSubmit, onCancel, form, confirmLoading }) => {
  console.log('渲染：监听UserFormModal组件');
  // 获取当前用户信息
  const currentUser = useSelector((state: { user: { role: string; userId: number } }) => state.user);
  const isCurrentUserAdmin = currentUser.role === 'ADMIN';
  const isCurrentUserSuperAdmin = currentUser.role === 'SUPER_ADMIN';
  const isEditingSelf = currentUser.userId && selectedRecord?.id?.toString() === currentUser.userId.toString();
  const isAddingNewUser = !selectedRecord;

  // 根据当前用户角色动态生成角色选项
  const getRoleOptions = () => {
    const allRoles = [
      { value: 'ADMIN', label: '管理员' },
      { value: 'USER', label: '普通用户' },
    ];

    // 如果当前用户是超级管理员，可以设置所有角色
    if (isCurrentUserSuperAdmin) {
      return allRoles;
    }

    // 如果当前用户是管理员，只能设置普通用户角色（新增用户时）
    // 编辑现有用户时，如果是管理员编辑自己，可以保持管理员角色
    if (isCurrentUserAdmin) {
      if (isAddingNewUser) {
        // 新增用户时，管理员只能创建普通用户
        return [allRoles[1]];
      } else if (isEditingSelf) {
        // 管理员编辑自己时，可以保持管理员角色
        return allRoles;
      } else {
        // 管理员编辑其他用户时，只能设置为普通用户
        return [allRoles[1]];
      }
    }
    return [allRoles[1]];
  };

  useEffect(() => {
    if (!visible) return;
    form.resetFields();

    if (selectedRecord) {
      // 编辑场景：回显已有数据
      form.setFieldsValue({ ...selectedRecord });
    } else {
      // 新增场景
      form.setFieldsValue({
        status: 1, // 默认启用
        ...(isCurrentUserAdmin ? { role: 'USER' } : {}),
      });
    }
  }, [visible, selectedRecord, form, isCurrentUserAdmin, isCurrentUserSuperAdmin]);

  return (
    <Modal
      confirmLoading={confirmLoading}
      maskClosable={false}
      open={visible}
      title={isAddingNewUser ? '新增' : '编辑'}
      onCancel={onCancel}
      onOk={() => form.submit()}
    >
      <Form
        autoComplete="off"
        form={form}
        onFinish={() => {
          void onSubmit();
        }}
      >
        <Form.Item label="昵称" name="nickname" rules={[v.required('昵称'), v.max(128), v.noWhitespace()]}>
          <Input allowClear maxLength={128} placeholder="请输入昵称" showCount />
        </Form.Item>
        <Form.Item
          label="邮箱"
          name="email"
          rules={[v.required('邮箱'), v.max(128), { type: 'email', message: '请输入有效的邮箱地址' }]}
        >
          <Input allowClear autoComplete="off" maxLength={128} placeholder="请输入邮箱" showCount />
        </Form.Item>

        <Form.Item label="角色" name="role" rules={[v.required('角色')]}>
          <Select allowClear options={getRoleOptions()} placeholder="请选择角色" />
        </Form.Item>

        {isAddingNewUser && (
          <Form.Item
            label="密码"
            name="password"
            preserve={false}
            rules={[v.required('密码'), v.passwordPolicy(6, 20)]}
          >
            <Input.Password allowClear autoComplete="new-password" maxLength={128} placeholder="请输入密码" />
          </Form.Item>
        )}
        <Form.Item
          getValueFromEvent={(checked: boolean) => (checked ? 1 : 0)}
          getValueProps={(value) => ({ checked: value === 1 || value === true })}
          label="状态"
          name="status"
          rules={[v.required('状态')]}
          valuePropName="checked"
        >
          <Switch checkedChildren="启用" unCheckedChildren="禁用" />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default memo(UserFormModal);
