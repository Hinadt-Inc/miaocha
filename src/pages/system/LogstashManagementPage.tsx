import { PlusOutlined } from '@ant-design/icons';
import { Button, message, Modal, Space, Table, Breadcrumb } from 'antd';
import './LogstashManagementPage.less';
import { Link } from 'react-router-dom';
import { useEffect, useState } from 'react';
import { 
  createLogstashProcess,
  deleteLogstashProcess,
  getLogstashProcesses,
  startLogstashProcess,
  stopLogstashProcess,
  updateLogstashProcess,
  getLogstashTaskStatus
} from '../../api/logstash';
import type { LogstashProcess } from '../../types/logstashTypes';
import LogstashEditModal from './components/LogstashEditModal';

export default function LogstashManagementPage() {
  const [data, setData] = useState<LogstashProcess[]>([]);
  const [loading, setLoading] = useState(false);
  const [editModalVisible, setEditModalVisible] = useState(false);
  const [currentProcess, setCurrentProcess] = useState<LogstashProcess | null>(null);

  const fetchData = async () => {
    setLoading(true);
    try {
      const res = await getLogstashProcesses();
      setData(res);
    } catch (err) {
      message.error('获取Logstash进程列表失败');
      console.error('获取Logstash进程列表失败:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleAdd = () => {
    setCurrentProcess(null);
    setEditModalVisible(true);
  };

  const handleEdit = (record: LogstashProcess) => {
    setCurrentProcess(record);
    setEditModalVisible(true);
  };

  const handleDelete = async (id: number) => {
    Modal.confirm({
      title: '确认删除',
      content: '确定要删除这个Logstash进程吗？',
      onOk: async () => {
        try {
          await deleteLogstashProcess(id);
          message.success('删除成功');
          await fetchData();
        } catch (err) {
          message.error('删除失败');
          console.error('删除Logstash进程失败:', err);
        }
      }
    });
  };

  const handleStart = async (id: number) => {
    try {
      await startLogstashProcess(id);
      message.success('启动成功');
      await fetchData();
      
      // 轮询任务状态
      const pollStatus = async () => {
        const status = await getLogstashTaskStatus(id);
        if (status.status === 'COMPLETED' || status.status === 'FAILED') {
          message.success(`任务${status.status === 'COMPLETED' ? '完成' : '失败'}`);
          await fetchData();
          return;
        }
        setTimeout(pollStatus, 2000);
      };
      pollStatus();
    } catch (err) {
      message.error('启动失败');
      console.error('启动Logstash进程失败:', err);
    }
  };

  const handleStop = async (id: number) => {
    try {
      await stopLogstashProcess(id);
      message.success('停止成功');
      await fetchData();
      
      // 轮询任务状态
      const pollStatus = async () => {
        const status = await getLogstashTaskStatus(id);
        if (status.status === 'COMPLETED' || status.status === 'FAILED') {
          message.success(`任务${status.status === 'COMPLETED' ? '完成' : '失败'}`);
          await fetchData();
          return;
        }
        setTimeout(pollStatus, 2000);
      };
      pollStatus();
    } catch (err) {
      message.error('停止失败');
      console.error('停止Logstash进程失败:', err);
    }
  };

  const columns = [
    {
      title: '名称',
      dataIndex: 'name',
      key: 'name'
    },
    {
      title: '模块',
      dataIndex: 'module',
      key: 'module'
    },
    {
      title: '状态',
      dataIndex: 'stateDescription',
      key: 'state'
    },
    {
      title: '操作',
      key: 'action',
      render: (_: unknown, record: LogstashProcess) => (
        <Space size="middle">
          <Button type="link" onClick={() => handleEdit(record)}>编辑</Button>
          <Button 
            type="link" 
            onClick={() => record.state === 'RUNNING' ? handleStop(record.id) : handleStart(record.id)}
          >
            {record.state === 'RUNNING' ? '停止' : '启动'}
          </Button>
          <Button type="link" danger onClick={() => handleDelete(record.id)}>删除</Button>
        </Space>
      )
    }
  ];

  return (
    <div className="logstash-management-page">
      <div className="header">
        <Breadcrumb>
          <Breadcrumb.Item>
            <Link to="/">首页</Link>
          </Breadcrumb.Item>
          <Breadcrumb.Item>
            <Link to="/system">系统设置</Link>
          </Breadcrumb.Item>
          <Breadcrumb.Item>Logstash管理</Breadcrumb.Item>
        </Breadcrumb>
        <div className="actions">
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
            新增Logstash进程
          </Button>
        </div>
      </div>
      
      <div className="table-container">
        <Table 
          columns={columns}
          dataSource={data}
          rowKey="id"
          loading={loading}
          bordered
        />
        <LogstashEditModal
        visible={editModalVisible}
        onCancel={() => setEditModalVisible(false)}
        onOk={async (values: Partial<LogstashProcess>) => {
          try {
            if (currentProcess) {
              await updateLogstashProcess(currentProcess.id, values);
              message.success('更新成功');
            } else {
              await createLogstashProcess(values);
              message.success('创建成功');
            }
            setEditModalVisible(false);
            await fetchData();
          } catch (err) {
            message.error(currentProcess ? '更新失败' : '创建失败');
            console.error('操作Logstash进程失败:', err);
          }
        }}
        initialValues={currentProcess}
        />
      </div>
    </div>
  );
}
