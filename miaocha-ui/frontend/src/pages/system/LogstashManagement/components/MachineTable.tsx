import { Table, Space, Button, Popconfirm, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import type { LogstashProcess } from '@/types/logstashTypes';

interface MachineTableProps {
  record: LogstashProcess;
  data: LogstashProcess[];
  onStartMachine: (machineId: number) => void;
  onStopMachine: (machineId: number) => void;
  onRefreshConfig: (processId: number, machineId: number) => void;
  onReinitializeMachine: (machineId: number) => void;
  onForceStopMachine: (processId: number, machineId: number) => void;
  onEditMachineConfig: (machineId: number, processId: number, data: LogstashProcess[]) => void;
  onShowMachineDetail: (machineId: number, moduleName?: string) => void;
  onShowMachineTasks: (processId: number, machineId: number) => void;
  onShowLog: (machineId: number, isBottom?: boolean) => void;
  onDeleteMachine: (processId: number, machineId: number) => void;
}

const MachineTable = ({
  record,
  data,
  onStartMachine,
  onStopMachine,
  onRefreshConfig,
  onReinitializeMachine,
  onForceStopMachine,
  onEditMachineConfig,
  onShowMachineDetail,
  onShowMachineTasks,
  onShowLog,
  onDeleteMachine,
}: MachineTableProps) => {
  const columns: ColumnsType<any> = [
    {
      title: '实例ID',
      dataIndex: 'logstashMachineId',
      key: 'logstashMachineId',
      render: (logstashMachineId: number) => (
        <Button type="link" onClick={() => onShowMachineDetail(logstashMachineId, record.moduleName)}>
          {logstashMachineId}
        </Button>
      ),
    },
    {
      title: 'pid',
      dataIndex: 'processPid',
      key: 'processPid',
    },
    {
      title: '部署路径',
      dataIndex: 'deployPath',
      key: 'deployPath',
    },
    {
      title: '机器名称',
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
      render: (state: string, machineRecord: any) => {
        let color = 'orange';
        if (state === 'RUNNING') color = 'green';
        else if (state === 'STOPPED') color = 'red';

        return <Tag color={color}>{machineRecord.stateDescription}</Tag>;
      },
    },
    {
      title: '最近更新时间',
      dataIndex: 'lastUpdateTime',
      key: 'lastUpdateTime',
      render: (lastUpdateTime: string) => {
        return lastUpdateTime ? dayjs(lastUpdateTime).format('YYYY-MM-DD HH:mm:ss') : '-';
      },
    },
    {
      title: '操作',
      key: 'action',
      render: (_, machine: any) => (
        <Space size="small">
          {machine.state !== 'INITIALIZE_FAILED' && (
            <>
              {!['RUNNING', 'STARTING', 'STOPPING', 'INITIALIZING'].includes(machine.state) && (
                <Popconfirm
                  title="确认启动"
                  description="确定要启动这台机器吗？"
                  onConfirm={() => onStartMachine(machine.logstashMachineId)}
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
              )}
              {!['STOPPED', 'STOPPING', 'NOT_STARTED', 'INITIALIZING'].includes(machine.state) && (
                <Popconfirm
                  title="确认停止"
                  description="确定要停止这台机器吗？"
                  onConfirm={() => onStopMachine(machine.logstashMachineId)}
                  okText="确认"
                  cancelText="取消"
                >
                  <Button
                    type="link"
                    disabled={['STOPPED', 'STOPPING', 'NOT_STARTED'].includes(machine.state)}
                    style={{ padding: '0 4px' }}
                  >
                    停止
                  </Button>
                </Popconfirm>
              )}
              {machine.state !== 'RUNNING' && (
                <Popconfirm
                  title="确认刷新配置"
                  description="确定要刷新这台机器的配置吗？"
                  onConfirm={() => onRefreshConfig(record.id, machine.logstashMachineId)}
                  okText="确认"
                  cancelText="取消"
                >
                  <Button type="link" disabled={machine.state === 'RUNNING'} style={{ padding: '0 4px' }}>
                    刷新配置
                  </Button>
                </Popconfirm>
              )}
              {machine.state !== 'RUNNING' && (
                <Button
                  type="link"
                  onClick={() => onEditMachineConfig(machine.logstashMachineId, record.id, data)}
                  disabled={machine.state === 'RUNNING'}
                  style={{ padding: '0 4px' }}
                >
                  编辑配置
                </Button>
              )}
            </>
          )}
          {machine.state === 'INITIALIZE_FAILED' && (
            <Popconfirm
              title="确认重新初始化"
              description="确定要重新初始化这台机器吗？"
              onConfirm={() => onReinitializeMachine(machine.logstashMachineId)}
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
              onConfirm={() => onForceStopMachine(record.id, machine.machineId)}
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
            onClick={() => onShowMachineTasks(record.id, machine.logstashMachineId)}
          >
            任务
          </Button>
          <Button type="link" style={{ padding: '0 4px' }} onClick={() => onShowLog(machine.logstashMachineId, true)}>
            日志
          </Button>
          {!['RUNNING', 'STOPPING', 'STARTING', 'STOPPING_FAILED'].includes(machine.state) && (
            <Popconfirm
              title="确认删除"
              description="确定要删除这台机器吗？此操作将执行缩容操作。"
              onConfirm={() => onDeleteMachine(record.id, machine.logstashMachineId)}
              okText="确认"
              cancelText="取消"
              okType="danger"
            >
              <Button type="link" danger style={{ padding: '0 4px' }} disabled={machine.state === 'RUNNING'}>
                删除
              </Button>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ];

  return (
    <Table
      size="small"
      bordered
      dataSource={record.logstashMachineStatusInfo}
      rowKey="machineId"
      columns={columns}
      pagination={false}
    />
  );
};

export default MachineTable;
