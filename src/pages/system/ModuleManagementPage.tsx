import { useState, useEffect, useRef } from 'react';
import ExecuteConfirmationModal from '@/pages/SQLEditor/components/ExecuteConfirmationModal';
import {
  Button,
  Input,
  Table,
  Space,
  Modal,
  Form,
  Select,
  Radio,
  message,
  Row,
  Col,
  Breadcrumb,
  Tag,
  Descriptions,
  Tooltip,
} from 'antd';
import { getModuleDetail } from '@/api/modules';
import type { ColumnsType } from 'antd/es/table';
import { SearchOutlined, PlusOutlined, ReloadOutlined, HomeOutlined, DatabaseOutlined } from '@ant-design/icons';
import {
  getModules,
  createModule,
  updateModule,
  deleteModule,
  executeDorisSql,
  type Module as BaseModule,
  type CreateModuleParams,
  type UpdateModuleParams,
} from '@/api/modules';
import { DORIS_TEMPLATE } from '@/utils/logstashTemplates';

interface Module extends BaseModule {}

import { Link } from 'react-router-dom';
import styles from './UserManagementPage.module.less';

import { getDataSources } from '@/api/datasource';
import type { DataSource } from '@/types/datasourceTypes';
import dayjs from 'dayjs';

interface ModuleData extends Module {
  key: string;
  users?: Array<{
    nickname: string;
    userId: string;
    role: string;
  }>;
}

const transformModuleData = (modules: Module[]): ModuleData[] => {
  return modules.map((module) => ({
    ...module,
    key: module.id.toString(),
    users: module.users?.map((user) => ({
      ...user,
      role: (user as any).role || 'USER',
    })),
  }));
};

