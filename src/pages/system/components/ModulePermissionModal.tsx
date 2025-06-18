import { Modal, Table, Space, Button, Input, Tag, message, Descriptions } from 'antd';
import styles from './ModulePermissionModal.module.less';
import { batchRevokeModules, authorizeModule, revokeModule } from '../../../api/modules';
import { SearchOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useState } from 'react';

import type { ModulePermission } from '@/types/permissionTypes';

interface ModulePermissionModalProps {
  open: boolean;
  onClose: () => void;
  userId: string;
  nickname: string;
  email: string;
  modules: ModulePermission[];
  userModulePermissions: Array<{ moduleName: string }>;
  allModules: Array<{ value: string; label: string }>;
  onSave: (userId: string, modules: Array<{ moduleId: string; moduleName?: string }>) => Promise<void>;
  onRefresh: () => void;
}

const ModulePermissionModal = ({
  open,
  onClose,
  userId,
  nickname,
  email,
  modules,
  userModulePermissions,
  allModules,
  onSave,
  onRefresh,
}: ModulePermissionModalProps) => {
  const [selectedModules, setSelectedModules] = useState<string[]>([]);
  const [searchText, setSearchText] = useState('');
  const [loading, setLoading] = useState(false);
  const [messageApi, contextHolder] = message.useMessage();

  // 转换所有模块数据
  const allModuleData = allModules.map((m) => ({
    moduleId: m.value,
    moduleName: m.label,
  }));
  // 合并当前用户已有模块和所有模块
  const mergedModules = allModuleData.map((module) => {
    const hasUserPermission = userModulePermissions.some((p) => p.moduleName === module.moduleName);
    const modulePermission = modules.find((m) => m.module === module.moduleName);
    return {
      ...module,
      authorized: hasUserPermission,
      modulePermission,
    };
  });
  console.log('Merged Modules:', mergedModules);

  // 处理搜索
  const filteredModules = mergedModules.filter((module) =>
    module.moduleName.toLowerCase().includes(searchText.toLowerCase()),
  );

  // 处理模块选择变化
  const handleSelectChange = (selectedRowKeys: React.Key[]) => {
    setSelectedModules(selectedRowKeys as string[]);
  };

  // 处理单个模块授权/撤销
  const handleModuleToggle = async (moduleId: string, isAuthorized: boolean) => {
    setLoading(true);
    try {
      const moduleName = allModules.find((m) => m.value === moduleId)?.label || moduleId;
      if (isAuthorized) {
        await revokeModule(userId, moduleName);
      } else {
        await authorizeModule(userId, moduleName);
      }
      messageApi.success(!isAuthorized ? '授权成功' : '撤销成功');
      // 成功后改变状态
      onClose();
      onRefresh();
    } catch (error) {
      messageApi.error('操作失败');
      console.error('模块权限操作失败:', error);
    } finally {
      setLoading(false);
    }
  };

  // 处理批量操作
  const handleBatchOperation = async (authorize: boolean) => {
    if (selectedModules.length === 0) {
      messageApi.warning('请至少选择一个模块');
      return;
    }

    setLoading(true);
    try {
      if (authorize) {
        const modulesToUpdate = selectedModules.map((moduleId) => ({
          moduleId,
          moduleName: allModules.find((m) => m.value === moduleId)?.label || moduleId,
        }));
        await onSave(userId, modulesToUpdate);
        onClose();
      } else {
        await batchRevokeModules(userId, selectedModules);
        messageApi.success('批量撤销成功');
        onClose();
      }
      setSelectedModules([]);
    } catch {
      messageApi.error('操作失败');
    } finally {
      setLoading(false);
    }
  };

  // 表格列定义
  const columns: ColumnsType<(typeof mergedModules)[0]> = [
    {
      title: '模块名称',
      dataIndex: 'moduleName',
      key: 'moduleName',
      render: (text) => <span>{text}</span>,
    },
    {
      title: '状态',
      dataIndex: 'authorized',
      key: 'status',
      render: (authorized, record) => (
        <Tag color={authorized ? 'green' : 'red'}>
          {authorized ? `已授权 (${record.modulePermission?.datasourceName})` : '未授权'}
        </Tag>
      ),
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Button type="link" onClick={() => handleModuleToggle(record.moduleId, record.authorized)} loading={loading}>
          {record.authorized ? '撤销' : '授权'}
        </Button>
      ),
    },
  ];

  return (
    <>
      {contextHolder}
      <Modal
        title="模块权限管理"
        open={open}
        onCancel={onClose}
        width={800}
        style={{ top: 20 }}
        footer={[
          <Space key="footer-buttons">
            <Button onClick={() => handleBatchOperation(false)} disabled={selectedModules.length === 0}>
              批量撤销
            </Button>
            <Button type="primary" onClick={() => handleBatchOperation(true)} disabled={selectedModules.length === 0}>
              批量授权
            </Button>
            <Button onClick={onClose}>关闭</Button>
          </Space>,
        ]}
      >
        <div className={styles.header}>
          <Descriptions size="small" column={2}>
            <Descriptions.Item label="用户">{nickname}</Descriptions.Item>
            <Descriptions.Item label="邮箱">{email}</Descriptions.Item>
          </Descriptions>
          <div>
            <Input
              placeholder="搜索模块名称"
              value={searchText}
              onChange={(e) => setSearchText(e.target.value)}
              style={{ width: 200 }}
              allowClear
              suffix={<SearchOutlined />}
            />
          </div>
        </div>
        <Table
          rowKey="moduleId"
          bordered
          columns={columns}
          dataSource={filteredModules}
          size="small"
          rowSelection={{
            selectedRowKeys: selectedModules,
            onChange: handleSelectChange,
            getCheckboxProps: (record) => ({
              disabled: record.authorized,
            }),
          }}
          pagination={{ pageSize: 10 }}
        />
      </Modal>
    </>
  );
};

export default ModulePermissionModal;
