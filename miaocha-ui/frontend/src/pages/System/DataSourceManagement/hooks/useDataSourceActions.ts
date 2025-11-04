import { useState, useRef } from 'react';
import { App } from 'antd';
import {
  createDataSource,
  updateDataSource,
  deleteDataSource,
  testDataSourceConnection,
  testExistingDataSourceConnection,
} from '@/api/datasource';
import type { CreateDataSourceParams, TestConnectionParams } from '@/types/datasourceTypes';
import type { DataSourceItem } from './useDataSourceData';
import type { DataSourceFormData } from '../components';
// import { useErrorContext, ErrorType } from '@/providers/ErrorProvider';
import type { ActionType } from '@ant-design/pro-components';

interface UseDataSourceActionsProps {
  setSubmitLoading: (loading: boolean) => void;
  setTestLoading: (loading: boolean) => void;
  setTestExistingLoading: (id: string, loading: boolean) => void;
}

export const useDataSourceActions = ({
  setSubmitLoading,
  setTestLoading,
  setTestExistingLoading,
}: UseDataSourceActionsProps) => {
  const [modalVisible, setModalVisible] = useState<boolean>(false);
  const [currentDataSource, setCurrentDataSource] = useState<DataSourceFormData | undefined>(undefined);
  const { message } = App.useApp();
  /**
   * 展示成功提示
   * @param content 成功提示内容
   */
  const showSuccess = (content: string) => {
    message.success(content);
  };
  // const { handleError, showSuccess } = useErrorContext();
  const actionRef = useRef<ActionType>(null);

  // 打开新增模态框
  const openCreateModal = () => {
    setCurrentDataSource(undefined);
    setModalVisible(true);
  };

  // 打开编辑模态框
  const openEditModal = (record: DataSourceItem) => {
    // 确保数据中的字段映射正确
    const mappedRecord: DataSourceFormData = {
      id: record.id,
      name: record.name,
      type: record.type,
      description: record.description,
      jdbcUrl: record.jdbcUrl || '',
      username: record.username,
      password: record.password,
      ip: record.ip,
      port: record.port,
      database: record.database,
      createTime: record.createdAt,
      createUser: record.createUser,
      updateTime: record.updatedAt,
      updateUser: record.updateUser,
    };
    setCurrentDataSource(mappedRecord);
    setModalVisible(true);
  };

  // 处理表单提交
  const handleFormSubmit = async (values: Omit<CreateDataSourceParams, 'id'>): Promise<boolean> => {
    setSubmitLoading(true);
    try {
      if (currentDataSource) {
        // 更新操作 - 如果密码为空，则不包含password字段，保持原有密码不变
        const updateParams: any = {
          ...values,
          id: currentDataSource.id!,
        };

        if (!values.password || values.password.trim() === '') {
          delete updateParams.password;
        }

        const updated = await updateDataSource(currentDataSource.id!, updateParams);
        if (updated) {
          showSuccess(`数据源 "${values.name}" 更新成功`);
        }
      } else {
        // 新增操作
        const newDataSource = await createDataSource(values);
        if (newDataSource) {
          showSuccess(`数据源 "${values.name}" 创建成功`);
        }
      }

      setModalVisible(false);
      // 保留当前分页设置进行重新加载
      actionRef.current?.reload();
      return true;
    } catch {
      // API 错误已由全局错误处理器处理，这里不再重复处理
      return false;
    } finally {
      setSubmitLoading(false);
    }
  };

  // 删除数据源
  const handleDelete = async (id: string, name: string) => {
    try {
      await deleteDataSource(id);
      showSuccess(`数据源 "${name}" 删除成功`);
      // 直接刷新表格数据
      actionRef.current?.reload();
    } catch {
      // API 错误已由全局错误处理器处理，这里不再重复处理
    }
  };

  // 测试数据库连接
  const handleTestConnection = async (values: TestConnectionParams) => {
    // 在编辑模式下，如果密码为空，验证是否有原有密码可用
    const passwordToUse = values.password || (currentDataSource?.password ?? '');
    if (!values.jdbcUrl || !values.username || !passwordToUse) {
      const missingFields: string[] = [];
      if (!values.jdbcUrl) missingFields.push('JDBC URL');
      if (!values.username) missingFields.push('用户名');
      if (!passwordToUse) missingFields.push('密码');
      message.error(`请完善连接信息：${missingFields.join('、')}不能为空`);
      // handleError(`请完善连接信息：${missingFields.join('、')}不能为空`, {
      //   type: ErrorType.VALIDATION,
      //   showType: 'message',
      // });
      return;
    }

    setTestLoading(true);
    try {
      // 准备测试连接参数
      const testParams = {
        name: values.name, // 添加数据源名称
        type: values.type,
        jdbcUrl: values.jdbcUrl,
        username: values.username,
        password: passwordToUse, // 使用处理后的密码
      } as TestConnectionParams;
      await testDataSourceConnection(testParams);
      showSuccess('数据库连接测试成功！连接配置正确，可以正常访问数据库');
    } catch {
      // API 错误已由全局错误处理器处理，这里不再重复处理
    } finally {
      setTestLoading(false);
    }
  };

  // 测试现有数据源连接
  const handleTestExistingConnection = async (id: string, name: string) => {
    setTestExistingLoading(id, true);
    try {
      await testExistingDataSourceConnection(id);
      showSuccess(`数据源 "${name}" 连接测试成功！数据库连接正常，可以正常使用`);
    } catch {
      // API 错误已由全局错误处理器处理，这里不再重复处理
    } finally {
      setTestExistingLoading(id, false);
    }
  };

  return {
    // 状态
    modalVisible,
    setModalVisible,
    currentDataSource,
    actionRef,

    // 操作方法
    openCreateModal,
    openEditModal,
    handleFormSubmit,
    handleDelete,
    handleTestConnection,
    handleTestExistingConnection,
  };
};
