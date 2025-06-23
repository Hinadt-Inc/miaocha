import { useEffect, useState } from 'react';
import { getMachines, createMachine, deleteMachine, updateMachine, testMachineConnection } from '../../api/machine';
import type { Machine, CreateMachineParams } from '../../types/machineTypes';
import { SimpleTable } from '../../components/common/SimpleTable';
import type { TableColumnsType } from 'antd';
import { Breadcrumb, Button, Form, Input, InputNumber, Modal, message, Row, Col } from 'antd';
import { PlusOutlined, HomeOutlined } from '@ant-design/icons';
import { Link } from 'react-router-dom';
import dayjs from 'dayjs';
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
  const [messageApi, contextHolder] = message.useMessage();

  useEffect(() => {
    fetchMachines();
  }, []);

  const fetchMachines = async () => {
    setLoading(true);
    try {
      const res = await getMachines();
      setMachines(res);
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = async (values: CreateMachineParams) => {
    setTestingConnection(true);
    const testResult = await testMachineConnection(values);
    if (!testResult) {
      messageApi.error('连接测试失败，请检查配置');
      return;
    }

    await createMachine(values);
    messageApi.success('机器创建成功');
    setCreateModalVisible(false);
    form.resetFields();
    fetchMachines();
    setTestingConnection(false);
  };

  const handleEdit = async (values: CreateMachineParams) => {
    if (!editingMachine) return;
    try {
      setLoading(true);
      const testResult = await testMachineConnection(values);
      if (!testResult) {
        messageApi.error('连接测试失败，请检查配置');
        return;
      }

      await updateMachine({
        ...values,
        id: editingMachine.id,
      });
      messageApi.success('机器更新成功');
      setEditModalVisible(false);
      form.resetFields();
      fetchMachines();
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async () => {
    if (!deletingId) return;

    await deleteMachine(deletingId);
    messageApi.success('删除成功');
    setDeleteConfirmVisible(false);
    fetchMachines();
  };

  const handleTestConnection = async () => {
    try {
      const values = await form.validateFields();
      setTestingConnection(true);
      const success = await testMachineConnection(values);
      if (success) {
        messageApi.success('连接测试成功');
      } else {
        messageApi.error('连接测试失败');
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
    <>
      {contextHolder}
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
          <SimpleTable dataSource={machines} columns={columns} loading={loading} size="small" rowKey="id" />
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
    </>
  );
};

import withSystemAccess from '@/utils/withSystemAccess';
export default withSystemAccess(MachineManagementPage);
