import { PlusOutlined, SyncOutlined, InfoCircleOutlined, HomeOutlined, CopyOutlined } from '@ant-design/icons';
import {
  Button,
  message,
  Popconfirm,
  Space,
  Table,
  Breadcrumb,
  Modal,
  Progress,
  Tag,
  Descriptions,
  Skeleton,
  Tooltip,
} from 'antd';
import styles from './LogstashManagementPage.module.less';
import { Link } from 'react-router-dom';
import { ReactNode, useEffect, useState } from 'react';
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
  updateLogstashConfig,
  getLogstashTaskSummaries,
  reinitializeFailedMachines,
  reinitializeMachine,
  scaleProcess,
  forceStopLogstashMachine,
  updateLogstashProcessMetadata,
  forceStopLogstashProcess,
  getLogstashInstanceTasks,
} from '../../api/logstash';
import type {
  LogstashProcess,
  LogstashTaskSummary,
  LogstashTaskStatus,
  LogstashTaskStep,
} from '../../types/logstashTypes';
import LogstashEditModal from './components/LogstashEditModal';
import LogstashMachineConfigModal from './components/LogstashMachineConfigModal';
import LogstashMachineDetailModal from './components/LogstashMachineDetailModal';
import LogstashScaleModal from './components/LogstashScaleModal';
import LogstashLogTailModal from './components/LogstashLogTailModal';
import { safeCopy } from '@/utils/clipboard';

