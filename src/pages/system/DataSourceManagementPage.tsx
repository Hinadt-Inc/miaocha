import styles from './DataSourceManagementPage.module.less';
import { useRef, useState } from 'react';
import {
  getAllDataSources,
  createDataSource,
  updateDataSource,
  deleteDataSource,
  testDataSourceConnection,
  testExistingDataSourceConnection,
} from '../../api/datasource';
import type { DataSource, CreateDataSourceParams, TestConnectionParams } from '../../types/datasourceTypes';
import dayjs from 'dayjs';
import { ProTable, ProFormText, ProFormSelect, ModalForm, ProFormTextArea } from '@ant-design/pro-components';
import { Button, Popconfirm, Breadcrumb, Input, Space } from 'antd';
import { PlusOutlined, LinkOutlined, HomeOutlined, SearchOutlined } from '@ant-design/icons';
import type { ProColumns, ActionType, RequestData, ParamsType } from '@ant-design/pro-components';
import type { SortOrder } from 'antd/lib/table/interface';
import { Link } from 'react-router-dom';
import { useErrorContext, ErrorType } from '../../providers/ErrorProvider';

type DataSourceItem = DataSource;

const dataSourceTypeOptions = [{ label: 'Doris', value: 'Doris' }];

const DataSourceManagementPage = () => {
  const [modalVisible, setModalVisible] = useState<boolean>(false);
  const [currentDataSource, setCurrentDataSource] = useState<DataSourceItem | undefined>(undefined);
  const [searchKeyword, setSearchKeyword] = useState<string>('');
  const { handleError, showSuccess } = useErrorContext();
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
  });
  const [loading, setLoading] = useState({
    table: false,
    submit: false,
    test: false,
    testExisting: {} as Record<string, boolean>, // 为每个数据源维护测试状态
  });

  const setSubmitLoading = (loading: boolean) => {
    setLoading((prev) => ({ ...prev, submit: loading }));
  };

  const setTestLoading = (loading: boolean) => {
    setLoading((prev) => ({ ...prev, test: loading }));
  };

  const setTestExistingLoading = (id: string, loading: boolean) => {
    setLoading((prev) => ({
      ...prev,
      testExisting: { ...prev.testExisting, [id]: loading },
    }));
  };
  const actionRef = useRef<ActionType>(null);

  // 获取数据源列表
  const fetchDataSources: (
    params: ParamsType & {
      current?: number;
      pageSize?: number;
    },
    _: Record<string, SortOrder>,
    __: Record<string, (string | number)[] | null>,
  ) => Promise<RequestData<DataSourceItem>> = async (params) => {
    try {
      const data = await getAllDataSources();
      if (!data) {
        return {
          data: [],
          success: false,
          total: 0,
        };
      }

      // 直接使用最新获取的数据进行过滤
      let filteredData = [...data];

      // 使用 searchKeyword 状态进行前端筛选
      const keyword = searchKeyword.trim();
      if (keyword) {
        const lowercaseKeyword = keyword.toLowerCase();
        filteredData = filteredData.filter(
          (item) =>
            item.name.toLowerCase().includes(lowercaseKeyword) ||
            item.database.toLowerCase().includes(lowercaseKeyword) ||
            item.ip?.toLowerCase().includes(lowercaseKeyword) ||
            item.description?.toLowerCase().includes(lowercaseKeyword),
        );
      }

      // 分页处理
      const pageSize = params.pageSize ?? pagination.pageSize;
      const current = params.current ?? pagination.current;
      const start = (current - 1) * pageSize;
      const end = start + pageSize;

      return {
        data: filteredData.slice(start, end),
        success: true,
        total: filteredData.length,
      };
    } catch {
      // 数据加载失败时的错误处理已由全局错误处理器处理
      return {
        data: [],
        success: false,
        total: 0,
      };
    }
  };

  // 删除数据源
  const handleDelete = async (id: string, name: string) => {
    try {
      await deleteDataSource(id);
      showSuccess(`数据源 "${name}" 删除成功`);
      // 直接刷新表格数据
      actionRef.current?.reload();
    } catch {
      // API 错误已由全局错误处理器处理，这里不再重复处理
    }
  };

  // 处理分页变更
  const handlePageChange = (page: number, pageSize: number) => {
    setPagination({
      current: page,
      pageSize,
    });
  };

  // 处理表单提交
  const handleFormSubmit = async (values: Omit<CreateDataSourceParams, 'id'>) => {
    setSubmitLoading(true);
    try {
      const formattedValues = {
        ...values,
      };

      if (currentDataSource) {
        // 更新操作
        const updated = await updateDataSource(currentDataSource.id, {
          ...formattedValues,
          id: currentDataSource.id,
        });
        if (updated) {
          showSuccess(`数据源 "${values.name}" 更新成功`);
        }
      } else {
        // 新增操作
        const newDataSource = await createDataSource(formattedValues);
        if (newDataSource) {
          showSuccess(`数据源 "${values.name}" 创建成功`);
        }
      }

      setModalVisible(false);
      // 保留当前分页设置进行重新加载
      void actionRef.current!.reload();
      return true;
    } catch {
      // API 错误已由全局错误处理器处理，这里不再重复处理
      return false;
    } finally {
      setSubmitLoading(false);
    }
  };

  const openCreateModal = () => {
    setCurrentDataSource(undefined);
    setModalVisible(true);
  };

  const openEditModal = (record: DataSourceItem) => {
    // 确保数据中的ip字段映射到ip字段上
    const mappedRecord = {
      ...record,
      // 如果record有ip字段但没有ip字段，则使用ip值作为ip值
      ip: record.ip,
      // 使用jdbcUrl字段
      jdbcUrl: record.jdbcUrl,
    };
    setCurrentDataSource(mappedRecord);
    setModalVisible(true);
  };

  // 测试数据库连接
  const handleTestConnection = async (values: TestConnectionParams) => {
    if (!values.jdbcUrl || !values.username || !values.password) {
      handleError('请完善连接信息：JDBC URL、用户名和密码不能为空', {
        type: ErrorType.VALIDATION,
        showType: 'message',
      });
      return;
    }

    setTestLoading(true);
    try {
      // 准备测试连接参数
      const testParams = {
        name: values.name, // 添加数据源名称
        type: values.type,
        jdbcUrl: values.jdbcUrl,
        username: values.username,
        password: values.password,
      } as TestConnectionParams;

      await testDataSourceConnection(testParams);
      showSuccess('数据库连接测试成功！连接配置正确，可以正常访问数据库');
    } catch {
      // API 错误已由全局错误处理器处理，这里不再重复处理
    } finally {
      setTestLoading(false);
    }
  };

  // 测试现有数据源连接
  const handleTestExistingConnection = async (id: string, name: string) => {
    setTestExistingLoading(id, true);
    try {
      await testExistingDataSourceConnection(id);
      showSuccess(`数据源 "${name}" 连接测试成功！数据库连接正常，可以正常使用`);
    } catch {
      // API 错误已由全局错误处理器处理，这里不再重复处理
    } finally {
      setTestExistingLoading(id, false);
    }
  };

  const columns: ProColumns<DataSourceItem>[] = [
    {
      title: '数据源名称',
      dataIndex: 'name',
      ellipsis: true,
      width: '10%',
      hideInSearch: true,
    },
    {
      title: '类型',
      dataIndex: 'type',
      width: '8%',
      valueEnum: Object.fromEntries(dataSourceTypeOptions.map((option) => [option.value, option.label])),
      render: (_, record) => {
        const typeOption = dataSourceTypeOptions.find((option) => option.value === record.type);
        return typeOption?.label ?? record.type;
      },
      hideInSearch: true,
    },
    {
      title: 'JDBC URL',
      dataIndex: 'jdbcUrl',
      width: '12%',
      ellipsis: true,
      hideInSearch: true,
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      width: '12%',
      hideInSearch: true,
      render: (text, record) => {
        const time = text || (record as any).createdAt;
        return time ? dayjs(time as string).format('YYYY-MM-DD HH:mm:ss') : '-';
      },
      responsive: ['lg'],
    },
    {
      title: '创建人',
      dataIndex: 'creator',
      width: '10%',
      hideInSearch: true,
      render: (_, record) => record.createUser || '-',
      responsive: ['lg'],
    },
    {
      title: '更新时间',
      dataIndex: 'updateTime',
      width: '12%',
      hideInSearch: true,
      render: (text, record) => {
        const time = text || (record as any).updatedAt;
        return time ? dayjs(time as string).format('YYYY-MM-DD HH:mm:ss') : '-';
      },
      responsive: ['lg'],
    },
    {
      title: '更新人',
      dataIndex: 'updater',
      width: '10%',
      hideInSearch: true,
      render: (_, record) => record.updateUser || '-',
      responsive: ['lg'],
    },
    {
      title: '操作',
      width: '12%',
      align: 'center',
      valueType: 'option',
      fixed: 'right',
      render: (_, record) => (
        <Space size={4} wrap>
          <Button
            key="testConnection"
            type="link"
            size="small"
            icon={<LinkOutlined />}
            loading={loading.testExisting[record.id] || false}
            onClick={() => handleTestExistingConnection(record.id, record.name)}
            style={{ padding: '0 4px' }}
            title={`测试 ${record.name} 的数据库连接是否正常`}
          >
            测试连接
          </Button>
          <Button
            key="edit"
            type="link"
            size="small"
            onClick={() => openEditModal(record)}
            style={{ padding: '0 4px' }}
          >
            编辑
          </Button>
          <Popconfirm
            key="delete"
            title={`确定要删除数据源 "${record.name}" 吗？`}
            description="删除后将无法恢复，请谨慎操作"
            placement="topRight"
            okText="确定删除"
            cancelText="取消"
            okButtonProps={{ danger: true }}
            onConfirm={() => {
              void handleDelete(record.id, record.name);
            }}
          >
            <Button type="link" size="small" danger style={{ padding: '0 4px' }}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div className={styles.container}>
      <ProTable<DataSourceItem>
        loading={loading.table}
        className={styles.tableContainer}
        bordered
        size="small"
        search={false}
        options={false}
        headerTitle={
          <Breadcrumb
            items={[
              {
                title: (
                  <Link to="/">
                    <HomeOutlined />
                  </Link>
                ),
              },
              { title: '数据源管理' },
            ]}
          ></Breadcrumb>
        }
        actionRef={actionRef}
        rowKey="id"
        scroll={{ x: 'max-content' }}
        cardProps={{ bodyStyle: { padding: '0px' } }}
        toolBarRender={() => [
          <Space key="search">
            <Input
              placeholder="搜索数据源（支持名称、JDBC地址、描述等关键词）"
              allowClear
              onChange={(e) => {
                // 当输入变化时立即搜索，提供更即时的反馈
                setSearchKeyword(e.target.value);
                // 搜索时重置为第一页，但保留每页条数
                setPagination((prev) => ({ ...prev, current: 1 }));
                actionRef.current?.reload();
              }}
              suffix={<SearchOutlined />}
              style={{ width: 300 }}
            />
          </Space>,
          <div className={styles.tableToolbar} key="toolbar">
            <Button key="button" icon={<PlusOutlined />} type="primary" onClick={openCreateModal}>
              新增数据源
            </Button>
          </div>,
        ]}
        request={fetchDataSources}
        defaultData={[]}
        columns={columns}
        pagination={{
          current: pagination.current,
          pageSize: pagination.pageSize,
          onChange: handlePageChange,
          showSizeChanger: true,
          responsive: true,
          showTotal: (total, range) => `第 ${range[0]}-${range[1]} 条，共 ${total} 个数据源`,
          pageSizeOptions: ['10', '20', '50', '100'],
        }}
      />

      <ModalForm
        title={currentDataSource ? '编辑数据源' : '新增数据源'}
        width="850px"
        open={modalVisible}
        onOpenChange={setModalVisible}
        onFinish={handleFormSubmit}
        modalProps={{
          destroyOnClose: true,
          maskClosable: false,
          centered: true,
        }}
        layout="horizontal"
        labelCol={{ span: 6 }}
        wrapperCol={{ span: 18 }}
        grid={true}
        rowProps={{ gutter: [16, 0] }}
        initialValues={currentDataSource}
        submitter={{
          render: (props, doms: React.ReactNode[]) => {
            return [
              <Button
                key="test"
                type="default"
                loading={loading.test}
                icon={<LinkOutlined />}
                onClick={() => {
                  // 获取当前表单的值，并测试连接
                  const values = props.form?.getFieldsValue() as TestConnectionParams;
                  void handleTestConnection(values);
                }}
                title="验证数据库连接配置是否正确"
              >
                测试数据库连接
              </Button>,
              ...doms,
            ];
          },
        }}
      >
        <ProFormText
          colProps={{ span: 12 }}
          name="name"
          label="数据源名称"
          placeholder="例如：生产环境数据库、测试环境数据库"
          rules={[
            { required: true, message: '请输入数据源名称，便于识别和管理' },
            { max: 50, message: '数据源名称不能超过50个字符' },
          ]}
        />
        <ProFormSelect
          colProps={{ span: 12 }}
          name="type"
          label="数据库类型"
          options={dataSourceTypeOptions}
          placeholder="请选择数据库类型"
          rules={[{ required: true, message: '请选择数据库类型' }]}
        />
        <ProFormText
          colProps={{ span: 12 }}
          name="username"
          label="数据库用户名"
          placeholder="请输入数据库登录用户名"
          rules={[
            { required: true, message: '请输入数据库用户名' },
            { max: 50, message: '用户名不能超过50个字符' },
          ]}
        />
        <ProFormText.Password
          colProps={{ span: 12 }}
          name="password"
          label="数据库密码"
          placeholder={currentDataSource ? '不修改密码请留空' : '请输入数据库密码'}
          rules={currentDataSource ? [] : [{ required: true, message: '请输入数据库密码' }]}
          tooltip={currentDataSource ? '如需修改密码请输入新密码，否则留空保持原密码不变' : ''}
        />

        <ProFormText
          colProps={{ span: 24 }}
          name="jdbcUrl"
          label="JDBC 连接地址"
          placeholder="例如：jdbc:mysql://192.168.1.100:3306/database_name?useSSL=false&serverTimezone=UTC"
          rules={[
            { required: true, message: '请输入JDBC连接地址' },
            {
              pattern: /^jdbc:/,
              message: 'JDBC连接地址必须以 jdbc: 开头',
            },
          ]}
          labelCol={{ span: 3 }}
          wrapperCol={{ span: 21 }}
          tooltip="请输入完整的JDBC连接字符串，包含数据库类型、主机地址、端口号、数据库名称等信息"
        />

        <ProFormTextArea
          colProps={{ span: 24 }}
          name="description"
          label="数据源描述"
          placeholder="请简要描述该数据源的用途，例如：生产环境主数据库，用于存储用户订单数据"
          labelCol={{ span: 3 }}
          wrapperCol={{ span: 21 }}
          fieldProps={{
            rows: 3,
            maxLength: 200,
            showCount: true,
          }}
          tooltip="详细描述数据源的用途和特点，便于团队成员理解和使用"
        />
      </ModalForm>
    </div>
  );
};

import withSystemAccess from '@/utils/withSystemAccess';
export default withSystemAccess(DataSourceManagementPage);
