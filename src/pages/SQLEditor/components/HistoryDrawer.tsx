import { Drawer, Space, Tag, Empty, Badge, Button, Typography, Alert, Popconfirm } from 'antd';
import { ClockCircleOutlined, CopyOutlined, HistoryOutlined, DeleteOutlined } from '@ant-design/icons';
import VirtualList from 'rc-virtual-list';
import { QueryHistory } from '../types';
import './HistoryDrawer.less';

const { Text } = Typography;

interface HistoryDrawerProps {
  visible: boolean;
  onClose: () => void;
  queryHistory: QueryHistory[];
  loadFromHistory: (historySql: string) => void;
  copyToClipboard: (text: string) => void;
  clearHistory: () => void;
  clearAllHistory?: () => void; // 新增清除所有历史的方法
  fullscreen: boolean;
}

const HistoryDrawer: React.FC<HistoryDrawerProps> = ({
  visible,
  onClose,
  queryHistory,
  loadFromHistory,
  copyToClipboard,
  clearHistory,
  clearAllHistory,
  fullscreen
}) => {
  return (
    <Drawer
      title={
        <Space>
          <HistoryOutlined />
          <span>查询历史</span>
          <Tag color="blue">{queryHistory.length} 条记录</Tag>
          <Space>
            <Popconfirm
              title="确定要清除当前数据源的历史记录吗？"
              okText="确定"
              cancelText="取消"
              onConfirm={(e) => {
                e?.stopPropagation();
                clearHistory();
              }}
            >
              <Button 
                type="text" 
                danger 
                size="small"
                icon={<DeleteOutlined />}
                onClick={(e) => e.stopPropagation()}
              >
                清除当前历史
              </Button>
            </Popconfirm>
            
            {clearAllHistory && (
              <Popconfirm
                title="确定要清除所有数据源的历史记录吗？此操作不可恢复！"
                okText="确定"
                cancelText="取消"
                onConfirm={(e) => {
                  e?.stopPropagation();
                  clearAllHistory();
                }}
              >
                <Button 
                  type="text" 
                  danger 
                  size="small"
                  onClick={(e) => e.stopPropagation()}
                >
                  清除所有历史
                </Button>
              </Popconfirm>
            )}
          </Space>
        </Space>
      }
      width={600}
      open={visible}
      onClose={onClose}
    >
      <div className="history-list">
        {queryHistory.length > 0 ? (
          <VirtualList
            data={queryHistory}
            height={fullscreen ? window.innerHeight - 120 : 500}
            itemHeight={120}
            itemKey="id"
          >
            {(history) => (
              <div 
                key={history.id} 
                className={`history-item ${history.status === 'error' ? 'error-history' : ''}`}
                onClick={() => loadFromHistory(history.sql)}
              >
                <div className="history-item-header">
                  <Space>
                    <ClockCircleOutlined style={{ color: '#1890ff' }}/>
                    <Text>{new Date(history.timestamp).toLocaleString()}</Text>
                  </Space>
                  <Space>
                    {history.status === 'success' ? (
                      <Badge status="success" text={<Text className="history-success">成功</Text>} />
                    ) : (
                      <Badge status="error" text={<Text className="history-error">失败</Text>} />
                    )}
                    {history.status === 'success' && history.executionTime && (
                      <Text type="secondary">耗时: {history.executionTime} ms</Text>
                    )}
                    <Button 
                      type="text" 
                      size="small" 
                      icon={<CopyOutlined />} 
                      onClick={(e) => {
                        e.stopPropagation();
                        copyToClipboard(history.sql);
                      }}
                    />
                  </Space>
                </div>
                <div className="history-sql">{history.sql}</div>
                {history.status === 'error' && history.message && (
                  <Alert 
                    message={history.message} 
                    type="error" 
                    showIcon 
                    style={{ marginTop: 8 }} 
                  />
                )}
              </div>
            )}
          </VirtualList>
        ) : (
          <Empty description="暂无查询历史记录" />
        )}
      </div>
    </Drawer>
  );
};

export default HistoryDrawer;
