import { useState } from 'react';
import { Form } from 'antd';
import { createUser, updateUser, deleteUser, changeUserPassword, changeMyPassword } from '@/api/user';
import { batchAuthorizeModules } from '@/api/modules';
import { useErrorContext, ErrorType } from '@/providers/ErrorProvider';
import type { UserData } from '../components';

interface UseUserActionsProps {
  setData: React.Dispatch<React.SetStateAction<UserData[]>>;
  data: UserData[];
  originalDataRef: { current: UserData[] };
  moduleList: Array<{ value: string; label: string }>;
  fetchUsers: () => Promise<void>;
}

export const useUserActions = ({ setData, data, originalDataRef, moduleList, fetchUsers }: UseUserActionsProps) => {
  const [isModalVisible, setIsModalVisible] = useState(false);
  const [isPasswordModalVisible, setIsPasswordModalVisible] = useState(false);
  const [moduleDrawerVisible, setModuleDrawerVisible] = useState(false);
  const [selectedRecord, setSelectedRecord] = useState<UserData | null>(null);
  const [selectedUserForDrawer, setSelectedUserForDrawer] = useState<UserData | null>(null);

  const [form] = Form.useForm();
  const [passwordForm] = Form.useForm();

  const { handleError, showSuccess } = useErrorContext();

  // 处理添加/编辑用户
  const handleAddEdit = (record?: UserData) => {
    setSelectedRecord(record ?? null);
    setIsModalVisible(true);
  };

  // 处理删除用户
  const handleDelete = async (key: string) => {
    try {
      // 检查是否为超级管理员
      const user = data.find(item => item.key === key);
      if (user?.role === 'SUPER_ADMIN') {
        handleError('超级管理员用户不能被删除', {
          type: ErrorType.VALIDATION,
          showType: 'message',
        });
        return;
      }
      
      await deleteUser(key);
      setData(data.filter((item) => item.key !== key));
      showSuccess('用户删除成功');
    } catch {
      // API 错误已由全局错误处理器处理，这里不再重复处理
    }
  };

  // 处理修改密码
  const handleChangePassword = (record: UserData) => {
    setSelectedRecord(record);
    setIsPasswordModalVisible(true);
  };

  // 提交用户表单
  const handleSubmit = async () => {
    try {
      const values = (await form.validateFields()) as {
        nickname: string;
        email: string;
        role: string;
        status: number;
        password?: string;
      };

      if (selectedRecord) {
        // 检查是否为超级管理员
        if (selectedRecord.role === 'SUPER_ADMIN') {
          handleError('超级管理员用户信息不能被修改', {
            type: ErrorType.VALIDATION,
            showType: 'message',
          });
          return;
        }
        
        // 编辑现有用户
        await updateUser({
          id: selectedRecord.key,
          nickname: values.nickname,
          email: values.email,
          role: values.role,
          status: values.status,
        });
        showSuccess(`用户 "${values.nickname}" 信息更新成功`);
      } else {
        // 添加新用户
        if (!values.password) {
          handleError('创建新用户时密码不能为空', {
            type: ErrorType.VALIDATION,
            showType: 'message',
          });
          return;
        }

        await createUser({
          username: values.nickname,
          nickname: values.nickname,
          password: values.password,
          email: values.email,
          role: values.role,
          status: values.status,
        });
        showSuccess(`用户 "${values.nickname}" 创建成功`);
      }

      setIsModalVisible(false);
      await fetchUsers(); // 刷新数据
    } catch (error) {
      if (error && typeof error === 'object' && 'errorFields' in error) {
        // 表单验证错误，这类错误不会触发全局错误处理器
        return;
      }
      // API 错误已由全局错误处理器处理，这里不再重复处理
    }
  };

  // 提交密码修改
  const handlePasswordSubmit = async () => {
    try {
      const values = await passwordForm.validateFields();
      if (selectedRecord) {
        const isSuperAdmin = selectedRecord.role === 'SUPER_ADMIN';
        if (isSuperAdmin) {
          await changeMyPassword({
            oldPassword: values.oldPassword,
            newPassword: values.newPassword,
          });
        } else {
          await changeUserPassword(selectedRecord.key, values.newPassword);
        }
        const userTitle = `用户 ${selectedRecord.nickname}`;
        const usperAdminTips = `密码修改成功`;
        showSuccess(isSuperAdmin ? usperAdminTips : `${userTitle} ${usperAdminTips}`);
        setIsPasswordModalVisible(false);
      }
    } catch (error) {
      if (error && typeof error === 'object' && 'errorFields' in error) {
        // 表单验证错误，这类错误不会触发全局错误处理器
        return;
      }
      // API 错误已由全局错误处理器处理，这里不再重复处理
    }
  };

  // 打开模块授权抽屉
  const handleOpenModuleDrawer = (record: UserData) => {
    setSelectedUserForDrawer(record);
    setModuleDrawerVisible(true);
  };

  // 保存模块权限
  const handleSaveModules = async (userId: string, modules: Array<{ moduleId: string; expireTime?: string }>) => {
    try {
      // 获取模块名称列表
      const moduleNames = modules.map((m) => {
        const moduleInfo = moduleList.find((ml) => ml.value === m.moduleId);
        return moduleInfo?.label || m.moduleId;
      });
      await batchAuthorizeModules(userId, moduleNames);
      showSuccess('模块权限更新成功');
      await fetchUsers();
      // 刷新抽屉用户数据，保证弹窗内容同步
      if (selectedUserForDrawer) {
        const latest = originalDataRef.current.find((u) => u.key === selectedUserForDrawer.key);
        if (latest) setSelectedUserForDrawer(latest);
      }
    } catch {
      // API 错误已由全局错误处理器处理，这里不再重复处理
    }
  };

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
  };
};
