import { useState } from 'react';
import { Button, Popconfirm } from 'antd';
import type { TableColumnsType, TablePaginationConfig } from 'antd';
import type { Machine } from '@/types/machineTypes';
import dayjs from 'dayjs';
import styles from '../MachineManagement.module.less';

interface UseTableConfigProps {
  onEdit: (record: Machine) => void;
  onDelete: (record: Machine) => Promise<void>;
}

export const useTableConfig = ({ onEdit, onDelete }: UseTableConfigProps) => {
  const [pagination, setPagination] = useState<TablePaginationConfig>({
    current: 1,
    pageSize: 10,
    showSizeChanger: true,
    showQuickJumper: true,
    showTotal: (total) => `共 ${total} 条`,
    pageSizeOptions: ['10', '20', '50', '100'],
  });

  const columns: TableColumnsType<Machine> = [
    { title: '名称', dataIndex: 'name', key: 'name' },
    { title: 'IP地址', dataIndex: 'ip', key: 'ip' },
    { title: '端口', dataIndex: 'port', key: 'port' },
    { title: '用户名', dataIndex: 'username', key: 'username' },
    {
      title: '更新时间',
      dataIndex: 'updateTime',
      key: 'updateTime',
      render: (text) => (text ? dayjs(text).format('YYYY-MM-DD HH:mm:ss') : '-'),
    },
    {
      title: '更新人',
      dataIndex: 'updateUser',
      key: 'updateUser',
      render: (text) => text || '-',
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <div className={styles.actions}>
          <Button type="link" size="small" onClick={() => onEdit(record)} style={{ padding: '0 8px' }}>
            编辑
          </Button>
          <Popconfirm
            title="确定要删除这台机器吗？"
            description="此操作不可撤销"
            onConfirm={() => {
              return void onDelete(record);
            }}
            okText="确定"
            cancelText="取消"
          >
            <Button type="link" size="small" danger style={{ padding: '0 8px' }}>
              删除
            </Button>
          </Popconfirm>
        </div>
      ),
    },
  ];

  // 处理表格变更（分页、排序、筛选）
  const handleTableChange = (newPagination: TablePaginationConfig) => {
    setPagination((prev) => ({
      ...prev,
      current: newPagination.current,
      pageSize: newPagination.pageSize,
    }));
  };

  return {
    columns,
    pagination,
    handleTableChange,
  };
};
