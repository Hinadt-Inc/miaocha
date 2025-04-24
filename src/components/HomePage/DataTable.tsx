import { Table, Divider, Tabs, Card, Tag, Spin, Empty, Typography, Space, Tooltip, Button } from 'antd';
import { LoadingOutlined, CopyOutlined, InfoCircleOutlined, SearchOutlined, TableOutlined, DatabaseOutlined } from '@ant-design/icons';
import { LogData } from '../../types/logDataTypes';
import { useState, useCallback, useRef } from 'react';
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
  // 使用字符串数组存储展开的行的key
  const [expandedRowKeys, setExpandedRowKeys] = useState<React.Key[]>([]);
  console.log('expandedRowKeys', expandedRowKeys);

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
    console.log('getTableColumns', selectedFields);
    return selectedFields.map(field => ({
      title: field,
      dataIndex: field,
      width: getColumnWidth(field),
      ellipsis: true,
      render: (text: string | number) => renderCell(text, field),
      // className: `table-column ${field === lastAddedField ? 'column-fade-in' : ''}`,
      // onHeaderCell: () => ({
      //   className: field === lastAddedField ? 'column-fade-in' : '',
      // }),
      // onCell: (record: LogData) => ({
      //   className: `${field === lastAddedField ? 'column-fade-in' : ''} ${record.key === activeRowKey ? 'active-row-cell' : ''}`,
      // }),
      // sorter: (a: LogData, b: LogData) => {
      //   const valA = a[field];
      //   const valB = b[field];
        
      //   if (typeof valA === 'number' && typeof valB === 'number') {
      //     return valA - valB;
      //   }
        
      //   return String(valA).localeCompare(String(valB));
      // },
    }));
  }, [selectedFields, lastAddedField, activeRowKey, renderCell, getColumnWidth]);

  // 复制当前行数据到剪贴板
  const copyRowData = useCallback((record: LogData) => {
    const jsonData = JSON.stringify(record, null, 2);
    navigator.clipboard.writeText(jsonData).then(() => {
      // 可以添加一个消息提示复制成功
    });
  }, []);

  const renderEmptyState = (): React.ReactNode => {
    const description = (
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
        </Space>
      </div>
    );

    return (
      <div className="custom-empty-state">
        <Empty
          image={Empty.PRESENTED_IMAGE_SIMPLE}
          imageStyle={{ height: 80 }}
          description={description}
        />
      </div>
    );
  };

  // 处理行展开状态改变
  const handleExpand = (expanded: boolean, record: LogData) => {
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
  };

  // 单独处理行点击事件
  const handleRowClick = (record: LogData) => {
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
  };

  console.log(222,data)

  return (
    <div 
      className="table-container-with-animation"
      style={{ 
        height: 'calc(100vh - 165px)', 
        overflowY: 'auto',
        position: 'relative'
      }} 
      onScroll={onScroll}
      ref={tableContainerRef}
    >
      {/* {renderTableToolbar()} */}
      
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
                key={tableWidth}
                expandable={{
                  // 关键配置：控制哪些行被展开
                  // expandedRowKeys: expandedRowKeys,
                  // 处理展开/收起事件
                  // onExpand: handleExpand,
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
                                key: `${record.key}_${key}_${index}_${Date.now()}`, // 增强唯一性
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
                rowKey="_id"
                className="data-table-with-animation"
                // rowClassName={(record) => record.key === activeRowKey ? 'active-table-row' : ''}
                // onRow={(record) => ({
                //   onClick: () => handleRowClick(record),
                //   className: record.key === activeRowKey ? 'active-table-row' : ''
                // })}
                // sticky={{ offsetHeader: 0 }}
              />
            ) : (
              !loading && (renderEmptyState() as React.ReactNode)
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
              )) : (
                !loading && (renderEmptyState() as React.ReactNode)
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