function LogstashManagementPage() {
  const checkSubTableStatus = (record: LogstashProcess, action: 'start' | 'stop') => {
    if (!record.logstashMachineStatusInfo) return false;

    return record.logstashMachineStatusInfo.every((machine) =>
      action === 'start'
        ? !['RUNNING', 'STARTING', 'STOPPING'].includes(machine.state)
        : ['STOPPED', 'STOPPING'].includes(machine.state),
    );
  };

  const hasInitializeFailedMachines = (record: LogstashProcess) => {
    return record.logstashMachineStatusInfo?.some((machine) => machine.state === 'INITIALIZE_FAILED') || false;
  };

  const allMachinesStopFailed = (record: LogstashProcess) => {
    return record.logstashMachineStatusInfo?.every((machine) => machine.state === 'STOP_FAILED') || false;
  };

  const [data, setData] = useState<LogstashProcess[]>([]);
  const [loading, setLoading] = useState(false);
  const [editModalVisible, setEditModalVisible] = useState(false);
  const [currentProcess, setCurrentProcess] = useState<LogstashProcess | null>(null);
  const [taskSummaries, setTaskSummaries] = useState<LogstashTaskSummary[]>([]);
  const [summaryModalVisible, setSummaryModalVisible] = useState(false);
  const [selectedTask, setSelectedTask] = useState<LogstashTaskSummary | null>(null);
  const [stepsModalVisible, setStepsModalVisible] = useState(false);
  const [machineTasks, setMachineTasks] = useState<LogstashTaskStatus[]>([]);
  const [machineTasksModalVisible, setMachineTasksModalVisible] = useState(false);
  const [machineTasksLoading, setMachineTasksLoading] = useState(false);
  const [currentMachine, setCurrentMachine] = useState<{
    configContent?: string;
    jvmOptions?: string;
    logstashYml?: string;
    logstashMachineId?: number;
    processId?: number;
  } | null>(null);
  const [machineConfigModalVisible, setMachineConfigModalVisible] = useState(false);
  const [messageApi, contextHolder] = message.useMessage();
  const [detailModalVisible, setDetailModalVisible] = useState<Record<string, boolean>>({});
  const [currentDetail, setCurrentDetail] = useState<LogstashProcess>();
  const [machineDetailModalVisible, setMachineDetailModalVisible] = useState(false);
  const [currentMachineDetail, setCurrentMachineDetail] = useState<LogstashProcess>();
  const [scaleModalVisible, setScaleModalVisible] = useState(false);
  const [logTailModalVisible, setLogTailModalVisible] = useState(false);
  const [bottomLogTailModalVisible, setBottomLogTailModalVisible] = useState(false);
  const [currentLogTailMachineId, setCurrentLogTailMachineId] = useState<number>();
  const [scaleParams, setScaleParams] = useState({
    addMachineIds: [] as number[],
    removeLogstashMachineIds: [] as number[],
    customDeployPath: '',
    forceScale: false,
  });

  const handleScale = async (
    processId: number,
    params?: {
      addMachineIds: number[];
      removeLogstashMachineIds: number[];
      customDeployPath: string;
      forceScale: boolean;
    },
  ) => {
    try {
      messageApi.loading('正在执行扩容/缩容操作...');
      const scaleParameters = params || scaleParams;
      await scaleProcess(processId, scaleParameters);
      messageApi.success('操作成功');
      setScaleModalVisible(false);
      await fetchData();
    } catch (err) {
      messageApi.error('操作失败');
      console.error('扩容/缩容操作失败:', err);
    }
  };

  const showTaskSteps = (task: LogstashTaskSummary) => {
    setSelectedTask(task);
    setStepsModalVisible(true);
  };

  const showMachineTasks = async (processId: number, logstashMachineId: number) => {
    setCurrentMachine({ logstashMachineId, processId });
    setMachineTasksModalVisible(true);
    setMachineTasksLoading(true);

    try {
      const tasks = await getLogstashInstanceTasks(logstashMachineId.toString());
      setMachineTasks(tasks);
    } catch (err) {
      messageApi.error('获取实例任务失败');
      console.error('获取实例任务失败:', err);
    } finally {
      setMachineTasksLoading(false);
    }
  };

  const fetchData = async () => {
    setLoading(true);
    try {
      const processes = await getLogstashProcesses();
      setData(processes);
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
    // 确保传递的数据包含datasourceId和tableName字段
    const editValues = {
      ...record,
      datasourceId: record.datasourceId,
      tableName: record.tableName,
    };
    setCurrentProcess(editValues);
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
    } catch (err) {
      messageApi.error('启动失败');
      console.error('启动Logstash进程失败:', err);
    }
  };

  const handleStartMachine = async (machineId: number) => {
    try {
      messageApi.loading(`正在启动机器 ${machineId} 的Logstash实例...`);
      await startLogstashMachine(machineId);
      messageApi.success('启动命令已发送');
      await fetchData();
    } catch (err) {
      messageApi.error('启动失败');
      console.error('启动Logstash机器实例失败:', err);
    }
  };

  const handleRefreshConfig = async (processId: number, machineId: number) => {
    try {
      await refreshLogstashConfig(processId, {
        logstashMachineIds: [machineId],
      });
      messageApi.success('配置刷新命令已发送');
      await fetchData();
    } catch (err) {
      messageApi.error('配置刷新失败');
      console.error('刷新Logstash机器配置失败:', err);
    }
  };

  const handleRefreshAllConfig = async (record: LogstashProcess) => {
    try {
      const logstashMachineIds = record.logstashMachineStatusInfo
        .filter((machine) => machine.state !== 'RUNNING')
        .map((machine) => machine.logstashMachineId);
      await refreshLogstashConfig(record.id, {
        logstashMachineIds,
      });
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

  const handleStopMachine = async (machineId: number) => {
    try {
      messageApi.loading(`正在停止机器 ${machineId} 的Logstash实例...`);
      await stopLogstashMachine(machineId);
      messageApi.success('停止命令已发送');
      await fetchData();
    } catch (err) {
      messageApi.error('停止失败');
      console.error('停止Logstash机器实例失败:', err);
    }
  };

  const handleReinitializeMachine = async (machineId: number) => {
    try {
      messageApi.loading(`正在重新初始化机器 ${machineId}...`);
      await reinitializeMachine(machineId);
      messageApi.success('重新初始化命令已发送');
      await fetchData();
    } catch (err) {
      messageApi.error('重新初始化失败');
      console.error('重新初始化机器失败:', err);
    }
  };

  const handleForceStopMachine = async (processId: number, machineId: number) => {
    try {
      messageApi.loading(`正在强制停止机器 ${machineId}...`);
      await forceStopLogstashMachine(processId, machineId);
      messageApi.success('强制停止命令已发送');
      await fetchData();
    } catch (err) {
      messageApi.error('强制停止失败');
      console.error('强制停止Logstash机器实例失败:', err);
    }
  };

  const handleForceStopProcess = async (id: number) => {
    try {
      messageApi.loading('正在强制停止所有机器...');
      await forceStopLogstashProcess(id);
      messageApi.success('全局强制停止命令已发送');
      await fetchData();
    } catch (err) {
      messageApi.error('全局强制停止失败');
      console.error('强制停止Logstash进程失败:', err);
    }
  };

  const handleCopy = async (text: string, label: string) => {
    const success = await safeCopy(text);
    if (success) {
      messageApi.success(`${label}已复制到剪贴板`);
    } else {
      messageApi.error(`复制${label}失败`);
    }
  };

  const handleStop = async (id: number) => {
    try {
      messageApi.loading('正在停止Logstash进程...');
      await stopLogstashProcess(id);
      messageApi.success('停止命令已发送');
      await fetchData();
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
            setDetailModalVisible({ ...detailModalVisible, [record.id]: true });
          }}
        >
          {name}
        </Button>
      ),
    },
    {
      title: '模块',
      dataIndex: 'moduleName',
      key: 'moduleName',
    },
    {
      title: '数据源',
      dataIndex: 'datasourceName',
      key: 'datasourceName',
    },
    {
      title: '表名',
      dataIndex: 'tableName',
      key: 'tableName',
    },
    {
      title: '状态',
      key: 'state',
      render: (_: unknown, record: LogstashProcess) => (
        <Space direction="vertical" size={4}>
          {record.logstashMachineStatusInfo?.map((machine) => (
            <div key={machine.machineId}>
              <Tag color={machine.state === 'RUNNING' ? 'green' : machine.state === 'STOPPED' ? 'red' : 'orange'}>
                {machine.machineName} ({machine.machineIp}): {machine.stateDescription}
              </Tag>
            </div>
          ))}
        </Space>
      ),
    },
    {
      title: '更新时间',
      dataIndex: 'updateTime',
      key: 'updateTime',
      render: (updateTime: string) => dayjs(updateTime).format('YYYY-MM-DD HH:mm:ss'),
    },
    {
      title: '更新人',
      dataIndex: 'updateUserName',
      key: 'updateUserName',
    },
    {
      title: '操作',
      key: 'action',
      fixed: 'right' as const,
      width: 300,
      render: (_: unknown, record: LogstashProcess) => (
        <Space size="small">
          <Button
            type="link"
            onClick={() => handleEdit(record)}
            disabled={['RUNNING', 'STARTING', 'STOPPING'].includes(record.state)}
            style={{ padding: '0 4px' }}
          >
            编辑
          </Button>
          <Popconfirm
            title="确认启动"
            description="确定要启动这个Logstash进程吗？"
            onConfirm={() => {
              void handleStart(record.id);
            }}
            okText="确认"
            cancelText="取消"
          >
            <Button type="link" disabled={checkSubTableStatus(record, 'start')} style={{ padding: '0 4px' }}>
              启动
            </Button>
          </Popconfirm>
          <Popconfirm
            title="确认停止"
            description="确定要停止这个Logstash进程吗？"
            onConfirm={() => {
              void handleStop(record.id);
            }}
            okText="确认"
            cancelText="取消"
          >
            <Button type="link" disabled={checkSubTableStatus(record, 'stop')} style={{ padding: '0 4px' }}>
              停止
            </Button>
          </Popconfirm>
          <Button
            type="link"
            onClick={() => {
              void showTaskSummaries(record.id);
            }}
            style={{ padding: '0 4px' }}
          >
            历史
          </Button>
          <Button
            type="link"
            onClick={async () => {
              setScaleModalVisible(true);
              setCurrentProcess(record);
              setScaleParams({
                addMachineIds: [],
                removeLogstashMachineIds: [],
                customDeployPath: record.customDeployPath || '',
                forceScale: false,
              });
            }}
            style={{ padding: '0 4px' }}
          >
            扩容/缩容
          </Button>
          <Popconfirm
            title="确认刷新配置"
            description="确定要刷新非运行状态下所有机器的配置吗？"
            onConfirm={() => {
              void handleRefreshAllConfig(record);
            }}
            okText="确认"
            cancelText="取消"
          >
            <Button type="link" disabled={record.state === 'RUNNING'} style={{ padding: '0 4px' }}>
              刷新配置
            </Button>
          </Popconfirm>
          {hasInitializeFailedMachines(record) && (
            <Popconfirm
              title="确认重新初始化"
              description="确定要重新初始化所有初始化失败的机器吗？"
              onConfirm={() => {
                void handleReinitializeFailedMachines(record.id);
              }}
              okText="确认"
              cancelText="取消"
            >
              <Button type="link" style={{ padding: '0 4px' }}>
                重新初始化失败机器
              </Button>
            </Popconfirm>
          )}
          {allMachinesStopFailed(record) && (
            <Popconfirm
              title="确认强制停止"
              description="确定要强制停止所有机器吗？这可能会导致数据丢失"
              onConfirm={() => {
                void handleForceStopProcess(record.id);
              }}
              okText="确认"
              cancelText="取消"
              okType="danger"
            >
              <Button type="link" danger style={{ padding: '0 4px' }}>
                强制停止
              </Button>
            </Popconfirm>
          )}
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
            <Button type="link" danger style={{ padding: '0 4px' }}>
              删除
            </Button>
          </Popconfirm>
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
            { title: 'Logstash管理' },
          ]}
        ></Breadcrumb>
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
          scroll={{ x: 'max-content' }}
          expandable={{
            expandedRowRender: (record) => (
              <Table
                size="small"
                bordered
                dataSource={record.logstashMachineStatusInfo}
                rowKey="machineId"
                columns={[
                  {
                    title: '机器ID',
                    dataIndex: 'machineId',
                    key: 'machineId',
                    render: (machineId: number, machine: any) => (
                      <Button
                        type="link"
                        onClick={async () => {
                          try {
                            const detail = await getLogstashProcess(machine.logstashMachineId);
                            setCurrentMachineDetail(detail);
                            setMachineDetailModalVisible(true);
                          } catch (err) {
                            messageApi.error('获取机器详情失败');
                            console.error('获取机器详情失败:', err);
                          }
                        }}
                      >
                        {machineId}
                      </Button>
                    ),
                  },
                  {
                    title: '实例ID',
                    dataIndex: 'logstashMachineId',
                    key: 'logstashMachineId',
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
                    render: (state: string, record) => (
                      <Tag color={state === 'RUNNING' ? 'green' : state === 'STOPPED' ? 'red' : 'orange'}>
                        {record.stateDescription}
                      </Tag>
                    ),
                  },
                  {
                    title: '操作',
                    key: 'action',
                    render: (
                      _: unknown,
                      machine: {
                        stateDescription: ReactNode;
                        machineId: number;
                        state: string;
                        logstashMachineId: number;
                      },
                    ) => (
                      <Space size="small">
                        {machine.state !== 'INITIALIZE_FAILED' && (
                          <>
                            <Popconfirm
                              title="确认启动"
                              description="确定要启动这台机器吗？"
                              onConfirm={() => handleStartMachine(machine.logstashMachineId)}
                              okText="确认"
                              cancelText="取消"
                            >
                              <Button
                                type="link"
                                disabled={['RUNNING', 'STARTING', 'STOPPING'].includes(machine.state)}
                                style={{ padding: '0 4px' }}
                              >
                                启动
                              </Button>
                            </Popconfirm>
                            <Popconfirm
                              title="确认停止"
                              description="确定要停止这台机器吗？"
                              onConfirm={() => handleStopMachine(machine.logstashMachineId)}
                              okText="确认"
                              cancelText="取消"
                            >
                              <Button
                                type="link"
                                disabled={['STOPPED', 'STOPPING'].includes(machine.state)}
                                style={{ padding: '0 4px' }}
                              >
                                停止
                              </Button>
                            </Popconfirm>
                            <Popconfirm
                              title="确认刷新配置"
                              description="确定要刷新这台机器的配置吗？"
                              onConfirm={() => handleRefreshConfig(record.id, machine.logstashMachineId)}
                              okText="确认"
                              cancelText="取消"
                            >
                              <Button type="link" disabled={machine.state === 'RUNNING'} style={{ padding: '0 4px' }}>
                                刷新配置
                              </Button>
                            </Popconfirm>
                            <Button
                              type="link"
                              onClick={() => {
                                const process = data.find((p) => p.id === record.id);
                                setCurrentMachine({
                                  logstashMachineId: machine.logstashMachineId,
                                  processId: record.id,
                                  configContent: process?.configContent,
                                  jvmOptions: process?.jvmOptions,
                                  logstashYml: process?.logstashYml,
                                });
                                setMachineConfigModalVisible(true);
                              }}
                              disabled={machine.state === 'RUNNING'}
                              style={{ padding: '0 4px' }}
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
                              void handleReinitializeMachine(machine.logstashMachineId);
                            }}
                            okText="确认"
                            cancelText="取消"
                          >
                            <Button type="link" style={{ padding: '0 4px' }}>
                              重新初始化
                            </Button>
                          </Popconfirm>
                        )}
                        {machine.state === 'STOP_FAILED' && (
                          <Popconfirm
                            title="确认强制停止"
                            description="确定要强制停止这台机器吗？这可能会导致数据丢失"
                            onConfirm={() => {
                              void handleForceStopMachine(record.id, machine.machineId);
                            }}
                            okText="确认"
                            cancelText="取消"
                            okType="danger"
                          >
                            <Button type="link" danger style={{ padding: '0 4px' }}>
                              强制停止
                            </Button>
                          </Popconfirm>
                        )}
                        <Button
                          type="link"
                          style={{ padding: '0 4px' }}
                          onClick={() => showMachineTasks(record.id, machine.logstashMachineId)}
                        >
                          任务
                        </Button>
                        <Button
                          type="link"
                          style={{ padding: '0 4px' }}
                          onClick={() => {
                            setCurrentLogTailMachineId(machine.logstashMachineId);
                            setBottomLogTailModalVisible(true);
                          }}
                        >
                          日志跟踪
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
                // 更新元数据
                await updateLogstashProcessMetadata(currentProcess.id, {
                  name: values.name || currentProcess.name,
                  moduleId: values.moduleId || currentProcess.moduleId,
                });

                // 更新配置相关字段
                // await updateLogstashConfig(currentProcess.id, {
                //   configContent: values.configContent,
                //   jvmOptions: values.jvmOptions,
                //   logstashYml: values.logstashYml,
                // });
                messageApi.success('更新成功');
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
              messageApi.error(currentProcess ? '更新失败' : '创建失败');
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
                render: (startTime: string) => dayjs(startTime).format('YYYY-MM-DD HH:mm:ss'),
                width: 200,
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
          title={`实例任务 - ${currentMachine?.logstashMachineId || ''}`}
          open={machineTasksModalVisible}
          onCancel={() => setMachineTasksModalVisible(false)}
          footer={null}
          width={1200}
        >
          {machineTasksLoading ? (
            <Skeleton active paragraph={{ rows: 8 }} />
          ) : (
            machineTasks.map((task) => (
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
                  <Descriptions.Item label="开始时间">
                    {dayjs(task.startTime).format('YYYY-MM-DD HH:mm:ss')}
                  </Descriptions.Item>
                  <Descriptions.Item label="结束时间">
                    {dayjs(task.endTime).format('YYYY-MM-DD HH:mm:ss') || '-'}
                  </Descriptions.Item>
                  <Descriptions.Item label="持续时间">{task.duration}ms</Descriptions.Item>
                  <Descriptions.Item label="错误信息" span={2}>
                    {task.errorMessage || '无'}
                  </Descriptions.Item>
                </Descriptions>

                <h4 style={{ marginBottom: 16 }}>步骤详情</h4>
                {Object.entries(task.instanceSteps).map(([machineName, steps]) => (
                  <div key={machineName} style={{ marginBottom: 16 }}>
                    <h5>
                      {machineName} (进度: {task.instanceProgressPercentages[machineName]}%)
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
                        {
                          title: '开始时间',
                          dataIndex: 'startTime',
                          key: 'startTime',
                          render: (startTime: string) => dayjs(startTime).format('YYYY-MM-DD HH:mm:ss'),
                        },
                        {
                          title: '结束时间',
                          dataIndex: 'endTime',
                          key: 'endTime',
                          render: (endTime: string) => dayjs(endTime).format('YYYY-MM-DD HH:mm:ss') || '-',
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
            ))
          )}
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
                <Descriptions.Item label="开始时间">
                  {dayjs(selectedTask.startTime).format('YYYY-MM-DD HH:mm:ss')}
                </Descriptions.Item>
                <Descriptions.Item label="结束时间">
                  {dayjs(selectedTask.endTime).format('YYYY-MM-DD HH:mm:ss') || '-'}
                </Descriptions.Item>
                <Descriptions.Item label="持续时间">{selectedTask.duration}ms</Descriptions.Item>
                <Descriptions.Item label="错误信息" span={2}>
                  {selectedTask.errorMessage || '无'}
                </Descriptions.Item>
              </Descriptions>

              <h4 style={{ marginBottom: 16 }}>机器步骤详情</h4>
              {Object.entries(selectedTask.instanceSteps as Record<string, LogstashTaskStep[]>).map(
                ([machineName, steps]) => (
                  <div key={machineName} style={{ marginBottom: 24 }}>
                    <h5>
                      {machineName} (进度: {selectedTask.instanceProgressPercentages[machineName]}%)
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
                          render: (startTime: string) => dayjs(startTime).format('YYYY-MM-DD HH:mm:ss'),
                        },
                        {
                          title: '结束时间',
                          dataIndex: 'endTime',
                          key: 'endTime',
                          render: (endTime: string) => dayjs(endTime).format('YYYY-MM-DD HH:mm:ss') || '-',
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
                ),
              )}
            </div>
          )}
        </Modal>
        <Modal
          title={`Logstash进程详情 - ${currentDetail?.name || ''}`}
          open={currentDetail ? !!detailModalVisible[currentDetail.id] : false}
          onCancel={() => {
            if (currentDetail) {
              setDetailModalVisible({ ...detailModalVisible, [currentDetail.id]: false });
            }
          }}
          footer={null}
          width={1000}
        >
          {currentDetail && (
            <div>
              <Descriptions bordered column={2} size="small">
                <Descriptions.Item label="ID" span={1}>
                  {currentDetail.id}
                </Descriptions.Item>
                <Descriptions.Item label="模块" span={1}>
                  {currentDetail.moduleName}
                </Descriptions.Item>
                <Descriptions.Item label="数据源" span={1}>
                  {currentDetail.datasourceName || '未设置'}
                </Descriptions.Item>
                <Descriptions.Item label="表名" span={1}>
                  {currentDetail.tableName || '未设置'}
                </Descriptions.Item>
                <Descriptions.Item label="创建时间" span={1}>
                  {dayjs(currentDetail.createTime).format('YYYY-MM-DD HH:mm:ss')}
                </Descriptions.Item>
                <Descriptions.Item label="更新时间" span={1}>
                  {dayjs(currentDetail.updateTime).format('YYYY-MM-DD HH:mm:ss')}
                </Descriptions.Item>
                <Descriptions.Item label="创建人" span={1}>
                  {currentDetail.createUserName || '未知'}
                </Descriptions.Item>
                <Descriptions.Item label="更新人" span={1}>
                  {currentDetail.updateUserName || '未知'}
                </Descriptions.Item>
                <Descriptions.Item label="描述" span={2}>
                  {currentDetail.description || '无描述'}
                </Descriptions.Item>
                <Descriptions.Item label="自定义包路径" span={2}>
                  {currentDetail.customPackagePath || '未设置'}
                </Descriptions.Item>
              </Descriptions>

              <div style={{ margin: '16px 0' }}>
                <div style={{ display: 'flex', alignItems: 'center', marginBottom: 8 }}>
                  <h4 style={{ margin: 0 }}>JVM参数</h4>
                  <Tooltip title="复制">
                    <Button
                      type="text"
                      icon={<CopyOutlined />}
                      onClick={() => handleCopy(currentDetail.jvmOptions || '', 'JVM参数')}
                      style={{ marginLeft: 8 }}
                    />
                  </Tooltip>
                </div>
                <pre
                  style={{
                    margin: 0,
                    padding: 12,
                    background: '#f5f5f5',
                    borderRadius: 4,
                    overflow: 'auto',
                    height: 200,
                  }}
                >
                  {currentDetail.jvmOptions}
                </pre>
              </div>

              <div style={{ margin: '16px 0' }}>
                <div style={{ display: 'flex', alignItems: 'center', marginBottom: 8 }}>
                  <h4 style={{ margin: 0 }}>Logstash配置</h4>
                  <Tooltip title="复制">
                    <Button
                      type="text"
                      icon={<CopyOutlined />}
                      onClick={() => handleCopy(currentDetail.logstashYml || '', 'Logstash配置')}
                      style={{ marginLeft: 8 }}
                    />
                  </Tooltip>
                </div>
                <pre
                  style={{
                    margin: 0,
                    padding: 12,
                    background: '#f5f5f5',
                    borderRadius: 4,
                    overflow: 'auto',
                    height: 200,
                  }}
                >
                  {currentDetail.logstashYml}
                </pre>
              </div>

              <div style={{ margin: '16px 0' }}>
                <div style={{ display: 'flex', alignItems: 'center', marginBottom: 8 }}>
                  <h4 style={{ margin: 0 }}>配置内容</h4>
                  <Tooltip title="复制">
                    <Button
                      type="text"
                      icon={<CopyOutlined />}
                      onClick={() => handleCopy(currentDetail.configContent || '', '配置内容')}
                      style={{ marginLeft: 8 }}
                    />
                  </Tooltip>
                </div>
                <pre
                  style={{
                    margin: 0,
                    padding: 12,
                    background: '#f5f5f5',
                    borderRadius: 4,
                    overflow: 'auto',
                    height: 200,
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
                  dataSource={currentDetail.logstashMachineStatusInfo}
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
          logstashMachineId={currentMachine?.logstashMachineId || 0}
          initialConfig={{
            configContent: currentMachine?.configContent,
            jvmOptions: currentMachine?.jvmOptions,
            logstashYml: currentMachine?.logstashYml,
          }}
        />
        <LogstashMachineDetailModal
          visible={machineDetailModalVisible}
          onCancel={() => {
            setMachineDetailModalVisible(false);
            setCurrentMachineDetail(undefined);
          }}
          detail={currentMachineDetail}
        />
        <LogstashScaleModal
          visible={scaleModalVisible}
          onCancel={() => setScaleModalVisible(false)}
          onOk={async (params) => {
            if (currentProcess) {
              setScaleParams(params);
              await handleScale(currentProcess.id, params);
            }
          }}
          currentProcess={currentProcess}
          initialParams={scaleParams}
        />
        <LogstashLogTailModal
          visible={logTailModalVisible}
          logstashMachineId={currentLogTailMachineId || 0}
          onCancel={() => setLogTailModalVisible(false)}
        />
        <LogstashLogTailModal
          visible={bottomLogTailModalVisible}
          logstashMachineId={currentLogTailMachineId || 0}
          onCancel={() => setBottomLogTailModalVisible(false)}
          style={{
            position: 'fixed',
            bottom: 0,
            left: 0,
            right: 0,
            margin: 0,
            maxWidth: '100%',
            height: '300px',
          }}
          bodyStyle={{
            padding: 0,
            height: '100%',
          }}
        />
      </div>
    </div>
  );
}

import withSystemAccess from '@/utils/withSystemAccess';
import dayjs from 'dayjs';
import { c } from 'vite/dist/node/moduleRunnerTransport.d-DJ_mE5sf';
export default withSystemAccess(LogstashManagementPage);
