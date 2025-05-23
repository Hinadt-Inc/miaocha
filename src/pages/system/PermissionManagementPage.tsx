import { useState, useEffect } from 'react';
import { Card, Table, Button, Space, Input, Select, Breadcrumb } from 'antd';
import { App } from 'antd';
import { getUsers } from '../../api/user';
import type { ColumnsType } from 'antd/es/table';
import {
  grantTablePermission,
  getMyTablePermissions,
  batchRevokeModulePermissions,
} from '../../api/permission';
import type { DatasourcePermission, TablePermission, PermissionResponse } from '../../types/permissionTypes';
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
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
  });
  const [users, setUsers] = useState<{ label: string; value: string }[]>([]);
  const [selectedUser, setSelectedUser] = useState<string>();

  // 将新格式的权限数据转换为原来的格式
  const transformPermissionsData = (data: PermissionResponse[]): DatasourcePermission[] => {
    // 按数据源ID分组
    const groupByDatasource = data.reduce(
      (acc, permission) => {
        const datasourceId = permission.datasourceId;

        if (!acc[datasourceId]) {
          acc[datasourceId] = {
            modules: [],
            datasourceId,
            datasourceName: `数据源 ${datasourceId} (JDBC)`, // 显示JDBC标识
            databaseName: 'JDBC连接', // 显示数据库类型
            tables: [],
            id: permission.id.toString(),
          };
        }

        // 添加模块信息
        acc[datasourceId].modules.push({
          moduleName: permission.module,
          permissionId: permission.id.toString(),
          tableName: permission.module, // 使用模块名作为表名
          permissions: ['read', 'write'], // 默认权限
          id: permission.id.toString(),
        });

        return acc;
      },
      {} as Record<number, DatasourcePermission>,
    );

    // 转换为数组
    return Object.values(groupByDatasource);
  };

  // 全局授权模态框
  const showGlobalGrantModal = () => {
    let currentSelectedUser = selectedUser;
    let selectedModules: string[] = []; // 改为数组以支持多选
    let allModules: string[] = [];

    // 收集所有模块
    permissions.forEach((datasource) => {
      datasource.modules.forEach((module) => {
        if (!allModules.includes(module.moduleName)) {
          allModules.push(module.moduleName);
        }
      });
    });

    modal.confirm({
      title: '授予新权限',
      width: 500,
      content: (
        <div style={{ margin: '16px 0' }}>
          <div style={{ marginBottom: 8 }}>选择用户:</div>
          <Select
            style={{ width: '100%', marginBottom: 16 }}
            options={users}
            placeholder="请选择用户"
            onChange={(value) => {
              currentSelectedUser = value;
              setSelectedUser(value);
            }}
          />
          <div style={{ marginBottom: 8 }}>选择模块（可多选）:</div>
          <Select
            mode="multiple"
            style={{ width: '100%' }}
            placeholder="请选择要授权的模块"
            onChange={(values) => {
              selectedModules = values;
            }}
            optionFilterProp="children"
            allowClear
          >
            {allModules.map((module) => (
              <Select.Option key={module} value={module}>
                {module}
              </Select.Option>
            ))}
          </Select>
        </div>
      ),
      okText: '授权',
      cancelText: '取消',
      onOk: async () => {
        if (!currentSelectedUser) {
          message.error('请选择用户');
          return Promise.reject();
        }
        if (!selectedModules.length) {
          message.error('请至少选择一个模块');
          return Promise.reject();
        }
        try {
          await grantTablePermission(currentSelectedUser, {
            userId: currentSelectedUser,
            modules: selectedModules,
          });
          message.success(`成功授予 ${selectedModules.length} 个模块权限`);
          fetchPermissions();
          return Promise.resolve();
        } catch (error: any) {
          message.error(`权限授予失败: ${error.message || '未知错误'}`);
          return Promise.reject();
        }
      },
    });
  };

  // 初始获取全部权限数据
  const fetchPermissions = async () => {
    setLoading(true);
    try {
      const data = (await getMyTablePermissions()) as unknown as PermissionResponse[];
      const transformedData = transformPermissionsData(data);
      setPermissions(transformedData);
      setFilteredPermissions(transformedData); // 初始化时显示全部数据
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
    const filtered = filterPermissions(permissions, searchParams.userId || searchParams.datasourceId);
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

  // 显示授权模态框（对数据源整体授权）
  const showGrantModal = (record: DatasourcePermission) => {
    let currentSelectedUser = selectedUser;
    let selectedModules: string[] = [];

    modal.confirm({
      title: '授予权限',
      width: 500,
      content: (
        <div style={{ margin: '16px 0' }}>
          <div style={{ marginBottom: 8 }}>选择用户:</div>
          <Select
            style={{ width: '100%', marginBottom: 16 }}
            options={users}
            placeholder="请选择用户"
            onChange={(value) => {
              currentSelectedUser = value;
              setSelectedUser(value);
            }}
          />
          <div style={{ marginBottom: 8 }}>选择模块（可多选）:</div>
          <Select
            mode="multiple"
            style={{ width: '100%' }}
            placeholder="请选择要授权的模块"
            onChange={(values) => {
              selectedModules = values;
            }}
            optionFilterProp="children"
            allowClear
          >
            {record.modules.map((module) => (
              <Select.Option key={module.moduleName} value={module.moduleName}>
                {module.moduleName}
              </Select.Option>
            ))}
          </Select>
        </div>
      ),
      okText: '授予',
      cancelText: '取消',
      onOk: async () => {
        if (!currentSelectedUser) {
          message.error('请选择用户');
          return Promise.reject();
        }
        if (!selectedModules.length) {
          message.error('请至少选择一个模块');
          return Promise.reject();
        }
        try {
          await grantTablePermission(currentSelectedUser, {
            userId: currentSelectedUser,
            modules: selectedModules,
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

  // 授权单个模块
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
            userId: currentSelectedUser,
            modules: [moduleName],
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
  const handleRevoke = async (permissionId: string, moduleName: string) => {
    if (!permissionId || !moduleName) {
      message.error('无效的权限信息');
      return;
    }

    let currentSelectedUser = selectedUser;

    modal.confirm({
      title: '撤销权限',
      width: 500,
      content: (
        <div style={{ margin: '16px 0' }}>
          <div style={{ marginBottom: 8 }}>选择用户:</div>
          <Select
            style={{ width: '100%' }}
            options={users}
            placeholder="请选择要撤销权限的用户"
            onChange={(value) => {
              currentSelectedUser = value;
              setSelectedUser(value);
            }}
          />
        </div>
      ),
      okText: '确认撤销',
      cancelText: '取消',
      okButtonProps: { danger: true },
      onOk: async () => {
        if (!currentSelectedUser) {
          message.error('请选择用户');
          return Promise.reject();
        }
        try {
          await batchRevokeModulePermissions(currentSelectedUser, [moduleName]);
          message.success('权限撤销成功');
          fetchPermissions();
          return Promise.resolve();
        } catch (error: any) {
          message.error(`权限撤销失败: ${error.message || '未知错误'}`);
          return Promise.reject();
        }
      },
    });
  };

  // 处理分页变更
  const handlePageChange = (page: number, pageSize: number) => {
    setPagination({
      current: page,
      pageSize,
    });
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
          <Space size="small">
            {table.permissionId && (
              <Button
                type="link"
                danger
                size="small"
                onClick={() => handleRevoke(table.permissionId as string, table.moduleName)}
                style={{ padding: '0 4px' }}
              >
                撤销
              </Button>
            )}
            {!table.permissionId && (
              <Button
                type="link"
                size="small"
                onClick={() => handleGrant(table.moduleName)}
                style={{ padding: '0 4px' }}
              >
                授予
              </Button>
            )}
          </Space>
        ),
      },
    ];

    return (
      <Table
        columns={columns}
        dataSource={record.modules}
        rowKey="moduleName"
        pagination={false}
        size="small"
        style={{ margin: '0' }}
      />
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
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Button type="primary" size="small" onClick={() => showGrantModal(record)}>
          授权
        </Button>
      ),
    },
  ];

  return (
    <Card>
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: 12,
        }}
      >
        <Breadcrumb>
          <Breadcrumb.Item>
            <Link to="/home">
              <HomeOutlined />
            </Link>
          </Breadcrumb.Item>
          <Breadcrumb.Item>权限管理</Breadcrumb.Item>
        </Breadcrumb>
        <Space>
          <Button type="primary" onClick={() => showGlobalGrantModal()}>
            授予新权限
          </Button>
          <Input
            placeholder="搜索数据源ID/名称/模块名"
            value={searchParams.userId || searchParams.datasourceId}
            onChange={(e) => {
              const value = e.target.value;
              setSearchParams({ ...searchParams, userId: value, datasourceId: value });
              setPagination((prev) => ({ ...prev, current: 1 })); // 搜索时重置到第一页
            }}
            allowClear
            suffix={<SearchOutlined />}
            style={{ width: 240 }}
          />
        </Space>
      </div>
      <Table
        columns={columns}
        dataSource={filteredPermissions}
        rowKey="datasourceId"
        loading={loading}
        size="small"
        pagination={{
          current: pagination.current,
          pageSize: pagination.pageSize,
          onChange: handlePageChange,
          showSizeChanger: true,
          responsive: true,
          showTotal: (total) => `共 ${total} 条`,
        }}
        expandable={{
          expandedRowRender,
          // rowExpandable: (record) => record.modules.length > 0,
        }}
      />
    </Card>
  );
};

import withSystemAccess from '@/utils/withSystemAccess';
export default withSystemAccess(PermissionManagementPage);
