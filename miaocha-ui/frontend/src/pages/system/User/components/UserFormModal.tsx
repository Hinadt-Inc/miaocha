import { Modal, Form, Input, Select, Row, Col } from 'antd';
import { useEffect } from 'react';
import { useSelector } from 'react-redux';

export interface UserData {
  id: string;
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

const UserFormModal: React.FC<UserFormModalProps> = ({ visible, selectedRecord, onSubmit, onCancel, form }) => {
  // 获取当前用户信息
  const currentUser = useSelector((state: { user: { role: string; userId: number } }) => state.user);

  const isSuperAdmin = selectedRecord?.role === 'SUPER_ADMIN';
  const isCurrentUserAdmin = currentUser.role === 'ADMIN';
  const isCurrentUserSuperAdmin = currentUser.role === 'SUPER_ADMIN';
  const isTargetUserAdmin = selectedRecord?.role === 'ADMIN';
  const isEditingSelf = currentUser.userId && selectedRecord?.id?.toString() === currentUser.userId.toString();
  const isAddingNewUser = !selectedRecord;

  // 权限检查：如果当前用户是管理员，且目标用户也是管理员，则不可编辑
  // 但是可以编辑自己的信息
  const isReadOnly = isSuperAdmin || (isCurrentUserAdmin && isTargetUserAdmin && !isEditingSelf);

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
        return [{ value: 'USER', label: '普通用户' }];
      } else if (isEditingSelf) {
        // 管理员编辑自己时，可以保持管理员角色
        return allRoles;
      } else {
        // 管理员编辑其他用户时，只能设置为普通用户
        return [{ value: 'USER', label: '普通用户' }];
      }
    }

    // 普通用户不应该有权限到达这里，但为了安全起见
    return [{ value: 'USER', label: '普通用户' }];
  };

  useEffect(() => {
    if (visible) {
      form.resetFields();
      if (selectedRecord) {
        form.setFieldsValue({
          ...selectedRecord,
        });
      } else if (isCurrentUserAdmin && !isCurrentUserSuperAdmin) {
        // 管理员新增用户时，默认设置为普通用户角色
        form.setFieldsValue({
          role: 'USER',
          status: 1,
        });
      }
    }
  }, [visible, selectedRecord, form, isCurrentUserAdmin, isCurrentUserSuperAdmin]);

  // 生成模态框标题
  const getModalTitle = () => {
    if (!selectedRecord) {
      if (isCurrentUserAdmin && !isCurrentUserSuperAdmin) {
        return '添加用户 (仅可创建普通用户)';
      }
      return '添加用户';
    }
    if (isSuperAdmin) return '查看用户信息 (超级管理员不可编辑)';
    if (isEditingSelf) return '编辑我的信息';
    if (isCurrentUserAdmin && isTargetUserAdmin) return '查看用户信息 (管理员不能编辑其他管理员)';
    return '编辑用户';
  };

  return (
    <Modal
      title={getModalTitle()}
      open={visible}
      onOk={onSubmit}
      onCancel={onCancel}
      width={600}
      maskClosable={false}
      okButtonProps={{ disabled: isReadOnly }}
      okText={isReadOnly ? '确定' : undefined}
    >
      <Form form={form} layout="vertical">
        <Row gutter={8}>
          <Col span={12}>
            <Form.Item
              name="nickname"
              label="昵称"
              rules={[
                { required: true, message: '请输入昵称' },
                { max: 128, message: '昵称长度不能超过128个字符' },
              ]}
            >
              <Input showCount placeholder="请输入昵称" maxLength={128} disabled={isReadOnly} />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item
              name="email"
              label="邮箱"
              rules={[
                { required: true, message: '请输入邮箱' },
                { type: 'email', message: '请输入有效的邮箱地址' },
                { max: 128, message: '邮箱长度不能超过128个字符' },
              ]}
            >
              <Input showCount placeholder="请输入邮箱" maxLength={128} disabled={isReadOnly} />
            </Form.Item>
          </Col>
        </Row>

        <Row gutter={8}>
          <Col span={12}>
            <Form.Item name="role" label="角色" rules={[{ required: true, message: '请选择角色' }]}>
              <Select options={getRoleOptions()} placeholder="请选择角色" disabled={isReadOnly} />
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
                disabled={isReadOnly}
              />
            </Form.Item>
          </Col>
        </Row>

        {!selectedRecord && (
          <Row gutter={8}>
            <Col span={24}>
              <Form.Item
                name="password"
                label="密码"
                rules={[
                  { required: true, message: '请输入密码' },
                  { min: 6, message: '密码长度不能少于6个字符' },
                  { max: 128, message: '密码长度不能超过128个字符' },
                ]}
              >
                <Input.Password placeholder="请输入密码" maxLength={128} />
              </Form.Item>
            </Col>
          </Row>
        )}
      </Form>
    </Modal>
  );
};

export default UserFormModal;
