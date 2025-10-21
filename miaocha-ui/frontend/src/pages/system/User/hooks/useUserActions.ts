import { useState, useCallback } from 'react';
import { useRequest } from 'ahooks';
import { Form, message } from 'antd';
import { createUser, updateUser, deleteUser, changePasswordByAdmin, changePasswordBySuperAdmin } from '@/api/user';
import { batchAuthorizeModules } from '@/api/user';
import type { UserListItem, ChangePasswordPayload, CreateUserPayload } from '../types';

interface Props {
  moduleList: IStatus[];
  fetchUsers: () => Promise<void>;
}

export const useUserActions = ({ moduleList, fetchUsers }: Props) => {
  // 获取当前用户信息
  const [messageApi, messageContextHolder] = message.useMessage();
  const [isModalVisible, setIsModalVisible] = useState(false);
  const [isPasswordModalVisible, setIsPasswordModalVisible] = useState(false);
  const [moduleDrawerVisible, setModuleDrawerVisible] = useState(false);
  const [selectedRecord, setSelectedRecord] = useState<UserListItem | null>(null);
  const [selectedUserForDrawer, setSelectedUserForDrawer] = useState<UserListItem | null>(null);

  const [form] = Form.useForm();
  const [passwordForm] = Form.useForm<ChangePasswordPayload>();

  // 处理添加/编辑用户
  const handleAddEdit = useCallback((record?: UserListItem) => {
    setSelectedRecord(record ?? null);
    setIsModalVisible(true);
  }, []);

  // 处理删除用户
  const { run: handleDelete } = useRequest(
    async (id: number) => {
      await deleteUser(id);
      messageApi.success('用户删除成功');
      await fetchUsers();
    },
    { manual: true },
  );

  // 处理修改密码
  const handleChangePassword = useCallback((record: UserListItem) => {
    setSelectedRecord(record);
    setIsPasswordModalVisible(true);
  }, []);

  // 提交用户表单
  const { runAsync: handleSubmit, loading: submitLoading } = useRequest(
    async () => {
      const values = (await form.validateFields()) as CreateUserPayload;
      if (selectedRecord) {
        // 编辑
        await updateUser({
          ...values,
          id: selectedRecord.id,
        });
        messageApi.success('更新成功');
      } else {
        // 添加
        await createUser({
          ...values,
          username: values.nickname,
        });
        messageApi.success('添加成功');
      }
      setIsModalVisible(false);
      await fetchUsers(); // 刷新数据
    },
    { manual: true },
  );

  // 提交密码修改（useRequest）
  const { runAsync: handlePasswordSubmit, loading: passwordSubmitting } = useRequest(
    async () => {
      if (!selectedRecord) return;
      const values = await passwordForm.validateFields();
      const isSuperAdmin = selectedRecord.role === 'SUPER_ADMIN';
      if (isSuperAdmin) {
        await changePasswordBySuperAdmin(values);
      } else {
        await changePasswordByAdmin(selectedRecord.id, values.newPassword);
      }
      messageApi.success('密码修改成功');
      setIsPasswordModalVisible(false);
      await fetchUsers();
    },
    { manual: true },
  );

  // 打开模块授权抽屉
  const handleOpenModuleDrawer = useCallback((record: UserListItem) => {
    setSelectedUserForDrawer(record);
    setModuleDrawerVisible(true);
  }, []);

  // 保存模块权限
  const handleSaveModules = useCallback(
    async (userId: number, modules: { moduleId: string; expireTime?: string }[]) => {
      // 获取模块名称列表
      const moduleNames = modules.map((m) => {
        const moduleInfo = moduleList.find((ml) => ml.value === m.moduleId);
        return moduleInfo?.label || m.moduleId;
      });
      await batchAuthorizeModules(userId, moduleNames);
    },
    [moduleList],
  );

  return {
    // 状态
    isModalVisible,
    setIsModalVisible,
    isPasswordModalVisible,
    setIsPasswordModalVisible,
    moduleDrawerVisible,
    setModuleDrawerVisible,
    selectedRecord,
    selectedUserForDrawer,
    setSelectedUserForDrawer,

    // 表单
    form,
    passwordForm,

    // 操作方法
    handleAddEdit,
    handleDelete,
    handleChangePassword,
    handleSubmit,
    handlePasswordSubmit,
    handleOpenModuleDrawer,
    handleSaveModules,
    submitLoading,
    passwordSubmitting,
    messageContextHolder,
  };
};
