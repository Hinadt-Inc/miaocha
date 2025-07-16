import { Modal, Form, Input, Select, Row, Col } from 'antd';
import { useEffect } from 'react';
import { useSelector } from 'react-redux';

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
  // 获取当前用户信息
  const currentUser = useSelector((state: { user: { role: string; userId: number } }) => state.user);
  
  const isSuperAdmin = selectedRecord?.role === 'SUPER_ADMIN';
  const isCurrentUserAdmin = currentUser.role === 'ADMIN';
  const isTargetUserAdmin = selectedRecord?.role === 'ADMIN';
  const isEditingSelf = currentUser.userId && selectedRecord?.key === currentUser.userId.toString();
  
  // 权限检查：如果当前用户是管理员，且目标用户也是管理员，则不可编辑
  // 但是可以编辑自己的信息
  const isReadOnly = isSuperAdmin || (isCurrentUserAdmin && isTargetUserAdmin && !isEditingSelf);
  
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

  // 生成模态框标题
  const getModalTitle = () => {
    if (!selectedRecord) return '添加用户';
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
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item
              name="nickname"
              label="昵称"
              rules={[
                { required: true, message: '请输入昵称' },
                { max: 128, message: '昵称长度不能超过128个字符' },
              ]}
            >
              <Input 
                placeholder="请输入昵称" 
                maxLength={128} 
                disabled={isReadOnly}
              />
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
              <Input 
                placeholder="请输入邮箱" 
                maxLength={128} 
                disabled={isReadOnly}
              />
            </Form.Item>
          </Col>
        </Row>

        <Row gutter={16}>
          <Col span={12}>
            <Form.Item name="role" label="角色" rules={[{ required: true, message: '请选择角色' }]}>
              <Select 
                options={roleOptions} 
                placeholder="请选择角色" 
                disabled={isReadOnly}
              />
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
          <Row gutter={16}>
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
