import { useState, useCallback, useEffect, useMemo, useRef, lazy, Suspense } from 'react';
import { Layout, Space, Button, Modal, Form, Input, Select, Tag, Skeleton } from 'antd';
import { getOperatorsByFieldType, getFieldTypeColor, debounce, memoize, throttle } from '../utils/logDataHelpers';
import { 
  CompressOutlined,
  ExpandOutlined,
  PlusOutlined,
  ReloadOutlined,
  WarningOutlined,
  InfoCircleOutlined
} from '@ant-design/icons';
import { useLogData } from '../hooks/useLogData';
import { useFilters } from '../hooks/useFilters';
import { useGlobalLoading } from '../hooks/useLoading';
import { SearchBar } from '../components/HomePage/SearchBar';
import { DataTable } from '../components/HomePage/DataTable';
import { FieldSelector } from '../components/HomePage/FieldSelector';
import { getMyTablePermissions } from '../api/permission';
import { getTableColumns } from '../api/logs';
import Loading from '../components/Loading';
import './HomePage.less';

// 使用懒加载优化初始加载时间
const HistogramChart = lazy(() => import('../components/HomePage/HistogramChart').then(module => ({ 
  default: module.HistogramChart 
})));
const KibanaTimePicker = lazy(() => import('../components/HomePage/KibanaTimePicker').then(module => ({ 
  default: module.KibanaTimePicker 
})));
const FilterPanel = lazy(() => import('../components/HomePage/FilterPanel').then(module => ({ 
  default: module.FilterPanel 
})));

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

