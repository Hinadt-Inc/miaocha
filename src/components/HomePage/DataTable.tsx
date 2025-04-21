import { Table, Divider, Tabs, Card, Tag, Spin } from 'antd';
import { LoadingOutlined } from '@ant-design/icons';
import { LogData } from '../../types/logDataTypes';

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
  const getTableColumns = () => {
    return selectedFields.map(field => ({
      title: field,
      dataIndex: field,
      key: field,
      render: (text: string | number) => {
        const searchQueryLower = searchQuery.toLowerCase();
        const textStr = String(text);
        
        if (field === 'timestamp') {
          return <span style={{ color: '#1890ff' }}>{textStr}</span>;
        }
        if (field === 'status') {
          const status = Number(text);
          return <Tag color={status === 200 ? 'green' : (status === 404 ? 'orange' : 'red')}>{textStr}</Tag>;
        }
        
        if (searchQuery && textStr.toLowerCase().includes(searchQueryLower)) {
          const parts = textStr.split(new RegExp(`(${searchQuery})`, 'gi'));
          return ( 
            <span>
              {parts.map((part, i) => 
                part.toLowerCase() === searchQueryLower ? (
                  <span key={i} className="highlight-text">{part}</span>
                ) : (
                  part
                )
              )}
            </span>
          );
        }
        
        return textStr;
      },
      className: `table-column ${field === lastAddedField ? 'column-fade-in' : ''}`,
      onHeaderCell: () => ({
        className: field === lastAddedField ? 'column-fade-in' : '',
      }),
      onCell: () => ({
        className: field === lastAddedField ? 'column-fade-in' : '',
      })
    }));
  };

  return (
    <div className="table-container-with-animation" 
      style={{ 
        height: 'calc(100vh - 165px)', 
        overflowY: 'auto' 
      }} 
      onScroll={onScroll}
    >
      {viewMode === 'table' ? (
        <Table 
          dataSource={data} 
          columns={getTableColumns()} 
          pagination={false}
          size="small"
          expandable={{
            expandedRowRender: (record: LogData) => (
              <div style={{ padding: 16 }}>
                <Tabs defaultActiveKey="json">
                  <Tabs.TabPane tab="JSON" key="json">
                    <pre style={{ background: '#f6f8fa', padding: 16, borderRadius: 4 }}>
                      {JSON.stringify(record, null, 2)}
                    </pre>
                  </Tabs.TabPane>
                  <Tabs.TabPane tab="表格" key="table">
                    <Table 
                      dataSource={Object.entries(record)
                        .filter(([key]) => key !== 'key')
                        .map(([key, value]) => ({ key, field: key, value }))} 
                      columns={[
                        { title: '字段', dataIndex: 'field', key: 'field' },
                        { title: '值', dataIndex: 'value', key: 'value' }
                      ]} 
                      pagination={false}
                      size="small"
                    />
                  </Tabs.TabPane>
                </Tabs>
              </div>
            )
          }}
          rowKey="key"
          className="data-table-with-animation"
        />
      ) : (
        <div style={{ overflowY: 'auto', height: 'calc(100vh - 215px)', padding: '0 16px' }}>
          {data.map(record => (
            <Card 
              key={record.key} 
              style={{ margin: '8px 0', borderLeft: '4px solid #1890ff' }} 
              size="small"
            >
              <div style={{ marginBottom: 8 }}>
                <Tag color="blue">{record.timestamp}</Tag>
                <Divider type="vertical" />
                <Tag color={record.status === 200 ? 'green' : (record.status === 404 ? 'orange' : 'red')}>
                  {record.status}
                </Tag>
                <Divider type="vertical" />
                <span>{record.host}</span>
              </div>
              <pre style={{ background: '#f6f8fa', padding: 16, borderRadius: 4, maxHeight: 200, overflow: 'auto' }}>
                {JSON.stringify(record, null, 2)}
              </pre>
            </Card>
          ))}
        </div>
      )}
      {loading && (
        <div style={{ textAlign: 'center', padding: 16 }}>
          <Spin indicator={<LoadingOutlined style={{ fontSize: 24 }} spin />} />
        </div>
      )}
      {!hasMore && data.length > 0 && (
        <div style={{ textAlign: 'center', padding: 16, color: '#999' }}>
          已加载全部数据
        </div>
      )}
    </div>
  );
};
