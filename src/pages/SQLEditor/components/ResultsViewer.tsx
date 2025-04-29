import React, { memo } from 'react';
import { Table, Spin, Empty, Alert, Typography } from 'antd';
import { QueryResult } from '../types';

const { Text } = Typography;

interface ResultsViewerProps {
  queryResults: QueryResult | null;
  loading: boolean;
  downloadResults: () => void;
  formatTableCell: (value: any) => React.ReactNode;
  fullscreen: boolean;
}

/**
 * 查询结果显示组件
 * 以表格形式展示SQL查询结果
 */
const ResultsViewer: React.FC<ResultsViewerProps> = ({
  queryResults,
  loading,
  downloadResults,
  formatTableCell,
  fullscreen
}) => {
  if (loading) {
    return <Spin tip="执行查询中..." />;
  }

  if (!queryResults) {
    return <Empty description="请执行查询以查看结果" />;
  }

  if (queryResults.status === 'error') {
    return (
      <Alert
        type="error"
        message="查询执行错误"
        description={queryResults.message || '未知错误'}
        showIcon
      />
    );
  }

  if (!queryResults.rows?.length) {
    return <Empty description="查询没有返回数据" />;
  }

  // 计算表格高度，在全屏模式下使用更多空间
  const tableHeight = fullscreen ? 'calc(100vh - 350px)' : '400px';

  // 构建表格列
  const columns = queryResults.columns?.map(col => ({
    title: col,
    dataIndex: col,
    key: col,
    render: (value: any) => formatTableCell(value),
    ellipsis: true,
    sorter: (a: any, b: any) => {
      const valueA = a[col];
      const valueB = b[col];
      
      if (typeof valueA === 'number' && typeof valueB === 'number') {
        return valueA - valueB;
      }
      
      if (typeof valueA === 'string' && typeof valueB === 'string') {
        return valueA.localeCompare(valueB);
      }
      
      return 0;
    }
  }));

  return (
    <div>
      {queryResults.executionTimeMs && (
        <div style={{ marginBottom: 16 }}>
          <Text type="secondary">
            查询执行时间: {queryResults.executionTimeMs}ms | 
            返回行数: {queryResults.rows.length} 
            {queryResults.affectedRows !== undefined && ` | 影响行数: ${queryResults.affectedRows}`}
          </Text>
        </div>
      )}

      <Table
        dataSource={queryResults.rows.map((row, index) => ({ ...row, key: index }))}
        columns={columns}
        scroll={{ x: true, y: tableHeight }}
        size="small"
        pagination={{
          showSizeChanger: true,
          showQuickJumper: true,
          pageSizeOptions: ['10', '20', '50', '100'],
          showTotal: (total) => `共 ${total} 条记录`
        }}
      />
    </div>
  );
};

// 使用 memo 避免不必要的重渲染
export default memo(ResultsViewer);
