import { useEffect, useState } from 'react';
import { getMachines, createMachine, deleteMachine, updateMachine, testMachineConnection } from '../../api/machine';
import type { Machine, CreateMachineParams } from '../../types/machineTypes';
import type { TableColumnsType, TablePaginationConfig } from 'antd';
import { Breadcrumb, Button, Form, Input, InputNumber, Modal, Row, Col, Table } from 'antd';
import { PlusOutlined, HomeOutlined } from '@ant-design/icons';
import { Link } from 'react-router-dom';
import dayjs from 'dayjs';
import { useErrorContext, ErrorType } from '../../providers/ErrorProvider';
import styles from './MachineManagementPage.module.less';

const MachineManagementPage = () => {
  const [machines, setMachines] = useState<Machine[]>([]);
  const [loading, setLoading] = useState(false);
  const [createModalVisible, setCreateModalVisible] = useState(false);
  const [editModalVisible, setEditModalVisible] = useState(false);
  const [deleteConfirmVisible, setDeleteConfirmVisible] = useState(false);
  const [deletingId, setDeletingId] = useState<string | number | null>(null);
  const [editingMachine, setEditingMachine] = useState<Machine | null>(null);
  const [testingConnection, setTestingConnection] = useState(false);
  const [form] = Form.useForm<CreateMachineParams>();
  const { handleError, showSuccess } = useErrorContext();
  const [pagination, setPagination] = useState<TablePaginationConfig>({
    current: 1,
    pageSize: 10,
    showSizeChanger: true,
    showQuickJumper: true,
    showTotal: (total) => `共 ${total} 条`,
    pageSizeOptions: ['10', '20', '50', '100'],
  });

  useEffect(() => {
    fetchMachines();
  }, []);

  const fetchMachines = async () => {
    setLoading(true);
    try {
      const res = await getMachines();
      setMachines(res);
    } catch (error) {
      handleError(error instanceof Error ? error : new Error('获取机器列表失败'), {
        type: ErrorType.BUSINESS,
        showType: 'notification',
      });
    } finally {
      setLoading(false);
    }
  };

  // 处理表格变更（分页、排序、筛选）
  const handleTableChange = (pagination: TablePaginationConfig) => {
    setPagination((prev) => ({
      ...prev,
      current: pagination.current,
      pageSize: pagination.pageSize,
    }));
  };

  const handleCreate = async (values: CreateMachineParams) => {
    try {
      setTestingConnection(true);
      const testResult = await testMachineConnection(values);
      if (!testResult) {
        handleError('连接测试失败，请检查配置', {
          type: ErrorType.VALIDATION,
          showType: 'message',
        });
        return;
      }

      await createMachine(values);
      showSuccess('机器创建成功');
      setCreateModalVisible(false);
      form.resetFields();
      fetchMachines();
    } catch (error) {
      handleError(error instanceof Error ? error : new Error('创建机器失败'), {
        type: ErrorType.BUSINESS,
        showType: 'message',
      });
    } finally {
      setTestingConnection(false);
    }
  };

  const handleEdit = async (values: CreateMachineParams) => {
    if (!editingMachine) return;
    try {
      setLoading(true);
      const testResult = await testMachineConnection(values);
      if (!testResult) {
        handleError('连接测试失败，请检查配置', {
          type: ErrorType.VALIDATION,
          showType: 'message',
        });
        return;
      }

      await updateMachine({
        ...values,
        id: editingMachine.id,
      });
      showSuccess('机器更新成功');
      setEditModalVisible(false);
      form.resetFields();
      fetchMachines();
    } catch (error) {
      handleError(error instanceof Error ? error : new Error('更新机器失败'), {
        type: ErrorType.BUSINESS,
        showType: 'message',
      });
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async () => {
    if (!deletingId) return;

    try {
      await deleteMachine(deletingId);
      showSuccess('删除成功');
      setDeleteConfirmVisible(false);
      fetchMachines();
    } catch (error) {
      handleError(error instanceof Error ? error : new Error('删除机器失败'), {
        type: ErrorType.BUSINESS,
        showType: 'message',
      });
    }
  };

  const handleTestConnection = async () => {
    try {
      const values = await form.validateFields();
      setTestingConnection(true);
      const success = await testMachineConnection(values);
      if (success) {
        showSuccess('连接测试成功');
      } else {
        handleError('连接测试失败', {
          type: ErrorType.VALIDATION,
          showType: 'message',
        });
      }
    } catch (error) {
      if (error && typeof error === 'object' && 'errorFields' in error) {
        // 表单验证错误
        handleError('请完善表单信息', {
          type: ErrorType.VALIDATION,
          showType: 'message',
        });
      } else {
        // 其他错误
        handleError(error instanceof Error ? error : new Error('连接测试失败'), {
          type: ErrorType.NETWORK,
          showType: 'message',
        });
      }
    } finally {
      setTestingConnection(false);
    }
  };

  const columns: TableColumnsType<Machine> = [
    { title: '名称', dataIndex: 'name', key: 'name' },
    { title: 'IP地址', dataIndex: 'ip', key: 'ip' },
    { title: '端口', dataIndex: 'port', key: 'port' },
    { title: '用户名', dataIndex: 'username', key: 'username' },
    {
      title: '更新时间',
      dataIndex: 'updateTime',
      key: 'updateTime',
      render: (text) => (text ? dayjs(text).format('YYYY-MM-DD HH:mm:ss') : '-'),
    },
    {
      title: '更新人',
      dataIndex: 'updateUser',
      key: 'updateUser',
      render: (text) => text || '-',
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <div className={styles.actions}>
          <Button
            type="link"
            size="small"
            onClick={() => {
              setEditingMachine(record);
              form.setFieldsValue(record);
              setEditModalVisible(true);
            }}
            style={{ padding: '0 8px' }}
          >
            编辑
          </Button>
          <Button
            type="link"
            size="small"
            danger
            onClick={() => {
              setDeletingId(record.id);
              setDeleteConfirmVisible(true);
            }}
            style={{ padding: '0 8px' }}
          >
            删除
          </Button>
        </div>
      ),
    },
  ];

  return (
    <div className={styles.container}>
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
            { title: '服务器管理' },
          ]}
        />
        <div className={styles.actions}>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateModalVisible(true)}>
            新增机器
          </Button>
        </div>
      </div>

      <div className={styles.tableContainer}>
        <Table
          dataSource={machines}
          columns={columns}
          loading={loading}
          size="small"
          rowKey="id"
          pagination={{
            ...pagination,
            total: machines.length,
          }}
          onChange={handleTableChange}
          bordered
        />
      </div>

      <Modal
        title="新增机器"
        open={createModalVisible}
        onCancel={() => setCreateModalVisible(false)}
        footer={[
          <Button key="test" loading={testingConnection} onClick={handleTestConnection}>
            测试连接
          </Button>,
          <Button key="submit" type="primary" loading={loading} onClick={() => form.submit()}>
            确定
          </Button>,
        ]}
        maskClosable={false}
      >
        <Form form={form} layout="vertical" onFinish={handleCreate}>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="name" label="名称" rules={[{ required: true, message: '请输入机器名称' }]}>
                <Input placeholder="测试服务器" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="ip" label="IP地址" rules={[{ required: true, message: '请输入IP地址' }]}>
                <Input placeholder="192.168.1.100" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="port" label="端口" rules={[{ required: true, message: '请输入端口号' }]}>
                <InputNumber min={1} max={65535} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="username" label="用户名" rules={[{ required: true, message: '请输入用户名' }]}>
                <Input placeholder="root" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={24}>
              <Form.Item name="password" label="密码">
                <Input.Password placeholder="可选" />
              </Form.Item>
            </Col>
            <Col span={24}>
              <Form.Item name="sshKey" label="SSH密钥">
                <Input.TextArea placeholder="可选" rows={4} />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>

      <Modal
        title="编辑机器"
        open={editModalVisible}
        onCancel={() => {
          setEditModalVisible(false);
          form.resetFields();
        }}
        footer={[
          <Button key="test" loading={testingConnection} onClick={handleTestConnection}>
            测试连接
          </Button>,
          <Button key="submit" type="primary" loading={loading} onClick={() => form.submit()}>
            确定
          </Button>,
        ]}
        maskClosable={false}
      >
        <Form form={form} layout="vertical" onFinish={handleEdit}>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="name" label="名称" rules={[{ required: true, message: '请输入机器名称' }]}>
                <Input placeholder="测试服务器" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="ip" label="IP地址" rules={[{ required: true, message: '请输入IP地址' }]}>
                <Input placeholder="192.168.1.100" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="port" label="端口" rules={[{ required: true, message: '请输入端口号' }]}>
                <InputNumber min={1} max={65535} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="username" label="用户名" rules={[{ required: true, message: '请输入用户名' }]}>
                <Input placeholder="root" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={24}>
              <Form.Item name="password" label="密码">
                <Input.Password placeholder="可选" />
              </Form.Item>
            </Col>
            <Col span={24}>
              <Form.Item name="sshKey" label="SSH密钥">
                <Input.TextArea placeholder="可选" rows={4} />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>

      <Modal
        title="确认删除"
        open={deleteConfirmVisible}
        onOk={handleDelete}
        onCancel={() => setDeleteConfirmVisible(false)}
        confirmLoading={loading}
      >
        <p>确定要删除这台机器吗？</p>
      </Modal>
    </div>
  );
};

import withSystemAccess from '@/utils/withSystemAccess';
export default withSystemAccess(MachineManagementPage);
