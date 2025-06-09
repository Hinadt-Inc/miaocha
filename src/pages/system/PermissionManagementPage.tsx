import { useState, useEffect } from 'react';
import { Table, Button, Space, Input, Select, Breadcrumb, Modal } from 'antd';
import { App } from 'antd';
import { getUsers } from '../../api/user';
import type { ColumnsType } from 'antd/es/table';
import {
  grantTablePermission,
  getMyTablePermissions,
  batchRevokeModulePermissions,
  getUserUnauthorizedModules,
} from '../../api/permission';
import type { DatasourcePermission, TablePermission, PermissionResponse } from '../../types/permissionTypes';
import { HomeOutlined, SearchOutlined } from '@ant-design/icons';
import { Link } from 'react-router-dom';
import styles from './PermissionManagementPage.module.less';

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
  const [unauthorizedModules, setUnauthorizedModules] = useState<string[]>([]);
  const [modulesLoading, setModulesLoading] = useState(false);

  // 将新格式的权限数据转换为原来的格式
  const [moduleAuthorizedUsers, setModuleAuthorizedUsers] = useState<Record<string, string[]>>({});

  const transformPermissionsData = (data: PermissionResponse[]): DatasourcePermission[] => {
    const authorizedUsersMap: Record<string, string[]> = {};

    // 按数据源ID分组
    const groupByDatasource = data.reduce(
      (acc, permission) => {
        const datasourceId = permission.datasourceId;

        if (!acc[datasourceId]) {
          acc[datasourceId] = {
            modules: [],
            datasourceId,
            datasourceName: permission.datasourceName || `数据源 ${datasourceId} (JDBC)`,
            databaseName: 'JDBC连接', // 显示数据库类型
            tables: [],
            id: datasourceId.toString(),
          };
        }

        // 合并同一模块的用户
        const moduleUsers = permission.users.map((user) => ({
          userId: user.userId,
          nickname: user.nickname,
          email: user.email,
          role: user.role,
        }));

        // 记录已授权用户
        if (!authorizedUsersMap[permission.module]) {
          authorizedUsersMap[permission.module] = [];
        }
        authorizedUsersMap[permission.module] = [
          ...new Set([
            ...(authorizedUsersMap[permission.module] || []),
            ...moduleUsers.map((u) => u.userId.toString()),
          ]),
        ];

        acc[datasourceId].modules.push({
          moduleName: permission.module,
          permissionId: permission.users[0].permissionId.toString(),
          tableName: permission.module,
          permissions: ['read', 'write'], // 默认权限
          id: permission.users[0].permissionId.toString(),
          users: moduleUsers,
        });

        return acc;
      },
      {} as Record<number, DatasourcePermission>,
    );

    setModuleAuthorizedUsers(authorizedUsersMap);

    // 转换为数组
    return Object.values(groupByDatasource);
  };

  // 全局授权模态框状态
  const [grantModalVisible, setGrantModalVisible] = useState(false);
  const [localSelectedUser, setLocalSelectedUser] = useState<string | undefined>(selectedUser);
  const [localSelectedModules, setLocalSelectedModules] = useState<string[]>([]);

  const handleUserChange = async (value: string) => {
    setLocalSelectedUser(value);
    setSelectedUser(value);
    setUnauthorizedModules([]);
    try {
      setModulesLoading(true);
      const modules = await getUserUnauthorizedModules(value);
      setUnauthorizedModules(modules);
    } catch (error) {
      message.error('获取未授权模块失败');
    } finally {
      setModulesLoading(false);
    }
  };

  const handleGrantSubmit = async () => {
    if (!localSelectedUser) {
      message.error('请选择用户');
      return;
    }
    if (localSelectedModules.length === 0) {
      message.error('请至少选择一个模块');
      return;
    }
    try {
      await grantTablePermission(localSelectedUser, {
        userId: localSelectedUser,
        modules: localSelectedModules,
      });
      message.success(`成功授予 ${localSelectedModules.length} 个模块权限`);
      fetchPermissions();
      setGrantModalVisible(false);
    } catch (error: any) {
      message.error(`权限授予失败: ${error.message || '未知错误'}`);
    }
  };

  const showGlobalGrantModal = () => {
    setGrantModalVisible(true);
  };

  // 在return中添加Modal组件
  const GrantModal = (
    <Modal
      title="授予新权限"
      width={500}
      open={grantModalVisible}
      onOk={handleGrantSubmit}
      onCancel={() => setGrantModalVisible(false)}
      okText="授权"
      cancelText="取消"
    >
      <div>
        <div>选择用户:</div>
        <Select
          style={{ width: '100%', marginBottom: 16 }}
          options={users}
          placeholder="请选择用户"
          onChange={handleUserChange}
          value={localSelectedUser}
        />
        <div style={{ marginBottom: 8 }}>选择模块（可多选）:</div>
        <Select
          mode="multiple"
          style={{ width: '100%' }}
          placeholder={modulesLoading ? '加载中...' : '请选择要授权的模块'}
          onChange={setLocalSelectedModules}
          value={localSelectedModules}
          optionFilterProp="children"
          allowClear
          loading={modulesLoading}
        >
          {unauthorizedModules.map((module) => (
            <Select.Option key={module} value={module}>
              {module}
            </Select.Option>
          ))}
        </Select>
      </div>
    </Modal>
  );

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

  // 授权单个模块
  const handleGrant = async (moduleName: string) => {
    let currentSelectedUser = selectedUser;
    const authorizedUserIds = moduleAuthorizedUsers[moduleName] || [];

    modal.confirm({
      title: '授予表权限',
      content: (
        <div style={{ margin: '16px 0' }}>
          <div style={{ marginBottom: 8 }}>选择用户:</div>
          <Select
            style={{ width: '100%' }}
            options={users.filter((user) => !authorizedUserIds?.includes(String(user.value)))}
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
  const handleRevoke = async (
    permissionId: string,
    moduleName: string,
    currentUsers: { userId: number | string; nickname: string }[],
  ) => {
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
            options={currentUsers.map((user) => ({
              label: user.nickname,
              value: user.userId,
            }))}
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
        title: '用户列表',
        key: 'users',
        render: (_, table) => (
          <div>
            {table.users?.map((user: { userId: number; nickname: string; role: string }) => (
              <div key={user.userId}>
                {user.nickname} ({user.role})
              </div>
            ))}
          </div>
        ),
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
                onClick={() => handleRevoke(table.permissionId as string, table.moduleName, table.users || [])}
                style={{ padding: '0 4px' }}
              >
                撤销
              </Button>
            )}
            {table.permissionId && (
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
  ];

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        {GrantModal}
        <Breadcrumb>
          <Breadcrumb.Item>
            <Link to="/">
              <HomeOutlined />
            </Link>
          </Breadcrumb.Item>
          <Breadcrumb.Item>权限管理</Breadcrumb.Item>
        </Breadcrumb>
        <Space>
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
          <Button type="primary" onClick={() => showGlobalGrantModal()}>
            授予新权限
          </Button>
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
        }}
      />
    </div>
  );
};

import withSystemAccess from '@/utils/withSystemAccess';
export default withSystemAccess(PermissionManagementPage);
