import { useState, useRef } from 'react';
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
  Typography
} from 'antd';
import { 
  SearchOutlined, 
  PlusOutlined, 
  EditOutlined, 
  DeleteOutlined, 
  UserOutlined, 
  ExportOutlined,
  ImportOutlined,
  ReloadOutlined,
  QuestionCircleOutlined
} from '@ant-design/icons';
import type { 
  ColumnsType, 
  TablePaginationConfig 
} from 'antd/es/table';
import type { 
  FilterConfirmProps,
  FilterValue,
  SorterResult 
} from 'antd/es/table/interface';
import { PageContainer } from '@ant-design/pro-components';

interface UserData {
  key: string;
  name: string;
  email: string;
  phone: string;
  role: string;
  department: string;
  status: boolean;
  createTime: string;
  lastLoginTime: string;
}

// 模拟数据
const generateMockData = (): UserData[] => {
  const roles = ['管理员', '编辑', '浏览者', '分析师', '数据员'];
  const departments = ['市场部', '销售部', '技术部', '财务部', '人力资源部'];
  
  return Array.from({ length: 35 }, (_, i) => ({
    key: i.toString(),
    name: `用户 ${i + 1}`,
    email: `user${i + 1}@example.com`,
    phone: `1${Math.floor(Math.random() * 10)}${Math.floor(Math.random() * 10)}${Math.floor(Math.random() * 10)}${Math.floor(Math.random() * 10)}${Math.floor(Math.random() * 10)}${Math.floor(Math.random() * 10)}${Math.floor(Math.random() * 10)}${Math.floor(Math.random() * 10)}${Math.floor(Math.random() * 10)}${Math.floor(Math.random() * 10)}`,
    role: roles[Math.floor(Math.random() * roles.length)],
    department: departments[Math.floor(Math.random() * departments.length)],
    status: Math.random() > 0.2,
    createTime: `2025-${Math.floor(Math.random() * 3) + 1}-${Math.floor(Math.random() * 28) + 1}`,
    lastLoginTime: `2025-0${Math.floor(Math.random() * 4) + 1}-${Math.floor(Math.random() * 28) + 1} ${Math.floor(Math.random() * 24)}:${Math.floor(Math.random() * 60)}:${Math.floor(Math.random() * 60)}`
  }));
};

// 角色选项
const roleOptions = [
  { value: '管理员', label: '管理员' },
  { value: '编辑', label: '编辑' },
  { value: '浏览者', label: '浏览者' },
  { value: '分析师', label: '分析师' },
  { value: '数据员', label: '数据员' },
];

// 部门选项
const departmentOptions = [
  { value: '市场部', label: '市场部' },
  { value: '销售部', label: '销售部' },
  { value: '技术部', label: '技术部' },
  { value: '财务部', label: '财务部' },
  { value: '人力资源部', label: '人力资源部' },
];

const { Text } = Typography;

