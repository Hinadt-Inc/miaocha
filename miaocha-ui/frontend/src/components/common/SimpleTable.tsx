import { Table } from 'antd';
import type { TableProps } from 'antd';
import Loading from '@/components/Loading';

interface SimpleTableProps<T> extends TableProps<T> {
  loading?: boolean;
}

export const SimpleTable = <T extends object>({ loading, ...props }: SimpleTableProps<T>) => {
  return (
    <div style={{ position: 'relative' }}>
      <Table size="middle" pagination={false} {...props} />
      {loading && (
        <Loading
          fullScreen={false}
          size="large"
          tip="加载中..."
          style={{
            position: 'absolute',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            zIndex: 10,
            backgroundColor: 'rgba(255, 255, 255, 0.8)',
            backdropFilter: 'blur(2px)',
          }}
        />
      )}
    </div>
  );
};
