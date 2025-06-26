import { useState } from 'react';
import { Form } from 'antd';
import {
  createModule,
  updateModule,
  deleteModule,
  executeDorisSql,
  type CreateModuleParams,
  type UpdateModuleParams,
} from '@/api/modules';
import { DORIS_TEMPLATE } from '@/utils/logstashTemplates';
import type { ModuleData, ModuleFormData, Module } from '../types';

interface UseModuleActionsProps {
  messageApi: any;
  onDataChange: () => void;
}

export const useModuleActions = ({ messageApi, onDataChange }: UseModuleActionsProps) => {
  const [form] = Form.useForm();

  // 表单模态框状态
  const [formModalVisible, setFormModalVisible] = useState(false);
  const [selectedRecord, setSelectedRecord] = useState<ModuleData | null>(null);

  // 详情模态框状态
  const [detailModalVisible, setDetailModalVisible] = useState(false);
  const [moduleDetail, setModuleDetail] = useState<Module | null>(null);

  // 删除模态框状态
  const [deleteModalVisible, setDeleteModalVisible] = useState(false);
  const [deleteRecord, setDeleteRecord] = useState<ModuleData | null>(null);
  const [deleteDorisTable, setDeleteDorisTable] = useState(false);

  // SQL执行模态框状态
  const [sqlModalVisible, setSqlModalVisible] = useState(false);
  const [currentRecord, setCurrentRecord] = useState<ModuleData | null>(null);
  const [executeSql, setExecuteSql] = useState('');
  const [executing, setExecuting] = useState(false);
  const [isReadOnlyMode, setIsReadOnlyMode] = useState(false);

  // 配置模态框状态
  const [configModalVisible, setConfigModalVisible] = useState(false);
  const [configRecord, setConfigRecord] = useState<ModuleData | null>(null);

  // 处理添加/编辑
  const handleAddEdit = (record?: ModuleData) => {
    setSelectedRecord(record ?? null);
    form.resetFields();
    if (record) {
      form.setFieldsValue({
        ...record,
      });
    }
    setFormModalVisible(true);
  };

  // 处理表单提交
  const handleFormSubmit = async (values: ModuleFormData) => {
    try {
      if (selectedRecord) {
        // 更新模块
        await updateModule({
          id: Number(selectedRecord.key),
          ...values,
        } as UpdateModuleParams);
        messageApi.success('模块信息已更新');
      } else {
        // 创建模块
        await createModule(values as CreateModuleParams);
        messageApi.success('模块已添加');
      }
      setFormModalVisible(false);
      onDataChange();
    } catch (error) {
      messageApi.error('操作失败');
      console.error('操作失败:', error);
    }
  };

  // 处理查看详情
  const handleViewDetail = (moduleDetail: Module) => {
    setModuleDetail(moduleDetail);
    setDetailModalVisible(true);
  };

  // 处理删除
  const handleDelete = (record: ModuleData) => {
    setDeleteRecord(record);
    setDeleteDorisTable(false);
    setDeleteModalVisible(true);
  };

  // 确认删除
  const handleDeleteConfirm = async () => {
    if (!deleteRecord) return;

    try {
      await deleteModule(Number(deleteRecord.key), deleteDorisTable);
      messageApi.success('模块已删除');
      setDeleteModalVisible(false);
      setDeleteRecord(null);
      onDataChange();
    } catch (error) {
      messageApi.error('删除失败');
      console.error('删除失败:', error);
    }
  };

  // 处理执行SQL
  const handleExecuteSql = (record: ModuleData) => {
    setCurrentRecord(record);
    const hasExistingSql = record.dorisSql?.trim();
    setIsReadOnlyMode(!!hasExistingSql);
    setExecuteSql(hasExistingSql ? record.dorisSql : '');
    setSqlModalVisible(true);
  };

  // 应用SQL模板
  const handleApplyTemplate = () => {
    if (currentRecord) {
      const templateValue = DORIS_TEMPLATE.replace('${tableName}', currentRecord.tableName || '');
      setExecuteSql(templateValue);
    }
  };

  // 确认执行SQL
  const handleSqlExecuteConfirm = async () => {
    if (!currentRecord || !executeSql.trim()) {
      messageApi.warning('请输入有效的SQL语句');
      return;
    }

    try {
      setExecuting(true);
      await executeDorisSql(Number(currentRecord.key), executeSql.trim());
      messageApi.success('SQL执行成功');
      setSqlModalVisible(false);
    } catch (error) {
      messageApi.error('SQL执行失败');
      console.error('SQL执行失败:', error);
    } finally {
      setExecuting(false);
    }
  };

  // 处理配置
  const handleConfig = (record: ModuleData) => {
    setConfigRecord(record);
    setConfigModalVisible(true);
  };

  // 配置成功回调
  const handleConfigSuccess = () => {
    onDataChange();
  };

  return {
    // Form Modal
    form,
    formModalVisible,
    selectedRecord,
    setFormModalVisible,
    handleAddEdit,
    handleFormSubmit,

    // Detail Modal
    detailModalVisible,
    moduleDetail,
    setDetailModalVisible,
    handleViewDetail,

    // Delete Modal
    deleteModalVisible,
    deleteRecord,
    deleteDorisTable,
    setDeleteModalVisible,
    setDeleteDorisTable,
    handleDelete,
    handleDeleteConfirm,

    // SQL Execute Modal
    sqlModalVisible,
    currentRecord,
    executeSql,
    executing,
    isReadOnlyMode,
    setSqlModalVisible,
    setExecuteSql,
    handleExecuteSql,
    handleApplyTemplate,
    handleSqlExecuteConfirm,

    // Config Modal
    configModalVisible,
    configRecord,
    setConfigModalVisible,
    handleConfig,
    handleConfigSuccess,
  };
};
