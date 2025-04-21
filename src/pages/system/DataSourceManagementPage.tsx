import { useRef, useState } from 'react';
import { 
  getAllDataSources,
  createDataSource,
  updateDataSource,
  deleteDataSource,
  testDataSourceConnection
} from '../../api/datasource';
import type { DataSource, CreateDataSourceParams, TestConnectionParams } from '../../types/datasourceTypes';
import { PageContainer } from '@ant-design/pro-components';
import { 
  ProTable, 
  ProFormText,
  ProFormSelect,
  DrawerForm,
  ProForm 
} from '@ant-design/pro-components';
import { Button, Tag, message, Popconfirm } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, LinkOutlined } from '@ant-design/icons';
import type { ProColumns, ActionType, RequestData, ParamsType } from '@ant-design/pro-components';
import type { SortOrder } from 'antd/lib/table/interface';

type DataSourceItem = DataSource;

const dataSourceTypeOptions = [
  { label: 'MySQL', value: 'mysql' },
  { label: 'PostgreSQL', value: 'postgresql' },
  { label: 'Oracle', value: 'oracle' },
  { label: 'SQL Server', value: 'sqlserver' },
  { label: 'MongoDB', value: 'mongodb' },
  { label: 'Redis', value: 'redis' },
  { label: 'Elasticsearch', value: 'elasticsearch' },
  { label: 'Hive', value: 'hive' },
  { label: 'ClickHouse', value: 'clickhouse' },
];

const statusOptions = [
  { label: '活跃', value: 'active' },
  { label: '未连接', value: 'inactive' },
];


const DataSourceManagementPage = () => {
  const [drawerVisible, setDrawerVisible] = useState<boolean>(false);
  const [currentDataSource, setCurrentDataSource] = useState<DataSourceItem | undefined>(undefined);
  const [loading, setLoading] = useState({
    table: false,
    submit: false,
    test: false
  });

  const setTableLoading: (isLoading: boolean) => void = (isLoading) => {
    setLoading(prev => ({ ...prev, table: isLoading }));
  };

  const setSubmitLoading = (loading: boolean) => {
    setLoading(prev => ({ ...prev, submit: loading }));
  };

  const setTestLoading = (loading: boolean) => {
    setLoading(prev => ({ ...prev, test: loading }));
  };
  const actionRef = useRef<ActionType>(null);
  
  // 获取数据源列表
  const fetchDataSources: (
    params: ParamsType & {
      current?: number;
      pageSize?: number;
      name?: string;
      type?: string;
      status?: string;
    },
    _: Record<string, SortOrder>,
    __: Record<string, (string | number)[] | null>
  ) => Promise<RequestData<DataSourceItem>> = async (params) => {
    setTableLoading(true);
    try {
      const data = await getAllDataSources();
      if (!data) {
        return {
          data: [],
          success: false,
          total: 0,
        };
      }
      // 前端筛选
      let filteredData = data;
      
      if (params.name && params.name.trim()) {
        filteredData = filteredData.filter(item => item.name.includes(params.name as string));
      }
      
      if (params.type) {
        filteredData = filteredData.filter(item => item.type === params.type);
      }
      
      if (params.status) {
        filteredData = filteredData.filter(item => item.status === params.status);
      }
      
      // 分页处理
      const pageSize = params.pageSize || 10;
      const current = params.current || 1;
      const start = (current - 1) * pageSize;
      const end = start + pageSize;
      
      return {
        data: filteredData.slice(start, end),
        success: true,
        total: filteredData.length,
      };
    } catch {
      message.error('获取数据源列表失败');
      return {
        data: [],
        success: false,
        total: 0,
      };
    } finally {
      setTableLoading(false);
    }
  };
  
  // 删除数据源
  const handleDelete = async (id: string) => {
    try {
      await deleteDataSource(id);
      message.success('数据源删除成功');
      if (actionRef.current) {
        actionRef.current.reload();
      }
    } catch {
      message.error('删除数据源失败');
    }
  };
  
  // 处理表单提交
  const handleFormSubmit = async (values: Omit<CreateDataSourceParams, 'id'>) => {
    setSubmitLoading(true);
    try {
      if (currentDataSource) {
        // 更新操作
        await updateDataSource(currentDataSource.id, {
          ...values,
          id: currentDataSource.id
        });
        message.success('数据源更新成功');
      } else {
        // 新增操作
        await createDataSource(values);
        message.success('数据源创建成功');
      }
      
      setDrawerVisible(false);
      if (actionRef.current) {
        actionRef.current.reload();
      }
      return true;
    } catch {
      message.error(currentDataSource ? '更新数据源失败' : '创建数据源失败');
      return false;
    } finally {
      setSubmitLoading(false);
    }
  };
  
  const openCreateDrawer = () => {
    setCurrentDataSource(undefined);
    setDrawerVisible(true);
  };
  
  const openEditDrawer = (record: DataSourceItem) => {
    setCurrentDataSource(record);
    setDrawerVisible(true);
  };
  
  // 测试数据库连接
  const handleTestConnection = async (values: TestConnectionParams) => {
    if (!values.host || !values.port || !values.database || !values.username) {
      message.error('请填写完整的连接信息');
      return;
    }
    
    setTestLoading(true);
    try {
      const success = await testDataSourceConnection({
        type: values.type,
        host: values.host,
        port: Number(values.port),
        database: values.database,
        username: values.username,
        password: values.password
      });
      
      if (success) {
        message.success('连接测试成功！');
      } else {
        message.error('连接测试失败，请检查连接信息');
      }
    } catch {
      message.error('连接测试失败');
    } finally {
      setTestLoading(false);
    }
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
      dataIndex: 'ip',
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
      dataIndex: 'createTime',
      width: 180,
      hideInSearch: true,
    },
    {
      title: '更新时间',
      dataIndex: 'updateTime',
      width: 180,
      hideInSearch: true,
    },
    {
      title: '操作',
      width: 140,
      valueType: 'option',
      render: (_, record) => [
        <Button
          key="edit"
          type="link"
          onClick={() => openEditDrawer(record)}
          icon={<EditOutlined />}
        >
          编辑
        </Button>,
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
        submitter={{
          render: (props, doms) => {
            return [
              <Button 
                key="test" 
                type="default"
                loading={loading.test}
                icon={<LinkOutlined />}
                onClick={() => {
                  // 获取当前表单的值，并测试连接
                  const values = props.form?.getFieldsValue();
                  handleTestConnection(values);
                }}
              >
                测试连接
              </Button>,
              ...doms,
            ];
          },
        }}
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
            name="ip"
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
