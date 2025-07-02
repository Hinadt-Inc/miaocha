import {
  Modal,
  Descriptions,
  Tag,
  Table,
  Divider,
  Skeleton,
  Timeline,
  Card,
  Space,
  Input,
  Select,
  Row,
  Col,
  Statistic,
} from 'antd';
import {
  PlayCircleOutlined,
  StopOutlined,
  SettingOutlined,
  CloudUploadOutlined,
  SearchOutlined,
  ClockCircleOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined,
} from '@ant-design/icons';
import type { LogstashTaskStatus } from '@/types/logstashTypes';
import dayjs from 'dayjs';
import { useState, useMemo } from 'react';
import styles from './MachineTasksModal.module.less';

interface MachineTasksModalProps {
  visible: boolean;
  onClose: () => void;
  machineTasks: LogstashTaskStatus[];
  loading: boolean;
}

const MachineTasksModal = ({ visible, onClose, machineTasks, loading }: MachineTasksModalProps) => {
  const [searchText, setSearchText] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('all');
  const [operationFilter, setOperationFilter] = useState<string>('all');

  const getStatusColor = (status: string) => {
    if (status === 'COMPLETED') return 'success';
    if (status === 'FAILED') return 'error';
    if (status === 'RUNNING') return 'processing';
    return 'default';
  };

  const getOperationIcon = (operationType: string) => {
    switch (operationType) {
      case 'START':
        return <PlayCircleOutlined style={{ color: '#52c41a' }} />;
      case 'STOP':
        return <StopOutlined style={{ color: '#ff4d4f' }} />;
      case 'UPDATE_CONFIG':
        return <SettingOutlined style={{ color: '#1890ff' }} />;
      case 'INITIALIZE':
        return <CloudUploadOutlined style={{ color: '#722ed1' }} />;
      default:
        return <SettingOutlined />;
    }
  };

  const getTimelineColor = (status: string) => {
    const statusColor = getStatusColor(status);
    if (statusColor === 'success') return 'green';
    if (statusColor === 'error') return 'red';
    return 'blue';
  };

  const getOperationText = (operationType: string) => {
    const operationMap = {
      START: '启动',
      STOP: '停止',
      UPDATE_CONFIG: '配置更新',
      INITIALIZE: '初始化',
    };
    return operationMap[operationType as keyof typeof operationMap] || operationType;
  };

  // 过滤和排序任务
  const filteredTasks = useMemo(() => {
    let filtered = machineTasks.filter((task) => {
      const matchesSearch =
        task.name.toLowerCase().includes(searchText.toLowerCase()) ||
        task.description.toLowerCase().includes(searchText.toLowerCase());
      const matchesStatus = statusFilter === 'all' || task.status === statusFilter;
      const matchesOperation = operationFilter === 'all' || task.operationType === operationFilter;

      return matchesSearch && matchesStatus && matchesOperation;
    });

    // 按创建时间降序排列
    return filtered.sort((a, b) => dayjs(b.createTime).valueOf() - dayjs(a.createTime).valueOf());
  }, [machineTasks, searchText, statusFilter, operationFilter]);

  // 统计信息
  const statistics = useMemo(() => {
    const total = machineTasks.length;
    const completed = machineTasks.filter((t) => t.status === 'COMPLETED').length;
    const failed = machineTasks.filter((t) => t.status === 'FAILED').length;
    const running = machineTasks.filter((t) => t.status === 'RUNNING').length;

    return { total, completed, failed, running };
  }, [machineTasks]);

  // 获取机器信息（从第一个任务中获取，因为都是同一台机器）
  const machineInfo = machineTasks[0] || null;

  const stepColumns = [
    { title: '步骤ID', dataIndex: 'stepId', key: 'stepId' },
    { title: '步骤名称', dataIndex: 'stepName', key: 'stepName' },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => <Tag color={getStatusColor(status)}>{status}</Tag>,
    },
    {
      title: '开始时间',
      dataIndex: 'startTime',
      key: 'startTime',
      render: (startTime: string) => (startTime ? dayjs(startTime).format('YYYY-MM-DD HH:mm:ss') : '-'),
    },
    {
      title: '结束时间',
      dataIndex: 'endTime',
      key: 'endTime',
      render: (endTime: string) => (endTime ? dayjs(endTime).format('YYYY-MM-DD HH:mm:ss') : '-'),
    },
    {
      title: '持续时间',
      dataIndex: 'duration',
      key: 'duration',
      render: (duration: number) => (duration != null ? `${duration}ms` : '-'),
    },
    {
      title: '错误信息',
      dataIndex: 'errorMessage',
      key: 'errorMessage',
      render: (errorMessage: string) => errorMessage || '-',
    },
  ];

  return (
    <Modal
      title={
        <div>
          <span>实例任务记录</span>
          {machineInfo && (
            <Tag color="blue" style={{ marginLeft: 8 }}>
              {machineInfo.machineName} ({machineInfo.machineIp})
            </Tag>
          )}
        </div>
      }
      open={visible}
      onCancel={onClose}
      footer={null}
      width={1400}
      style={{ top: 20 }}
    >
      {loading ? (
        <Skeleton active paragraph={{ rows: 8 }} />
      ) : (
        <>
          {/* 统计信息 */}
          <Row gutter={16} className={styles.statisticsRow}>
            <Col span={6}>
              <Card size="small">
                <Statistic title="总任务数" value={statistics.total} prefix={<ClockCircleOutlined />} />
              </Card>
            </Col>
            <Col span={6}>
              <Card size="small">
                <Statistic
                  title="已完成"
                  value={statistics.completed}
                  prefix={<CheckCircleOutlined />}
                  valueStyle={{ color: '#3f8600' }}
                />
              </Card>
            </Col>
            <Col span={6}>
              <Card size="small">
                <Statistic
                  title="执行中"
                  value={statistics.running}
                  prefix={<ClockCircleOutlined />}
                  valueStyle={{ color: '#1890ff' }}
                />
              </Card>
            </Col>
            <Col span={6}>
              <Card size="small">
                <Statistic
                  title="失败"
                  value={statistics.failed}
                  prefix={<ExclamationCircleOutlined />}
                  valueStyle={{ color: '#cf1322' }}
                />
              </Card>
            </Col>
          </Row>

          {/* 筛选和搜索 */}
          <Card size="small" className={styles.filterCard}>
            <Row gutter={16}>
              <Col span={8}>
                <Input
                  placeholder="搜索任务名称或描述"
                  prefix={<SearchOutlined />}
                  value={searchText}
                  onChange={(e) => setSearchText(e.target.value)}
                  allowClear
                />
              </Col>
              <Col span={8}>
                <Select
                  placeholder="按状态筛选"
                  value={statusFilter}
                  onChange={setStatusFilter}
                  style={{ width: '100%' }}
                >
                  <Select.Option value="all">全部状态</Select.Option>
                  <Select.Option value="COMPLETED">已完成</Select.Option>
                  <Select.Option value="RUNNING">执行中</Select.Option>
                  <Select.Option value="FAILED">失败</Select.Option>
                </Select>
              </Col>
              <Col span={8}>
                <Select
                  placeholder="按操作类型筛选"
                  value={operationFilter}
                  onChange={setOperationFilter}
                  style={{ width: '100%' }}
                >
                  <Select.Option value="all">全部操作</Select.Option>
                  <Select.Option value="START">启动</Select.Option>
                  <Select.Option value="STOP">停止</Select.Option>
                  <Select.Option value="UPDATE_CONFIG">配置更新</Select.Option>
                  <Select.Option value="INITIALIZE">初始化</Select.Option>
                </Select>
              </Col>
            </Row>
          </Card>

          {/* 任务时间线 */}
          <Card title="任务执行时间线" size="small">
            <Timeline mode="left">
              {filteredTasks.map((task) => (
                <Timeline.Item
                  key={task.taskId}
                  color={getTimelineColor(task.status)}
                  dot={getOperationIcon(task.operationType)}
                >
                  <Card size="small" className={styles.taskCard}>
                    <Row>
                      <Col span={16}>
                        <div className={styles.taskHeader}>
                          <Space>
                            <Tag color={getStatusColor(task.status)}>{task.status}</Tag>
                            <span className={styles.taskTitle}>{getOperationText(task.operationType)}</span>
                            <span>{task.processName}</span>
                          </Space>
                        </div>
                        <div className={styles.taskDescription}>{task.description}</div>
                      </Col>
                      <Col span={8}>
                        <div className={styles.taskTimeInfo}>
                          <Space size="middle" wrap>
                            <span>开始: {dayjs(task.startTime).format('MM-DD HH:mm:ss')}</span>
                            {task.endTime && <span>结束: {dayjs(task.endTime).format('MM-DD HH:mm:ss')}</span>}
                            <span>耗时: {task.duration != null ? `${(task.duration / 1000).toFixed(1)}s` : '-'}</span>
                          </Space>
                        </div>
                      </Col>
                    </Row>

                    {/* 任务详情 */}
                    <Divider className={styles.taskDivider} />
                    <Descriptions size="small" column={4} style={{ marginBottom: 8 }}>
                      <Descriptions.Item label="总步骤">{task.totalSteps}</Descriptions.Item>
                      <Descriptions.Item label="成功">{task.successCount}</Descriptions.Item>
                      <Descriptions.Item label="失败">{task.failedCount}</Descriptions.Item>
                      <Descriptions.Item label="跳过">{task.skippedCount}</Descriptions.Item>
                    </Descriptions>

                    {task.errorMessage && (
                      <div className={styles.errorMessage}>
                        <strong>错误信息:</strong> {task.errorMessage}
                      </div>
                    )}

                    {/* 步骤详情 */}
                    {Object.entries(task.instanceSteps).map(([instanceName, steps]) => (
                      <div key={instanceName} className={styles.instanceContainer}>
                        <div className={styles.instanceHeader}>
                          <strong>{instanceName}</strong>
                          <span className={styles.instanceProgress}>
                            进度: {task.instanceProgressPercentages[instanceName]}%
                          </span>
                        </div>
                        <Table
                          size="small"
                          bordered
                          dataSource={steps}
                          rowKey="stepId"
                          pagination={false}
                          columns={stepColumns}
                        />
                      </div>
                    ))}
                  </Card>
                </Timeline.Item>
              ))}
            </Timeline>

            {filteredTasks.length === 0 && <div className={styles.emptyState}>暂无匹配的任务记录</div>}
          </Card>
        </>
      )}
    </Modal>
  );
};

export default MachineTasksModal;
