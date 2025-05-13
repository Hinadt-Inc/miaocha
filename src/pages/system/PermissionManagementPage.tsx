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
import { HomeOutlined } from '@ant-design/icons';
import { Link } from 'react-router-dom';

const PermissionManagementPage = () => {
  const { message, modal } = App.useApp();
  const [permissions, setPermissions] = useState<DatasourcePermission[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchParams, setSearchParams] = useState({
    userId: '',
    datasourceId: '',
  });
  const [users, setUsers] = useState<{ label: string; value: string }[]>([]);
  const [selectedUser, setSelectedUser] = useState<string>();

  // 获取权限列表
  const fetchPermissions = async () => {
    setLoading(true);
    try {
      if (searchParams.userId && searchParams.datasourceId) {
        const data = await getUserDatasourcePermissions(
          searchParams.userId,
          searchParams.datasourceId,
        );
        setPermissions(data);
      } else {
        const data = await getMyTablePermissions();
        setPermissions(data);
      }
    } catch {
      message.error('获取权限列表失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPermissions();
  }, [searchParams]);

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
        title: '表名',
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
          placeholder="用户ID"
          value={searchParams.userId}
          onChange={(e) => setSearchParams({ ...searchParams, userId: e.target.value })}
          allowClear
        />
        <Input
          placeholder="数据源ID"
          value={searchParams.datasourceId}
          onChange={(e) => setSearchParams({ ...searchParams, datasourceId: e.target.value })}
          allowClear
        />
        <Button type="primary" onClick={fetchPermissions}>
          搜索
        </Button>
      </Space>
      <Table
        columns={columns}
        dataSource={permissions}
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
