import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  PlayCircleOutlined,
  StopOutlined,
  SyncOutlined,
  HistoryOutlined,
  InfoCircleOutlined,
  CodeOutlined,
  SettingOutlined,
  HomeOutlined,
} from '@ant-design/icons';
import { Button, message, Popconfirm, Space, Table, Breadcrumb, Modal, Progress, Tag, Descriptions, Card } from 'antd';
import styles from './LogstashManagementPage.module.less';
import { Link } from 'react-router-dom';
import { useEffect } from 'react';
import {
  createLogstashProcess,
  deleteLogstashProcess,
  getLogstashProcesses,
  getLogstashProcess,
  startLogstashProcess,
  startLogstashMachine,
  stopLogstashMachine,
  refreshLogstashMachineConfig,
  refreshLogstashConfig,
  stopLogstashProcess,
  executeLogstashSQL,
  updateLogstashConfig,
  getLogstashTaskStatus,
  getLogstashTaskSummaries,
  getMachineTasks,
  reinitializeFailedMachines,
  reinitializeMachine,
} from '../../api/logstash';
import type { LogstashProcess, LogstashTaskSummary, MachineTask } from '../../types/logstashTypes';
import LogstashEditModal from './components/LogstashEditModal';
import LogstashMachineConfigModal from './components/LogstashMachineConfigModal';
import { useState } from 'react';

