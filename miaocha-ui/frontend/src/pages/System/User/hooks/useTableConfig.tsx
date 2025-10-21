import { useState, useMemo, useCallback } from 'react';
import { Space, Tag, Button, Popconfirm } from 'antd';
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table';
import type { FilterValue, SorterResult } from 'antd/es/table/interface';
import dayjs from 'dayjs';
import { useSelector } from 'react-redux';
import type { UserListItem } from '../types';

// 角色选项
const roleOptions = [
  { value: 'SUPER_ADMIN', label: '超级管理员', color: 'magenta' },
  { value: 'ADMIN', label: '管理员', color: 'orange' },
  { value: 'USER', label: '普通用户', color: 'cyan' },
];

interface Props {
  onEdit: (record?: UserListItem) => void;
  onDelete: (id: number) => void;
  onChangePassword: (record: UserListItem) => void;
  onOpenModuleDrawer: (record: UserListItem) => void;
}

export const useTableConfig = ({ onEdit, onDelete, onChangePassword, onOpenModuleDrawer }: Props) => {
  // 获取当前用户信息
  const currentUser = useSelector((state: { user: { role: string; userId: number } }) => state.user);
  // 当前用户角色布尔
  const currentIsSuper = currentUser.role === 'SUPER_ADMIN';
  const currentIsAdmin = currentUser.role === 'ADMIN';

  const [pagination, setPagination] = useState<TablePaginationConfig>({
    current: 1,
    pageSize: 10,
    showSizeChanger: true,
    showTotal: (total) => `共 ${total} 条`,
    pageSizeOptions: ['10', '20', '50', '100'],
  });
  const [filteredInfo, setFilteredInfo] = useState<Record<string, FilterValue | null>>({});
  const [sortedInfo, setSortedInfo] = useState<SorterResult<UserListItem>>({});

  // 处理表格变更（分页、排序、筛选）
  const handleTableChange = useCallback(
    (
      pagination: TablePaginationConfig,
      filters: Record<string, FilterValue | null>,
      sorter: SorterResult<UserListItem> | SorterResult<UserListItem>[],
    ) => {
      setPagination((prev) => ({
        ...prev,
        current: pagination.current,
        pageSize: pagination.pageSize,
      }));
      setFilteredInfo(filters);
      setSortedInfo(Array.isArray(sorter) ? sorter[0] : sorter);
    },
    [],
  );

  // 表格列定义
  const columns: ColumnsType<UserListItem> = useMemo(
    () => [
      {
        title: '昵称',
        dataIndex: 'nickname',
        key: 'nickname',
        sorter: (a, b) => (a.nickname ?? '').localeCompare(b.nickname ?? ''),
        sortOrder: sortedInfo.columnKey === 'nickname' ? sortedInfo.order : null,
      },
      {
        title: '邮箱',
        dataIndex: 'email',
        key: 'email',
        sorter: (a, b) => (a.email ?? '').localeCompare(b.email ?? ''),
        sortOrder: sortedInfo.columnKey === 'email' ? sortedInfo.order : null,
      },
      {
        title: '角色',
        dataIndex: 'role',
        key: 'role',
        filters: roleOptions.map((role) => ({ text: role.label, value: role.value })),
        filteredValue: filteredInfo.role ?? null,
        onFilter: (value, record) => record.role === value,
        render: (role: string) => {
          const target = roleOptions.find((option) => option.value === role);
          if (!target) return role;
          return (
            <Tag bordered={false} color={target.color}>
              {target.label}
            </Tag>
          );
        },
      },
      {
        title: '状态',
        dataIndex: 'status',
        key: 'status',
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
        sorter: (a, b) => new Date(a.createTime ?? '').getTime() - new Date(b.createTime ?? '').getTime(),
        sortOrder: sortedInfo.columnKey === 'createTime' ? sortedInfo.order : null,
        render: (createTime: string) => (createTime ? dayjs(createTime).format('YYYY-MM-DD HH:mm:ss') : '-'),
      },
      {
        title: '更新时间',
        dataIndex: 'updateTime',
        key: 'updateTime',
        sorter: (a, b) => new Date(a.updateTime ?? '').getTime() - new Date(b.updateTime ?? '').getTime(),
        sortOrder: sortedInfo.columnKey === 'updateTime' ? sortedInfo.order : null,
        render: (updateTime: string) => (updateTime ? dayjs(updateTime).format('YYYY-MM-DD HH:mm:ss') : '-'),
      },
      {
        title: '操作',
        key: 'action',
        fixed: 'right',
        width: 183,
        render: (_, record) => {
          let canChangePassword = false; // 修改密码
          let canEdit = false; // 编辑
          let canDelete = false; // 删除

          const targetIsAdmin = record.role === 'ADMIN';
          const targetIsUser = record.role === 'USER';
          const targetIsSelf = record?.id?.toString() === currentUser.userId.toString();

          // 超级管理员 -> 自己：改密码
          // 超级管理员 -> 管理：改密码+编辑+删除
          // 超级管理员 -> 普通：改密码+编辑+删除+授权
          if (currentIsSuper) {
            canChangePassword = true;
            if (targetIsAdmin || targetIsUser) {
              canChangePassword = true;
              canEdit = true;
              canDelete = true;
            }
          }

          // 管理员 -> 超级：无
          // 管理员 -> 自己：改密码+编辑
          // 管理员 -> 普通：改密码+编辑+删除+授权
          if (currentIsAdmin) {
            if (targetIsSelf) {
              canChangePassword = true;
              canEdit = true;
            } else if (targetIsUser) {
              canChangePassword = true;
              canEdit = true;
              canDelete = true;
            }
          }

          return (
            <Space className="global-table-action">
              {canChangePassword && (
                <Button type="link" onClick={() => onChangePassword(record)}>
                  修改密码
                </Button>
              )}
              {canEdit && (
                <Button type="link" onClick={() => onEdit(record)}>
                  编辑
                </Button>
              )}

              {canDelete && (
                <Popconfirm
                  cancelText="取消"
                  okText="确定"
                  title="确定要删除此用户吗？"
                  onConfirm={() => void onDelete(record.id)}
                >
                  <Button danger type="link">
                    删除
                  </Button>
                </Popconfirm>
              )}

              {record.role === 'USER' && (
                <Button type="link" onClick={() => onOpenModuleDrawer(record)}>
                  授权
                </Button>
              )}
            </Space>
          );
        },
      },
    ],
    [
      sortedInfo.columnKey,
      sortedInfo.order,
      filteredInfo.role,
      filteredInfo.status,
      currentUser.userId,
      currentIsSuper,
      currentIsAdmin,
      onChangePassword,
      onEdit,
      onDelete,
      onOpenModuleDrawer,
    ],
  );

  return {
    columns,
    pagination,
    handleTableChange,
  };
};
