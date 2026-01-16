import { Table, Space, Button, Popconfirm, Tag, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import type { LogstashProcess } from '@/types/logstashTypes';
import { useState } from 'react';
import { batchStartLogstashInstances, batchStopLogstashInstances } from '@/api/logstash';

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
  onRefresh?: () => Promise<void>;
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
  onRefresh,
}: MachineTableProps) => {
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);

  const handleBatchStart = async () => {
    try {
      const instanceIds = selectedRowKeys.map((key) => Number(key));
      await batchStartLogstashInstances(instanceIds);
      message.success('批量启动成功');
      setSelectedRowKeys([]);
      if (onRefresh) {
        await onRefresh();
      }
    } catch (error) {
      // Error already handled by global error handler
    }
  };

  const handleBatchStop = async () => {
    try {
      const instanceIds = selectedRowKeys.map((key) => Number(key));
      await batchStopLogstashInstances(instanceIds);
      message.success('批量停止成功');
      setSelectedRowKeys([]);
      if (onRefresh) {
        await onRefresh();
      }
    } catch (error) {
      // Error already handled by global error handler
    }
  };

  const rowSelection = {
    selectedRowKeys,
    onChange: (newSelectedRowKeys: React.Key[]) => {
      setSelectedRowKeys(newSelectedRowKeys);
    },
  };
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
                  cancelText="取消"
                  description="确定要启动这台机器吗？"
                  okText="确认"
                  title="确认启动"
                  onConfirm={() => onStartMachine(machine.logstashMachineId)}
                >
                  <Button
                    disabled={['RUNNING', 'STARTING', 'STOPPING'].includes(machine.state)}
                    style={{ padding: '0 4px' }}
                    type="link"
                  >
                    启动
                  </Button>
                </Popconfirm>
              )}
              {!['STOPPED', 'STOPPING', 'NOT_STARTED', 'INITIALIZING'].includes(machine.state) && (
                <Popconfirm
                  cancelText="取消"
                  description="确定要停止这台机器吗？"
                  okText="确认"
                  title="确认停止"
                  onConfirm={() => onStopMachine(machine.logstashMachineId)}
                >
                  <Button
                    disabled={['STOPPED', 'STOPPING', 'NOT_STARTED'].includes(machine.state)}
                    style={{ padding: '0 4px' }}
                    type="link"
                  >
                    停止
                  </Button>
                </Popconfirm>
              )}
              {machine.state !== 'RUNNING' && (
                <Popconfirm
                  cancelText="取消"
                  description="确定要刷新这台机器的配置吗？"
                  okText="确认"
                  title="确认刷新配置"
                  onConfirm={() => onRefreshConfig(record.id, machine.logstashMachineId)}
                >
                  <Button disabled={machine.state === 'RUNNING'} style={{ padding: '0 4px' }} type="link">
                    刷新配置
                  </Button>
                </Popconfirm>
              )}
              {machine.state !== 'RUNNING' && (
                <Button
                  disabled={machine.state === 'RUNNING'}
                  style={{ padding: '0 4px' }}
                  type="link"
                  onClick={() => onEditMachineConfig(machine.logstashMachineId, record.id, data)}
                >
                  编辑配置
                </Button>
              )}
            </>
          )}
          {machine.state === 'INITIALIZE_FAILED' && (
            <Popconfirm
              cancelText="取消"
              description="确定要重新初始化这台机器吗？"
              okText="确认"
              title="确认重新初始化"
              onConfirm={() => onReinitializeMachine(machine.logstashMachineId)}
            >
              <Button style={{ padding: '0 4px' }} type="link">
                重新初始化
              </Button>
            </Popconfirm>
          )}
          {machine.state === 'STOP_FAILED' && (
            <Popconfirm
              cancelText="取消"
              description="确定要强制停止这台机器吗？这可能会导致数据丢失"
              okText="确认"
              okType="danger"
              title="确认强制停止"
              onConfirm={() => onForceStopMachine(record.id, machine.machineId)}
            >
              <Button danger style={{ padding: '0 4px' }} type="link">
                强制停止
              </Button>
            </Popconfirm>
          )}
          <Button
            style={{ padding: '0 4px' }}
            type="link"
            onClick={() => onShowMachineTasks(record.id, machine.logstashMachineId)}
          >
            任务
          </Button>
          <Button style={{ padding: '0 4px' }} type="link" onClick={() => onShowLog(machine.logstashMachineId, true)}>
            日志
          </Button>
          {!['RUNNING', 'STOPPING', 'STARTING', 'STOPPING_FAILED'].includes(machine.state) && (
            <Popconfirm
              cancelText="取消"
              description="确定要删除这台机器吗？此操作将执行缩容操作。"
              okText="确认"
              okType="danger"
              title="确认删除"
              onConfirm={() => onDeleteMachine(record.id, machine.logstashMachineId)}
            >
              <Button danger disabled={machine.state === 'RUNNING'} style={{ padding: '0 4px' }} type="link">
                删除
              </Button>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ];

  return (
    <>
      {selectedRowKeys.length > 0 && (
        <div style={{ marginBottom: 8 }}>
          <Space>
            <span>已选择 {selectedRowKeys.length} 个实例</span>
            <Popconfirm
              cancelText="取消"
              description={`确定要批量启动选中的 ${selectedRowKeys.length} 个实例吗？`}
              okText="确认"
              title="确认批量启动"
              onConfirm={handleBatchStart}
            >
              <Button size="small" type="primary">
                批量启动
              </Button>
            </Popconfirm>
            <Popconfirm
              cancelText="取消"
              description={`确定要批量停止选中的 ${selectedRowKeys.length} 个实例吗？`}
              okText="确认"
              title="确认批量停止"
              onConfirm={handleBatchStop}
            >
              <Button danger size="small" type="primary">
                批量停止
              </Button>
            </Popconfirm>
            <Button size="small" onClick={() => setSelectedRowKeys([])}>
              取消选择
            </Button>
          </Space>
        </div>
      )}
      <Table
        bordered
        columns={columns}
        dataSource={record.logstashMachineStatusInfo}
        pagination={false}
        rowKey="logstashMachineId"
        rowSelection={rowSelection}
        size="small"
      />
    </>
  );
};

export default MachineTable;
