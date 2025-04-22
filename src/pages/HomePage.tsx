import { useState, useCallback, useEffect } from 'react';
import { Layout, Space, Button, Modal, Form, Input, Select, Tag } from 'antd';
import { getOperatorsByFieldType, getFieldTypeColor } from '../utils/logDataHelpers';
import { 
  CompressOutlined,
  ExpandOutlined,
  PlusOutlined
} from '@ant-design/icons';
import { useLogData } from '../hooks/useLogData';
import { useFilters } from '../hooks/useFilters';
import { SearchBar } from '../components/HomePage/SearchBar';
import { FilterPanel } from '../components/HomePage/FilterPanel';
import { DataTable } from '../components/HomePage/DataTable';
import { HistogramChart } from '../components/HomePage/HistogramChart';
import { FieldSelector } from '../components/HomePage/FieldSelector';
import { getMyTablePermissions } from '../api/permission';

interface TableColumn {
  columnName: string;
  dataType: string;
  columnComment?: string;
  isPrimaryKey?: boolean;
  isNullable?: boolean;
}

interface TablePermission {
  tableName: string;
  tableComment?: string;
  columns?: TableColumn[];
}

import './HomePage.less';

const { Content, Sider } = Layout;

export default function HomePage() {
  const [form] = Form.useForm();
  const [collapsed, setCollapsed] = useState(false);
  const [selectedFields, setSelectedFields] = useState<string[]>(['timestamp', 'message', 'host', 'source']);
  const [viewMode, setViewMode] = useState<'table' | 'json'>('table');
  const [searchQuery, setSearchQuery] = useState('');
  const [showHistogram, setShowHistogram] = useState(true);
  const [timeRange, setTimeRange] = useState<[string, string] | null>(null);
  const [selectedTable, setSelectedTable] = useState<string>(''); // 保留以兼容FieldSelector组件
  const [lastAddedField, setLastAddedField] = useState<string | null>(null);
  const [lastRemovedField, setLastRemovedField] = useState<string | null>(null);
  const [availableTables, setAvailableTables] = useState<Array<{
    datasourceId: number;
    datasourceName: string;
    databaseName: string;
    tables: Array<{
      tableName: string;
      tableComment: string;
      columns: Array<{
        columnName: string;
        dataType: string;
        columnComment: string;
        isPrimaryKey: boolean;
        isNullable: boolean;
      }>;
    }>;
  }>>([]);

  useEffect(() => {
    const fetchTablePermissions = async () => {
      try {
        const data = await getMyTablePermissions();
        // 转换接口数据格式
        const transformedData = data.map(ds => ({
          datasourceId: ds.datasourceId,
          datasourceName: ds.datasourceName,
          databaseName: ds.databaseName,
          tables: ds.tables.map((table: TablePermission) => ({
            tableName: table.tableName,
            tableComment: table.tableComment || '',
            columns: (table.columns || []).map((col: TableColumn) => ({
              columnName: col.columnName,
              dataType: col.dataType,
              columnComment: col.columnComment || '',
              isPrimaryKey: col.isPrimaryKey || false,
              isNullable: col.isNullable || false
            }))
          }))
        }));
        setAvailableTables(transformedData);
      } catch (error) {
        console.error('获取表权限失败:', error);
      }
    };
    fetchTablePermissions();
  }, []);

  const { tableData, loading, hasMore, loadMoreData } = useLogData(searchQuery);
  const { 
    filters,
    showFilterModal,
    setShowFilterModal,
    selectedFilterField,
    openFilterModal,
    handleFilterFieldChange,
    addFilter,
    removeFilter
  } = useFilters();

  const toggleFieldSelection = (fieldName: string) => {
    if (selectedFields.includes(fieldName)) {
      setLastRemovedField(fieldName);
      setLastAddedField(null);
      setSelectedFields(selectedFields.filter(f => f !== fieldName));
    } else {
      setLastAddedField(fieldName);
      setLastRemovedField(null);
      setSelectedFields([...selectedFields, fieldName]);
    }
    
    setTimeout(() => {
      setLastAddedField(null);
      setLastRemovedField(null);
    }, 5000);
  };

  const handleScroll = useCallback((e: React.UIEvent<HTMLDivElement>) => {
    const { scrollTop, clientHeight, scrollHeight } = e.currentTarget;
    if (scrollHeight - scrollTop - clientHeight < 100 && !loading && hasMore) {
      loadMoreData();
    }
  }, [loadMoreData, loading, hasMore]);

  const availableFields = [
    { name: 'timestamp', type: 'date' },
    { name: 'message', type: 'text' },
    { name: 'host', type: 'keyword' },
    { name: 'source', type: 'keyword' },
    { name: 'user_agent', type: 'text' },
    { name: 'status', type: 'number' },
    { name: 'bytes', type: 'number' },
    { name: 'response_time', type: 'number' },
    { name: 'ip', type: 'ip' },
    { name: 'method', type: 'keyword' },
    { name: 'path', type: 'keyword' },
    { name: 'referer', type: 'text' },
    { name: 'geo.country', type: 'keyword' },
    { name: 'geo.city', type: 'keyword' },
  ];

  return (
    <>
    <Layout className="layout-main">
      <SearchBar 
        searchQuery={searchQuery}
        timeRange={timeRange}
        onSearch={setSearchQuery}
        onTimeRangeChange={setTimeRange}
      />
      
      <FilterPanel 
        filters={filters}
        onRemoveFilter={removeFilter}
        onAddFilter={openFilterModal}
      />
      
      <Layout className="layout-content">
        <Sider 
          width={280} 
          theme="light" 
          collapsible 
          collapsed={collapsed} 
          onCollapse={setCollapsed}
          className="sider-container"
        >
          <FieldSelector
            availableFields={availableFields}
            selectedFields={selectedFields}
            onToggleField={toggleFieldSelection}
            lastAddedField={lastAddedField}
            lastRemovedField={lastRemovedField}
            availableTables={availableTables}
            onTableChange={setSelectedTable}
            collapsed={collapsed}
          />
        </Sider>
        
        <Layout className="layout-inner">
          <HistogramChart
            show={showHistogram}
            onTimeRangeChange={setTimeRange}
            onToggle={() => setShowHistogram(false)}
          />
          
            <Content className="content-container">
            <div className="table-header">
              <div>找到 {tableData.length} 条记录</div>
              <Space>
                {!showHistogram && (
                  <Button 
                    size="small" 
                    type="text" 
                    icon={<PlusOutlined />} 
                    onClick={() => setShowHistogram(true)}
                  >
                    显示直方图
                  </Button>
                )}
                <Space.Compact>
                  <Button 
                    type={viewMode === 'table' ? 'primary' : 'default'} 
                    icon={<CompressOutlined />} 
                    onClick={() => setViewMode('table')}
                  />
                  <Button 
                    type={viewMode === 'json' ? 'primary' : 'default'} 
                    icon={<ExpandOutlined />} 
                    onClick={() => setViewMode('json')}
                  />
                </Space.Compact>
              </Space>
            </div>
            
            <DataTable
              data={tableData}
              loading={loading}
              hasMore={hasMore}
              selectedFields={selectedFields}
              searchQuery={searchQuery}
              viewMode={viewMode}
              onScroll={handleScroll}
              lastAddedField={lastAddedField}
            />
          </Content>
        </Layout>
      </Layout>
    </Layout>

    {/* 添加过滤器对话框 */}
    <Modal
      title="添加过滤器"
      open={showFilterModal}
      onCancel={() => {
        setShowFilterModal(false);
      }}
      onOk={() => {
        form.validateFields().then(values => {
          addFilter(values);
          form.resetFields();
        });
      }}
      okText="添加"
      cancelText="取消"
    >
      <Form
        form={form}
        layout="vertical"
        initialValues={{ operator: 'is' }}
      >
        <Form.Item 
          name="field" 
          label="字段"
          rules={[{ required: true, message: '请选择字段' }]}
        >
          <Select
            placeholder="选择字段"
            onChange={handleFilterFieldChange}
            showSearch
            optionFilterProp="children"
          >
            {availableFields.map(field => (
              <Select.Option key={field.name} value={field.name}>
                <Space>
                  <Tag color={getFieldTypeColor(field.type)} style={{ marginRight: 0 }}>
                    {field.type.slice(0, 1).toUpperCase()}
                  </Tag>
                  {field.name}
                </Space>
              </Select.Option>
            ))}
          </Select>
        </Form.Item>
        
        <Form.Item 
          name="operator" 
          label="操作符"
          rules={[{ required: true, message: '请选择操作符' }]}
        >
          <Select 
            placeholder="选择操作符"
          >
            {getOperatorsByFieldType(selectedFilterField ? 
              availableFields.find(f => f.name === selectedFilterField)?.type : undefined)
              .map(op => (
                <Select.Option key={op.value} value={op.value}>{op.label}</Select.Option>
              ))
            }
          </Select>
        </Form.Item>
        
        <Form.Item 
          name="value" 
          label="值"
        >
          <Input placeholder="输入值" />
        </Form.Item>
      </Form>
    </Modal>
    </>
  );
}
