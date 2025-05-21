import { useState, useEffect, useRef } from 'react';

import { getUsers, createUser, updateUser, deleteUser, type User } from '../../api/user';
import {
  Button,
  Input,
  Table,
  Space,
  Tag,
  Modal,
  Form,
  Select,
  Popconfirm,
  message,
  Avatar,
  Card,
  Row,
  Col,
  Breadcrumb,
} from 'antd';
import {
  SearchOutlined,
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  UserOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import type { AxiosRequestConfig } from 'axios';
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table';
import type { FilterValue, SorterResult } from 'antd/es/table/interface';
import dayjs from 'dayjs';
import { Link } from 'react-router-dom';
import './UserManagementPage.less';

interface UserData extends User {
  key: string;
  department: string;
  lastLoginTime: string;
  name: string;
  role: string;
  createTime: string;
}

// 转换API数据到表格数据
const transformUserData = (users: User[]): UserData[] => {
  return users.map((user) => {
    // 确保nickname字段有值
    const displayName = user.nickname ?? user.username;

    return {
      ...user,
      key: user.id.toString(),
      name: displayName,
      nickname: displayName, // 确保nickname字段有值
      username: user.uid ?? user.username,
      createTime: user.createTime ?? user.createdAt,
      role: user.role,
      department: '', // Add default value for department
      lastLoginTime: '', // Add default value for lastLoginTime
    };
  });
};

// 角色选项
const roleOptions = [
  { value: 'ADMIN', label: '管理员' },
  { value: 'USER', label: '普通用户' },
];

const UserManagementPage = () => {
  const [data, setData] = useState<UserData[]>([]);
  const searchTimeoutRef = useRef<number | null>(null);
  const originalDataRef = useRef<UserData[]>([]);

  // 清理定时器
  useEffect(() => {
    return () => {
      if (searchTimeoutRef.current !== null) {
        window.clearTimeout(searchTimeoutRef.current);
      }
    };
  }, []);

  // 加载用户数据
  useEffect(() => {
    const abortController = new AbortController();
    fetchUsers({ signal: abortController.signal }).catch((error: { name: string }) => {
      if (error.name !== 'CanceledError') {
        message.error('加载用户数据失败');
      }
    });
    return () => abortController.abort();
  }, []);

  const fetchUsers = async (config?: AxiosRequestConfig) => {
    setLoading(true);
    try {
      const users = await getUsers(config);
      const transformedUsers = transformUserData(users);
      setData(transformedUsers);
      // 保存原始数据供搜索使用
      originalDataRef.current = transformedUsers;
      message.success('用户数据加载成功');
    } catch (error) {
      if (error instanceof Error) {
        if (error.name !== 'CanceledError') {
          message.error('加载用户数据失败');
        }
      } else {
        console.error('Unexpected error:', error);
      }
    } finally {
      setLoading(false);
    }
  };
  const [loading, setLoading] = useState(false);
  const [searchText, setSearchText] = useState('');
  const [isModalVisible, setIsModalVisible] = useState(false);
  const [selectedRecord, setSelectedRecord] = useState<UserData | null>(null);
  const [form] = Form.useForm();
  const [pagination, setPagination] = useState<TablePaginationConfig>({
    current: 1,
    pageSize: 10,
  });
  const [filteredInfo, setFilteredInfo] = useState<Record<string, FilterValue | null>>({});
  const [sortedInfo, setSortedInfo] = useState<SorterResult<UserData>>({});

  // 重新加载数据
  const handleReload = () => {
    fetchUsers().catch(() => {
      message.error('加载用户数据失败');
    });
  };

  // 处理表格变更（分页、排序、筛选）
  const handleTableChange = (
    pagination: TablePaginationConfig,
    filters: Record<string, FilterValue | null>,
    sorter: SorterResult<UserData> | SorterResult<UserData>[],
  ) => {
    setPagination(pagination);
    setFilteredInfo(filters);
    setSortedInfo(Array.isArray(sorter) ? sorter[0] : sorter);
  };

  // 处理搜索（带防抖功能）
  const handleSearch = (value: string) => {
    setSearchText(value);

    // 清除之前的定时器
    if (searchTimeoutRef.current !== null) {
      window.clearTimeout(searchTimeoutRef.current);
      searchTimeoutRef.current = null;
    }

    // 设置新的定时器，300ms后执行搜索
    searchTimeoutRef.current = window.setTimeout(() => {
      if (!value.trim()) {
        // 如果搜索词为空，则恢复原始数据
        if (originalDataRef.current.length > 0) {
          setData(originalDataRef.current);
        } else {
          // 如果原始数据不存在，则重新加载
          fetchUsers().catch(() => {
            message.error('加载用户数据失败');
          });
        }
        return;
      }

      // 去除特殊字符（如中文输入法中的单引号等）
      const cleanValue = value.replace(/[''"]/g, '');

      // 分词处理，按空格拆分搜索词
      const searchTerms = cleanValue
        .toLowerCase()
        .split(/\s+/)
        .filter((term) => term);

      // 定义搜索匹配函数
      const matchesSearchTerms = (user: UserData) => {
        if (searchTerms.length === 0) return true;

        // 获取用户的各个字段以供搜索
        const userNickname = user.nickname?.toLowerCase() || '';
        const userName = user.name?.toLowerCase() || '';
        const userEmail = user.email?.toLowerCase() || '';
        const userUsername = user.username?.toLowerCase() || '';

        // 普通字符串包含搜索
        return searchTerms.every((term) => {
          const textToSearch = `${userNickname} ${userName} ${userEmail} ${userUsername}`;
          return textToSearch.includes(term);
        });
      };

      // 先在当前数据中搜索
      const currentDataFiltered = data.filter(matchesSearchTerms);

      // 如果当前数据中有匹配项，直接返回
      if (currentDataFiltered.length > 0) {
        setData(currentDataFiltered);
        return;
      }

      // 如果当前数据中没有匹配项，在原始数据中搜索
      const originalDataFiltered = originalDataRef.current.filter(matchesSearchTerms);

      // 设置搜索结果
      setData(originalDataFiltered);
    }, 300); // 300ms防抖延迟
  };

  // 处理添加/编辑用户
  const handleAddEdit = (record?: UserData) => {
    setSelectedRecord(record ?? null);
    form.resetFields();
    if (record) {
      form.setFieldsValue({
        ...record,
      });
    }
    setIsModalVisible(true);
  };

  // 处理删除用户
  const handleDelete = async (key: string) => {
    try {
      await deleteUser(key);
      setData(data.filter((item) => item.key !== key));
      message.success('用户已删除');
    } catch {
      message.error('删除用户失败');
    }
  };

  // 处理表单提交
  const handleSubmit = async () => {
    try {
      const values = (await form.validateFields()) as {
        nickname: string;
        email: string;
        role: string;
        status: number;
        password?: string;
        confirmPassword?: string;
      };

      if (selectedRecord) {
        // 编辑现有用户
        await updateUser({
          id: selectedRecord.key,
          nickname: values.nickname,
          email: values.email,
          role: values.role,
          status: values.status,
        });
        message.success('用户信息已更新');
      } else {
        // 添加新用户
        if (!values.password || values.password !== values.confirmPassword) {
          message.error('密码和确认密码必须一致');
          return;
        }

        await createUser({
          username: values.nickname,
          nickname: values.nickname,
          password: values.password,
          email: values.email,
          role: values.role,
          status: values.status,
        });
        message.success('用户已添加');
      }

      setIsModalVisible(false);
      await fetchUsers(); // 刷新数据
    } catch (error) {
      message.error('操作失败');
      console.error('操作失败:', error);
    }
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
      render: (text) => (
        <Space>
          <Avatar icon={<UserOutlined />} />
          <span>{text}</span>
        </Space>
      ),
    },
    {
      title: '邮箱',
      dataIndex: 'email',
      key: 'email',
      width: 180,
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
        let color = 'blue';
        let label = '普通用户';
        if (role === 'ADMIN') {
          color = 'red';
          label = '管理员';
        } else if (role === 'SUPER_ADMIN') {
          color = 'volcano';
          label = '超级管理员';
        }
        return <Tag color={color}>{label}</Tag>;
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
        <Tag color={status === 1 ? 'green' : 'red'}>{status === 1 ? '启用' : '禁用'}</Tag>
      ),
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 160,
      sorter: (a, b) => new Date(a.createTime).getTime() - new Date(b.createTime).getTime(),
      sortOrder: sortedInfo.columnKey === 'createTime' ? sortedInfo.order : null,
      render: (createTime: string) => dayjs(createTime).format('YYYY-MM-DD HH:mm:ss'),
    },
    {
      title: '操作',
      key: 'action',
      fixed: 'right',
      width: 150,
      render: (_, record) => (
        <Space size="middle">
          <Button type="link" icon={<EditOutlined />} onClick={() => handleAddEdit(record)}>
            编辑
          </Button>
          {!['SUPER_ADMIN'].includes(record.role) && (
            <Popconfirm
              title="确定要删除此用户吗？"
              description="此操作不可撤销"
              onConfirm={() => {
                return void handleDelete(record.key);
              }}
              okText="确定"
              cancelText="取消"
            >
              <Button type="link" danger icon={<DeleteOutlined />}>
                删除
              </Button>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ];

  return (
    <div className="user-management-page">
      <Card>
        <div className="user-management-page-header">
          <Breadcrumb>
            <Breadcrumb.Item>
              <Link to="/">首页</Link>
            </Breadcrumb.Item>
            <Breadcrumb.Item>用户管理</Breadcrumb.Item>
          </Breadcrumb>
          <Space style={{ marginBottom: 16 }}>
            <Input
              placeholder="搜索昵称/邮箱/用户名"
              value={searchText}
              onChange={(e) => handleSearch(e.target.value)}
              style={{ width: 240 }}
              prefix={<SearchOutlined />}
              allowClear
            />
            <Button type="primary" icon={<PlusOutlined />} onClick={() => handleAddEdit()}>
              添加用户
            </Button>
            <Button icon={<ReloadOutlined />} onClick={handleReload} loading={loading}>
              刷新
            </Button>
          </Space>
        </div>

        <div className="table-container">
          <Table
            columns={columns}
            dataSource={data}
            rowKey="key"
            pagination={pagination}
            loading={loading}
            scroll={{ x: 1300 }}
            onChange={handleTableChange}
            size="small"
            bordered
          />
        </div>

        <Modal
          title={selectedRecord ? '编辑用户' : '添加用户'}
          open={isModalVisible}
          onOk={handleSubmit}
          onCancel={() => setIsModalVisible(false)}
          width={600}
          maskClosable={false}
        >
          <Form form={form} layout="vertical">
            <Row gutter={16}>
              <Col span={12}>
                <Form.Item
                  name="nickname"
                  label="昵称"
                  rules={[{ required: true, message: '请输入昵称' }]}
                >
                  <Input placeholder="请输入昵称" />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item
                  name="email"
                  label="邮箱"
                  rules={[
                    { required: true, message: '请输入邮箱' },
                    { type: 'email', message: '请输入有效的邮箱地址' },
                  ]}
                >
                  <Input placeholder="请输入邮箱" />
                </Form.Item>
              </Col>
            </Row>

            <Row gutter={16}>
              <Col span={12}>
                <Form.Item
                  name="role"
                  label="角色"
                  rules={[{ required: true, message: '请选择角色' }]}
                >
                  <Select options={roleOptions} placeholder="请选择角色" />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item
                  name="status"
                  label="状态"
                  initialValue={1}
                  rules={[{ required: true, message: '请选择状态' }]}
                >
                  <Select
                    options={[
                      { value: 1, label: '启用' },
                      { value: 0, label: '禁用' },
                    ]}
                    placeholder="请选择状态"
                  />
                </Form.Item>
              </Col>
            </Row>

            <Row gutter={16}>
              <Col span={12}>
                <Form.Item
                  name="password"
                  label="密码"
                  rules={[
                    { required: !selectedRecord, message: '请输入密码' },
                    { min: 6, message: '密码长度不能少于6个字符' },
                  ]}
                >
                  <Input.Password
                    placeholder={selectedRecord ? '留空则不修改密码' : '请输入密码'}
                  />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item
                  name="confirmPassword"
                  label="确认密码"
                  dependencies={['password']}
                  rules={[
                    { required: !selectedRecord, message: '请确认密码' },
                    ({ getFieldValue }) => ({
                      validator(_, value) {
                        if (!value || getFieldValue('password') === value) {
                          return Promise.resolve();
                        }
                        return Promise.reject(new Error('两次输入的密码不一致'));
                      },
                    }),
                  ]}
                >
                  <Input.Password
                    placeholder={selectedRecord ? '留空则不修改密码' : '请确认密码'}
                  />
                </Form.Item>
              </Col>
            </Row>
          </Form>
        </Modal>
      </Card>
    </div>
  );
};

import withSystemAccess from '@/utils/withSystemAccess';
export default withSystemAccess(UserManagementPage);
