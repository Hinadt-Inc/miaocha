import { useState } from 'react';
import { Button, Space, Tag, Popconfirm } from 'antd';
import type { TablePaginationConfig, ColumnsType } from 'antd/es/table';
import type { LogstashProcess } from '@/types/logstashTypes';
import dayjs from 'dayjs';

interface UseTableConfigProps {
  onEdit: (record: LogstashProcess) => void;
  onDelete: (id: number) => void;
  onStart: (id: number) => void;
  onStop: (id: number) => void;
  onShowHistory: (id: number) => void;
  onScale: (record: LogstashProcess) => void;
  onShowAlert: (record: LogstashProcess) => void;
  onRefreshAllConfig: (record: LogstashProcess) => void;
  onReinitializeFailedMachines: (processId: number) => void;
  onForceStopProcess: (id: number) => void;
  onShowDetail: (record: LogstashProcess) => void;
}

export const useTableConfig = (props: UseTableConfigProps) => {
  const [pagination, setPagination] = useState<TablePaginationConfig>({
    current: 1,
    pageSize: 10,
    showSizeChanger: true,
    showQuickJumper: true,
    showTotal: (total) => `共 ${total} 条`,
    pageSizeOptions: ['10', '20', '50', '100'],
  });

  const checkSubTableStatus = (record: LogstashProcess, action: 'start' | 'stop') => {
    if (!record.logstashMachineStatusInfo) return false;

    return record.logstashMachineStatusInfo.every((machine) =>
      action === 'start'
        ? ['RUNNING', 'STARTING', 'STOPPING'].includes(machine.state)
        : ['STOPPED', 'STOPPING', 'NOT_STARTED'].includes(machine.state),
    );
  };

  const hasInitializeFailedMachines = (record: LogstashProcess) => {
    return record.logstashMachineStatusInfo?.some((machine) => machine.state === 'INITIALIZE_FAILED') || false;
  };

  const allMachinesStopFailed = (record: LogstashProcess) => {
    return record.logstashMachineStatusInfo?.every((machine) => machine.state === 'STOP_FAILED') || false;
  };

  const columns: ColumnsType<LogstashProcess> = [
    {
      title: '名称',
      dataIndex: 'name',
      key: 'name',
      render: (name: string, record: LogstashProcess) => (
        <Button style={{ color: '#1890ff', padding: 0 }} type="link" onClick={() => props.onShowDetail(record)}>
          {name}
        </Button>
      ),
    },
    // {
    //   title: '进程ID',
    //   dataIndex: 'id',
    //   key: 'id',
    // },
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
          {record.logstashMachineStatusInfo?.map((machine) => {
            let color = 'orange';
            if (machine.state === 'RUNNING') {
              color = 'green';
            } else if (machine.state === 'STOPPED') {
              color = 'red';
            }

            return (
              <div key={machine.machineId}>
                <Tag color={color}>
                  {machine.machineName} ({machine.machineIp}): {machine.stateDescription}
                </Tag>
              </div>
            );
          })}
        </Space>
      ),
    },
    {
      title: '更新时间',
      dataIndex: 'updateTime',
      key: 'updateTime',
      render: (updateTime: string) => (updateTime ? dayjs(updateTime).format('YYYY-MM-DD HH:mm:ss') : '-'),
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
            disabled={['RUNNING', 'STARTING', 'STOPPING'].includes(record.state)}
            style={{ padding: '0 4px' }}
            type="link"
            onClick={() => props.onEdit(record)}
          >
            编辑
          </Button>
          {!checkSubTableStatus(record, 'start') && (
            <Popconfirm
              cancelText="取消"
              description="确定要启动这个Logstash进程吗？"
              okText="确认"
              title="确认启动"
              onConfirm={() => {
                props.onStart(record.id);
              }}
            >
              <Button disabled={checkSubTableStatus(record, 'start')} style={{ padding: '0 4px' }} type="link">
                启动
              </Button>
            </Popconfirm>
          )}
          {!checkSubTableStatus(record, 'stop') && (
            <Popconfirm
              cancelText="取消"
              description="确定要停止这个Logstash进程吗？"
              okText="确认"
              title="确认停止"
              onConfirm={() => {
                props.onStop(record.id);
              }}
            >
              <Button disabled={checkSubTableStatus(record, 'stop')} style={{ padding: '0 4px' }} type="link">
                停止
              </Button>
            </Popconfirm>
          )}
          <Button
            style={{ padding: '0 4px' }}
            type="link"
            onClick={() => {
              props.onShowHistory(record.id);
            }}
          >
            历史
          </Button>
          <Button style={{ padding: '0 4px' }} type="link" onClick={() => props.onScale(record)}>
            扩容
          </Button>
          <Button style={{ padding: '0 4px' }} type="link" onClick={() => props.onShowAlert(record)}>
            告警
          </Button>
          {!record.logstashMachineStatusInfo.every((el) => el.state === 'RUNNING') && (
            <Popconfirm
              cancelText="取消"
              description="确定要刷新非运行状态下所有机器的配置吗？"
              okText="确认"
              title="确认刷新配置"
              onConfirm={() => {
                props.onRefreshAllConfig(record);
              }}
            >
              <Button
                disabled={record.logstashMachineStatusInfo.every((el) => el.state === 'RUNNING')}
                style={{ padding: '0 4px' }}
                type="link"
              >
                刷新配置
              </Button>
            </Popconfirm>
          )}
          {hasInitializeFailedMachines(record) && (
            <Popconfirm
              cancelText="取消"
              description="确定要重新初始化所有初始化失败的机器吗？"
              okText="确认"
              title="确认重新初始化"
              onConfirm={() => {
                props.onReinitializeFailedMachines(record.id);
              }}
            >
              <Button style={{ padding: '0 4px' }} type="link">
                重新初始化失败机器
              </Button>
            </Popconfirm>
          )}
          {allMachinesStopFailed(record) && (
            <Popconfirm
              cancelText="取消"
              description="确定要强制停止所有机器吗？这可能会导致数据丢失"
              okText="确认"
              okType="danger"
              title="确认强制停止"
              onConfirm={() => {
                props.onForceStopProcess(record.id);
              }}
            >
              <Button danger style={{ padding: '0 4px' }} type="link">
                强制停止
              </Button>
            </Popconfirm>
          )}
          <Popconfirm
            cancelText="取消"
            description="确定要删除这个Logstash进程吗？"
            okText="确认"
            okType="danger"
            title="确认删除"
            onConfirm={() => {
              props.onDelete(record.id);
            }}
          >
            <Button danger style={{ padding: '0 4px' }} type="link">
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const handleTableChange = (pagination: TablePaginationConfig) => {
    setPagination((prev) => ({
      ...prev,
      current: pagination.current,
      pageSize: pagination.pageSize,
    }));
  };

  return {
    columns,
    pagination,
    handleTableChange,
  };
};
