import { Modal, Table, Space, Button, Input, Tag, message } from 'antd';
import { batchRevokeModules } from '../../../api/modules';
import { SearchOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useState } from 'react';

interface Module {
  moduleId: string;
  moduleName: string;
}

interface ModulePermissionModalProps {
  open: boolean;
  onClose: () => void;
  userId: string;
  modules: Module[];
  allModules: Array<{ value: string; label: string }>;
  onSave: (userId: string, modules: Array<{ moduleId: string }>) => Promise<void>;
}

const ModulePermissionModal = ({ open, onClose, userId, modules, allModules, onSave }: ModulePermissionModalProps) => {
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
    const isAuthorized = modules.some((m) => m.moduleId === module.moduleId);
    return {
      ...module,
      authorized: isAuthorized,
    };
  });

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
      await onSave(userId, isAuthorized ? [] : [{ moduleId }]);
      messageApi.success(isAuthorized ? '模块权限已撤销' : '模块授权成功');
    } catch {
      messageApi.error('操作失败');
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
        const modulesToUpdate = selectedModules.map((moduleId) => ({ moduleId }));
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
      render: (authorized) => <Tag color={authorized ? 'green' : 'red'}>{authorized ? '已授权' : '未授权'}</Tag>,
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
        <div style={{ marginBottom: 16 }}>
          <Input
            placeholder="搜索模块名称"
            value={searchText}
            onChange={(e) => setSearchText(e.target.value)}
            style={{ width: 200 }}
            allowClear
            suffix={<SearchOutlined />}
          />
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
