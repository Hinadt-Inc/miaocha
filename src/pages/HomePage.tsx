import { useState, useCallback, useEffect, useMemo, useRef } from 'react';
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
import { getTableColumns } from '../api/logs';
import './HomePage.less';

const { Content, Sider } = Layout;

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

export default function HomePage() {
  const [form] = Form.useForm();
  const [collapsed, setCollapsed] = useState(false);
  const [selectedFields, setSelectedFields] = useState<string[]>(['log_time', 'message']);
  const [viewMode, setViewMode] = useState<'table' | 'json'>('table');
  const [searchQuery, setSearchQuery] = useState('');
  const [showHistogram, setShowHistogram] = useState(true);
  const [timeRange, setTimeRange] = useState<[string, string] | null>(null);
  const [selectedTable, setSelectedTable] = useState<string>('');
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
  const [availableFields, setAvailableFields] = useState<Array<{columnName: string; dataType: string}>>([]);
  const prevSelectedTable = useRef<string>('');

  useEffect(() => {
    const fetchTableColumns = async () => {
      if (!selectedTable || selectedTable === prevSelectedTable.current) return;
      prevSelectedTable.current = selectedTable;
      
      try {
        const [datasourceId, tableName] = selectedTable.split('-');
        const columns = await getTableColumns(datasourceId, tableName) as Array<{columnName: string; dataType: string}>;
        setAvailableFields(columns);
      } catch (error) {
        console.error('获取表字段失败:', error);
      }
    };
    
    fetchTableColumns();
  }, [selectedTable]);
  const searchParams = useMemo(() => ({
    datasourceId: selectedTable ? Number(selectedTable.split('-')[0]) : 1,
    tableName: selectedTable ? selectedTable.split('-')[1] : '',
    keyword: searchQuery,
    whereSql: '',
    timeRange: timeRange ? `${timeRange[0]}_${timeRange[1]}` : undefined,
    pageSize: 50,
    offset: 0,
    fields: selectedFields,
    startTime: timeRange ? timeRange[0] : '2025-04-01T16:30:00',
    endTime: timeRange ? timeRange[1] : '2025-04-01T16:40:00',
  }), [selectedTable, searchQuery, timeRange, selectedFields]);

  // 获取表权限数据
  useEffect(() => {
    const fetchTablePermissions = async () => {
      try {
        const data = await getMyTablePermissions();
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

        // 默认选择第一个数据源和第一个表
        if (data.length > 0 && data[0].tables.length > 0) {
          const defaultTable = `${data[0].datasourceId}-${data[0].tables[0].tableName}`;
          setSelectedTable(defaultTable);
        }
      } catch (error) {
        console.error('获取表权限失败:', error);
      }
    };
    fetchTablePermissions();
  }, []);

  // 表选择变化时获取字段
  useEffect(() => {
    const fetchTableColumns = async () => {
      if (!selectedTable) return;
      
      try {
        // 从级联选择器的value中解析datasourceId和tableName
        const [datasourceId, tableName] = selectedTable.split('-');
        const columns = await getTableColumns(datasourceId, tableName);
        setAvailableFields(columns.map(col => ({
          columnName: col.columnName,
          dataType: col.dataType
        })));
      } catch (error) {
        console.error('获取表字段失败:', error);
      }
    };
    
    fetchTableColumns();
  }, [selectedTable]);

  const { tableData, loading, hasMore, loadMoreData, distributionData = [] } = useLogData({
    ...searchParams,
    tableName: selectedTable ? selectedTable.split('-')[1] : '',
    datasourceId: selectedTable ? Number(selectedTable.split('-')[0]) : 1,
    fields: selectedFields
  });
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
            selectedTable={selectedTable}
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
          {(distributionData && distributionData.length > 0) && (
            <HistogramChart
              show={showHistogram}
              onTimeRangeChange={setTimeRange}
              onToggle={() => setShowHistogram(false)}
              distributionData={distributionData}
            />
          )}
          
          <Content className="content-container">
            <div className="table-header">
              <div>找到 {tableData.length} 条记录</div>
              <Space>
                {!showHistogram && (distributionData && distributionData.length > 0) && (
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
              <Select.Option key={field.columnName} value={field.columnName}>
                <Space>
                  <Tag color={getFieldTypeColor(field.dataType)} style={{ marginRight: 0 }}>
                    {field.dataType}
                  </Tag>
                  {field.columnName}
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
              availableFields.find(f => f.columnName === selectedFilterField)?.dataType : undefined)
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
