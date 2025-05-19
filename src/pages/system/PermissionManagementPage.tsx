import { useState, useEffect } from 'react';
import { Card, Table, Button, Space, Input, Select, Breadcrumb } from 'antd';
import { App } from 'antd';
import { getUsers } from '../../api/user';
import type { ColumnsType } from 'antd/es/table';
import {
  grantTablePermission,
  getUserDatasourcePermissions,
  getMyTablePermissions,
  revokePermissionById,
} from '../../api/permission';
import type { DatasourcePermission, TablePermission } from '../../types/permissionTypes';
import { HomeOutlined, SearchOutlined } from '@ant-design/icons';
import { Link } from 'react-router-dom';

const PermissionManagementPage = () => {
  const { message, modal } = App.useApp();
  const [permissions, setPermissions] = useState<DatasourcePermission[]>([]);
  const [filteredPermissions, setFilteredPermissions] = useState<DatasourcePermission[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchParams, setSearchParams] = useState({
    userId: '',
    datasourceId: '',
  });
  const [users, setUsers] = useState<{ label: string; value: string }[]>([]);
  const [selectedUser, setSelectedUser] = useState<string>();

  // 初始获取全部权限数据
  const fetchPermissions = async () => {
    setLoading(true);
    try {
      const data = await getMyTablePermissions();
      setPermissions(data);
      setFilteredPermissions(data); // 初始化时显示全部数据
    } catch {
      message.error('获取权限列表失败');
    } finally {
      setLoading(false);
    }
  };

  // 前端搜索过滤方法
  const filterPermissions = (data: DatasourcePermission[], searchValue: string) => {
    if (!searchValue) return data;
    const lowerValue = searchValue.toLowerCase();
    return data.filter(
      (permission) =>
        permission.datasourceId.toString().toLowerCase().includes(lowerValue) ||
        permission.datasourceName.toLowerCase().includes(lowerValue) ||
        permission.modules.some((module) => module.moduleName.toLowerCase().includes(lowerValue)),
    );
  };

  useEffect(() => {
    // 当搜索参数变化时执行前端过滤
    const filtered = filterPermissions(
      permissions,
      searchParams.userId || searchParams.datasourceId,
    );
    setFilteredPermissions(filtered);
  }, [searchParams, permissions]);

  // 授予权限 (保留用于未来扩展)
  useEffect(() => {
    const loadUsers = async () => {
      try {
        const userList = await getUsers();
        setUsers(
          userList.map((user) => ({
            label: user.nickname ?? user.username,
            value: user.id,
          })),
        );
      } catch {
        message.error('加载用户列表失败');
      }
    };
    loadUsers();
    fetchPermissions();
  }, []);

  const handleGrant = async (moduleName: string) => {
    let currentSelectedUser = selectedUser;

    modal.confirm({
      title: '授予表权限',
      content: (
        <div style={{ margin: '16px 0' }}>
          <div style={{ marginBottom: 8 }}>选择用户:</div>
          <Select
            style={{ width: '100%' }}
            options={users}
            placeholder="请选择用户"
            onChange={(value) => {
              currentSelectedUser = value;
              setSelectedUser(value);
            }}
          />
        </div>
      ),
      okText: '授予',
      cancelText: '取消',
      onOk: async () => {
        if (!currentSelectedUser) {
          message.error('请选择用户');
          return Promise.reject();
        }
        try {
          await grantTablePermission(currentSelectedUser, {
            module: moduleName,
          });
          message.success('权限授予成功');
          fetchPermissions();
          return Promise.resolve();
        } catch {
          message.error('权限授予失败');
          return Promise.reject();
        }
      },
    });
  };

  // 撤销权限
  const handleRevoke = async (permissionId: string) => {
    if (!permissionId) {
      message.error('无效的权限ID');
      return;
    }
    try {
      await revokePermissionById(permissionId);
      message.success('权限撤销成功');
      fetchPermissions();
    } catch {
      message.error('权限撤销失败');
    }
  };

  const expandedRowRender = (record: DatasourcePermission) => {
    const columns: ColumnsType<TablePermission> = [
      {
        title: '模块名',
        dataIndex: 'moduleName',
        key: 'moduleName',
      },
      {
        title: '操作',
        key: 'action',
        render: (_, table) => (
          <Space size="middle">
            {table.permissionId && (
              <Button type="link" danger onClick={() => handleRevoke(table.permissionId as string)}>
                撤销
              </Button>
            )}
            {!table.permissionId && (
              <Button type="link" onClick={() => handleGrant(table.moduleName)}>
                授予
              </Button>
            )}
          </Space>
        ),
      },
    ];

    return (
      <Table columns={columns} dataSource={record.modules} rowKey="tableName" pagination={false} />
    );
  };

  const columns: ColumnsType<DatasourcePermission> = [
    {
      title: '数据源ID',
      dataIndex: 'datasourceId',
      key: 'datasourceId',
    },
    {
      title: '数据源名称',
      dataIndex: 'datasourceName',
      key: 'datasourceName',
    },
    {
      title: '数据库名',
      dataIndex: 'databaseName',
      key: 'databaseName',
    },
  ];

  return (
    <Card>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Breadcrumb style={{ marginBottom: 16 }}>
          <Breadcrumb.Item>
            <Link to="/home">
              <HomeOutlined />
            </Link>
          </Breadcrumb.Item>
          <Breadcrumb.Item>权限管理</Breadcrumb.Item>
        </Breadcrumb>
        <Space style={{ marginBottom: 16 }}>
          <Input
            placeholder="搜索数据源ID/名称/模块名"
            value={searchParams.userId || searchParams.datasourceId}
            onChange={(e) => {
              const value = e.target.value;
              setSearchParams({ ...searchParams, userId: value, datasourceId: value });
            }}
            allowClear
            suffix={<SearchOutlined />}
            style={{ width: 300 }}
          />
        </Space>
      </div>
      <Table
        columns={columns}
        dataSource={filteredPermissions}
        rowKey="datasourceId"
        loading={loading}
        expandable={{
          expandedRowRender,
          rowExpandable: (record) => record.modules.length > 0,
        }}
      />
    </Card>
  );
};

export default PermissionManagementPage;