const ModuleManagementPage = () => {
  const [data, setData] = useState<ModuleData[]>([]);
  const [dataSources, setDataSources] = useState<DataSource[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchText, setSearchText] = useState('');
  const [isModalVisible, setIsModalVisible] = useState(false);
  const [isDetailVisible, setIsDetailVisible] = useState(false);
  const [selectedRecord, setSelectedRecord] = useState<ModuleData | null>(null);
  const [moduleDetail, setModuleDetail] = useState<Module | null>(null);
  const [executeModalVisible, setExecuteModalVisible] = useState(false);
  const [deleteModalVisible, setDeleteModalVisible] = useState(false);
  const [deleteRecord, setDeleteRecord] = useState<ModuleData | null>(null);
  const [deleteDorisTable, setDeleteDorisTable] = useState(false);
  const [executeSql, setExecuteSql] = useState('');
  const [currentRecord, setCurrentRecord] = useState<ModuleData | null>(null);
  const [executing, setExecuting] = useState(false);
  const [isReadOnlyMode, setIsReadOnlyMode] = useState(false); // 新增只读模式状态
  const [form] = Form.useForm();
  const searchTimeoutRef = useRef<number | null>(null);
  const originalDataRef = useRef<ModuleData[]>([]);
  const [messageApi, contextHolder] = message.useMessage();

  useEffect(() => {
    return () => {
      if (searchTimeoutRef.current !== null) {
        window.clearTimeout(searchTimeoutRef.current);
      }
    };
  }, []);

  useEffect(() => {
    const abortController = new AbortController();
    fetchModules({ signal: abortController.signal }).catch((error: { name: string }) => {
      if (error.name !== 'CanceledError') {
        messageApi.error('加载模块数据失败');
      }
    });
    fetchDataSources().catch(() => {
      messageApi.error('加载数据源失败');
    });
    return () => abortController.abort();
  }, []);

  const fetchModules = async (config?: any) => {
    setLoading(true);
    try {
      const modules = await getModules(config);
      const transformedModules = transformModuleData(modules);
      setData(transformedModules);
      originalDataRef.current = transformedModules;
    } catch (error) {
      if (error instanceof Error) {
        if (error.name !== 'CanceledError') {
          messageApi.error('加载模块数据失败');
        }
      }
    } finally {
      setLoading(false);
    }
  };

  const fetchDataSources = async () => {
    try {
      const sources = await getDataSources();
      setDataSources(sources);
    } catch (error) {
      console.error('加载数据源失败:', error);
    }
  };

  const handleReload = () => {
    fetchModules().catch(() => {
      messageApi.error('加载模块数据失败');
    });
  };

  const handleSearch = (value: string) => {
    setSearchText(value);

    if (searchTimeoutRef.current !== null) {
      window.clearTimeout(searchTimeoutRef.current);
    }

    searchTimeoutRef.current = window.setTimeout(() => {
      if (!value.trim()) {
        if (originalDataRef.current.length > 0) {
          setData(originalDataRef.current);
        } else {
          fetchModules().catch(() => {
            messageApi.error('加载模块数据失败');
          });
        }
        return;
      }

      const cleanValue = value.replace(/['"]/g, '');
      const searchTerms = cleanValue
        .toLowerCase()
        .split(/\s+/)
        .filter((term) => term);

      const matchesSearchTerms = (module: ModuleData) => {
        if (searchTerms.length === 0) return true;
        const moduleName = module.name?.toLowerCase() || '';
        const datasourceName = module.datasourceName?.toLowerCase() || '';
        const tableName = module.tableName?.toLowerCase() || '';
        return searchTerms.every((term) => `${moduleName} ${datasourceName} ${tableName}`.includes(term));
      };

      const currentDataFiltered = data.filter(matchesSearchTerms);
      if (currentDataFiltered.length > 0) {
        setData(currentDataFiltered);
        return;
      }

      const originalDataFiltered = originalDataRef.current.filter(matchesSearchTerms);
      setData(originalDataFiltered);
    }, 300);
  };

  const handleAddEdit = (record?: ModuleData) => {
    setSelectedRecord(record ?? null);
    form.resetFields();
    if (record) {
      form.setFieldsValue({
        ...record,
      });
    }
    setIsModalVisible(true);
  };

  const handleDelete = (record: ModuleData) => {
    setDeleteRecord(record);
    setDeleteDorisTable(false);
    setDeleteModalVisible(true);
  };

  const handleDeleteConfirm = async () => {
    if (!deleteRecord) return;

    await deleteModule(Number(deleteRecord.key), deleteDorisTable);
    setData(data.filter((item) => item.key !== deleteRecord.key));
    messageApi.success('模块已删除');
    setDeleteModalVisible(false);
    setDeleteRecord(null);
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();

      if (selectedRecord) {
        // 更新模块
        await updateModule({
          id: Number(selectedRecord.key),
          ...values,
        } as UpdateModuleParams);
        messageApi.success('模块信息已更新');
      } else {
        // 创建模块
        await createModule(values as CreateModuleParams);
        messageApi.success('模块已添加');
      }

      setIsModalVisible(false);
      await fetchModules();
    } catch (error) {
      messageApi.error('操作失败');
      console.error('操作失败:', error);
    }
  };

  const handleViewDetail = async (record: ModuleData) => {
    try {
      setLoading(true);
      const detail = await getModuleDetail(Number(record.key));
      setModuleDetail({
        ...detail,
        users: detail.users?.map((user) => ({
          ...user,
          role: (user as any).role || 'USER',
        })),
      });
      setIsDetailVisible(true);
    } catch {
      messageApi.error('获取模块详情失败');
    } finally {
      setLoading(false);
    }
  };

  const handleExecuteDorisSql = async (record: ModuleData) => {
    setCurrentRecord(record);
    // 检查dorisSql是否已存在，决定是否为只读模式
    const hasExistingSql = record.dorisSql?.trim();
    setIsReadOnlyMode(!!hasExistingSql);
    // 如果dorisSql不为null且不为空，则设置为已有的SQL（只读模式）
    // 如果dorisSql为null或空，则设置为空字符串（可编辑模式）
    setExecuteSql(hasExistingSql ? record.dorisSql : '');
    setExecuteModalVisible(true);
  };

  const handleApplyTemplate = () => {
    if (currentRecord) {
      const templateValue = DORIS_TEMPLATE.replace('${tableName}', currentRecord.tableName || '');
      setExecuteSql(templateValue);
    }
  };

  const handleExecuteSqlChange = (value: string) => {
    setExecuteSql(value);
  };

  const handleExecuteConfirm = async () => {
    if (!currentRecord || !executeSql.trim()) {
      messageApi.warning('请输入有效的SQL语句');
      return;
    }

    try {
      setExecuting(true);
      await executeDorisSql(Number(currentRecord.key), executeSql.trim());
      messageApi.success('SQL执行成功');
      setExecuteModalVisible(false);
    } finally {
      setExecuting(false);
    }
  };

  const columns: ColumnsType<ModuleData> = [
    {
      title: '模块名称',
      dataIndex: 'name',
      key: 'name',
      width: 150,
      render: (text: string) => <Tag icon={<DatabaseOutlined />}>{text}</Tag>,
    },
    {
      title: '数据源',
      dataIndex: 'datasourceName',
      key: 'datasourceName',
      width: 150,
    },
    {
      title: '表名',
      dataIndex: 'tableName',
      key: 'tableName',
      width: 150,
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 160,
      render: (createTime: string) => dayjs(createTime).format('YYYY-MM-DD HH:mm:ss'),
    },
    {
      title: '创建人',
      dataIndex: 'createUserName',
      key: 'createUserName',
      width: 120,
      render: (createUserName: string, record: ModuleData) => createUserName || record.createUser,
    },
    {
      title: '更新时间',
      dataIndex: 'updateTime',
      key: 'updateTime',
      width: 160,
      render: (updateTime: string) => dayjs(updateTime).format('YYYY-MM-DD HH:mm:ss'),
    },
    {
      title: '更新人',
      dataIndex: 'updateUserName',
      key: 'updateUserName',
      width: 120,
      render: (updateUserName: string, record: ModuleData) => updateUserName || record.updateUser,
    },
    {
      title: '操作',
      key: 'action',
      fixed: 'right' as const,
      width: 300,
      render: (_: any, record: ModuleData) => (
        <Space size={0}>
          <Button type="link" onClick={() => handleViewDetail(record)} style={{ padding: '0 8px' }}>
            详情
          </Button>
          <Button type="link" onClick={() => handleAddEdit(record)} style={{ padding: '0 8px' }}>
            编辑
          </Button>
          <Button type="link" onClick={() => handleExecuteDorisSql(record)} style={{ padding: '0 8px' }}>
            {record.dorisSql?.trim() ? '查看SQL' : '执行SQL'}
          </Button>
          <Button type="link" danger style={{ padding: '0 8px' }} onClick={() => handleDelete(record)}>
            删除
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <div className={styles.container}>
      {contextHolder}
      <div className={styles.header}>
        <Breadcrumb
          items={[
            {
              title: (
                <Link to="/">
                  <HomeOutlined />
                </Link>
              ),
            },
            { title: '模块管理', key: 'system/module' },
          ]}
        />
        <Space>
          <Input
            placeholder="搜索模块/数据源/表名"
            value={searchText}
            onChange={(e) => handleSearch(e.target.value)}
            style={{ width: 240 }}
            allowClear
            suffix={<SearchOutlined />}
          />
          <Button type="primary" icon={<PlusOutlined />} onClick={() => handleAddEdit()}>
            添加模块
          </Button>
          <Button icon={<ReloadOutlined />} onClick={handleReload} loading={loading}>
            刷新
          </Button>
        </Space>
      </div>

      <div className={styles.antTable}>
        <Table
          columns={columns}
          dataSource={data}
          rowKey="key"
          loading={loading}
          scroll={{ x: 1300 }}
          size="small"
          bordered
        />
      </div>

      <Modal
        title={selectedRecord ? '编辑模块' : '添加模块'}
        open={isModalVisible}
        onOk={handleSubmit}
        onCancel={() => setIsModalVisible(false)}
        width={600}
        maskClosable={false}
      >
        <Form form={form} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="name" label="模块名称" rules={[{ required: true, message: '请输入模块名称' }]}>
                <Input placeholder="请输入模块名称" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="datasourceId" label="数据源" rules={[{ required: true, message: '请选择数据源' }]}>
                <Select
                  placeholder="请选择数据源"
                  options={dataSources.map((ds) => ({
                    value: ds.id,
                    label: ds.name,
                  }))}
                />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="tableName" label="表名" rules={[{ required: true, message: '请输入表名' }]}>
                <Input placeholder="请输入表名" />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>

      <Modal
        title="模块详情"
        open={isDetailVisible}
        onCancel={() => setIsDetailVisible(false)}
        footer={null}
        width={800}
      >
        {moduleDetail && (
          <Descriptions bordered column={2}>
            <Descriptions.Item label="模块名称">{moduleDetail.name}</Descriptions.Item>
            <Descriptions.Item label="数据源名称">{moduleDetail.datasourceName}</Descriptions.Item>
            <Descriptions.Item label="表名">{moduleDetail.tableName}</Descriptions.Item>
            <Descriptions.Item label="创建时间">
              {dayjs(moduleDetail.createTime).format('YYYY-MM-DD HH:mm:ss')}
            </Descriptions.Item>
            <Descriptions.Item label="更新时间">
              {dayjs(moduleDetail.updateTime).format('YYYY-MM-DD HH:mm:ss')}
            </Descriptions.Item>
            <Descriptions.Item label="创建人">
              {moduleDetail.createUserName || moduleDetail.createUser}
            </Descriptions.Item>
            <Descriptions.Item label="更新人">
              {moduleDetail.updateUserName || moduleDetail.updateUser}
            </Descriptions.Item>
            <Descriptions.Item label="Doris SQL" span={2}>
              <pre style={{ margin: 0 }}>{moduleDetail.dorisSql}</pre>
            </Descriptions.Item>
          </Descriptions>
        )}
      </Modal>

      {/* 删除确认模态框 */}
      <Modal
        title="删除模块"
        open={deleteModalVisible}
        onOk={handleDeleteConfirm}
        onCancel={() => setDeleteModalVisible(false)}
        okText="确定删除"
        cancelText="取消"
        okButtonProps={{ danger: true }}
      >
        <div>
          <p>
            确定要删除模块 <strong>{deleteRecord?.name}</strong> 吗？
          </p>
          <Radio.Group value={deleteDorisTable} onChange={(e) => setDeleteDorisTable(e.target.value)}>
            <Radio value={false}>仅删除模块，保留Doris表数据</Radio>
            <Radio value={true}>同时删除底层Doris表数据</Radio>
          </Radio.Group>
          <div style={{ marginTop: 16, color: '#ff4d4f' }}>
            <strong>警告：</strong>此操作不可撤销，请谨慎选择！
          </div>
        </div>
      </Modal>

      <ExecuteConfirmationModal
        visible={executeModalVisible}
        sql={executeSql}
        onConfirm={handleExecuteConfirm}
        onCancel={() => setExecuteModalVisible(false)}
        onSqlChange={handleExecuteSqlChange}
        loading={executing}
        readonly={isReadOnlyMode}
        title={
          <div style={{ display: 'flex', alignItems: 'center' }}>
            <span>
              {isReadOnlyMode ? '查看' : '执行'}Doris SQL - <strong>{currentRecord?.name}</strong>
            </span>
            {!isReadOnlyMode && (
              <Tooltip title="应用模板的Doris SQL语句">
                <Button
                  type="text"
                  icon={<DatabaseOutlined />}
                  style={{ marginLeft: 8 }}
                  onClick={handleApplyTemplate}
                />
              </Tooltip>
            )}
          </div>
        }
      />
    </div>
  );
};
export default ModuleManagementPage;
