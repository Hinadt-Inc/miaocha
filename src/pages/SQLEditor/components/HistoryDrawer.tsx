import { Drawer, Space, Tag, Empty, Badge, Button, Typography, Pagination } from 'antd';
import { ClockCircleOutlined, CopyOutlined, HistoryOutlined } from '@ant-design/icons';
import VirtualList from 'rc-virtual-list';
import type { QueryHistoryItem } from '@/api/sql';
import './HistoryDrawer.less';

const { Text } = Typography;

interface HistoryDrawerProps {
  visible: boolean;
  onClose: () => void;
  queryHistory: QueryHistoryItem[];
  loadFromHistory: (historySql: string) => void;
  copyToClipboard: (text: string) => void;
  pagination?: {
    pageNum: number;
    pageSize: number;
    total: number;
  };
  onPaginationChange?: (page: number, pageSize: number) => void;
}

const HistoryDrawer: React.FC<HistoryDrawerProps> = ({
  visible,
  onClose,
  queryHistory,
  loadFromHistory,
  copyToClipboard,
  pagination,
  onPaginationChange,
}) => {
  return (
    <Drawer
      title={
        <Space>
          <HistoryOutlined />
          <span>查询历史</span>
          <Tag color="blue">{pagination?.total || 0} 条记录</Tag>
        </Space>
      }
      width={800}
      open={visible}
      onClose={onClose}
    >
      <div className="history-list">
        {queryHistory.length > 0 ? (
          <>
            <VirtualList data={queryHistory} itemHeight={120} itemKey="id">
              {(history) => (
                <div key={history.id} className="history-item" onClick={() => loadFromHistory(history.sqlQuery)}>
                  <div className="history-item-header">
                    <Space>
                      <ClockCircleOutlined style={{ color: '#1890ff' }} />
                      <Text>{new Date(history.createTime).toLocaleString()}</Text>
                    </Space>
                    <Space>
                      <Badge status="success" text={<Text className="history-success">成功</Text>} />
                      <Button
                        type="text"
                        size="small"
                        icon={<CopyOutlined />}
                        onClick={(e) => {
                          e.stopPropagation();
                          copyToClipboard(history.sqlQuery);
                        }}
                      />
                    </Space>
                  </div>
                  <div className="history-sql">{history.sqlQuery}</div>
                </div>
              )}
            </VirtualList>
            <div style={{ marginTop: 16, textAlign: 'right' }}>
              <Pagination
                size="small"
                current={pagination?.pageNum || 1}
                pageSize={pagination?.pageSize || 10}
                total={pagination?.total || 0}
                onChange={(page, pageSize) => {
                  if (onPaginationChange) {
                    onPaginationChange(page, pageSize);
                  }
                }}
                showSizeChanger
                showQuickJumper
                showTotal={(total) => `共 ${total} 条记录`}
              />
            </div>
          </>
        ) : (
          <Empty description="暂无查询历史记录" />
        )}
      </div>
    </Drawer>
  );
};

export default HistoryDrawer;
