// import { LinkOutlined } from '@ant-design/icons';
import type { ProColumns } from '@ant-design/pro-components';
import { Button, Space, Popconfirm } from 'antd';
import dayjs from 'dayjs';

import type { DataSourceItem } from './useDataSourceData';

interface UseTableConfigProps {
  testExistingLoading: Record<string, boolean>;
  onEdit: (record: DataSourceItem) => void;
  onDelete: (id: string, name: string) => Promise<void>;
  onTestConnection: (id: string, name: string) => Promise<void>;
}

const dataSourceTypeOptions = [{ label: 'Doris', value: 'Doris' }];

export const useTableConfig = ({ testExistingLoading, onEdit, onDelete, onTestConnection }: UseTableConfigProps) => {
  const columns: ProColumns<DataSourceItem>[] = [
    {
      title: '数据源名称',
      dataIndex: 'name',
      ellipsis: true,
      // width: '10%',
      hideInSearch: true,
    },
    {
      title: '类型',
      dataIndex: 'type',
      // width: 80,
      valueEnum: Object.fromEntries(dataSourceTypeOptions.map((option) => [option.value, option.label])),
      render: (_, record) => {
        const typeOption = dataSourceTypeOptions.find((option) => option.value === record.type);
        return typeOption?.label ?? record.type;
      },
      hideInSearch: true,
    },
    {
      title: 'JDBC URL',
      dataIndex: 'jdbcUrl',
      // width: '12%',
      ellipsis: true,
      hideInSearch: true,
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      width: 160,
      hideInSearch: true,
      render: (text, record) => {
        const time = text || (record as any).createdAt;
        return time ? dayjs(time as string).format('YYYY-MM-DD HH:mm:ss') : '-';
      },
      responsive: ['lg'],
    },
    {
      title: '创建人',
      dataIndex: 'creator',
      // width: '10%',
      hideInSearch: true,
      render: (_, record) => record.createUser || '-',
      responsive: ['lg'],
    },
    {
      title: '更新时间',
      dataIndex: 'updateTime',
      width: 160,
      hideInSearch: true,
      render: (text, record) => {
        const time = text || (record as any).updatedAt;
        return time ? dayjs(time as string).format('YYYY-MM-DD HH:mm:ss') : '-';
      },
      responsive: ['lg'],
    },
    {
      title: '更新人',
      dataIndex: 'updater',
      // width: '10%',
      hideInSearch: true,
      render: (_, record) => record.updateUser || '-',
      responsive: ['lg'],
    },
    {
      title: '操作',
      width: '100px',
      align: 'center',
      fixed: 'right',
      render: (_, record) => (
        <Space size="small">
          <Button
            loading={testExistingLoading[record.id] || false}
            size="small"
            title={`测试 ${record.name} 的数据库连接是否正常`}
            type="link"
            onClick={() => {
              void onTestConnection(record.id, record.name);
            }}
          >
            测试连接
          </Button>
          <Button size="small" type="link" onClick={() => onEdit(record)}>
            编辑
          </Button>
          <Popconfirm
            cancelText="取消"
            description="删除后将无法恢复，请谨慎操作"
            okButtonProps={{ danger: true }}
            okText="确定删除"
            placement="topRight"
            title={`确定要删除数据源 "${record.name}" 吗？`}
            onConfirm={() => {
              void onDelete(record.id, record.name);
            }}
          >
            <Button danger size="small" type="link">
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return {
    columns,
  };
};
