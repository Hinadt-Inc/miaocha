import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  PlayCircleOutlined,
  StopOutlined,
  HistoryOutlined,
  InfoCircleOutlined,
} from '@ant-design/icons';
import {
  Button,
  message,
  Popconfirm,
  Space,
  Table,
  Breadcrumb,
  Tooltip,
  Modal,
  Progress,
  Tag,
  Descriptions,
} from 'antd';
import './LogstashManagementPage.less';
import { Link } from 'react-router-dom';
import { useEffect } from 'react';
import {
  createLogstashProcess,
  deleteLogstashProcess,
  getLogstashProcesses,
  startLogstashProcess,
  stopLogstashProcess,
  updateLogstashProcess,
  getLogstashTaskStatus,
  getLogstashTaskSummaries,
  getTaskSteps,
} from '../../api/logstash';
import type {
  LogstashProcess,
  LogstashTaskSummary,
  TaskStepsResponse,
} from '../../types/logstashTypes';
import LogstashEditModal from './components/LogstashEditModal';
import { useRef, useState } from 'react';

export default function LogstashManagementPage() {
  const [data, setData] = useState<LogstashProcess[]>([]);
  const [loading, setLoading] = useState(false);
  const [editModalVisible, setEditModalVisible] = useState(false);
  const [currentProcess, setCurrentProcess] = useState<LogstashProcess | null>(null);
  const [taskSummaries, setTaskSummaries] = useState<LogstashTaskSummary[]>([]);
  const [summaryModalVisible, setSummaryModalVisible] = useState(false);
  const [taskSteps, setTaskSteps] = useState<TaskStepsResponse | null>(null);
  const [stepsModalVisible, setStepsModalVisible] = useState(false);

  const showTaskSteps = async (taskId: string) => {
    try {
      const steps = await getTaskSteps(taskId);
      setTaskSteps(steps);
      setStepsModalVisible(true);
    } catch (err) {
      message.error('获取任务步骤详情失败');
      console.error('获取任务步骤详情失败:', err);
    }
  };

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

  const showTaskSummaries = async (id: number) => {
    try {
      const summaries = await getLogstashTaskSummaries(id);
      setTaskSummaries(summaries);
      setSummaryModalVisible(true);
    } catch (err) {
      message.error('获取任务历史失败');
      console.error('获取任务历史失败:', err);
    }
  };

  useEffect(() => {
    fetchData().catch((err) => {
      message.error('组件加载失败');
      console.error('组件加载失败:', err);
    });
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
    try {
      await deleteLogstashProcess(id);
      message.success('删除成功');
      await fetchData();
    } catch (err) {
      message.error('删除失败');
      console.error('删除Logstash进程失败:', err);
    }
  };

  const handleStart = async (id: number) => {
    try {
      await startLogstashProcess(id);
      message.success('启动成功');
      await fetchData();

      const pollStatus = async () => {
        const status = await getLogstashTaskStatus(id);
        if (status.status === 'COMPLETED' || status.status === 'FAILED') {
          message.success(`任务${status.status === 'COMPLETED' ? '完成' : '失败'}`);
          await fetchData();
          return;
        }
        setTimeout(() => pollStatus, 2000);
      };
      pollStatus().catch(() => {
        message.error('获取任务状态失败');
      });
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

      const pollStatus = async () => {
        const status = await getLogstashTaskStatus(id);
        if (status.status === 'COMPLETED' || status.status === 'FAILED') {
          message.success(`任务${status.status === 'COMPLETED' ? '完成' : '失败'}`);
          await fetchData();
          return;
        }
        setTimeout(() => pollStatus, 2000);
      };
      pollStatus().catch(() => {
        message.error('获取任务状态失败');
      });
    } catch (err) {
      message.error('停止失败');
      console.error('停止Logstash进程失败:', err);
    }
  };

  const columns = [
    {
      title: '名称',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: '模块',
      dataIndex: 'module',
      key: 'module',
    },
    {
      title: '状态',
      dataIndex: 'stateDescription',
      key: 'state',
    },
    {
      title: '操作',
      key: 'action',
      render: (_: unknown, record: LogstashProcess) => (
        <Space size="middle">
          <Button type="link" icon={<EditOutlined />} onClick={() => handleEdit(record)}>
            编辑
          </Button>
          <Button
            type="link"
            icon={record.state === 'RUNNING' ? <StopOutlined /> : <PlayCircleOutlined />}
            onClick={() =>
              record.state === 'RUNNING' ? handleStop(record.id) : handleStart(record.id)
            }
          >
            {record.state === 'RUNNING' ? '停止' : '启动'}
          </Button>
          <Button
            type="link"
            icon={<HistoryOutlined />}
            onClick={() => {
              void showTaskSummaries(record.id);
            }}
          >
            历史
          </Button>
          <Popconfirm
            title="确认删除"
            description="确定要删除这个Logstash进程吗？"
            onConfirm={() => {
              void handleDelete(record.id);
            }}
            okText="确认"
            cancelText="取消"
            okType="danger"
          >
            <Button type="link" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div className="logstash-management-page">
      <div className="header">
        <Breadcrumb>
          <Breadcrumb.Item>
            <Link to="/">首页</Link>
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
          size="small"
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
        <Modal
          title="任务历史"
          open={summaryModalVisible}
          onCancel={() => setSummaryModalVisible(false)}
          footer={null}
          width={1000}
        >
          <Table
            dataSource={taskSummaries}
            rowKey="taskId"
            columns={[
              {
                title: '任务ID',
                dataIndex: 'taskId',
                key: 'taskId',
              },
              {
                title: '操作类型',
                dataIndex: 'operationType',
                key: 'operationType',
              },
              {
                title: '状态',
                dataIndex: 'status',
                key: 'status',
                render: (status: string) => (
                  <Tag
                    color={
                      status === 'COMPLETED'
                        ? 'success'
                        : status === 'FAILED'
                          ? 'error'
                          : 'processing'
                    }
                  >
                    {status}
                  </Tag>
                ),
              },
              {
                title: '进度',
                key: 'progress',
                width: 100,
                render: (_: unknown, record: LogstashTaskSummary) => (
                  <Progress
                    percent={record.progressPercentage}
                    status={
                      record.status === 'FAILED'
                        ? 'exception'
                        : record.status === 'COMPLETED'
                          ? 'success'
                          : 'active'
                    }
                  />
                ),
              },
              {
                title: '开始时间',
                dataIndex: 'startTime',
                key: 'startTime',
              },
              {
                title: '操作',
                key: 'action',
                render: (_: unknown, record: LogstashTaskSummary) => (
                  <Button
                    type="link"
                    icon={<InfoCircleOutlined />}
                    onClick={() => {
                      void showTaskSteps(record.taskId);
                    }}
                  >
                    详情
                  </Button>
                ),
              },
            ]}
          />
        </Modal>
        <Modal
          title="任务步骤详情"
          open={stepsModalVisible}
          onCancel={() => setStepsModalVisible(false)}
          footer={null}
          width={1200}
        >
          {taskSteps && (
            <div>
              <Descriptions bordered size="small" column={2} style={{ marginBottom: 16 }}>
                <Descriptions.Item label="任务ID">{taskSteps.taskId}</Descriptions.Item>
                <Descriptions.Item label="任务名称">{taskSteps.taskName}</Descriptions.Item>
                <Descriptions.Item label="任务状态">
                  <Tag
                    color={
                      taskSteps.taskStatus === 'COMPLETED'
                        ? 'success'
                        : taskSteps.taskStatus === 'FAILED'
                          ? 'error'
                          : 'processing'
                    }
                  >
                    {taskSteps.taskStatus}
                  </Tag>
                </Descriptions.Item>
              </Descriptions>

              <Table
                dataSource={taskSteps.steps}
                rowKey="stepId"
                columns={[
                  {
                    title: '步骤ID',
                    dataIndex: 'stepId',
                    key: 'stepId',
                  },
                  {
                    title: '步骤名称',
                    dataIndex: 'stepName',
                    key: 'stepName',
                  },
                  {
                    title: '完成',
                    dataIndex: 'completedCount',
                    key: 'completedCount',
                  },
                  {
                    title: '失败',
                    dataIndex: 'failedCount',
                    key: 'failedCount',
                  },
                  {
                    title: '待处理',
                    dataIndex: 'pendingCount',
                    key: 'pendingCount',
                  },
                  {
                    title: '运行中',
                    dataIndex: 'runningCount',
                    key: 'runningCount',
                  },
                  {
                    title: '跳过',
                    dataIndex: 'skippedCount',
                    key: 'skippedCount',
                  },
                  {
                    title: '总计',
                    dataIndex: 'totalCount',
                    key: 'totalCount',
                  },
                ]}
                expandable={{
                  expandedRowRender: (step) => (
                    <div style={{ padding: '8px 16px', background: '#fafafa' }}>
                      {step.machineSteps.map((machine) => (
                        <Descriptions
                          key={machine.machineId}
                          bordered
                          size="small"
                          column={2}
                          style={{ marginBottom: 16 }}
                        >
                          <Descriptions.Item label="机器ID">{machine.machineId}</Descriptions.Item>
                          <Descriptions.Item label="名称">{machine.machineName}</Descriptions.Item>
                          <Descriptions.Item label="IP">{machine.machineIp}</Descriptions.Item>
                          <Descriptions.Item label="状态">
                            <Tag
                              color={
                                machine.status === 'COMPLETED'
                                  ? 'success'
                                  : machine.status === 'FAILED'
                                    ? 'error'
                                    : 'processing'
                              }
                            >
                              {machine.status}
                            </Tag>
                          </Descriptions.Item>
                          <Descriptions.Item label="开始时间">
                            {machine.startTime}
                          </Descriptions.Item>
                          <Descriptions.Item label="结束时间">
                            {machine.endTime || '-'}
                          </Descriptions.Item>
                          {machine.errorMessage && (
                            <Descriptions.Item label="错误信息" span={2}>
                              {machine.errorMessage}
                            </Descriptions.Item>
                          )}
                        </Descriptions>
                      ))}
                    </div>
                  ),
                }}
              />
            </div>
          )}
        </Modal>
      </div>
    </div>
  );
}
