import { Drawer, Space, Tag, Empty, Badge, Button, Typography, Pagination } from 'antd';
import { ClockCircleOutlined, CopyOutlined, HistoryOutlined } from '@ant-design/icons';
import VirtualList from 'rc-virtual-list';
import type { QueryHistoryItem } from '../../../api/sql';
import styles from './HistoryDrawer.module.less';

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
      open={visible}
      title={
        <Space>
          <HistoryOutlined />
          <span>查询历史</span>
          <Tag color="blue">{pagination?.total || 0} 条记录</Tag>
        </Space>
      }
      width={800}
      onClose={onClose}
    >
      <div className={styles.historyList}>
        {queryHistory.length > 0 ? (
          <>
            <VirtualList data={queryHistory} itemHeight={120} itemKey="id">
              {(history) => (
                <div key={history.id} className={styles.historyItem} onClick={() => loadFromHistory(history.sqlQuery)}>
                  <div className={styles.historyItemHeader}>
                    <Space>
                      <ClockCircleOutlined style={{ color: '#1890ff' }} />
                      <Text>{new Date(history.createTime).toLocaleString()}</Text>
                    </Space>
                    <Space>
                      <Badge status="success" text={<Text className={styles.historySuccess}>成功</Text>} />
                      <Button
                        icon={<CopyOutlined />}
                        size="small"
                        type="text"
                        onClick={(e) => {
                          e.stopPropagation();
                          copyToClipboard(history.sqlQuery);
                        }}
                      />
                    </Space>
                  </div>
                  <div className={styles.historySql}>{history.sqlQuery}</div>
                </div>
              )}
            </VirtualList>
            <div style={{ marginTop: 16, textAlign: 'right' }}>
              <Pagination
                current={pagination?.pageNum || 1}
                pageSize={pagination?.pageSize || 10}
                showQuickJumper
                showSizeChanger
                showTotal={(total) => `共 ${total} 条记录`}
                size="small"
                total={pagination?.total || 0}
                onChange={(page, pageSize) => {
                  if (onPaginationChange) {
                    onPaginationChange(page, pageSize);
                  }
                }}
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
