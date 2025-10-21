import { Modal, Descriptions, Tag, Table, Divider } from 'antd';
import type { LogstashTaskSummary, LogstashTaskStep } from '@/types/logstashTypes';
import dayjs from 'dayjs';

interface TaskDetailModalProps {
  visible: boolean;
  onClose: () => void;
  selectedTask: LogstashTaskSummary | null;
}

const TaskDetailModal = ({ visible, onClose, selectedTask }: TaskDetailModalProps) => {
  if (!selectedTask) return null;

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
    <Modal footer={null} open={visible} title="任务步骤详情" width={1200} onCancel={onClose}>
      <div>
        <Descriptions bordered column={2} size="small" style={{ marginBottom: 16 }}>
          <Descriptions.Item label="任务ID">{selectedTask.taskId}</Descriptions.Item>
          <Descriptions.Item label="任务名称">{selectedTask.name}</Descriptions.Item>
          <Descriptions.Item label="任务状态">
            <Tag color={getStatusColor(selectedTask.status)}>{selectedTask.status}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="开始时间">
            {selectedTask.startTime ? dayjs(selectedTask.startTime).format('YYYY-MM-DD HH:mm:ss') : '-'}
          </Descriptions.Item>
          <Descriptions.Item label="结束时间">
            {selectedTask.endTime ? dayjs(selectedTask.endTime).format('YYYY-MM-DD HH:mm:ss') : '-'}
          </Descriptions.Item>
          <Descriptions.Item label="持续时间">
            {selectedTask.duration != null ? `${selectedTask.duration}s` : '-'}
          </Descriptions.Item>
          <Descriptions.Item label="错误信息" span={2}>
            {selectedTask.errorMessage || '无'}
          </Descriptions.Item>
        </Descriptions>

        <h4 style={{ marginBottom: 16 }}>机器步骤详情</h4>
        {Object.entries(selectedTask.instanceSteps as Record<string, LogstashTaskStep[]>).map(
          ([machineName, steps], index) => (
            <div key={machineName}>
              {index > 0 && <Divider />}
              <div style={{ marginBottom: 12 }}>
                <h5>
                  {machineName} (进度: {selectedTask.instanceProgressPercentages[machineName]}%)
                </h5>
                <Table
                  bordered
                  columns={stepColumns}
                  dataSource={steps}
                  pagination={false}
                  rowKey="stepId"
                  size="small"
                />
              </div>
            </div>
          ),
        )}
      </div>
    </Modal>
  );
};

export default TaskDetailModal;