function LogstashManagementPage() {
  const [data, setData] = useState<LogstashProcess[]>([]);
  const [loading, setLoading] = useState(false);
  const [editModalVisible, setEditModalVisible] = useState(false);
  const [currentProcess, setCurrentProcess] = useState<LogstashProcess | null>(null);
  const [taskSummaries, setTaskSummaries] = useState<LogstashTaskSummary[]>([]);
  const [summaryModalVisible, setSummaryModalVisible] = useState(false);
  const [selectedTask, setSelectedTask] = useState<LogstashTaskSummary | null>(null);
  const [stepsModalVisible, setStepsModalVisible] = useState(false);
  const [machineTasks, setMachineTasks] = useState<MachineTask[]>([]);
  const [machineTasksModalVisible, setMachineTasksModalVisible] = useState(false);
  const [currentMachine, setCurrentMachine] = useState<{
    processId: number;
    machineId: number;
    configContent?: string;
    jvmOptions?: string;
    logstashYml?: string;
  } | null>(null);
  const [machineConfigModalVisible, setMachineConfigModalVisible] = useState(false);
  const [sqlModalVisible, setSqlModalVisible] = useState(false);
  const [sql, setSql] = useState('');
  const [messageApi, contextHolder] = message.useMessage();
  const [detailModalVisible, setDetailModalVisible] = useState(false);
  const [currentDetail, setCurrentDetail] = useState<LogstashProcess | null>(null);

  const showTaskSteps = (task: LogstashTaskSummary) => {
    setSelectedTask(task);
    setStepsModalVisible(true);
  };

  const showMachineTasks = async (processId: number, machineId: number) => {
    try {
      setCurrentMachine({ processId, machineId });
      const tasks = await getMachineTasks(processId, machineId);
      setMachineTasks(tasks);
      setMachineTasksModalVisible(true);
    } catch (err) {
      messageApi.error('获取机器任务失败');
      console.error('获取机器任务失败:', err);
    }
  };

  const fetchData = async () => {
    setLoading(true);
    try {
      const processes = await getLogstashProcesses();
      // 获取每个进程的详细信息
      const detailedProcesses = await Promise.all(
        processes.map(async (process) => {
          try {
            return await getLogstashProcess(process.id);
          } catch (err) {
            console.error(`获取进程 ${process.id} 详情失败:`, err);
            return process; // 返回基本信息如果获取详情失败
          }
        }),
      );
      setData(detailedProcesses);
    } catch (err) {
      messageApi.error('获取Logstash进程列表失败');
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
      messageApi.error('获取任务历史失败');
      console.error('获取任务历史失败:', err);
    }
  };

  useEffect(() => {
    fetchData().catch((err) => {
      messageApi.error('组件加载失败');
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
      messageApi.success('删除成功');
      await fetchData();
    } catch (err) {
      messageApi.error('删除失败');
      console.error('删除Logstash进程失败:', err);
    }
  };

  const handleStart = async (id: number) => {
    try {
      messageApi.loading('正在启动Logstash进程...');
      await startLogstashProcess(id);
      messageApi.success('启动命令已发送');
      await fetchData();

      const pollInterval = 2000;
      let pollTimer: NodeJS.Timeout;

      const pollStatus = async () => {
        try {
          const status = await getLogstashTaskStatus(id);
          if (status.status === 'COMPLETED') {
            messageApi.success('所有机器启动完成');
            await fetchData();
            clearTimeout(pollTimer);
            return;
          }
          if (status.status === 'FAILED') {
            messageApi.error('部分机器启动失败');
            await fetchData();
            clearTimeout(pollTimer);
            return;
          }
          messageApi.info(`启动中: ${status.progressPercentage}%`);
          pollTimer = setTimeout(pollStatus, pollInterval);
        } catch (err) {
          messageApi.error('获取任务状态失败');
          clearTimeout(pollTimer);
        }
      };

      pollTimer = setTimeout(pollStatus, pollInterval);
    } catch (err) {
      messageApi.error('启动失败');
      console.error('启动Logstash进程失败:', err);
    }
  };

  const handleStartMachine = async (processId: number, machineId: number) => {
    try {
      messageApi.loading(`正在启动机器 ${machineId} 的Logstash实例...`);
      await startLogstashMachine(processId, machineId);
      messageApi.success('启动命令已发送');
      await fetchData();
    } catch (err) {
      messageApi.error('启动失败');
      console.error('启动Logstash机器实例失败:', err);
    }
  };

  const handleRefreshConfig = async (processId: number, machineId: number) => {
    try {
      messageApi.loading(`正在刷新机器 ${machineId} 的配置...`);
      await refreshLogstashMachineConfig(processId, machineId);
      messageApi.success('配置刷新命令已发送');
      await fetchData();
    } catch (err) {
      messageApi.error('配置刷新失败');
      console.error('刷新Logstash机器配置失败:', err);
    }
  };

  const handleRefreshAllConfig = async (processId: number) => {
    try {
      messageApi.loading('正在刷新所有机器配置...');
      await refreshLogstashConfig(processId, {});
      messageApi.success('配置刷新命令已发送');
      await fetchData();
    } catch (err) {
      messageApi.error('配置刷新失败');
      console.error('刷新Logstash配置失败:', err);
    }
  };

  const handleReinitializeFailedMachines = async (processId: number) => {
    try {
      messageApi.loading('正在重新初始化失败机器...');
      await reinitializeFailedMachines(processId);
      messageApi.success('重新初始化命令已发送');
      await fetchData();
    } catch (err) {
      messageApi.error('重新初始化失败');
      console.error('重新初始化失败机器失败:', err);
    }
  };

  const handleStopMachine = async (processId: number, machineId: number) => {
    try {
      messageApi.loading(`正在停止机器 ${machineId} 的Logstash实例...`);
      await stopLogstashMachine(processId, machineId);
      messageApi.success('停止命令已发送');
      await fetchData();
    } catch (err) {
      messageApi.error('停止失败');
      console.error('停止Logstash机器实例失败:', err);
    }
  };

  const handleReinitializeMachine = async (processId: number, machineId: number) => {
    try {
      messageApi.loading(`正在重新初始化机器 ${machineId}...`);
      await reinitializeMachine(processId, machineId);
      messageApi.success('重新初始化命令已发送');
      await fetchData();
    } catch (err) {
      messageApi.error('重新初始化失败');
      console.error('重新初始化机器失败:', err);
    }
  };

  const handleExecuteSQL = async (processId: number) => {
    try {
      if (!sql.trim()) {
        messageApi.warning('请输入SQL语句');
        return;
      }

      messageApi.loading('正在执行SQL...');
      await executeLogstashSQL(processId, sql);
      messageApi.success('SQL执行成功');
      setSqlModalVisible(false);
      setSql('');
      await fetchData();
    } catch (err) {
      messageApi.error('SQL执行失败');
      console.error('执行SQL失败:', err);
    }
  };

  const handleStop = async (id: number) => {
    try {
      messageApi.loading('正在停止Logstash进程...');
      await stopLogstashProcess(id);
      messageApi.success('停止命令已发送');
      await fetchData();

      const pollInterval = 2000;
      let pollTimer: NodeJS.Timeout;

      const pollStatus = async () => {
        try {
          const status = await getLogstashTaskStatus(id);
          if (status.status === 'COMPLETED') {
            messageApi.success('所有机器停止完成');
            await fetchData();
            clearTimeout(pollTimer);
            return;
          }
          if (status.status === 'FAILED') {
            messageApi.error('部分机器停止失败');
            await fetchData();
            clearTimeout(pollTimer);
            return;
          }
          messageApi.info(`停止中: ${status.progressPercentage}%`);
          pollTimer = setTimeout(pollStatus, pollInterval);
        } catch (err) {
          messageApi.error('获取任务状态失败');
          clearTimeout(pollTimer);
        }
      };

      pollTimer = setTimeout(pollStatus, pollInterval);
    } catch (err) {
      messageApi.error('停止失败');
      console.error('停止Logstash进程失败:', err);
    }
  };

  const columns = [
    {
      title: '名称',
      dataIndex: 'name',
      key: 'name',
      render: (name: string, record: LogstashProcess) => (
        <Button
          type="link"
          style={{ color: '#1890ff', padding: 0 }}
          onClick={() => {
            setCurrentDetail(record);
            setDetailModalVisible(true);
          }}
        >
          {name}
        </Button>
      ),
    },
    {
      title: '模块',
      dataIndex: 'module',
      key: 'module',
    },
    {
      title: '状态',
      key: 'state',
      render: (_: unknown, record: LogstashProcess) => (
        <Space direction="vertical" size={4}>
          {record.machineStatuses?.map((machine) => (
            <div key={machine.machineId}>
              <Tag color={machine.state === 'RUNNING' ? 'green' : machine.state === 'STOPPED' ? 'red' : 'orange'}>
                {machine.machineName} ({machine.machineIp}): {machine.state}
              </Tag>
            </div>
          ))}
        </Space>
      ),
    },
    {
      title: '操作',
      key: 'action',
      render: (_: unknown, record: LogstashProcess) => (
        <Space size="small">
          <Button
            type="link"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
            disabled={['RUNNING', 'STARTING', 'STOPPING'].includes(record.state)}
          >
            编辑
          </Button>
          <Button
            type="link"
            icon={<PlayCircleOutlined />}
            onClick={() => handleStart(record.id)}
            disabled={['RUNNING', 'STARTING', 'STOPPING'].includes(record.state)}
          >
            启动
          </Button>
          <Button
            type="link"
            icon={<StopOutlined />}
            onClick={() => handleStop(record.id)}
            disabled={['STOPPED', 'STARTING', 'STOPPING'].includes(record.state)}
          >
            停止
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
          <Button
            type="link"
            icon={<CodeOutlined />}
            onClick={() => {
              setSqlModalVisible(true);
              setCurrentProcess(record);
              setSql(record.dorisSql || '');
            }}
            // disabled={!!record.dorisSql}
          >
            SQL
          </Button>
          <Button
            type="link"
            icon={<SyncOutlined />}
            onClick={() => handleRefreshAllConfig(record.id)}
            disabled={record.state === 'RUNNING'}
          >
            刷新配置
          </Button>
          <Popconfirm
            title="确认重新初始化"
            description="确定要重新初始化所有初始化失败的机器吗？"
            onConfirm={() => {
              void handleReinitializeFailedMachines(record.id);
            }}
            okText="确认"
            cancelText="取消"
          >
            <Button type="link" icon={<SyncOutlined />} disabled={record.state === 'RUNNING'}>
              重新初始化失败机器
            </Button>
          </Popconfirm>
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
    <Card>
      <div className={styles.container}>
        {contextHolder}
        <div className={styles.header}>
          <Breadcrumb>
            <Breadcrumb.Item>
              <Link to="/">
                <HomeOutlined />
              </Link>
            </Breadcrumb.Item>
            <Breadcrumb.Item>Logstash管理</Breadcrumb.Item>
          </Breadcrumb>
          <div className={styles.tableToolbar}>
            <Button
              type="default"
              icon={<SyncOutlined />}
              onClick={fetchData}
              loading={loading}
              style={{ marginRight: 8 }}
            >
              刷新
            </Button>
            <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
              新增Logstash进程
            </Button>
          </div>
        </div>

        <div>
          <Table
            columns={columns}
            dataSource={data}
            size="small"
            rowKey="id"
            loading={loading}
            bordered
            expandable={{
              expandedRowRender: (record) => (
                <Table
                  size="small"
                  bordered
                  dataSource={record.machineStatuses}
                  rowKey="machineId"
                  columns={[
                    {
                      title: '机器ID',
                      dataIndex: 'machineId',
                      key: 'machineId',
                    },
                    {
                      title: '名称',
                      dataIndex: 'machineName',
                      key: 'machineName',
                    },
                    {
                      title: 'IP',
                      dataIndex: 'machineIp',
                      key: 'machineIp',
                    },
                    {
                      title: '状态',
                      dataIndex: 'state',
                      key: 'state',
                      render: (state: string) => (
                        <Tag color={state === 'RUNNING' ? 'green' : state === 'STOPPED' ? 'red' : 'orange'}>
                          {state}
                        </Tag>
                      ),
                    },
                    {
                      title: '状态描述',
                      dataIndex: 'stateDescription',
                      key: 'stateDescription',
                    },
                    {
                      title: '操作',
                      key: 'action',
                      render: (_: unknown, machine: { machineId: number; state: string }) => (
                        <Space size="small">
                          {machine.state !== 'INITIALIZE_FAILED' && (
                            <>
                              <Button
                                type="link"
                                icon={<PlayCircleOutlined />}
                                onClick={() => handleStartMachine(record.id, machine.machineId)}
                                disabled={machine.state === 'RUNNING'}
                              >
                                启动
                              </Button>
                              <Button
                                type="link"
                                icon={<StopOutlined />}
                                onClick={() => handleStopMachine(record.id, machine.machineId)}
                                disabled={machine.state === 'STOPPED'}
                              >
                                停止
                              </Button>
                              <Button
                                type="link"
                                icon={<SyncOutlined />}
                                onClick={() => handleRefreshConfig(record.id, machine.machineId)}
                                disabled={machine.state === 'RUNNING'}
                              >
                                刷新配置
                              </Button>
                              <Button
                                type="link"
                                icon={<SettingOutlined />}
                                onClick={() => {
                                  const process = data.find((p) => p.id === record.id);
                                  setCurrentMachine({
                                    processId: record.id,
                                    machineId: machine.machineId,
                                    configContent: process?.configContent,
                                    jvmOptions: process?.jvmOptions,
                                    logstashYml: process?.logstashYml,
                                  });
                                  setMachineConfigModalVisible(true);
                                }}
                                disabled={machine.state === 'RUNNING'}
                              >
                                编辑配置
                              </Button>
                            </>
                          )}
                          {machine.state === 'INITIALIZE_FAILED' && (
                            <Popconfirm
                              title="确认重新初始化"
                              description="确定要重新初始化这台机器吗？"
                              onConfirm={() => {
                                void handleReinitializeMachine(record.id, machine.machineId);
                              }}
                              okText="确认"
                              cancelText="取消"
                            >
                              <Button type="link" icon={<SyncOutlined />}>
                                重新初始化
                              </Button>
                            </Popconfirm>
                          )}
                          <Button
                            type="link"
                            icon={<HistoryOutlined />}
                            onClick={() => showMachineTasks(record.id, machine.machineId)}
                          >
                            任务
                          </Button>
                        </Space>
                      ),
                    },
                  ]}
                  pagination={false}
                />
              ),
            }}
          />
          <LogstashEditModal
            visible={editModalVisible}
            onCancel={() => setEditModalVisible(false)}
            onOk={async (values: Partial<LogstashProcess>) => {
              try {
                if (currentProcess) {
                  // 只更新配置相关字段
                  await updateLogstashConfig(currentProcess.id, {
                    configContent: values.configContent,
                    jvmOptions: values.jvmOptions,
                    logstashYml: values.logstashYml,
                  });
                  messageApi.success('配置更新成功');
                } else {
                  const process = await createLogstashProcess(values);
                  messageApi.success('创建成功');

                  // 如果jvmOptions或logstashYml为空，异步同步配置
                  if (!values.jvmOptions || !values.logstashYml) {
                    setTimeout(async () => {
                      try {
                        await updateLogstashConfig(process.id, {
                          jvmOptions: values.jvmOptions || '默认JVM参数',
                          logstashYml: values.logstashYml || '默认Logstash配置',
                        });
                        await fetchData();
                      } catch (err) {
                        console.error('异步更新配置失败:', err);
                      }
                    }, 3000);
                  }
                }
                setEditModalVisible(false);
                await fetchData();
              } catch (err) {
                messageApi.error(currentProcess ? '配置更新失败' : '创建失败');
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
                    <Tag color={status === 'COMPLETED' ? 'success' : status === 'FAILED' ? 'error' : 'processing'}>
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
                        record.status === 'FAILED' ? 'exception' : record.status === 'COMPLETED' ? 'success' : 'active'
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
                    <Button type="link" icon={<InfoCircleOutlined />} onClick={() => showTaskSteps(record)}>
                      详情
                    </Button>
                  ),
                },
              ]}
            />
          </Modal>
          <Modal
            title={`机器任务 - ${currentMachine?.machineId || ''}`}
            open={machineTasksModalVisible}
            onCancel={() => setMachineTasksModalVisible(false)}
            footer={null}
            width={1200}
          >
            {machineTasks.map((task) => (
              <div key={task.taskId} style={{ marginBottom: 24 }}>
                <Descriptions bordered size="small" column={2} style={{ marginBottom: 16 }}>
                  <Descriptions.Item label="任务ID">{task.taskId}</Descriptions.Item>
                  <Descriptions.Item label="任务名称">{task.name}</Descriptions.Item>
                  <Descriptions.Item label="操作类型">{task.operationType}</Descriptions.Item>
                  <Descriptions.Item label="状态">
                    <Tag
                      color={
                        task.status === 'COMPLETED' ? 'success' : task.status === 'FAILED' ? 'error' : 'processing'
                      }
                    >
                      {task.status}
                    </Tag>
                  </Descriptions.Item>
                  <Descriptions.Item label="开始时间">{task.startTime}</Descriptions.Item>
                  <Descriptions.Item label="结束时间">{task.endTime || '-'}</Descriptions.Item>
                  <Descriptions.Item label="持续时间">{task.duration}ms</Descriptions.Item>
                  <Descriptions.Item label="错误信息" span={2}>
                    {task.errorMessage || '无'}
                  </Descriptions.Item>
                </Descriptions>

                <h4 style={{ marginBottom: 16 }}>步骤详情</h4>
                {Object.entries(task.machineSteps).map(([machineName, steps]) => (
                  <div key={machineName} style={{ marginBottom: 16 }}>
                    <h5>
                      {machineName} (进度: {task.machineProgressPercentages[machineName]}%)
                    </h5>
                    <Table
                      size="small"
                      bordered
                      dataSource={steps}
                      rowKey="stepId"
                      columns={[
                        { title: '步骤ID', dataIndex: 'stepId', key: 'stepId' },
                        { title: '步骤名称', dataIndex: 'stepName', key: 'stepName' },
                        {
                          title: '状态',
                          dataIndex: 'status',
                          key: 'status',
                          render: (status: string) => (
                            <Tag
                              color={status === 'COMPLETED' ? 'success' : status === 'FAILED' ? 'error' : 'processing'}
                            >
                              {status}
                            </Tag>
                          ),
                        },
                        { title: '开始时间', dataIndex: 'startTime', key: 'startTime' },
                        {
                          title: '结束时间',
                          dataIndex: 'endTime',
                          key: 'endTime',
                          render: (endTime: string) => endTime || '-',
                        },
                        {
                          title: '持续时间',
                          dataIndex: 'duration',
                          key: 'duration',
                          render: (duration: number) => `${duration}ms`,
                        },
                        {
                          title: '错误信息',
                          dataIndex: 'errorMessage',
                          key: 'errorMessage',
                          render: (errorMessage: string) => errorMessage || '-',
                        },
                      ]}
                    />
                  </div>
                ))}
              </div>
            ))}
          </Modal>
          <Modal
            title="任务步骤详情"
            open={stepsModalVisible}
            onCancel={() => setStepsModalVisible(false)}
            footer={null}
            width={1200}
          >
            {selectedTask && (
              <div>
                <Descriptions bordered size="small" column={2} style={{ marginBottom: 16 }}>
                  <Descriptions.Item label="任务ID">{selectedTask.taskId}</Descriptions.Item>
                  <Descriptions.Item label="任务名称">{selectedTask.name}</Descriptions.Item>
                  <Descriptions.Item label="任务状态">
                    <Tag
                      color={
                        selectedTask.status === 'COMPLETED'
                          ? 'success'
                          : selectedTask.status === 'FAILED'
                            ? 'error'
                            : 'processing'
                      }
                    >
                      {selectedTask.status}
                    </Tag>
                  </Descriptions.Item>
                  <Descriptions.Item label="开始时间">{selectedTask.startTime}</Descriptions.Item>
                  <Descriptions.Item label="结束时间">{selectedTask.endTime || '-'}</Descriptions.Item>
                  <Descriptions.Item label="持续时间">{selectedTask.duration}ms</Descriptions.Item>
                  <Descriptions.Item label="错误信息" span={2}>
                    {selectedTask.errorMessage || '无'}
                  </Descriptions.Item>
                </Descriptions>

                <h4 style={{ marginBottom: 16 }}>机器步骤详情</h4>
                {Object.entries(selectedTask.machineSteps).map(([machineName, steps]) => (
                  <div key={machineName} style={{ marginBottom: 24 }}>
                    <h5>
                      {machineName} (进度: {selectedTask.machineProgressPercentages[machineName]}%)
                    </h5>
                    <Table
                      size="small"
                      bordered
                      dataSource={steps}
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
                          title: '状态',
                          dataIndex: 'status',
                          key: 'status',
                          render: (status: string) => (
                            <Tag
                              color={status === 'COMPLETED' ? 'success' : status === 'FAILED' ? 'error' : 'processing'}
                            >
                              {status}
                            </Tag>
                          ),
                        },
                        {
                          title: '开始时间',
                          dataIndex: 'startTime',
                          key: 'startTime',
                        },
                        {
                          title: '结束时间',
                          dataIndex: 'endTime',
                          key: 'endTime',
                          render: (endTime: string) => endTime || '-',
                        },
                        {
                          title: '持续时间',
                          dataIndex: 'duration',
                          key: 'duration',
                          render: (duration: number) => `${duration}ms`,
                        },
                        {
                          title: '错误信息',
                          dataIndex: 'errorMessage',
                          key: 'errorMessage',
                          render: (errorMessage: string) => errorMessage || '-',
                        },
                      ]}
                    />
                  </div>
                ))}
              </div>
            )}
          </Modal>
          <Modal
            title="执行Doris SQL"
            open={sqlModalVisible}
            onCancel={() => {
              setSqlModalVisible(false);
              setSql('');
            }}
            onOk={() => {
              if (currentProcess) {
                void handleExecuteSQL(currentProcess.id);
              }
            }}
            okButtonProps={{ disabled: !!currentProcess?.dorisSql.trim() }}
            width={800}
          >
            <div style={{ marginBottom: 16 }}>
              <p>请输入要执行的Doris SQL语句（主要用于创建表）：</p>
            </div>
            <textarea
              value={sql}
              onChange={(e) => setSql(e.target.value)}
              style={{ width: '100%', height: '200px' }}
              placeholder="例如：CREATE TABLE log_table_test_env (...) ENGINE=OLAP ..."
              disabled={!!currentProcess?.dorisSql}
            />
          </Modal>
          <Modal
            title={`Logstash进程详情 - ${currentDetail?.name || ''}`}
            open={detailModalVisible}
            onCancel={() => setDetailModalVisible(false)}
            footer={null}
            width={1000}
          >
            {currentDetail && (
              <div style={{ maxHeight: '70vh', overflowY: 'auto' }}>
                <Descriptions bordered column={2} size="small">
                  <Descriptions.Item label="ID" span={1}>
                    {currentDetail.id}
                  </Descriptions.Item>
                  <Descriptions.Item label="模块" span={1}>
                    {currentDetail.module}
                  </Descriptions.Item>
                  <Descriptions.Item label="创建时间" span={1}>
                    {currentDetail.createTime}
                  </Descriptions.Item>
                  <Descriptions.Item label="更新时间" span={1}>
                    {currentDetail.updateTime}
                  </Descriptions.Item>
                  <Descriptions.Item label="描述" span={2}>
                    {currentDetail.description || '无描述'}
                  </Descriptions.Item>
                  <Descriptions.Item label="自定义包路径" span={2}>
                    {currentDetail.customPackagePath || '未设置'}
                  </Descriptions.Item>
                </Descriptions>

                <div style={{ margin: '16px 0' }}>
                  <h4 style={{ marginBottom: 8 }}>JVM参数</h4>
                  <pre
                    style={{
                      margin: 0,
                      padding: 12,
                      background: '#f5f5f5',
                      borderRadius: 4,
                      maxHeight: 200,
                      overflow: 'auto',
                    }}
                  >
                    {currentDetail.jvmOptions}
                  </pre>
                </div>

                <div style={{ margin: '16px 0' }}>
                  <h4 style={{ marginBottom: 8 }}>Logstash配置</h4>
                  <pre
                    style={{
                      margin: 0,
                      padding: 12,
                      background: '#f5f5f5',
                      borderRadius: 4,
                      maxHeight: 200,
                      overflow: 'auto',
                    }}
                  >
                    {currentDetail.logstashYml}
                  </pre>
                </div>

                <div style={{ margin: '16px 0' }}>
                  <h4 style={{ marginBottom: 8 }}>配置内容</h4>
                  <pre
                    style={{
                      margin: 0,
                      padding: 12,
                      background: '#f5f5f5',
                      borderRadius: 4,
                      maxHeight: 200,
                      overflow: 'auto',
                    }}
                  >
                    {currentDetail.configContent}
                  </pre>
                </div>

                <div style={{ marginTop: 24 }}>
                  <h4 style={{ marginBottom: 16 }}>机器状态</h4>
                  <Table
                    size="small"
                    bordered
                    dataSource={currentDetail.machineStatuses}
                    rowKey="machineId"
                    pagination={false}
                    scroll={{ x: true }}
                    columns={[
                      {
                        title: '机器名称',
                        dataIndex: 'machineName',
                        fixed: 'left',
                        width: 150,
                      },
                      {
                        title: 'IP',
                        dataIndex: 'machineIp',
                        width: 120,
                      },
                      {
                        title: '状态',
                        dataIndex: 'state',
                        width: 120,
                        render: (state: string) => (
                          <Tag color={state === 'RUNNING' ? 'green' : state === 'STOPPED' ? 'red' : 'orange'}>
                            {state}
                          </Tag>
                        ),
                      },
                      {
                        title: '状态描述',
                        dataIndex: 'stateDescription',
                        width: 200,
                        ellipsis: true,
                      },
                    ]}
                  />
                </div>
              </div>
            )}
          </Modal>
          <LogstashMachineConfigModal
            visible={machineConfigModalVisible}
            onCancel={() => setMachineConfigModalVisible(false)}
            processId={currentMachine?.processId || 0}
            machineId={currentMachine?.machineId || 0}
            initialConfig={{
              configContent: currentMachine?.configContent,
              jvmOptions: currentMachine?.jvmOptions,
              logstashYml: currentMachine?.logstashYml,
            }}
          />
        </div>
      </div>
    </Card>
  );
}

import withSystemAccess from '@/utils/withSystemAccess';
export default withSystemAccess(LogstashManagementPage);
