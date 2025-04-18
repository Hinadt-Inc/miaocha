import { useRef, useState } from 'react';
import { PageContainer } from '@ant-design/pro-components';
import { 
  ProTable, 
  ProFormText,
  ProFormSelect,
  DrawerForm,
  ProForm 
} from '@ant-design/pro-components';
import { Space, Button, Tag, message, Popconfirm } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import type { ProColumns, ActionType } from '@ant-design/pro-components';

interface DataSourceItem {
  id: string;
  name: string;
  type: string;
  host: string;
  port: string;
  database: string;
  username: string;
  status: 'active' | 'inactive';
  createdAt: string;
  updatedAt: string;
}

const dataSourceTypeOptions = [
  { label: 'MySQL', value: 'mysql' },
  { label: 'PostgreSQL', value: 'postgresql' },
  { label: 'Oracle', value: 'oracle' },
  { label: 'SQL Server', value: 'sqlserver' },
  { label: 'Hive', value: 'hive' },
  { label: 'ClickHouse', value: 'clickhouse' },
];

const statusOptions = [
  { label: '活跃', value: 'active' },
  { label: '未连接', value: 'inactive' },
];

// 模拟数据
const mockDataSources: DataSourceItem[] = [
  {
    id: '1',
    name: '生产数据库',
    type: 'mysql',
    host: '192.168.1.100',
    port: '3306',
    database: 'prod_db',
    username: 'admin',
    status: 'active',
    createdAt: '2025-03-15 10:30:00',
    updatedAt: '2025-04-10 14:22:05',
  },
  {
    id: '2',
    name: '测试数据库',
    type: 'postgresql',
    host: '192.168.1.101',
    port: '5432',
    database: 'test_db',
    username: 'test_user',
    status: 'active',
    createdAt: '2025-03-20 09:15:30',
    updatedAt: '2025-04-12 11:45:22',
  },
  {
    id: '3',
    name: '开发数据库',
    type: 'clickhouse',
    host: '192.168.1.102',
    port: '8123',
    database: 'dev_db',
    username: 'dev_user',
    status: 'inactive',
    createdAt: '2025-03-25 14:20:10',
    updatedAt: '2025-04-15 16:30:45',
  },
];

