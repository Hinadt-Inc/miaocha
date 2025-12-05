import { useState } from 'react';

import { Form } from 'antd';

import { createMachine, updateMachine, deleteMachine, testMachineConnection } from '@/api/machine';
import type { Machine, CreateMachineParams } from '@/types/machineTypes';

interface UseMachineActionsProps {
  fetchMachines: () => Promise<void>;
}

export const useMachineActions = ({ fetchMachines }: UseMachineActionsProps) => {
  const [createModalVisible, setCreateModalVisible] = useState(false);
  const [editModalVisible, setEditModalVisible] = useState(false);
  const [editingMachine, setEditingMachine] = useState<Machine | null>(null);
  const [testingConnection, setTestingConnection] = useState(false);
  const [loading, setLoading] = useState(false);

  const [form] = Form.useForm<CreateMachineParams>();

  // 处理创建机器
  const handleCreate = async (values: CreateMachineParams) => {
    try {
      setTestingConnection(true);
      const testResult = await testMachineConnection(values);
      if (!testResult) {
        window.messageApi.warning('连接测试失败，请检查配置');
        return;
      }

      await createMachine(values);
      window.messageApi.success('机器创建成功');
      setCreateModalVisible(false);
      form.resetFields();
      fetchMachines();
    } catch {
      // API 错误已由全局错误处理器处理，这里不再重复处理
    } finally {
      setTestingConnection(false);
    }
  };

  // 处理编辑机器
  const handleEdit = async (values: CreateMachineParams) => {
    if (!editingMachine) return;
    try {
      setLoading(true);
      const testResult = await testMachineConnection(values);
      if (!testResult) {
        window.messageApi.warning('连接测试失败，请检查配置');
        return;
      }

      await updateMachine({
        ...values,
        id: editingMachine.id,
      });
      window.messageApi.success('机器更新成功');
      setEditModalVisible(false);
      form.resetFields();
      fetchMachines();
    } catch {
      // API 错误已由全局错误处理器处理，这里不再重复处理
    } finally {
      setLoading(false);
    }
  };

  // 处理删除机器
  const handleDelete = async (record: Machine) => {
    try {
      await deleteMachine(record.id);
      window.messageApi.success('删除成功');
      fetchMachines();
    } catch {
      // API 错误已由全局错误处理器处理，这里不再重复处理
    }
  };

  // 处理测试连接
  const handleTestConnection = async () => {
    try {
      const values = await form.validateFields();
      setTestingConnection(true);
      const success = await testMachineConnection(values);
      if (success) {
        window.messageApi.success('连接测试成功');
      } else {
        window.messageApi.warning('连接测试失败');
      }
    } catch (error) {
      if (error && typeof error === 'object' && 'errorFields' in error) {
        // 表单验证错误 - 这类错误不会触发全局错误处理器
        window.messageApi.warning('请完善表单信息');
      }
      // 其他错误（如网络错误、API错误）已由全局错误处理器处理，这里不再重复处理
    } finally {
      setTestingConnection(false);
    }
  };

  // 打开创建模态框
  const handleOpenCreate = () => {
    setCreateModalVisible(true);
  };

  // 打开编辑模态框
  const handleOpenEdit = (record: Machine) => {
    setEditingMachine(record);
    form.setFieldsValue(record);
    setEditModalVisible(true);
  };

  // 关闭创建模态框
  const handleCloseCreate = () => {
    setCreateModalVisible(false);
    form.resetFields();
  };

  // 关闭编辑模态框
  const handleCloseEdit = () => {
    setEditModalVisible(false);
    form.resetFields();
    setEditingMachine(null);
  };

  return {
    // 状态
    createModalVisible,
    editModalVisible,
    editingMachine,
    testingConnection,
    loading,
    form,

    // 操作方法
    handleCreate,
    handleEdit,
    handleDelete,
    handleTestConnection,
    handleOpenCreate,
    handleOpenEdit,
    handleCloseCreate,
    handleCloseEdit,
  };
};
