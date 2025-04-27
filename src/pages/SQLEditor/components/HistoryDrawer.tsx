import { Drawer, Space, Tag, Empty, Badge, Button, Typography, Alert } from 'antd';
import { ClockCircleOutlined, CopyOutlined, HistoryOutlined } from '@ant-design/icons';
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
  fullscreen: boolean;
}

const HistoryDrawer: React.FC<HistoryDrawerProps> = ({
  visible,
  onClose,
  queryHistory,
  loadFromHistory,
  copyToClipboard,
  fullscreen
}) => {
  return (
    <Drawer
      title={
        <Space>
          <HistoryOutlined />
          <span>查询历史</span>
          <Tag color="blue">{queryHistory.length} 条记录</Tag>
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
                    <Text>{history.timestamp}</Text>
                  </Space>
                  <Space>
                    {history.status === 'success' ? (
                      <Badge status="success" text={<Text className="history-success">成功</Text>} />
                    ) : (
                      <Badge status="error" text={<Text className="history-error">失败</Text>} />
                    )}
                    {history.status === 'success' && (
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