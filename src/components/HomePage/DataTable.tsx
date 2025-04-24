import { Table, Divider, Tabs, Card, Tag, Spin, Empty, Typography, Space, Tooltip, Button } from 'antd';
import { LoadingOutlined, DownloadOutlined, CopyOutlined, FullscreenOutlined, InfoCircleOutlined, SearchOutlined, TableOutlined, DatabaseOutlined } from '@ant-design/icons';
import { LogData } from '../../types/logDataTypes';
import { useState, useCallback, useRef, useEffect } from 'react';
import ResizeObserver from 'rc-resize-observer';

const { Text } = Typography;

interface DataTableProps {
  data: LogData[];
  loading: boolean;
  hasMore: boolean;
  selectedFields: string[];
  searchQuery: string;
  viewMode: 'table' | 'json';
  onScroll: (e: React.UIEvent<HTMLDivElement>) => void;
  lastAddedField: string | null;
}

export const DataTable = ({
  data,
  loading,
  hasMore,
  selectedFields,
  searchQuery,
  viewMode,
  onScroll,
  lastAddedField
}: DataTableProps) => {
  const [tableWidth, setTableWidth] = useState<number>(0);
  const [activeRowKey, setActiveRowKey] = useState<string | null>(null);
  const tableContainerRef = useRef<HTMLDivElement>(null);
  const [isFullscreen, setIsFullscreen] = useState(false);

  // 自适应列宽
  const getColumnWidth = useCallback((field: string) => {
    if (!tableWidth) return undefined;
    
    // 根据字段类型和表格宽度分配不同的宽度比例
    if (field === 'timestamp') return 160;
    if (field === 'status') return 80;
    if (field === 'message') return Math.max(300, tableWidth * 0.4);
    if (field === 'host' || field === 'source') return 120;
    
    // 默认宽度
    return 150;
  }, [tableWidth]);

  // 处理表格单元格的渲染逻辑
  const renderCell = useCallback((text: string | number, field: string) => {
    const searchQueryLower = searchQuery.toLowerCase();
    const textStr = String(text || '');
    
    if (textStr === '' || textStr === 'undefined' || textStr === 'null') {
      return <Text type="secondary" italic>空</Text>;
    }
    
    if (field === 'timestamp') {
      return (
        <Tooltip title={textStr}>
          <Text style={{ color: '#1890ff' }}>{textStr}</Text>
        </Tooltip>
      );
    }
    
    if (field === 'status') {
      const status = Number(text);
      let color = 'default';
      const statusText = textStr;
      
      if (status >= 200 && status < 300) color = 'success';
      else if (status >= 300 && status < 400) color = 'processing';
      else if (status >= 400 && status < 500) color = 'warning';
      else if (status >= 500) color = 'error';
      
      return <Tag color={color}>{statusText}</Tag>;
    }
    
    // 高亮搜索关键词
    if (searchQuery && textStr.toLowerCase().includes(searchQueryLower)) {
      const parts = textStr.split(new RegExp(`(${searchQuery})`, 'gi'));
      return ( 
        <Text ellipsis={{ tooltip: textStr }}>
          {parts.map((part, i) => 
            part.toLowerCase() === searchQueryLower ? (
              <Text key={i} className="highlight-text" mark>{part}</Text>
            ) : (
              <Text key={i}>{part}</Text>
            )
          )}
        </Text>
      );
    }
    
    // 处理长文本省略
    if (textStr.length > 100 && (field === 'message' || field.includes('text'))) {
      return (
        <Text ellipsis={{ tooltip: textStr }}>
          {textStr}
        </Text>
      );
    }
    
    return textStr;
  }, [searchQuery]);

  // 生成表格列配置
  const getTableColumns = useCallback(() => {
    return selectedFields.map(field => ({
      title: (
        <Space>
          {field}
          <Tooltip title={`字段: ${field}`}>
            <InfoCircleOutlined style={{ fontSize: '12px', color: '#8c8c8c' }} />
          </Tooltip>
        </Space>
      ),
      dataIndex: field,
      key: field,
      width: getColumnWidth(field),
      ellipsis: true,
      render: (text: string | number) => renderCell(text, field),
      className: `table-column ${field === lastAddedField ? 'column-fade-in' : ''}`,
      onHeaderCell: () => ({
        className: field === lastAddedField ? 'column-fade-in' : '',
      }),
      onCell: (record: LogData) => ({
        className: `${field === lastAddedField ? 'column-fade-in' : ''} ${record.key === activeRowKey ? 'active-row-cell' : ''}`,
      }),
      sorter: (a: LogData, b: LogData) => {
        const valA = a[field];
        const valB = b[field];
        
        if (typeof valA === 'number' && typeof valB === 'number') {
          return valA - valB;
        }
        
        return String(valA).localeCompare(String(valB));
      },
    }));
  }, [selectedFields, lastAddedField, activeRowKey, renderCell, getColumnWidth]);

  // 切换全屏显示
  const toggleFullscreen = useCallback(() => {
    if (!isFullscreen) {
      const element = tableContainerRef.current;
      if (element) {
        if (element.requestFullscreen) {
          element.requestFullscreen();
        }
      }
    } else {
      if (document.exitFullscreen) {
        document.exitFullscreen();
      }
    }
  }, [isFullscreen]);

  // 监听全屏状态变化
  useEffect(() => {
    const handleFullscreenChange = () => {
      setIsFullscreen(!!document.fullscreenElement);
    };

    document.addEventListener('fullscreenchange', handleFullscreenChange);
    return () => {
      document.removeEventListener('fullscreenchange', handleFullscreenChange);
    };
  }, []);

  // 复制当前行数据到剪贴板
  const copyRowData = useCallback((record: LogData) => {
    const jsonData = JSON.stringify(record, null, 2);
    navigator.clipboard.writeText(jsonData).then(() => {
      // 可以添加一个消息提示复制成功
    });
  }, []);

  // 表格工具栏
  const renderTableToolbar = () => (
    <div className="table-toolbar">
      <Space>
        <Button 
          icon={<FullscreenOutlined />} 
          onClick={toggleFullscreen}
          size="small"
          title="全屏显示"
        />
        <Button 
          icon={<DownloadOutlined />} 
          size="small"
          title="导出数据"
          onClick={() => {
            const jsonData = JSON.stringify(data, null, 2);
            const blob = new Blob([jsonData], { type: 'application/json' });
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `log_data_${new Date().toISOString()}.json`;
            a.click();
            URL.revokeObjectURL(url);
          }}
        />
      </Space>
    </div>
  );

  const renderEmptyState = () => (
    <div className="custom-empty-state">
      <Empty
        image={Empty.PRESENTED_IMAGE_SIMPLE}
        imageStyle={{ height: 80 }}
        description={
          <div>
            <Typography.Title level={4} style={{ marginBottom: 16 }}>
              未找到匹配的数据
            </Typography.Title>
            <Space direction="vertical" style={{ width: '100%', textAlign: 'center' }}>
              <Text type="secondary">
                {searchQuery ? 
                  '没有符合当前搜索条件的数据记录' : 
                  '当前视图没有数据可显示'}
              </Text>
              {searchQuery && (
                <div>
                  <Text type="secondary">您可以尝试：</Text>
                  <ul style={{ textAlign: 'left', display: 'inline-block', marginTop: 8 }}>
                    <li><SearchOutlined /> 修改搜索关键词</li>
                    <li><TableOutlined /> 调整选择的表或字段</li>
                    <li><DatabaseOutlined /> 检查时间范围设置</li>
                  </ul>
                </div>
              )}
            </Space>
          </div>
        }
      >
        {!searchQuery && (
          <Space direction="vertical" size="middle" style={{ marginTop: 16 }}>
            <Text type="secondary">
              请从左侧选择数据表和字段，或使用顶部搜索框进行查询
            </Text>
            <Button type="primary" icon={<SearchOutlined />}>
              开始搜索
            </Button>
          </Space>
        )}
      </Empty>
    </div>
  );

  return (
    <div 
      className={`table-container-with-animation ${isFullscreen ? 'fullscreen-table' : ''}`}
      style={{ 
        height: 'calc(100vh - 165px)', 
        overflowY: 'auto',
        position: 'relative'
      }} 
      onScroll={onScroll}
      ref={tableContainerRef}
    >
      {renderTableToolbar()}
      
      <ResizeObserver
        onResize={({ width }) => {
          setTableWidth(width);
        }}
      >
        <div className="resize-container">
          {viewMode === 'table' ? (
            data.length > 0 ? (
              <Table 
                dataSource={data} 
                columns={getTableColumns()} 
                pagination={false}
                size="middle"
                scroll={{ x: 'max-content' }}
                expandable={{
                  expandedRowRender: (record: LogData) => (
                    <div style={{ padding: 16 }}>
                      <Tabs defaultActiveKey="json">
                        <Tabs.TabPane tab="JSON" key="json">
                          <div className="json-preview-header">
                            <Button
                              icon={<CopyOutlined />}
                              size="small"
                              onClick={() => copyRowData(record)}
                              title="复制JSON数据"
                            >
                              复制
                            </Button>
                          </div>
                          <pre className="json-preview">
                            {JSON.stringify(record, null, 2)}
                          </pre>
                        </Tabs.TabPane>
                        <Tabs.TabPane tab="表格" key="table">
                          <Table 
                            dataSource={Object.entries(record)
                              .filter(([key]) => key !== 'key')
                              .map(([key, value]) => ({ key, field: key, value: String(value) }))} 
                            columns={[
                              { 
                                title: '字段', 
                                dataIndex: 'field', 
                                key: 'field',
                                width: 150
                              },
                              { 
                                title: '值', 
                                dataIndex: 'value', 
                                key: 'value',
                                render: (text) => <Text ellipsis={{ tooltip: text }}>{text}</Text>
                              }
                            ]} 
                            pagination={false}
                            size="small"
                          />
                        </Tabs.TabPane>
                      </Tabs>
                    </div>
                  ),
                  onExpand: (expanded, record) => {
                    setActiveRowKey(expanded ? record.key : null);
                  }
                }}
                rowKey="key"
                className="data-table-with-animation"
                rowClassName={(record) => record.key === activeRowKey ? 'active-table-row' : ''}
                onRow={(record) => ({
                  onClick: () => {
                    setActiveRowKey(record.key === activeRowKey ? null : record.key);
                  },
                  className: record.key === activeRowKey ? 'active-table-row' : ''
                })}
                sticky={{ offsetHeader: 0 }}
              />
            ) : (
              !loading && renderEmptyState()
            )
          ) : (
            <div className="json-card-view">
              {data.length > 0 ? data.map(record => (
                <Card 
                  key={record.key} 
                  className={`json-card ${record.key === activeRowKey ? 'active-card' : ''}`}
                  size="small"
                  onClick={() => setActiveRowKey(record.key === activeRowKey ? null : record.key)}
                  extra={
                    <Button
                      icon={<CopyOutlined />}
                      size="small"
                      onClick={(e) => {
                        e.stopPropagation();
                        copyRowData(record);
                      }}
                      type="text"
                    />
                  }
                >
                  <div className="json-card-header">
                    <Space wrap>
                      <Tag color="blue">{record.timestamp}</Tag>
                      {record.status !== undefined && (
                        <Tag color={
                          Number(record.status) >= 200 && Number(record.status) < 300 ? 'green' : 
                          Number(record.status) >= 300 && Number(record.status) < 400 ? 'blue' :
                          Number(record.status) >= 400 && Number(record.status) < 500 ? 'orange' : 'red'
                        }>
                          {record.status}
                        </Tag>
                      )}
                      {record.host && <Tag>{record.host}</Tag>}
                      {record.source && <Tag>{record.source}</Tag>}
                    </Space>
                  </div>
                  <div className="json-card-content">
                    <pre className="json-preview">
                      {JSON.stringify(
                        Object.fromEntries(
                          Object.entries(record).filter(([key]) => key !== 'key')
                        ), 
                        null, 
                        2
                      )}
                    </pre>
                  </div>
                </Card>
              )) : (
                !loading && renderEmptyState()
              )}
            </div>
          )}
        </div>
      </ResizeObserver>
      
      {loading && (
        <div className="loading-container">
          <Spin indicator={<LoadingOutlined style={{ fontSize: 24 }} spin />} />
          <div className="loading-text">加载数据中...</div>
        </div>
      )}
      
      {!hasMore && data.length > 0 && (
        <div className="end-of-data-message">
          <Divider plain>已加载全部数据</Divider>
        </div>
      )}
    </div>
  );
};