const HomePage = () => {
  const [form] = Form.useForm();
  const [collapsed, setCollapsed] = useState(false);
  const [selectedFields, setSelectedFields] = useState<string[]>(['log_time', 'message']);
  const [viewMode, setViewMode] = useState<'table' | 'json'>('table');
  const [searchQuery, setSearchQuery] = useState('');
  const [whereSql, setWhereSql] = useState('');
  const [showHistogram, setShowHistogram] = useState(true);
  const [timeRange, setTimeRange] = useState<[string, string] | undefined>(undefined);
  const [timeRangePreset, setTimeRangePreset] = useState<string | undefined>(undefined);
  const [timeDisplayText, setTimeDisplayText] = useState<string | undefined>(undefined);
  const [showTimePicker, setShowTimePicker] = useState(false);
  const [selectedTable, setSelectedTable] = useState<string>('');

  // 使用全局加载状态
  const { isLoading: isGlobalLoading, startLoading, endLoading } = useGlobalLoading();

  // 使用refs优化状态管理，减少重新渲染
  const lastAddedFieldRef = useRef<string | null>(null);
  const lastRemovedFieldRef = useRef<string | null>(null);
  const [renderKey, setRenderKey] = useState<number>(0); // 用于强制更新UI的key

  // 将状态放入ref以减少不必要的重渲染
  const availableTablesRef = useRef<Array<{
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
  
  // 缓存表格数据引用
  const tableDataRef = useRef<any[]>([]);
  
  // 新增数据加载状态
  const [tableLoading, setTableLoading] = useState<boolean>(false);
  const [fieldLoading, setFieldLoading] = useState<boolean>(false);
  
  // 添加请求缓存
  const tableColumnsCache = useRef<Record<string, any[]>>({});

  // 将表格加载状态与全局加载状态集成
  useEffect(() => {
    if (tableLoading && !isGlobalLoading) {
      startLoading('tableLoading');
    } else if (!tableLoading && isGlobalLoading) {
      endLoading('tableLoading');
    }
  }, [tableLoading, isGlobalLoading, startLoading, endLoading]);

  // 将字段加载状态与全局加载状态集成
  useEffect(() => {
    if (fieldLoading && !isGlobalLoading) {
      startLoading('fieldLoading');
    } else if (!fieldLoading && isGlobalLoading) {
      endLoading('fieldLoading');
    }
  }, [fieldLoading, isGlobalLoading, startLoading, endLoading]);

  // 获取表字段，添加缓存和错误处理
  const fetchTableColumns = useCallback(async (tableIdentifier: string) => {
    if (!tableIdentifier || tableIdentifier === prevSelectedTable.current) return;
    
    setFieldLoading(true);
    
    try {
      // 检查缓存中是否已存在该表的字段
      if (tableColumnsCache.current[tableIdentifier]) {
        setAvailableFields(tableColumnsCache.current[tableIdentifier]);
        setFieldLoading(false);
        prevSelectedTable.current = tableIdentifier;
        return;
      }
      
      const [datasourceId, tableName] = tableIdentifier.split('-');
      const columns = await getTableColumns(datasourceId, tableName) as Array<{columnName: string; dataType: string}>;
      
      // 更新缓存
      tableColumnsCache.current[tableIdentifier] = columns;
      setAvailableFields(columns);
      prevSelectedTable.current = tableIdentifier;
    } catch (error) {
      console.error('获取表字段失败:', error);
      // 显示友好的错误提示
      Modal.error({
        title: '获取字段失败',
        content: '无法获取表字段，请检查网络连接或联系管理员'
      });
    } finally {
      setFieldLoading(false);
    }
  }, []);
  
  // 使用节流函数优化表选择
  const throttledFetchColumns = useMemo(() => 
    throttle(fetchTableColumns, 500),
    [fetchTableColumns]
  );
  
  // 表选择变化处理
  const handleTableChange = useCallback((tableId: string) => {
    setSelectedTable(tableId);
    if (tableId !== prevSelectedTable.current) {
      throttledFetchColumns(tableId);
    }
  }, [throttledFetchColumns]);
  
  // 设置默认时间范围为最近15分钟
  useEffect(() => {
    if (!timeRange) {
      const now = new Date();
      const fifteenMinutesAgo = new Date(now.getTime() - 15 * 60 * 1000);
      
      setTimeRange([
        fifteenMinutesAgo.toISOString().substring(0, 19).replace('T', ' '),
        now.toISOString().substring(0, 19).replace('T', ' ')
      ]);
      setTimeRangePreset('last_15m');
    }
  }, []);

  // 使用useMemo优化搜索参数构建，减少不必要的对象创建
  const [timeGrouping, setTimeGrouping] = useState<'minute' | 'hour' | 'day' | 'month'>('minute');

  const searchParams = useMemo(() => ({
    datasourceId: selectedTable ? Number(selectedTable.split('-')[0]) : 1,
    tableName: selectedTable ? selectedTable.split('-')[1] : '',
    keyword: searchQuery,
    whereSql: whereSql,
    timeRange: timeRange ? `${timeRange[0]}-${timeRange[1]}` : undefined,
    timeGrouping: timeGrouping,
    pageSize: 50,
    offset: 0,
    fields: selectedFields,
    startTime: timeRange ? timeRange[0] : undefined,
    endTime: timeRange ? timeRange[1] : undefined,
  }), [selectedTable, searchQuery, whereSql, timeRange, selectedFields, timeGrouping]);

  // 获取表权限数据，优化为仅在组件挂载时执行一次
  useEffect(() => {
    const fetchTablePermissions = async () => {
      setTableLoading(true);
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
        
        // 使用ref存储数据，减少重渲染
        availableTablesRef.current = transformedData;
        setRenderKey(prev => prev + 1);

        // 默认选择第一个数据源和第一个表
        if (data.length > 0 && data[0].tables.length > 0) {
          const defaultTable = `${data[0].datasourceId}-${data[0].tables[0].tableName}`;
          setSelectedTable(defaultTable);
          throttledFetchColumns(defaultTable);
        }
      } catch (error) {
        console.error('获取表权限失败:', error);
        Modal.error({
          title: '获取表权限失败',
          content: '无法获取表权限信息，请检查网络连接或联系管理员'
        });
      } finally {
        setTableLoading(false);
      }
    };
    
    fetchTablePermissions();
  }, [throttledFetchColumns]);

  // 使用优化的 useLogData 钩子
  const { tableData, loading, hasMore, loadMoreData, resetData, distributionData = [], totalCount } = useLogData({
    ...searchParams,
    tableName: selectedTable ? selectedTable.split('-')[1] : '',
    datasourceId: selectedTable ? Number(selectedTable.split('-')[0]) : 1,
    fields: selectedFields
  });
  
  // 将数据加载状态与全局加载状态集成
  useEffect(() => {
    if (loading && !isGlobalLoading) {
      startLoading('dataLoading');
    } else if (!loading && isGlobalLoading) {
      // 延迟结束加载状态，避免闪烁
      const timer = setTimeout(() => {
        endLoading('dataLoading');
      }, 300);
      return () => clearTimeout(timer);
    }
  }, [loading, isGlobalLoading, startLoading, endLoading]);
  
  // 更新ref，但不触发重新渲染
  if (tableDataRef.current !== tableData) {
    tableDataRef.current = tableData;
  }
  
  // 优化 useFilters 钩子的使用
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

  // 优化字段选择逻辑，使用带记忆的回调
  const toggleFieldSelection = useCallback((fieldName: string) => {
    setSelectedFields(prev => {
      if (prev.includes(fieldName)) {
        lastRemovedFieldRef.current = fieldName;
        lastAddedFieldRef.current = null;
        return prev.filter(f => f !== fieldName);
      } else {
        lastAddedFieldRef.current = fieldName;
        lastRemovedFieldRef.current = null;
        return [...prev, fieldName];
      }
    });
    
    // 使用setTimeout清除动画状态
    setTimeout(() => {
      lastAddedFieldRef.current = null;
      lastRemovedFieldRef.current = null;
      // 强制更新以反映新的ref值
      setRenderKey(prev => prev + 1);
    }, 2000);
  }, []);

  // 优化滚动逻辑，添加节流机制避免频繁触发
  const handleScroll = useCallback(
    throttle((e: React.UIEvent<HTMLDivElement>) => {
      if (loading || !hasMore) return;
      
      const element = e.currentTarget;
      if (!element) return;
      
      const scrollTop = element.scrollTop;
      const scrollHeight = element.scrollHeight;
      const clientHeight = element.clientHeight;
      
      // 当滚动到底部前200px时开始加载更多，提供更好的用户体验
      if (scrollHeight - scrollTop - clientHeight < 100) {
        loadMoreData();
      }
    }, 300), // 300ms的节流时间，避免短时间内多次触发
    [loadMoreData, loading, hasMore]
  );

  // 优化时间范围变更处理
  const handleTimeRangeChange = useCallback((range: [string, string] | undefined, preset?: string | undefined, displayText?: string) => {
    setTimeRange(range);
    setTimeRangePreset(preset);
    setTimeDisplayText(displayText);
    
    
    // 当有时间范围变化时，如果是有数据的情况下，重新加载数据
    if (range && tableDataRef.current.length > 0) {
      resetData();
    }
  }, [resetData]);

  // 优化搜索提交处理
  const handleSubmitSearch = useCallback(() => {
    // 重置数据，触发新查询
    if (tableDataRef.current.length > 0) {
      resetData();
    }
  }, [resetData]);

  // 优化SQL查询提交处理
  const handleSubmitSql = useCallback(() => {
    // 重置数据，触发新查询
    if (tableDataRef.current.length > 0) {
      resetData();
    }
  }, [resetData]);

  // 优化字段选择组件的props
  const fieldSelectorProps = useMemo(() => ({
    selectedTable,
    availableFields,
    selectedFields,
    onToggleField: toggleFieldSelection,
    lastAddedField: lastAddedFieldRef.current,
    lastRemovedField: lastRemovedFieldRef.current,
    availableTables: availableTablesRef.current,
    onTableChange: handleTableChange,
    collapsed,
    loading: fieldLoading
  }), [
    selectedTable, 
    availableFields, 
    selectedFields, 
    toggleFieldSelection, 
    collapsed, 
    renderKey, 
    fieldLoading,
    handleTableChange
  ]);

  // 优化DataTable组件的props
  const dataTableProps = useMemo(() => ({
    data: tableData,
    loading,
    hasMore,
    selectedFields,
    searchQuery,
    viewMode,
    onScroll: handleScroll,
    lastAddedField: lastAddedFieldRef.current
  }), [
    tableData, 
    loading, 
    hasMore, 
    selectedFields, 
    searchQuery, 
    viewMode, 
    handleScroll, 
    renderKey
  ]);

  // 优化视图模式切换的处理函数
  const handleViewModeChange = useCallback((mode: 'table' | 'json') => {
    setViewMode(mode);
  }, []);

  // 优化直方图显示切换的处理函数
  const handleToggleHistogram = useCallback((show: boolean) => {
    setShowHistogram(show);
  }, []);

  useEffect(() => {
    // 添加调试代码，检查HomePage中的直方图显示条件
    console.log('HomePage 直方图显示条件:', {
      showHistogram, 
      hasDistributionData: distributionData && distributionData.length > 0,
      distributionDataLength: distributionData ? distributionData.length : 0
    });
  }, [showHistogram, distributionData]);

  return (
    <>
    <Layout className="layout-main">
      <SearchBar 
        searchQuery={searchQuery}
        whereSql={whereSql}
        timeRange={timeRange}
        timeRangePreset={timeRangePreset}
        timeDisplayText={timeDisplayText}
        timeGrouping={timeGrouping}
        onSearch={setSearchQuery}
        onWhereSqlChange={setWhereSql}
        onSubmitSearch={handleSubmitSearch}
        onSubmitSql={handleSubmitSql}
        onTimeRangeChange={handleTimeRangeChange}
        onOpenTimeSelector={() => setShowTimePicker(true)}
        onTimeGroupingChange={setTimeGrouping}
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
          {tableLoading ? (
            <div style={{ padding: "20px" }}>
              <Loading tip="加载表结构..." size="small" />
            </div>
          ) : (
            <FieldSelector {...fieldSelectorProps} />
          )}
        </Sider>
        
        <Layout className="layout-inner">
          {showHistogram && distributionData && distributionData.length > 0 && (
            <Suspense fallback={<Loading tip="加载直方图..." delay={300} />}>
              <HistogramChart
                show={showHistogram}
                onTimeRangeChange={handleTimeRangeChange}
                onToggle={() => handleToggleHistogram(false)}
                distributionData={distributionData}
                timeGrouping={searchParams.timeGrouping}
              />
            </Suspense>
          )}
          
          <Content className="content-container">
            <div className="table-header">
              <div>
                <span>找到 <b>{ totalCount }</b> 条记录</span>
                {!selectedTable && (
                  <Tag color="warning" icon={<InfoCircleOutlined />}>请选择数据表</Tag>
                )}
              </div>
              <Space>
                {!showHistogram && distributionData && distributionData.length > 0 && (
                  <Button
                    size="small"
                    type="text"
                    icon={<PlusOutlined />}
                    onClick={() => handleToggleHistogram(true)}
                  >
                    显示直方图
                  </Button>
                )}
                <Space.Compact>
                  <Button 
                    type={viewMode === 'table' ? 'primary' : 'default'} 
                    icon={<CompressOutlined />} 
                    onClick={() => handleViewModeChange('table')}
                  />
                  <Button 
                    type={viewMode === 'json' ? 'primary' : 'default'} 
                    icon={<ExpandOutlined />} 
                    onClick={() => handleViewModeChange('json')}
                  />
                </Space.Compact>
              </Space>
            </div>
            
            <div 
              className="scroll-container" 
              style={{ height: '100%', overflow: 'auto' }}
              onScroll={handleScroll}
            >
              <DataTable {...dataTableProps} />
            </div>
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

    {/* 时间选择器对话框 */}
    {showTimePicker && (
      <Modal
        title="自定义时间范围"
        open={showTimePicker}
        onCancel={() => setShowTimePicker(false)}
        footer={null}
        width={600}
      >
        <Suspense fallback={<Skeleton active paragraph={{ rows: 5 }} />}>
              <KibanaTimePicker
                value={timeRange}
                presetKey={timeRangePreset || undefined}
                onChange={(range, preset, displayText) => {
                  handleTimeRangeChange(range, preset, displayText);
                  setShowTimePicker(false);
                }}
                onTimeGroupingChange={(value: 'minute' | 'hour' | 'day' | 'month') => {
                  // 确保searchParams包含最新timeGrouping
                  searchParams.timeGrouping = value;
                  setTimeGrouping(value);
                }}
                timeGrouping={timeGrouping}
            />
        </Suspense>
      </Modal>
    )}
    </>
  );
}

// 确保使用命名导出和默认导出两种方式
export { HomePage };
export default HomePage;
