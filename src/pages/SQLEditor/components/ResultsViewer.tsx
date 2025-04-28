import {useMemo} from 'react';
import { Badge, Button, Empty, Space, Table, Typography } from 'antd';
import { DownloadOutlined } from '@ant-design/icons';
import { QueryResult } from '../types';
import Loading from '../../../components/Loading';
import './ResultsViewer.less';

const { Text } = Typography;

interface ResultsViewerProps {
  loading: boolean;
  queryResults: QueryResult | null;
  downloadResults: () => void;
  formatTableCell: (value: unknown) => React.ReactNode;
  fullscreen: boolean;
}

const ResultsViewer: React.FC<ResultsViewerProps> = ({
  loading,
  queryResults,
  downloadResults,
  formatTableCell,
  fullscreen
}) => {
  // 表格列定义
  const tableColumns = useMemo(() => {
    return queryResults?.columns?.map(col => ({
      title: col,
      dataIndex: col,
      key: col,
      render: formatTableCell,
      ellipsis: {
        showTitle: false,
      },
      width: 150,
    })) || [];
  }, [queryResults?.columns, formatTableCell]);

  return (
    <div style={{ overflow: 'hidden' }}>
      {loading ? (
        <div className="query-results-spinner">
          <Loading tip="执行查询中..." />
        </div>
      ) : queryResults ? (
        <div>
          {(
            queryResults.rows && queryResults.rows.length > 0 ? (
              <div>
                <div className="results-header">
                  <Space>
                    <Badge status="success" text={<Text strong className="success-text">查询成功</Text>} />
                    <Text>耗时: {queryResults.executionTimeMs} ms</Text>
                    <Text>总行数: {queryResults.total || queryResults.rows.length}</Text>
                  </Space>
                  <Button 
                    type="text" 
                    icon={<DownloadOutlined />}
                    onClick={downloadResults}
                  >
                    下载 CSV
                  </Button>
                </div>
                <Table
                  className="results-table"
                  columns={tableColumns}
                  dataSource={queryResults.rows.map((row, index) => ({
                    ...row,
                    key: `row-${index}`
                  }))}
                  pagination={{
                    pageSize: 20,
                    showSizeChanger: true,
                    showTotal: (total) => `共 ${total} 行`,
                    pageSizeOptions: ['10', '20', '50', '100']
                  }}
                  scroll={{ x: 'max-content', y: fullscreen ? window.innerHeight - 350 : 'calc(100vh - 400px)' }}
                  size="small"
                  bordered
                />
              </div>
            ) : (
              <Empty description="查询未返回数据" />
            )
          )}
        </div>
      ) : (
        <Empty description="请执行查询获取结果" />
      )}
    </div>
  );
};

export default ResultsViewer;
