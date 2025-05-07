
import './UserManagementPage.less';
import { useState, useEffect } from 'react';

import { 
  getUsers,
  createUser,
  updateUser,
  deleteUser,
  type User
} from '../../api/user';
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
  Tooltip,
  Switch,
  Avatar,
  Card,
  Row,
  Col,
  Breadcrumb
} from 'antd';
import { 
  SearchOutlined, 
  PlusOutlined, 
  EditOutlined, 
  DeleteOutlined, 
  UserOutlined, 
  ReloadOutlined
} from '@ant-design/icons';
import type { AxiosRequestConfig } from 'axios';
import type { 
  ColumnsType, 
  TablePaginationConfig 
} from 'antd/es/table';
import type { 
  FilterValue,
  SorterResult 
} from 'antd/es/table/interface';
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
  return users.map(user => ({
    ...user,
    key: user.id.toString(),
    name: user.nickname ?? user.username,
    username: user.uid ?? user.username,
    createTime: user.createTime ?? user.createdAt,
    role: user.role,
    department: '',  // Add default value for department
    lastLoginTime: ''  // Add default value for lastLoginTime
  }));
};

// 角色选项
const roleOptions = [
  { value: 'ADMIN', label: '管理员' },
  { value: 'USER', label: '普通用户' },
];


const UserManagementPage = () => {
  const [data, setData] = useState<UserData[]>([]);

  // 加载用户数据
  useEffect(() => {
    const abortController = new AbortController();
    fetchUsers({ signal: abortController.signal });
    return () => abortController.abort();
  }, []);

  const fetchUsers = async (config?: AxiosRequestConfig) => {
    setLoading(true);
    try {
      const users = await getUsers(config);
      setData(transformUserData(users));
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
    fetchUsers();
  };

  // 处理表格变更（分页、排序、筛选）
  const handleTableChange = (
    pagination: TablePaginationConfig,
    filters: Record<string, FilterValue | null>,
    sorter: SorterResult<UserData> | SorterResult<UserData>[]
  ) => {
    setPagination(pagination);
    setFilteredInfo(filters);
    setSortedInfo(Array.isArray(sorter) ? sorter[0] : sorter);
  };

  // 处理搜索
  const handleSearch = (value: string) => {
    setSearchText(value);
    if (!value) {
      fetchUsers();
      return;
    }
    const filteredData = data.filter(user => 
      (user.nickname?.toLowerCase().includes(value.toLowerCase())) ||
      (user.name?.toLowerCase().includes(value.toLowerCase())) ||
      (user.email?.toLowerCase().includes(value.toLowerCase()))
    );
    setData(filteredData);
  };

  // 处理添加/编辑用户
  const handleAddEdit = (record?: UserData) => {
    setSelectedRecord(record || null);
    form.resetFields();
    if (record) {
      form.setFieldsValue({
        ...record
      });
    }
    setIsModalVisible(true);
  };

  // 处理删除用户
  const handleDelete = async (key: string) => {
    try {
      await deleteUser(key);
      setData(data.filter(item => item.key !== key));
      message.success('用户已删除');
    } catch {
      message.error('删除用户失败');
    }
  };

  // 处理表单提交
  const handleSubmit = async () => {
    try {
      const values = await form.validateFields() as {
        nickname: string;
        email: string;
        role: string;
        password?: string;
        confirmPassword?: string;
      };
      
      if (selectedRecord) {
        // 编辑现有用户
        await updateUser({
          id: selectedRecord.key,
          nickname: values.nickname,
          email: values.email,
          role: values.role
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
          role: values.role
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
      sorter: (a, b) => a.name.localeCompare(b.name),
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
      filters: roleOptions.map(role => ({ text: role.label, value: role.value })),
      filteredValue: filteredInfo.role || null,
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
      }
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
      width: 100,
      render: (_, record) => (
        <Space size="middle">
          <Tooltip title="编辑">
            <Button 
              type="text" 
              icon={<EditOutlined />} 
              onClick={() => handleAddEdit(record)}
            />
          </Tooltip>
          {!['SUPER_ADMIN'].includes(record.role) && (
            <Tooltip title="删除">
              <Popconfirm
                title="确定要删除此用户吗？"
                description="此操作不可撤销"
                onConfirm={() => {return void handleDelete(record.key)}}
                okText="确定"
                cancelText="取消"
              >
                <Button type="text" danger icon={<DeleteOutlined />} />
              </Popconfirm>
            </Tooltip>
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
          <Breadcrumb.Item>
            <Link to="/system">系统设置</Link>
          </Breadcrumb.Item>
          <Breadcrumb.Item>用户管理</Breadcrumb.Item>
        </Breadcrumb>
        <Space style={{ marginBottom: 16 }}>
          <Input
            placeholder="搜索昵称/邮箱"
            value={searchText}
            onChange={e => handleSearch(e.target.value)}
            style={{ width: 240 }}
            prefix={<SearchOutlined />}
            allowClear
          />
          <Button 
            type="primary" 
            icon={<PlusOutlined />} 
            onClick={() => handleAddEdit()}
          >
            添加用户
          </Button>
          <Button 
            icon={<ReloadOutlined />} 
            onClick={handleReload}
            loading={loading}
          >
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
        <Form
          form={form}
          layout="vertical"
        >
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
                  { type: 'email', message: '请输入有效的邮箱地址' }
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
                <Select 
                  options={roleOptions}
                  placeholder="请选择角色"
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
                  { min: 6, message: '密码长度不能少于6个字符' }
                ]}
              >
                <Input.Password placeholder={selectedRecord ? '留空则不修改密码' : '请输入密码'} />
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
                <Input.Password placeholder={selectedRecord ? '留空则不修改密码' : '请确认密码'} />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>
      </Card>
    </div>
  );
};

export default UserManagementPage;
