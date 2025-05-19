import { useRef, useState, useEffect } from 'react';
import {
  getAllDataSources,
  createDataSource,
  updateDataSource,
  deleteDataSource,
  testDataSourceConnection,
} from '../../api/datasource';
import type {
  DataSource,
  CreateDataSourceParams,
  TestConnectionParams,
} from '../../types/datasourceTypes';
import dayjs from 'dayjs';
import {
  ProTable,
  ProFormText,
  ProFormSelect,
  ModalForm,
  ProForm,
  ProFormDigit,
  ProFormTextArea,
} from '@ant-design/pro-components';
import { Button, message, Popconfirm, Breadcrumb, Card, Input, Space } from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  LinkOutlined,
  HomeOutlined,
  SearchOutlined,
} from '@ant-design/icons';
import type { ProColumns, ActionType, RequestData, ParamsType } from '@ant-design/pro-components';
import type { SortOrder } from 'antd/lib/table/interface';

type DataSourceItem = DataSource;

const dataSourceTypeOptions = [{ label: 'Doris', value: 'Doris' }];

const DataSourceManagementPage = () => {
  const [modalVisible, setModalVisible] = useState<boolean>(false);
  const [currentDataSource, setCurrentDataSource] = useState<DataSourceItem | undefined>(undefined);
  const [searchKeyword, setSearchKeyword] = useState<string>('');
  const [allDataSources, setAllDataSources] = useState<DataSourceItem[]>([]);
  const [dataSourcesLoaded, setDataSourcesLoaded] = useState<boolean>(false);
  const [loading, setLoading] = useState({
    table: false,
    submit: false,
    test: false,
  });

  const setTableLoading: (isLoading: boolean) => void = (isLoading) => {
    setLoading((prev) => ({ ...prev, table: isLoading }));
  };

  const setSubmitLoading = (loading: boolean) => {
    setLoading((prev) => ({ ...prev, submit: loading }));
  };

  const setTestLoading = (loading: boolean) => {
    setLoading((prev) => ({ ...prev, test: loading }));
  };
  const actionRef = useRef<ActionType>(null);

  // 在组件挂载后自动加载数据源数据
  useEffect(() => {
    setTableLoading(true);
    getAllDataSources()
      .then((data) => {
        if (data) {
          setAllDataSources(data);
        }
      })
      .catch(() => {
        message.error('获取数据源列表失败');
      })
      .finally(() => {
        setTableLoading(false);
      });
  }, []); // 空依赖数组，只在组件挂载时执行一次

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
            ((item.host || (item as any).ip) &&
              (item.host || (item as any).ip).toLowerCase().includes(lowercaseKeyword)) ||
            (item.description && item.description.toLowerCase().includes(lowercaseKeyword)),
        );
      }

      // 分页处理
      const pageSize = params.pageSize ?? 10;
      const current = params.current ?? 1;
      const start = (current - 1) * pageSize;
      const end = start + pageSize;

      return {
        data: filteredData.slice(start, end),
        success: true,
        total: filteredData.length,
      };
    } catch {
      return {
        data: [],
        success: false,
        total: 0,
      };
    }
  };

  // 删除数据源
  const handleDelete = async (id: string) => {
    try {
      await deleteDataSource(id);
      // 更新本地缓存
      setAllDataSources((prev) => prev.filter((item) => item.id !== id));
      message.success('数据源删除成功');
      // 刷新表格显示
      actionRef.current?.reloadAndRest?.();
    } catch {
      message.error('删除数据源失败');
    }
  };

  // 处理表单提交
  const handleFormSubmit = async (values: Omit<CreateDataSourceParams, 'id'>) => {
    setSubmitLoading(true);
    try {
      // 确保数据兼容性，如果后端期望ip字段而不是host
      const formattedValues = {
        ...values,
        // 如果后端API实际使用的是ip字段，可以启用下面的映射
        // ip: values.host,
      };

      if (currentDataSource) {
        // 更新操作
        const updated = await updateDataSource(currentDataSource.id, {
          ...formattedValues,
          id: currentDataSource.id,
        });
        if (updated) {
          // 更新本地缓存
          setAllDataSources((prev) =>
            prev.map((item) =>
              item.id === currentDataSource.id ? { ...item, ...formattedValues } : item,
            ),
          );
          message.success('数据源更新成功');
        }
      } else {
        // 新增操作
        const newDataSource = await createDataSource(formattedValues);
        if (newDataSource) {
          // 更新本地缓存
          setAllDataSources((prev) => [...prev, newDataSource]);
          message.success('数据源创建成功');
        }
      }

      setModalVisible(false);
      void actionRef.current!.reload();
      return true;
    } catch {
      message.error(currentDataSource ? '更新数据源失败' : '创建数据源失败');
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
    // 确保数据中的ip字段映射到host字段上
    const mappedRecord = {
      ...record,
      // 如果record有ip字段但没有host字段，则使用ip值作为host值
      host: record.host || (record as any).ip,
    };
    setCurrentDataSource(mappedRecord as DataSourceItem);
    setModalVisible(true);
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
        password: values.password,
        jdbcParams: values.jdbcParams,
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
      ellipsis: true,
      width: '10%',
      hideInSearch: true,
    },
    {
      title: '类型',
      dataIndex: 'type',
      width: '8%',
      valueEnum: Object.fromEntries(
        dataSourceTypeOptions.map((option) => [option.value, option.label]),
      ),
      render: (_, record) => {
        const typeOption = dataSourceTypeOptions.find((option) => option.value === record.type);
        return typeOption?.label ?? record.type;
      },
      hideInSearch: true,
    },
    {
      title: '描述',
      dataIndex: 'description',
      width: '12%',
      ellipsis: true,
      hideInSearch: true,
    },
    {
      title: '主机',
      dataIndex: 'host',
      width: '10%',
      ellipsis: true,
      hideInSearch: true,
      render: (_, record) => record.host || (record as any).ip,
    },
    {
      title: '端口',
      dataIndex: 'port',
      width: '6%',
      hideInSearch: true,
    },
    {
      title: '数据库名称',
      dataIndex: 'database',
      width: '10%',
      ellipsis: true,
      hideInSearch: true,
    },
    {
      title: 'JDBC参数',
      dataIndex: 'jdbcParams',
      width: '12%',
      ellipsis: true,
      hideInSearch: true,
      render: (text) => {
        if (!text) return '-';
        try {
          return typeof text === 'string' ? text : JSON.stringify(text);
        } catch {
          return '-';
        }
      },
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
      title: '操作',
      width: '8%',
      align: 'center',
      valueType: 'option',
      fixed: 'right',
      render: (_, record) => (
        <Space size={0}>
          <Button
            key="edit"
            type="link"
            size="small"
            onClick={() => openEditModal(record)}
            icon={<EditOutlined />}
            style={{ padding: '0 4px' }}
          >
            编辑
          </Button>
          <Popconfirm
            key="delete"
            title="确定要删除此数据源吗?"
            placement="topRight"
            okText="确定"
            cancelText="取消"
            onConfirm={() => {
              void handleDelete(record.id);
            }}
          >
            <Button
              type="link"
              size="small"
              danger
              icon={<DeleteOutlined />}
              style={{ padding: '0 4px' }}
            >
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div className="data-source-management-page">
      <Card>
        <ProTable<DataSourceItem>
          className="table-container"
          bordered
          search={false}
          options={false}
          headerTitle={
            <Breadcrumb>
              <Breadcrumb.Item href="/">
                <HomeOutlined />
              </Breadcrumb.Item>
              <Breadcrumb.Item>数据源管理</Breadcrumb.Item>
            </Breadcrumb>
          }
          actionRef={actionRef}
          rowKey="id"
          scroll={{ x: 'max-content' }}
          cardProps={{ bodyStyle: { padding: '0px' } }}
          toolBarRender={() => [
            <Space key="search" style={{ width: '100%' }}>
              <Input.Search
                placeholder="搜索数据源名称/主机/数据库/描述"
                allowClear
                style={{ width: '100%', minWidth: 200, maxWidth: 300 }}
                onChange={(e) => {
                  // 当输入变化时立即搜索，提供更即时的反馈
                  setSearchKeyword(e.target.value);
                  actionRef.current?.reloadAndRest?.();
                }}
                onSearch={(value) => {
                  setSearchKeyword(value);
                  actionRef.current?.reloadAndRest?.();
                }}
              />
            </Space>,
            <div className="table-toolbar" key="toolbar">
              <Button key="button" icon={<PlusOutlined />} type="primary" onClick={openCreateModal}>
                新增数据源
              </Button>
            </div>,
          ]}
          request={fetchDataSources}
          defaultData={[]}
          columns={columns}
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            responsive: true,
            showTotal: (total) => `共 ${total} 条`,
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
            bodyStyle: { padding: '24px 24px 8px' },
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
                    void handleTestConnection(values).catch(() => {
                      message.error('连接测试失败');
                    });
                  }}
                >
                  测试连接
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
            placeholder="请输入数据源名称"
            rules={[{ required: true, message: '请输入数据源名称' }]}
          />
          <ProFormSelect
            colProps={{ span: 12 }}
            name="type"
            label="类型"
            options={dataSourceTypeOptions}
            placeholder="请选择数据源类型"
            rules={[{ required: true, message: '请选择数据源类型' }]}
          />
          <ProFormText
            colProps={{ span: 12 }}
            name="host"
            label="主机"
            placeholder="请输入主机地址"
            rules={[{ required: true, message: '请输入主机地址' }]}
            tooltip="数据库服务器地址，可以是IP或域名"
          />
          <ProFormText
            colProps={{ span: 12 }}
            name="port"
            label="端口"
            placeholder="请输入端口号"
            rules={[
              { required: true, message: '请输入端口号' },
              { pattern: /^\d+$/, message: '端口号必须为数字' },
            ]}
          />
          <ProFormText
            colProps={{ span: 12 }}
            name="database"
            label="数据库名称"
            placeholder="请输入数据库名称"
            rules={[{ required: true, message: '请输入数据库名称' }]}
          />
          <ProFormText
            colProps={{ span: 12 }}
            name="username"
            label="用户名"
            placeholder="请输入用户名"
            rules={[{ required: true, message: '请输入用户名' }]}
          />
          <ProFormText.Password
            colProps={{ span: 12 }}
            name="password"
            label="密码"
            placeholder="请输入密码"
            rules={[{ required: true, message: '请输入密码' }]}
            tooltip={currentDataSource ? '不修改密码请留空' : ''}
          />
          <ProFormTextArea
            colProps={{ span: 24 }}
            name="description"
            label="数据源描述"
            placeholder="请输入数据源描述信息（选填）"
            labelCol={{ span: 3 }}
            wrapperCol={{ span: 21 }}
            fieldProps={{
              rows: 2,
              maxLength: 200,
              showCount: true,
            }}
          />
          <ProFormTextArea
            colProps={{ span: 24 }}
            name="jdbcParams"
            label="JDBC参数"
            placeholder='请输入JDBC参数，例如: {"connectTimeout": 3000}'
            initialValue={JSON.stringify({ connectTimeout: 3000 })}
            labelCol={{ span: 3 }}
            wrapperCol={{ span: 21 }}
            fieldProps={{
              rows: 3,
              allowClear: true,
            }}
            tooltip="JSON格式的额外连接参数配置"
            rules={[
              {
                validator: async (_: any, value: string) => {
                  if (!value) return Promise.resolve();
                  try {
                    JSON.parse(value);
                    return Promise.resolve();
                  } catch (e) {
                    return Promise.reject(new Error('请输入有效的JSON格式'));
                  }
                },
              },
            ]}
            transform={(value: any) => {
              if (!value) return { jdbcParams: { connectTimeout: 3000 } };
              try {
                return { jdbcParams: JSON.parse(value) };
              } catch (e) {
                return { jdbcParams: { connectTimeout: 3000 } };
              }
            }}
          />
        </ModalForm>
      </Card>
    </div>
  );
};

export default DataSourceManagementPage;
