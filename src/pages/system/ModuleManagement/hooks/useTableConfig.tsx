import { useState } from 'react';
import { Space, Button, Tag, Popconfirm, Switch } from 'antd';
import { DatabaseOutlined } from '@ant-design/icons';
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table';
import dayjs from 'dayjs';
import type { ModuleData } from '../types';
import styles from '../ModuleManagement.module.less';

interface UseTableConfigProps {
  onViewDetail: (record: ModuleData) => void;
  onEdit: (record: ModuleData) => void;
  onExecuteSql: (record: ModuleData) => void;
  onDelete: (record: ModuleData) => void;
  onConfig: (record: ModuleData) => void;
  onStatusToggle: (record: ModuleData) => void;
}

export const useTableConfig = ({
  onViewDetail,
  onEdit,
  onExecuteSql,
  onDelete,
  onConfig,
  onStatusToggle,
}: UseTableConfigProps) => {
  const [pagination, setPagination] = useState<TablePaginationConfig>({
    current: 1,
    pageSize: 10,
    showSizeChanger: true,
    showQuickJumper: true,
    showTotal: (total) => `共 ${total} 条`,
    pageSizeOptions: ['10', '20', '50', '100'],
  });

  const columns: ColumnsType<ModuleData> = [
    {
      title: '模块名称',
      dataIndex: 'name',
      key: 'name',
      width: 150,
      render: (text: string, record: ModuleData) => (
        <Tag
          color="#1677ff"
          onClick={() => onViewDetail(record)}
          style={{ cursor: 'pointer' }}
          icon={<DatabaseOutlined />}
        >
          {text}
        </Tag>
      ),
    },
    {
      title: '数据源',
      dataIndex: 'datasourceName',
      key: 'datasourceName',
      width: 150,
    },
    {
      title: '表名',
      dataIndex: 'tableName',
      key: 'tableName',
      width: 150,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 80,
      render: (status: number, record: ModuleData) => (
        <Popconfirm
          title={`确认${status ? '禁用' : '启用'}该模块吗？`}
          description={`${status ? '禁用后该模块将无法使用' : '启用后该模块可以正常使用'}`}
          onConfirm={() => onStatusToggle(record)}
          okText="确认"
          cancelText="取消"
        >
          <Switch checked={status === 1} size="small" />
        </Popconfirm>
      ),
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 160,
      render: (createTime: string) => dayjs(createTime).format('YYYY-MM-DD HH:mm:ss'),
    },
    {
      title: '创建人',
      dataIndex: 'createUserName',
      key: 'createUserName',
      width: 120,
      render: (createUserName: string, record: ModuleData) => createUserName || record.createUser,
    },
    {
      title: '更新时间',
      dataIndex: 'updateTime',
      key: 'updateTime',
      width: 160,
      render: (updateTime: string) => dayjs(updateTime).format('YYYY-MM-DD HH:mm:ss'),
    },
    {
      title: '更新人',
      dataIndex: 'updateUserName',
      key: 'updateUserName',
      width: 120,
      render: (updateUserName: string, record: ModuleData) => updateUserName || record.updateUser,
    },

    {
      title: '操作',
      key: 'action',
      fixed: 'right' as const,
      width: 220,
      render: (_: any, record: ModuleData) => (
        <Space size={0} className={styles.tableActionButtons}>
          {/* <Button type="link" onClick={() => onViewDetail(record)} className={styles.actionButton}>
            详情
          </Button> */}
          <Button type="link" onClick={() => onEdit(record)} className={styles.actionButton}>
            编辑
          </Button>
          <Button type="link" onClick={() => onExecuteSql(record)} className={styles.actionButton}>
            {record.dorisSql?.trim() ? '查看SQL' : '执行SQL'}
          </Button>
          <Button
            type="link"
            // icon={<SettingOutlined />}
            onClick={() => onConfig(record)}
            className={styles.actionButton}
          >
            配置
          </Button>
          <Button type="link" danger className={styles.actionButton} onClick={() => onDelete(record)}>
            删除
          </Button>
        </Space>
      ),
    },
  ];

  const handleTableChange = (paginationConfig: TablePaginationConfig) => {
    setPagination((prev) => ({
      ...prev,
      current: paginationConfig.current,
      pageSize: paginationConfig.pageSize,
    }));
  };

  return {
    columns,
    pagination,
    handleTableChange,
    setPagination,
  };
};
