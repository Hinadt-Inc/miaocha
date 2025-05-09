import React, { memo } from 'react';
import { Table, Spin, Empty, Alert, Typography, Button } from 'antd';
import { QueryResult } from '../types';

const { Text } = Typography;

interface ResultsViewerProps {
  queryResults: QueryResult | null;
  loading: boolean;
  formatTableCell: (value: any) => React.ReactNode;
  downloadResults: () => void;
}

// 常量
const TABLE_HEIGHT = '420px';
const PAGE_SIZE_OPTIONS = ['10', '20', '50', '100'];

// 比较值的工具函数，用于排序
const compareValues = (a: unknown, b: unknown): number => {
  if (typeof a === 'number' && typeof b === 'number') {
    return a - b;
  }
  if (typeof a === 'string' && typeof b === 'string') {
    return a.localeCompare(b);
  }
  return 0;
};

/**
 * 查询结果显示组件
 * 以表格形式展示 SQL 查询结果
 */
const ResultsViewer: React.FC<ResultsViewerProps> = ({
  queryResults,
  loading,
  formatTableCell,
  downloadResults,
}) => {
  // 加载中或无结果的早期返回
  if (loading) return <Spin tip="执行查询中..." />;
  if (!queryResults) return <Empty description="请执行查询以查看结果" />;

  // 错误状态
  if (queryResults.status === 'error') {
    return (
      <Alert
        type="error"
        message="查询执行错误"
        description={queryResults.message ?? '未知错误'}
        showIcon
      />
    );
  }

  // 无数据返回
  if (!queryResults.rows?.length) {
    return <Empty description="查询没有返回数据" />;
  }

  // 构建表格列
  const columns = queryResults.columns?.map(col => ({
    title: col,
    dataIndex: col,
    key: col,
    width: 150, // 统一宽度
    render: formatTableCell,
    ellipsis: true,
    sorter: (a: Record<string, unknown>, b: Record<string, unknown>) =>
      compareValues(a[col], b[col]),
  }));

  // 查询执行信息
  const executionInfo = queryResults.executionTimeMs && (
    <Text type="secondary" style={{ display: 'block', marginBottom: 16 }}>
      查询执行时间: {queryResults.executionTimeMs}ms | 返回行数: {queryResults.rows.length}
      {queryResults.affectedRows !== undefined && ` | 影响行数: ${queryResults.affectedRows}`}
    </Text>
  );

  return (
    <div>
      {executionInfo}
      <Table
        dataSource={queryResults.rows.map((row, index) => ({ ...row, key: index }))}
        columns={columns}
        scroll={{ x: 'max-content', y: TABLE_HEIGHT }}
        size="small"
        pagination={{
          showSizeChanger: true,
          showQuickJumper: true,
          pageSizeOptions: PAGE_SIZE_OPTIONS,
          showTotal: total => `共 ${total} 条记录`,
        }}
      />
    </div>
  );
};

export default memo(ResultsViewer);