const UserManagementPage = () => {
  const [data, setData] = useState<UserData[]>(generateMockData());
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
    setLoading(true);
    // 模拟API请求
    setTimeout(() => {
      setData(generateMockData());
      setLoading(false);
      message.success('数据已刷新');
    }, 1000);
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
    const filteredData = generateMockData().filter(
      item => 
        item.name.toLowerCase().includes(value.toLowerCase()) ||
        item.email.toLowerCase().includes(value.toLowerCase()) ||
        item.phone.includes(value)
    );
    setData(filteredData);
  };

  // 处理添加/编辑用户
  const handleAddEdit = (record?: UserData) => {
    setSelectedRecord(record || null);
    form.resetFields();
    if (record) {
      form.setFieldsValue(record);
    }
    setIsModalVisible(true);
  };

  // 处理删除用户
  const handleDelete = (key: string) => {
    setData(data.filter(item => item.key !== key));
    message.success('用户已删除');
  };

  // 处理表单提交
  const handleSubmit = () => {
    form.validateFields()
      .then(values => {
        if (selectedRecord) {
          // 编辑现有用户
          setData(data.map(item => 
            item.key === selectedRecord.key ? { ...item, ...values } : item
          ));
          message.success('用户信息已更新');
        } else {
          // 添加新用户
          const newUser = {
            key: Date.now().toString(),
            ...values,
            createTime: new Date().toISOString().split('T')[0],
            lastLoginTime: '-'
          };
          setData([newUser, ...data]);
          message.success('用户已添加');
        }
        setIsModalVisible(false);
      })
      .catch(info => {
        console.log('验证失败:', info);
      });
  };

  // 处理状态变更
  const handleStatusChange = (checked: boolean, key: string) => {
    setData(data.map(item => 
      item.key === key ? { ...item, status: checked } : item
    ));
    message.success(`用户状态已${checked ? '启用' : '禁用'}`);
  };

  // 表格列定义
  const columns: ColumnsType<UserData> = [
    {
      title: '用户名',
      dataIndex: 'name',
      key: 'name',
      sorter: (a, b) => a.name.localeCompare(b.name),
      sortOrder: sortedInfo.columnKey === 'name' ? sortedInfo.order : null,
      render: (text, record) => (
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
    },
    {
      title: '手机号',
      dataIndex: 'phone',
      key: 'phone',
    },
    {
      title: '角色',
      dataIndex: 'role',
      key: 'role',
      filters: roleOptions.map(role => ({ text: role.label, value: role.value })),
      filteredValue: filteredInfo.role || null,
      onFilter: (value, record) => record.role === value,
      render: (role: string) => {
        let color = 'blue';
        if (role === '管理员') color = 'red';
        if (role === '编辑') color = 'green';
        if (role === '浏览者') color = 'geekblue';
        if (role === '分析师') color = 'purple';
        return <Tag color={color}>{role}</Tag>;
      }
    },
    {
      title: '部门',
      dataIndex: 'department',
      key: 'department',
      filters: departmentOptions.map(dept => ({ text: dept.label, value: dept.value })),
      filteredValue: filteredInfo.department || null,
      onFilter: (value, record) => record.department === value,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      filters: [
        { text: '启用', value: true },
        { text: '禁用', value: false }
      ],
      filteredValue: filteredInfo.status || null,
      onFilter: (value, record) => record.status === value,
      render: (status: boolean, record) => (
        <Switch 
          checked={status} 
          onChange={(checked) => handleStatusChange(checked, record.key)}
          size="small"
        />
      ),
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      sorter: (a, b) => new Date(a.createTime).getTime() - new Date(b.createTime).getTime(),
      sortOrder: sortedInfo.columnKey === 'createTime' ? sortedInfo.order : null,
    },
    {
      title: '上次登录',
      dataIndex: 'lastLoginTime',
      key: 'lastLoginTime',
      sorter: (a, b) => {
        if (a.lastLoginTime === '-') return 1;
        if (b.lastLoginTime === '-') return -1;
        return new Date(a.lastLoginTime).getTime() - new Date(b.lastLoginTime).getTime();
      },
      sortOrder: sortedInfo.columnKey === 'lastLoginTime' ? sortedInfo.order : null,
    },
    {
      title: '操作',
      key: 'action',
      fixed: 'right',
      width: 120,
      render: (_, record) => (
        <Space size="middle">
          <Tooltip title="编辑">
            <Button 
              type="text" 
              icon={<EditOutlined />} 
              onClick={() => handleAddEdit(record)}
            />
          </Tooltip>
          <Tooltip title="删除">
            <Popconfirm
              title="确定要删除此用户吗？"
              description="此操作不可撤销"
              onConfirm={() => handleDelete(record.key)}
              okText="确定"
              cancelText="取消"
            >
              <Button type="text" danger icon={<DeleteOutlined />} />
            </Popconfirm>
          </Tooltip>
        </Space>
      ),
    },
  ];

  return (
    <PageContainer
      header={{
        title: '用户管理',
        subTitle: '管理系统用户、分配角色和权限',
      }}
    >
      <Card>
        <Space style={{ marginBottom: 16 }}>
          <Input
            placeholder="搜索用户名/邮箱/电话"
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
            icon={<ImportOutlined />} 
          >
            批量导入
          </Button>
          <Button 
            icon={<ExportOutlined />} 
          >
            导出数据
          </Button>
          <Button 
            icon={<ReloadOutlined />} 
            onClick={handleReload}
            loading={loading}
          >
            刷新
          </Button>
        </Space>

        <Table 
          columns={columns} 
          dataSource={data}
          rowKey="key"
          pagination={pagination}
          loading={loading}
          scroll={{ x: 1300 }}
          onChange={handleTableChange}
        />
      </Card>

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
          initialValues={{ status: true }}
        >
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="name"
                label="用户名"
                rules={[{ required: true, message: '请输入用户名' }]}
              >
                <Input placeholder="请输入用户名" />
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
                name="phone"
                label="手机号"
                rules={[
                  { required: true, message: '请输入手机号' },
                  { pattern: /^1\d{10}$/, message: '请输入有效的手机号' }
                ]}
              >
                <Input placeholder="请输入手机号" />
              </Form.Item>
            </Col>
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
                name="department"
                label="部门"
                rules={[{ required: true, message: '请选择部门' }]}
              >
                <Select 
                  options={departmentOptions}
                  placeholder="请选择部门"
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="status"
                label="状态"
                valuePropName="checked"
              >
                <Switch checkedChildren="启用" unCheckedChildren="禁用" />
              </Form.Item>
            </Col>
          </Row>

          {!selectedRecord && (
            <Row gutter={16}>
              <Col span={12}>
                <Form.Item
                  name="password"
                  label="密码"
                  rules={[
                    { required: true, message: '请输入密码' },
                    { min: 6, message: '密码长度不能少于6个字符' }
                  ]}
                >
                  <Input.Password placeholder="请输入密码" />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item
                  name="confirmPassword"
                  label="确认密码"
                  dependencies={['password']}
                  rules={[
                    { required: true, message: '请确认密码' },
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
                  <Input.Password placeholder="请确认密码" />
                </Form.Item>
              </Col>
            </Row>
          )}
        </Form>
      </Modal>
    </PageContainer>
  );
};

export default UserManagementPage;
