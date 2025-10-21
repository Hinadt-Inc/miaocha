import { Modal, Table, Space, Button, Input, Tag, message, Descriptions } from 'antd';
import { revokeModule, authorizeModule, batchRevokeModules } from '@/api/user';
import type { ColumnsType } from 'antd/es/table';
import { useState, useMemo, memo } from 'react';
import type { ModulePermissionListItem } from '../types';

interface Props {
  open: boolean;
  onClose: () => void;
  userId: number;
  nickname: string;
  email: string;
  modules: ModulePermissionListItem[];
  userModulePermissions: { moduleName: string }[];
  allModules: { value: string; label: string }[];
  onSave: (userId: number, modules: { moduleId: string; moduleName?: string }[]) => Promise<void>;
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
}: Props) => {
  console.log('渲染：监听ModulePermissionModal组件');

  const [selectedModules, setSelectedModules] = useState<string[]>([]);
  const [searchText, setSearchText] = useState('');
  const [loading, setLoading] = useState(false);
  const [messageApi, contextHolder] = message.useMessage();
  // 直接计算表格数据，不维护本地状态
  const mergedModules = useMemo(() => {
    return allModules.map((m) => {
      const hasUserPermission = userModulePermissions.some((p) => p.moduleName === m.label);
      const modulePermission = modules.find((mod) => mod.module === m.label);
      return {
        moduleId: m.value,
        moduleName: m.label,
        authorized: hasUserPermission,
        modulePermission,
      };
    });
  }, [modules, userModulePermissions, allModules]);

  // 处理搜索
  const filteredModules = useMemo(() => {
    return mergedModules.filter((module) => module.moduleName.toLowerCase().includes(searchText.toLowerCase()));
  }, [mergedModules, searchText]);

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
      messageApi.success('操作成功');
      // 更新数据
      onRefresh();
    } finally {
      setLoading(false);
    }
  };

  // 批量
  const handleBatchOperation = async (authorize: boolean) => {
    setLoading(true);
    try {
      if (authorize) {
        // 批量授权
        const modulesToUpdate = selectedModules.map((moduleId) => ({
          moduleId,
          moduleName: allModules.find((m) => m.value === moduleId)?.label || moduleId,
        }));
        await onSave(userId, modulesToUpdate);
      } else {
        // 批量撤销
        await batchRevokeModules(userId, selectedModules);
      }
      messageApi.success('操作成功');
      setSelectedModules([]);
      onRefresh();
    } finally {
      setLoading(false);
    }
  };

  // 表格列定义
  const columns: ColumnsType<(typeof mergedModules)[0]> = [
    {
      title: '模块名称',
      dataIndex: 'moduleName',
    },
    {
      title: '状态',
      dataIndex: 'authorized',
      render: (authorized) => <Tag color={authorized ? 'success' : 'default'}>{authorized ? '已授权' : '未授权'}</Tag>,
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Space className="global-table-action">
          <Button
            danger={record.authorized}
            type="link"
            onClick={() => handleModuleToggle(record.moduleId, record.authorized)}
          >
            {record.authorized ? '撤销' : '授权'}
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <>
      {contextHolder}
      <Modal
        footer={[
          <Space key="footer-buttons">
            <Button disabled={selectedModules.length === 0} onClick={() => handleBatchOperation(false)}>
              批量撤销
            </Button>
            <Button disabled={selectedModules.length === 0} type="primary" onClick={() => handleBatchOperation(true)}>
              批量授权
            </Button>
            <Button onClick={onClose}>关闭</Button>
          </Space>,
        ]}
        open={open}
        title="模块权限管理"
        width={800}
        onCancel={onClose}
      >
        <Descriptions column={3} size="small">
          <Descriptions.Item label="昵称">{nickname}</Descriptions.Item>
          <Descriptions.Item label="邮箱">{email}</Descriptions.Item>
          <Descriptions.Item>
            <Input
              allowClear
              placeholder="搜索模块名称"
              value={searchText}
              onChange={(e) => setSearchText(e.target.value)}
            />
          </Descriptions.Item>
        </Descriptions>
        <Table
          bordered
          columns={columns}
          dataSource={filteredModules}
          loading={loading}
          pagination={{ pageSize: 10 }}
          rowKey="moduleName"
          rowSelection={{
            selectedRowKeys: selectedModules,
            onChange: handleSelectChange,
          }}
          size="small"
          style={{ marginTop: 16 }}
        />
      </Modal>
    </>
  );
};

export default memo(ModulePermissionModal);
