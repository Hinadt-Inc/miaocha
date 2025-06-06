import { useEffect, useState } from 'react';
import { getMachines, createMachine, deleteMachine, updateMachine, testMachineConnection } from '../../api/machine';
import type { Machine, CreateMachineParams } from '../../types/machineTypes';
import { SimpleTable } from '../../components/common/SimpleTable';
import type { TableColumnsType } from 'antd';
import { Breadcrumb, Button, Form, Input, InputNumber, Modal, message, Card } from 'antd';
import { EditOutlined, DeleteOutlined, ThunderboltOutlined, PlusOutlined, HomeOutlined } from '@ant-design/icons';
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
  const [testingConnectionId, setTestingConnectionId] = useState<string | number | null>(null);
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
    try {
      await createMachine(values);
      messageApi.success('机器创建成功');
      setCreateModalVisible(false);
      form.resetFields();
      fetchMachines();
    } catch {
      messageApi.error('机器创建失败');
    }
  };

  const handleEdit = async (values: CreateMachineParams) => {
    if (!editingMachine) return;

    try {
      await updateMachine({
        ...values,
        id: editingMachine.id,
      });
      messageApi.success('机器更新成功');
      setEditModalVisible(false);
      form.resetFields();
      fetchMachines();
    } catch {
      messageApi.error('机器更新失败');
    }
  };

  const handleDelete = async () => {
    if (!deletingId) return;

    try {
      await deleteMachine(deletingId);
      messageApi.success('删除成功');
      setDeleteConfirmVisible(false);
      fetchMachines();
    } catch {
      messageApi.error('删除失败');
    }
  };

  const handleTestConnection = async (id: string | number) => {
    setTestingConnectionId(id);
    try {
      const success = await testMachineConnection(id);
      if (success) {
        messageApi.success('连接测试成功');
      } else {
        messageApi.error('连接测试失败');
      }
    } catch {
      messageApi.error('连接测试失败');
    } finally {
      setTestingConnectionId(null);
    }
  };

  const columns: TableColumnsType<Machine> = [
    { title: '名称', dataIndex: 'name', key: 'name' },
    { title: 'IP地址', dataIndex: 'ip', key: 'ip' },
    { title: '端口', dataIndex: 'port', key: 'port' },
    { title: '用户名', dataIndex: 'username', key: 'username' },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      render: (text) => (text ? dayjs(text).format('YYYY-MM-DD HH:mm:ss') : '-'),
    },
    {
      title: '更新时间',
      dataIndex: 'updateTime',
      key: 'updateTime',
      render: (text) => (text ? dayjs(text).format('YYYY-MM-DD HH:mm:ss') : '-'),
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <div className={styles.actions}>
          <Button
            type="link"
            size="small"
            loading={testingConnectionId === record.id}
            onClick={() => handleTestConnection(record.id)}
            disabled={!!testingConnectionId}
            style={{ padding: '0 8px' }}
          >
            测试连接
          </Button>
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
              { title: '机器管理' },
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
          onOk={() => form.submit()}
        >
          <Form form={form} layout="vertical" onFinish={handleCreate}>
            <Form.Item name="name" label="名称" rules={[{ required: true, message: '请输入机器名称' }]}>
              <Input placeholder="测试服务器" />
            </Form.Item>
            <Form.Item name="ip" label="IP地址" rules={[{ required: true, message: '请输入IP地址' }]}>
              <Input placeholder="192.168.1.100" />
            </Form.Item>
            <Form.Item name="port" label="端口" rules={[{ required: true, message: '请输入端口号' }]}>
              <InputNumber min={1} max={65535} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="username" label="用户名" rules={[{ required: true, message: '请输入用户名' }]}>
              <Input placeholder="root" />
            </Form.Item>
            <Form.Item name="password" label="密码">
              <Input.Password placeholder="可选" />
            </Form.Item>
            <Form.Item name="sshKey" label="SSH密钥">
              <Input.TextArea placeholder="可选" rows={4} />
            </Form.Item>
          </Form>
        </Modal>

        <Modal
          title="编辑机器"
          open={editModalVisible}
          onCancel={() => {
            setEditModalVisible(false);
            form.resetFields();
          }}
          onOk={() => form.submit()}
        >
          <Form form={form} layout="vertical" onFinish={handleEdit}>
            <Form.Item name="name" label="名称" rules={[{ required: true, message: '请输入机器名称' }]}>
              <Input placeholder="测试服务器" />
            </Form.Item>
            <Form.Item name="ip" label="IP地址" rules={[{ required: true, message: '请输入IP地址' }]}>
              <Input placeholder="192.168.1.100" />
            </Form.Item>
            <Form.Item name="port" label="端口" rules={[{ required: true, message: '请输入端口号' }]}>
              <InputNumber min={1} max={65535} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="username" label="用户名" rules={[{ required: true, message: '请输入用户名' }]}>
              <Input placeholder="root" />
            </Form.Item>
            <Form.Item name="password" label="密码">
              <Input.Password placeholder="可选" />
            </Form.Item>
            <Form.Item name="sshKey" label="SSH密钥">
              <Input.TextArea placeholder="可选" rows={4} />
            </Form.Item>
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
