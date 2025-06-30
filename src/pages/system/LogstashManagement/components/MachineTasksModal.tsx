import { Modal, Descriptions, Tag, Table, Divider, Skeleton } from 'antd';
import type { LogstashTaskStatus } from '@/types/logstashTypes';
import dayjs from 'dayjs';

interface MachineTasksModalProps {
  visible: boolean;
  onClose: () => void;
  machineTasks: LogstashTaskStatus[];
  loading: boolean;
  machineId: number;
  styles?: {
    taskItem?: string;
    taskDescriptions?: string;
    stepsTitle?: string;
    machineSteps?: string;
  };
}

const MachineTasksModal = ({
  visible,
  onClose,
  machineTasks,
  loading,
  machineId,
  styles: customStyles,
}: MachineTasksModalProps) => {
  const getStatusColor = (status: string) => {
    if (status === 'COMPLETED') return 'success';
    if (status === 'FAILED') return 'error';
    return 'processing';
  };

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
    <Modal title={`实例任务 - ${machineId || ''}`} open={visible} onCancel={onClose} footer={null} width={1200}>
      {loading ? (
        <Skeleton active paragraph={{ rows: 8 }} />
      ) : (
        machineTasks.map((task) => (
          <div key={task.taskId} className="task-item">
            <Descriptions bordered size="small" column={2} className="task-descriptions">
              <Descriptions.Item label="任务ID">{task.taskId}</Descriptions.Item>
              <Descriptions.Item label="任务名称">{task.name}</Descriptions.Item>
              <Descriptions.Item label="操作类型">{task.operationType}</Descriptions.Item>
              <Descriptions.Item label="状态">
                <Tag color={getStatusColor(task.status)}>{task.status}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="开始时间">
                {task.startTime ? dayjs(task.startTime).format('YYYY-MM-DD HH:mm:ss') : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="结束时间">
                {task.endTime ? dayjs(task.endTime).format('YYYY-MM-DD HH:mm:ss') : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="持续时间">
                {task.duration != null ? `${task.duration}ms` : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="错误信息" span={2}>
                {task.errorMessage || '无'}
              </Descriptions.Item>
            </Descriptions>

            <h4 className="steps-title">步骤详情</h4>
            {Object.entries(task.instanceSteps).map(([machineName, steps], index) => (
              <div key={machineName}>
                {index > 0 && <Divider />}
                <div className="machine-steps">
                  <h5>
                    {machineName} (进度: {task.instanceProgressPercentages[machineName]}%)
                  </h5>
                  <Table
                    size="small"
                    bordered
                    dataSource={steps}
                    rowKey="stepId"
                    pagination={false}
                    columns={stepColumns}
                  />
                </div>
              </div>
            ))}
          </div>
        ))
      )}
    </Modal>
  );
};

export default MachineTasksModal;
