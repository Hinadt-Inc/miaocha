import { Table } from 'antd';
import type { TableProps } from 'antd';

interface SimpleTableProps<T> extends TableProps<T> {
  loading?: boolean;
}

export const SimpleTable = <T extends object>({ loading, ...props }: SimpleTableProps<T>) => {
  return <Table size="middle" pagination={false} loading={loading} {...props} />;
};