const DataSourceManagementPage = () => {
  const [drawerVisible, setDrawerVisible] = useState<boolean>(false);
  const [currentDataSource, setCurrentDataSource] = useState<DataSourceItem | undefined>(undefined);
  const actionRef = useRef<ActionType>();
  
  // 模拟API调用
  const fetchDataSources = async (params: any) => {
    console.log('查询参数:', params);
    
    // 模拟分页和筛选
    let dataSource = [...mockDataSources];
    
    if (params.name) {
      dataSource = dataSource.filter(item => item.name.includes(params.name));
    }
    
    if (params.type) {
      dataSource = dataSource.filter(item => item.type === params.type);
    }
    
    if (params.status) {
      dataSource = dataSource.filter(item => item.status === params.status);
    }
    
    return {
      data: dataSource,
      success: true,
      total: dataSource.length,
    };
  };
  
  // 模拟删除数据源
  const handleDelete = (id: string) => {
    message.success(`数据源 ${id} 已删除`);
    if (actionRef.current) {
      actionRef.current.reload();
    }
  };
  
  // 处理表单提交
  const handleFormSubmit = async (values: any) => {
    if (currentDataSource) {
      // 更新操作
      message.success(`数据源 ${values.name} 已更新`);
    } else {
      // 新增操作
      message.success(`数据源 ${values.name} 已创建`);
    }
    
    setDrawerVisible(false);
    if (actionRef.current) {
      actionRef.current.reload();
    }
    return true;
  };
  
  const openCreateDrawer = () => {
    setCurrentDataSource(undefined);
    setDrawerVisible(true);
  };
  
  const openEditDrawer = (record: DataSourceItem) => {
    setCurrentDataSource(record);
    setDrawerVisible(true);
  };
  
  const columns: ProColumns<DataSourceItem>[] = [
    {
      title: '数据源名称',
      dataIndex: 'name',
      width: 180,
      ellipsis: true,
    },
    {
      title: '类型',
      dataIndex: 'type',
      width: 120,
      valueEnum: Object.fromEntries(dataSourceTypeOptions.map(option => [option.value, option.label])),
      render: (_, record) => {
        const typeOption = dataSourceTypeOptions.find(option => option.value === record.type);
        return typeOption?.label || record.type;
      },
    },
    {
      title: '主机',
      dataIndex: 'host',
      width: 150,
      ellipsis: true,
    },
    {
      title: '端口',
      dataIndex: 'port',
      width: 80,
    },
    {
      title: '数据库名称',
      dataIndex: 'database',
      width: 140,
      ellipsis: true,
    },
    {
      title: '用户名',
      dataIndex: 'username',
      width: 120,
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      valueEnum: {
        active: { text: '活跃', status: 'Success' },
        inactive: { text: '未连接', status: 'Error' },
      },
      render: (_, record) => (
        <Tag color={record.status === 'active' ? 'green' : 'red'}>
          {record.status === 'active' ? '活跃' : '未连接'}
        </Tag>
      ),
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      width: 180,
      hideInSearch: true,
    },
    {
      title: '更新时间',
      dataIndex: 'updatedAt',
      width: 180,
      hideInSearch: true,
    },
    {
      title: '操作',
      width: 140,
      valueType: 'option',
      render: (_, record) => [
        <a key="edit" onClick={() => openEditDrawer(record)}>
          <EditOutlined /> 编辑
        </a>,
        <Popconfirm
          key="delete"
          title="确定要删除此数据源吗?"
          onConfirm={() => handleDelete(record.id)}
        >
          <a><DeleteOutlined /> 删除</a>
        </Popconfirm>,
      ],
    },
  ];
  
  return (
    <PageContainer>
      <ProTable<DataSourceItem>
        headerTitle="数据源管理"
        actionRef={actionRef}
        rowKey="id"
        search={{
          labelWidth: 'auto',
        }}
        toolBarRender={() => [
          <Button 
            key="button" 
            icon={<PlusOutlined />} 
            type="primary" 
            onClick={openCreateDrawer}
          >
            新增数据源
          </Button>,
        ]}
        request={fetchDataSources}
        columns={columns}
        pagination={{
          pageSize: 10,
          showSizeChanger: true,
        }}
      />
      
      <DrawerForm
        title={currentDataSource ? '编辑数据源' : '新增数据源'}
        width="500px"
        open={drawerVisible}
        onVisibleChange={setDrawerVisible}
        onFinish={handleFormSubmit}
        initialValues={currentDataSource}
      >
        <ProForm.Group>
          <ProFormText
            width="md"
            name="name"
            label="数据源名称"
            placeholder="请输入数据源名称"
            rules={[{ required: true, message: '请输入数据源名称' }]}
          />
          <ProFormSelect
            width="md"
            name="type"
            label="类型"
            options={dataSourceTypeOptions}
            placeholder="请选择数据源类型"
            rules={[{ required: true, message: '请选择数据源类型' }]}
          />
        </ProForm.Group>
        
        <ProForm.Group>
          <ProFormText
            width="md"
            name="host"
            label="主机"
            placeholder="请输入主机地址"
            rules={[{ required: true, message: '请输入主机地址' }]}
          />
          <ProFormText
            width="md"
            name="port"
            label="端口"
            placeholder="请输入端口号"
            rules={[{ required: true, message: '请输入端口号' }]}
          />
        </ProForm.Group>
        
        <ProForm.Group>
          <ProFormText
            width="md"
            name="database"
            label="数据库名称"
            placeholder="请输入数据库名称"
            rules={[{ required: true, message: '请输入数据库名称' }]}
          />
          <ProFormText
            width="md"
            name="username"
            label="用户名"
            placeholder="请输入用户名"
            rules={[{ required: true, message: '请输入用户名' }]}
          />
        </ProForm.Group>
        
        <ProFormText.Password
          width="md"
          name="password"
          label="密码"
          placeholder="请输入密码"
          rules={[{ required: !currentDataSource, message: '请输入密码' }]}
        />
        
        <ProFormSelect
          width="md"
          name="status"
          label="状态"
          options={statusOptions}
          placeholder="请选择状态"
          rules={[{ required: true, message: '请选择状态' }]}
        />
      </DrawerForm>
    </PageContainer>
  );
};

export default DataSourceManagementPage;
