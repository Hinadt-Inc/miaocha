import { Button, Breadcrumb, Space } from 'antd';
import { PlusOutlined, SyncOutlined, HomeOutlined, PlayCircleOutlined, StopOutlined } from '@ant-design/icons';
import { Link } from 'react-router-dom';

interface LogstashPageHeaderProps {
  loading: boolean;
  onAdd: () => void;
  onReload: () => void;
  className?: string;
  // Batch operation props
  selectedInstanceIds?: number[];
  onBatchStart?: () => void;
  onBatchStop?: () => void;
  batchLoading?: boolean;
}

const LogstashPageHeader = ({ 
  loading, 
  onAdd, 
  onReload, 
  className,
  selectedInstanceIds = [],
  onBatchStart,
  onBatchStop,
  batchLoading = false
}: LogstashPageHeaderProps) => {
  const hasSelection = selectedInstanceIds.length > 0;

  return (
    <>
      <Breadcrumb
        items={[
          {
            title: (
              <Link to="/">
                <HomeOutlined />
              </Link>
            ),
          },
          { title: 'Logstash管理' },
        ]}
      />
      <div className={className || 'table-toolbar'}>
        <Space>
          <Button type="default" icon={<SyncOutlined />} onClick={onReload} loading={loading}>
            刷新
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={onAdd}>
            新增Logstash进程
          </Button>
        </Space>
        
        {hasSelection && (
          <Space style={{ marginLeft: 16 }}>
            <span style={{ color: '#666', fontSize: '14px' }}>
              已选择 {selectedInstanceIds.length} 个实例
            </span>
            <Button
              type="primary"
              icon={<PlayCircleOutlined />}
              onClick={onBatchStart}
              loading={batchLoading}
              disabled={!hasSelection}
            >
              批量启动
            </Button>
            <Button
              danger
              icon={<StopOutlined />}
              onClick={onBatchStop}
              loading={batchLoading}
              disabled={!hasSelection}
            >
              批量停止
            </Button>
          </Space>
        )}
      </div>
    </>
  );
};

export default LogstashPageHeader;
