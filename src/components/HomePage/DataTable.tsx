import { Table, Divider, Tabs, Card, Tag, Spin, Empty, Typography, Space, Tooltip, Button } from 'antd';
import { LoadingOutlined, CopyOutlined, InfoCircleOutlined, SearchOutlined, TableOutlined, DatabaseOutlined } from '@ant-design/icons';
import { LogData } from '../../types/logDataTypes';
import { useState, useCallback, useRef, useMemo, memo } from 'react';
import ResizeObserver from 'rc-resize-observer';
import { VirtualList } from '../common/VirtualList';
import { memoize, chunkProcess } from '../../utils/logDataHelpers';

const { Text } = Typography;

// 将单元格渲染为独立组件以减少不必要的重新渲染
const TableCell = memo(({ text, field, searchQuery }: { 
  text: string | number, 
  field: string, 
  searchQuery: string 
}) => {
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
});

// 使用 memo 优化卡片视图中的单个卡片渲染
const JsonCard = memo(({ 
  record, 
  isActive, 
  onCardClick, 
  onCopy 
}: { 
  record: LogData, 
  isActive: boolean,
  onCardClick: () => void,
  onCopy: (e: React.MouseEvent) => void
}) => (
  <Card 
    key={record.key} 
    className={`json-card ${isActive ? 'active-card' : ''}`}
    size="small"
    onClick={onCardClick}
    extra={
      <Button
        icon={<CopyOutlined />}
        size="small"
        onClick={onCopy}
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
            {String(record.status)}
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
));

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
  // 使用字符串数组存储展开的行的key
  const [expandedRowKeys, setExpandedRowKeys] = useState<React.Key[]>([]);
  // 添加一个useRef跟踪当前视图中的数据，避免不必要的重新渲染
  const dataRef = useRef<LogData[]>(data);
  
  // 当数据变化时更新ref，但不触发重新渲染
  if (dataRef.current !== data) {
    dataRef.current = data;
  }
  // 缓存列宽计算函数
  const getColumnWidth = useMemo(() => memoize((field: string, width: number) => {
    if (!width) return undefined;
    
    // 根据字段类型和表格宽度分配不同的宽度比例
    if (field === 'timestamp') return 160;
    if (field === 'status') return 80;
    if (field === 'message') return Math.max(300, width * 0.4);
    if (field === 'host' || field === 'source') return 120;
    
    // 默认宽度
    return 150;
  }), []);

  // 生成表格列配置，并使用useMemo缓存结果
  const tableColumns = useMemo(() => {
    return selectedFields.map(field => ({
      title: field,
      dataIndex: field,
      key: field,
      width: getColumnWidth(field, tableWidth),
      ellipsis: true,
      render: (text: string | number) => (
        <TableCell text={text} field={field} searchQuery={searchQuery} />
      ),
      className: `table-column ${field === lastAddedField ? 'column-fade-in' : ''}`,
      onHeaderCell: () => ({
        className: field === lastAddedField ? 'column-fade-in' : '',
      }),
      onCell: (record: LogData) => ({
        className: `${field === lastAddedField ? 'column-fade-in' : ''} 
                   ${record.key === activeRowKey ? 'active-row-cell' : ''}`,
      }),
      sorter: (a: LogData, b: LogData) => {
        const valA = a[field];
        const valB = b[field];
        
        if (typeof valA === 'number' && typeof valB === 'number') {
          return valA - valB;
        }
        
        return String(valA || '').localeCompare(String(valB || ''));
      }
    }));
  }, [selectedFields, lastAddedField, activeRowKey, searchQuery, tableWidth, getColumnWidth]);

  // 复制当前行数据到剪贴板
  const copyRowData = useCallback((record: LogData) => {
    const jsonData = JSON.stringify(record, null, 2);
    navigator.clipboard.writeText(jsonData).then(() => {
      // 可以添加一个消息提示复制成功
    });
  }, []);

  // 缓存空状态渲染
  const emptyState = useMemo(() => {
    const description = (
      <div>
        <Typography.Title level={4} style={{ marginBottom: 16 }}>
          未找到匹配的数据
        </Typography.Title>
      </div>
    );

    return (
      <div className="custom-empty-state">
        <Empty
          image={Empty.PRESENTED_IMAGE_SIMPLE}
          description={description}
        />
      </div>
    );
  }, [searchQuery]);

  // 处理行展开状态改变
  const handleExpand = useCallback((expanded: boolean, record: LogData) => {
    const key = record.key as React.Key;
    console.log('handleExpand', expanded, record);
    if (expanded) {

      // 只展开当前行，不影响其他行
      setExpandedRowKeys([key]);
      setActiveRowKey(record.key as string);
    } else {
      // 收起当前行
      setExpandedRowKeys([]);
      setActiveRowKey(null);
    }
  }, []);

  // 单独处理行点击事件
  const handleRowClick = useCallback((record: LogData) => {
    const key = record.key as React.Key;
    const isCurrentExpanded = expandedRowKeys.includes(key);
    
    if (isCurrentExpanded) {
      // 如果当前行已展开，则收起
      setExpandedRowKeys([]);
      setActiveRowKey(null);
    } else {
      // 如果当前行未展开，则展开它并收起其他行
      setExpandedRowKeys([key]);
      setActiveRowKey(key as string);
    }
  }, [expandedRowKeys]);

  // 使用useMemo优化渲染大型JSON卡片列表
  const jsonCards = useMemo(() => (
    data.map(record => (
      <JsonCard
        key={record.key}
        record={record}
        isActive={record.key === activeRowKey}
        onCardClick={() => setActiveRowKey(record.key === activeRowKey ? null : record.key)}
        onCopy={(e) => {
          e.stopPropagation();
          copyRowData(record);
        }}
      />
    ))
  ), [data, activeRowKey, copyRowData]);

  // 包装onScroll函数，利用requestAnimationFrame优化滚动性能
  const handleScroll = useCallback((e: React.UIEvent<HTMLDivElement>) => {
    if (!loading) {
      requestAnimationFrame(() => {
        onScroll(e);
      });
    }
  }, [loading, onScroll]);

  console.log(222,data)

  return (
    <div 
      className="table-container-with-animation"
      style={{ 
        height: 'calc(100vh - 165px)', 
        overflowY: 'auto',
        position: 'relative'
      }} 
      onScroll={handleScroll}
      ref={tableContainerRef}
    >
      <ResizeObserver
        onResize={({ width }) => {
          setTableWidth(width);
        }}
      >
        <div className="resize-container">
          {viewMode === 'table' ? (
            data.length > 0 ? (
              data.length > 300 ? (
                // 数据量大时使用虚拟列表优化渲染性能
                <VirtualList
                  data={data}
                  columns={tableColumns}
                  itemHeight={53} // 表格行高，根据实际需要调整
                  expandedRowKeys={expandedRowKeys}
                  onRowClick={handleRowClick}
                  activeRowKey={activeRowKey}
                  expandRowRender={(record) => (
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
                              .map(([key, value], index) => ({ 
                                key: `${record.key}_${key}_${index}`, 
                                field: key, 
                                value: String(value) 
                              }))} 
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
                            rowKey="key"
                          />
                        </Tabs.TabPane>
                      </Tabs>
                    </div>
                  )}
                />
              ) : (
                // 数据量小的时候使用普通Table
                <Table 
                  dataSource={data} 
                  columns={tableColumns} 
                  pagination={false}
                  size="middle"
                  scroll={{ x: 'max-content' }}
                  rowKey="key"
                  expandable={{
                    expandedRowKeys: expandedRowKeys,
                    onExpand: handleExpand,
                    expandRowByClick: false,
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
                                .map(([key, value], index) => ({ 
                                  key: `${record.key}_${key}_${index}`, 
                                  field: key, 
                                  value: String(value) 
                                }))} 
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
                              rowKey="key"
                            />
                          </Tabs.TabPane>
                        </Tabs>
                      </div>
                    )
                  }}
                  className="data-table-with-animation"
                  rowClassName={(record) => record.key === activeRowKey ? 'active-table-row' : ''}
                  onRow={(record) => ({
                    onClick: () => handleRowClick(record),
                    className: record.key === activeRowKey ? 'active-table-row' : ''
                  })}
                  sticky={{ offsetHeader: 0 }}
                />
              )
            ) : (
              !loading && emptyState
            )
          ) : (
            <div className="json-card-view">
              {data.length > 0 ? (
                data.length > 100 ? (
                  // 数据量大时使用虚拟滚动优化JSON卡片视图
                  <VirtualList
                    data={data}
                    itemHeight={200} // 卡片高度，根据实际需要调整
                    // 自定义渲染函数
                    renderItem={(record: LogData) => (
                      <JsonCard
                        key={record.key}
                        record={record}
                        isActive={record.key === activeRowKey}
                        onCardClick={() => setActiveRowKey(record.key === activeRowKey ? null : record.key)}
                        onCopy={(e) => {
                          e.stopPropagation();
                          copyRowData(record);
                        }}
                      />
                    )}
                  />
                ) : (
                  // 数据量较小时直接渲染所有卡片
                  jsonCards
                )
              ) : (
                !loading && emptyState
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
