import { useState } from 'react';
import { Space, Tag, Button, Popconfirm } from 'antd';
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table';
import type { FilterValue, SorterResult } from 'antd/es/table/interface';
import dayjs from 'dayjs';
import { useSelector } from 'react-redux';
import type { UserData } from '../components';

// 角色选项
const roleOptions = [
  { value: 'SUPER_ADMIN', label: '超级管理员', color: 'magenta' },
  { value: 'ADMIN', label: '管理员', color: 'orange' },
  { value: 'USER', label: '普通用户', color: 'cyan' },
];

interface UseTableConfigProps {
  onEdit: (record?: UserData) => void;
  onDelete: (key: string) => Promise<void>;
  onChangePassword: (record: UserData) => void;
  onOpenModuleDrawer: (record: UserData) => void;
}

export const useTableConfig = ({ onEdit, onDelete, onChangePassword, onOpenModuleDrawer }: UseTableConfigProps) => {
  // 获取当前用户信息
  const currentUser = useSelector((state: { user: { role: string; userId: number } }) => state.user);

  const [pagination, setPagination] = useState<TablePaginationConfig>({
    current: 1,
    pageSize: 10,
    showSizeChanger: true,
    showTotal: (total) => `共 ${total} 条`,
    pageSizeOptions: ['10', '20', '50', '100'],
  });
  const [filteredInfo, setFilteredInfo] = useState<Record<string, FilterValue | null>>({});
  const [sortedInfo, setSortedInfo] = useState<SorterResult<UserData>>({});

  // 处理表格变更（分页、排序、筛选）
  const handleTableChange = (
    pagination: TablePaginationConfig,
    filters: Record<string, FilterValue | null>,
    sorter: SorterResult<UserData> | SorterResult<UserData>[],
  ) => {
    setPagination((prev) => ({
      ...prev,
      current: pagination.current,
      pageSize: pagination.pageSize,
    }));
    setFilteredInfo(filters);
    setSortedInfo(Array.isArray(sorter) ? sorter[0] : sorter);
  };

  // 表格列定义
  const columns: ColumnsType<UserData> = [
    {
      title: '昵称',
      dataIndex: 'nickname',
      key: 'nickname',
      width: 150,
      sorter: (a, b) => (a.nickname || '').localeCompare(b.nickname || ''),
      sortOrder: sortedInfo.columnKey === 'nickname' ? sortedInfo.order : null,
    },
    {
      title: '邮箱',
      dataIndex: 'email',
      key: 'email',
      width: 180,
      sorter: (a, b) => (a.email || '').localeCompare(b.email || ''),
      sortOrder: sortedInfo.columnKey === 'email' ? sortedInfo.order : null,
    },
    {
      title: '角色',
      dataIndex: 'role',
      key: 'role',
      width: 100,
      filters: roleOptions.map((role) => ({ text: role.label, value: role.value })),
      filteredValue: filteredInfo.role ?? null,
      onFilter: (value, record) => record.role === value,
      render: (role: string) => {
        const target = roleOptions.filter((option) => option.value === role)[0];
        if (target) {
          return (
            <Tag bordered={false} color={target.color}>
              {target.label}
            </Tag>
          );
        }
        return role;
      },
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 80,
      filters: [
        { text: '启用', value: 1 },
        { text: '禁用', value: 0 },
      ],
      filteredValue: filteredInfo.status ?? null,
      onFilter: (value, record) => record.status === value,
      render: (status: number) => (
        <Tag color={status === 1 ? 'success' : 'error'}>{status === 1 ? '启用' : '禁用'}</Tag>
      ),
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 160,
      sorter: (a, b) => new Date(a.createTime || '').getTime() - new Date(b.createTime || '').getTime(),
      sortOrder: sortedInfo.columnKey === 'createTime' ? sortedInfo.order : null,
      render: (createTime: string) => (createTime ? dayjs(createTime).format('YYYY-MM-DD HH:mm:ss') : '-'),
    },
    {
      title: '更新时间',
      dataIndex: 'updateTime',
      key: 'updateTime',
      width: 160,
      sorter: (a, b) => new Date(a.updateTime || '').getTime() - new Date(b.updateTime || '').getTime(),
      sortOrder: sortedInfo.columnKey === 'updateTime' ? sortedInfo.order : null,
      render: (updateTime: string) => (updateTime ? dayjs(updateTime).format('YYYY-MM-DD HH:mm:ss') : '-'),
    },
    {
      title: '操作',
      key: 'action',
      fixed: 'right',
      width: 200,
      render: (_, record) => {
        const isSuperAdmin = record.role === 'SUPER_ADMIN';
        const isCurrentUserAdmin = currentUser.role === 'ADMIN';
        const isTargetUserAdmin = record.role === 'ADMIN';
        const isEditingSelf = currentUser.userId && record?.id?.toString() === currentUser.userId.toString();

        // 权限检查：如果当前用户是管理员，且目标用户也是管理员，则不能编辑或删除
        // 但是可以编辑自己的信息，不能删除自己的账号
        const canEditOrDelete = !isSuperAdmin && (!(isCurrentUserAdmin && isTargetUserAdmin) || isEditingSelf);
        const canDelete = canEditOrDelete && !isEditingSelf; // 不能删除自己的账号

        // 密码修改权限：管理员不能修改超级管理员和其他管理员的密码，但可以修改自己的密码
        const canChangePassword = !(isCurrentUserAdmin && (isSuperAdmin || isTargetUserAdmin)) || isEditingSelf;

        return (
          <Space size={0}>
            {canEditOrDelete && (
              <Button type="link" onClick={() => onEdit(record)} style={{ padding: '0 8px' }}>
                编辑
              </Button>
            )}
            {record.role === 'USER' && (
              <Button type="link" onClick={() => onOpenModuleDrawer(record)} style={{ padding: '0 8px' }}>
                授权
              </Button>
            )}
            {canChangePassword && (
              <Button type="link" onClick={() => onChangePassword(record)} style={{ padding: '0 8px' }}>
                改密码
              </Button>
            )}
            {canDelete && (
              <Popconfirm
                title="确定要删除此用户吗？"
                description="此操作不可撤销"
                onConfirm={() => {
                  return void onDelete(record.key);
                }}
                okText="确定"
                cancelText="取消"
              >
                <Button type="link" danger style={{ padding: '0 8px' }}>
                  删除
                </Button>
              </Popconfirm>
            )}
          </Space>
        );
      },
    },
  ];

  return {
    columns,
    pagination,
    handleTableChange,
  };
};
