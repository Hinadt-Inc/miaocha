import { Modal, Table, Tag, Progress, Button } from 'antd';
import { InfoCircleOutlined } from '@ant-design/icons';
import type { LogstashTaskSummary } from '@/types/logstashTypes';
import dayjs from 'dayjs';

interface TaskHistoryModalProps {
  visible: boolean;
  onClose: () => void;
  taskSummaries: LogstashTaskSummary[];
  onShowSteps: (task: LogstashTaskSummary) => void;
}

const TaskHistoryModal = ({ visible, onClose, taskSummaries, onShowSteps }: TaskHistoryModalProps) => {
  const columns = [
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
      title: 'IP',
      dataIndex: 'machineIp',
      key: 'machineIp',
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => {
        let color = 'processing';
        if (status === 'COMPLETED') color = 'success';
        else if (status === 'FAILED') color = 'error';

        return <Tag color={color}>{status}</Tag>;
      },
    },
    {
      title: '进度',
      key: 'progress',
      width: 100,
      render: (_: unknown, record: LogstashTaskSummary) => {
        let status: 'active' | 'success' | 'exception' = 'active';
        if (record.status === 'FAILED') status = 'exception';
        else if (record.status === 'COMPLETED') status = 'success';

        return <Progress percent={record.progressPercentage} status={status} />;
      },
    },
    {
      title: '开始时间',
      dataIndex: 'startTime',
      key: 'startTime',
      render: (startTime: string) => (startTime ? dayjs(startTime).format('YYYY-MM-DD HH:mm:ss') : '-'),
      width: 200,
    },
    {
      title: '操作',
      key: 'action',
      render: (_: unknown, record: LogstashTaskSummary) => (
        <Button type="link" icon={<InfoCircleOutlined />} onClick={() => onShowSteps(record)}>
          详情
        </Button>
      ),
    },
  ];

  return (
    <Modal title="任务历史" open={visible} onCancel={onClose} footer={null} width={1000}>
      <Table
        dataSource={taskSummaries}
        rowKey="taskId"
        columns={columns}
        pagination={{
          pageSize: 10,
          showSizeChanger: true,
          showTotal: (total) => `共 ${total} 条`,
        }}
      />
    </Modal>
  );
};

export default TaskHistoryModal;
