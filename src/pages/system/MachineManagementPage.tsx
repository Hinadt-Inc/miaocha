import { useEffect, useState } from 'react'
import { getMachines, createMachine, deleteMachine, updateMachine, testMachineConnection } from '../../api/machine'
import type { Machine, CreateMachineParams } from '../../types/machineTypes'
import { SimpleTable } from '../../components/common/SimpleTable'
import type { TableColumnsType } from 'antd'
import { Breadcrumb, Button, Form, Input, InputNumber, Modal, message, Tooltip } from 'antd'
import { EditOutlined, DeleteOutlined, ThunderboltOutlined } from '@ant-design/icons'
import { Link } from 'react-router-dom'
import './MachineManagementPage.less'

const MachineManagementPage = () => {
  const [machines, setMachines] = useState<Machine[]>([])
  const [loading, setLoading] = useState(false)
  const [createModalVisible, setCreateModalVisible] = useState(false)
  const [editModalVisible, setEditModalVisible] = useState(false)
  const [deleteConfirmVisible, setDeleteConfirmVisible] = useState(false)
  const [deletingId, setDeletingId] = useState<string | number | null>(null)
  const [editingMachine, setEditingMachine] = useState<Machine | null>(null)
  const [testingConnectionId, setTestingConnectionId] = useState<string | number | null>(null)
  const [form] = Form.useForm<CreateMachineParams>()
  const [messageApi, contextHolder] = message.useMessage();

  useEffect(() => {
    fetchMachines()
  }, [])

  const fetchMachines = async () => {
    setLoading(true)
    try {
      const res = await getMachines()
      setMachines(res)
    } finally {
      setLoading(false)
    }
  }

  const handleCreate = async (values: CreateMachineParams) => {
    try {
      await createMachine(values)
      messageApi.success('机器创建成功')
      setCreateModalVisible(false)
      form.resetFields()
      fetchMachines()
    } catch {
      messageApi.error('机器创建失败')
    }
  }

  const handleEdit = async (values: CreateMachineParams) => {
    if (!editingMachine) return
    
    try {
      await updateMachine({
        ...values,
        id: editingMachine.id
      })
      messageApi.success('机器更新成功')
      setEditModalVisible(false)
      form.resetFields()
      fetchMachines()
    } catch {
      messageApi.error('机器更新失败')
    }
  }

  const handleDelete = async () => {
    if (!deletingId) return
    
    try {
      await deleteMachine(deletingId)
      messageApi.success('删除成功')
      setDeleteConfirmVisible(false)
      fetchMachines()
    } catch {
      messageApi.error('删除失败')
    }
  }

  const handleTestConnection = async (id: string | number) => {
    setTestingConnectionId(id)
    try {
      const success = await testMachineConnection(id)
      if (success) {
        message.success('连接测试成功')
      } else {
        message.error('连接测试失败')
      }
    } catch {
      message.error('连接测试失败')
    } finally {
      setTestingConnectionId(null)
    }
  }

  const columns: TableColumnsType<Machine> = [
    { title: '名称', dataIndex: 'name', key: 'name' },
    { title: 'IP地址', dataIndex: 'ip', key: 'ip' },
    { title: '端口', dataIndex: 'port', key: 'port' },
    { title: '用户名', dataIndex: 'username', key: 'username' },
    { title: '创建时间', dataIndex: 'createTime', key: 'createTime' },
    { title: '更新时间', dataIndex: 'updateTime', key: 'updateTime' },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <div className="actions">
          <Tooltip title="测试连接">
            <Button 
              type="text"
              icon={<ThunderboltOutlined />}
              loading={testingConnectionId === record.id}
              onClick={() => handleTestConnection(record.id)}
              disabled={!!testingConnectionId}
            />
          </Tooltip>
          <Tooltip title="编辑">
            <Button 
              type="text"
              icon={<EditOutlined />}
              onClick={() => {
                setEditingMachine(record)
                form.setFieldsValue(record)
                setEditModalVisible(true)
              }}
            />
          </Tooltip>
          <Tooltip title="删除">
            <Button 
              type="text"
              danger
              icon={<DeleteOutlined />}
              onClick={() => {
                setDeletingId(record.id)
                setDeleteConfirmVisible(true)
              }}
            />
          </Tooltip>
        </div>
      )
    }
  ]

  return (
    <>
    {contextHolder}
    <div className="machine-management-page">
      <div className="header">
        <Breadcrumb
          items={[
            { title: <Link to="/system">系统设置</Link> },
            { title: '机器管理' }
          ]}
        />
        <div className="actions">
          <Button 
            type="primary" 
            onClick={() => setCreateModalVisible(true)}
          >
            新增机器
          </Button>
        </div>
      </div>

      <div className="table-container">
        <SimpleTable
          dataSource={machines}
          columns={columns}
          loading={loading}
          rowKey="id"
        />
      </div>

      <Modal
        title="新增机器"
        open={createModalVisible}
        onCancel={() => setCreateModalVisible(false)}
        onOk={() => form.submit()}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleCreate}
        >
          <Form.Item
            name="name"
            label="名称"
            rules={[{ required: true, message: '请输入机器名称' }]}
          >
            <Input placeholder="测试服务器" />
          </Form.Item>
          <Form.Item
            name="ip"
            label="IP地址"
            rules={[{ required: true, message: '请输入IP地址' }]}
          >
            <Input placeholder="192.168.1.100" />
          </Form.Item>
          <Form.Item
            name="port"
            label="端口"
            rules={[{ required: true, message: '请输入端口号' }]}
          >
            <InputNumber min={1} max={65535} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item
            name="username"
            label="用户名"
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input placeholder="root" />
          </Form.Item>
          <Form.Item
            name="password"
            label="密码"
          >
            <Input.Password placeholder="可选" />
          </Form.Item>
          <Form.Item
            name="sshKey"
            label="SSH密钥"
          >
            <Input.TextArea placeholder="可选" rows={4} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="编辑机器"
        open={editModalVisible}
        onCancel={() => {
          setEditModalVisible(false)
          form.resetFields()
        }}
        onOk={() => form.submit()}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleEdit}
        >
          <Form.Item
            name="name"
            label="名称"
            rules={[{ required: true, message: '请输入机器名称' }]}
          >
            <Input placeholder="测试服务器" />
          </Form.Item>
          <Form.Item
            name="ip"
            label="IP地址"
            rules={[{ required: true, message: '请输入IP地址' }]}
          >
            <Input placeholder="192.168.1.100" />
          </Form.Item>
          <Form.Item
            name="port"
            label="端口"
            rules={[{ required: true, message: '请输入端口号' }]}
          >
            <InputNumber min={1} max={65535} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item
            name="username"
            label="用户名"
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input placeholder="root" />
          </Form.Item>
          <Form.Item
            name="password"
            label="密码"
          >
            <Input.Password placeholder="可选" />
          </Form.Item>
          <Form.Item
            name="sshKey"
            label="SSH密钥"
          >
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
    
  )
}

export default MachineManagementPage
