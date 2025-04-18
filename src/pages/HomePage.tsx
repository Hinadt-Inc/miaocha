import { useState, useEffect, useRef, useCallback } from 'react'
import { 
  AutoComplete,
  Dropdown,
  Layout, 
  Select,
  Space, 
  DatePicker, 
  Button, 
  Table, 
  Card, 
  Collapse, 
  Tag, 
  Tabs, 
  Tooltip, 
  Divider,
  message,
  Spin,
  Modal,
  Form,
  Input,
  Radio,
} from 'antd'
import { 
  SearchOutlined, 
  EyeOutlined,
  ExpandOutlined,
  CompressOutlined,
  InfoCircleOutlined,
  PlusOutlined,
  MinusOutlined,
  LoadingOutlined,
  FilterOutlined,
  CloseOutlined,
} from '@ant-design/icons'
import ReactECharts from 'echarts-for-react'
import { Outlet } from 'react-router-dom'
import type { EChartsOption } from 'echarts'
import dayjs from 'dayjs'
import './HomePage.less'

const { Content, Sider } = Layout
const { RangePicker } = DatePicker
const { TabPane } = Tabs
const { Option } = Select

// 过滤器操作符类型
type FilterOperator = 
  | 'is' // 等于 
  | 'is_not' // 不等于
  | 'contains' // 包含
  | 'does_not_contain' // 不包含
  | 'exists' // 存在
  | 'does_not_exist' // 不存在
  | 'is_one_of' // 是其中之一
  | 'is_not_one_of' // 不是其中之一
  | 'greater_than' // 大于
  | 'less_than' // 小于
  | 'is_between' // 在...之间

// 过滤器类型定义
interface Filter {
  id: string; // 唯一标识符
  field: string; // 字段名
  operator: FilterOperator; // 操作符
  value: string | string[] | [number, number] | null; // 值，可以是字符串、字符串数组（用于多选）或范围值
  color: string; // 标签颜色
}

function HomePage() {
  const [collapsed, setCollapsed] = useState(false)
  const [selectedFields, setSelectedFields] = useState<string[]>(['timestamp', 'message', 'host', 'source'])
  const [viewMode, setViewMode] = useState<'table' | 'json'>('table')
  const [searchQuery, setSearchQuery] = useState('')
  const [showHistogram, setShowHistogram] = useState(true)
  const [timeRange, setTimeRange] = useState<[string, string] | null>(null)
  const [showTimePicker, setShowTimePicker] = useState(false)
  const [selectedTable, setSelectedTable] = useState<string>('')
  const [searchHistory, setSearchHistory] = useState<string[]>(() => {
    const saved = localStorage.getItem('searchHistory')
    return saved ? JSON.parse(saved) : []
  })
  
  // 过滤器相关状态
  const [filters, setFilters] = useState<Filter[]>([
    { id: 'default-source', field: 'source', operator: 'is', value: 'nginx', color: 'blue' },
    { id: 'default-status', field: 'status', operator: 'is', value: '200', color: 'green' }
  ])
  const [showFilterModal, setShowFilterModal] = useState(false)
  const [filterForm] = Form.useForm()
  const [selectedFilterField, setSelectedFilterField] = useState<string>('')
  const [selectedFilterOperator, setSelectedFilterOperator] = useState<FilterOperator>('is')
  
  // 无限滚动相关状态
  interface LogData {
    key: number;
    timestamp: string;
    message: string;
    host: string;
    source: string;
    status: number;
    bytes: number;
    response_time: number;
    ip: string;
    method: string;
    path: string;
    user_agent: string;
    referer: string;
    'geo.country': string;
    'geo.city': string;
  }

  const [tableData, setTableData] = useState<LogData[]>([])
  const [loading, setLoading] = useState(false)
  const [hasMore, setHasMore] = useState(true)
  const pageSize = 20
  const tableContainerRef = useRef<HTMLDivElement>(null)

  // 保存所有需要持久化的状态到localStorage
  useEffect(() => {
    const stateToSave = {
      selectedFields,
      viewMode,
      searchQuery,
      showHistogram,
      timeRange,
      selectedTable
    }
    localStorage.setItem('homePageState', JSON.stringify(stateToSave))
  }, [selectedFields, viewMode, searchQuery, showHistogram, timeRange, selectedTable])

  // 初始化时恢复状态
  useEffect(() => {
    const savedState = localStorage.getItem('homePageState')
    if (savedState) {
      const parsedState = JSON.parse(savedState)
      setSelectedFields(parsedState.selectedFields || ['timestamp', 'message', 'host', 'source'])
      setViewMode(parsedState.viewMode || 'table')
      setSearchQuery(parsedState.searchQuery || '')
      setShowHistogram(parsedState.showHistogram !== false)
      setTimeRange(parsedState.timeRange || null)
      setSelectedTable(parsedState.selectedTable || '')
    }
  }, [])
  const [lastAddedField, setLastAddedField] = useState<string | null>(null)
  const [lastRemovedField, setLastRemovedField] = useState<string | null>(null)

  // 模拟表数据
  const availableTables = [
    { 
      name: 'nginx_logs',
      fields: ['timestamp', 'host', 'message', 'status', 'bytes']
    },
    { 
      name: 'app_logs',
      fields: ['timestamp', 'level', 'message', 'service', 'trace_id']
    },
    { 
      name: 'db_logs',
      fields: ['timestamp', 'query', 'duration', 'database', 'user']
    }
  ]

  // 模拟字段列表
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
  ]

  // 生成模拟数据
  const generateMockData = (start: number, count: number) => {
    return Array(count).fill(null).map((_, index) => {
      const actualIndex = start + index;
      return {
        key: actualIndex,
        timestamp: `2025-04-${(actualIndex % 14) + 1} ${(actualIndex % 24)}:${(actualIndex % 60).toString().padStart(2, '0')}:00`,
        message: `这是日志消息 ${actualIndex}`,
        host: `server-${actualIndex % 5}.example.com`,
        source: actualIndex % 3 === 0 ? 'nginx' : (actualIndex % 3 === 1 ? 'application' : 'database'),
        status: actualIndex % 10 === 0 ? 500 : (actualIndex % 5 === 0 ? 404 : 200),
        bytes: Math.floor(Math.random() * 10000),
        response_time: Math.round(Math.random() * 1000) / 10,
        ip: `192.168.1.${actualIndex % 256}`,
        method: actualIndex % 4 === 0 ? 'POST' : (actualIndex % 3 === 0 ? 'PUT' : 'GET'),
        path: index % 3 === 0 ? '/api/users' : (index % 5 === 0 ? '/api/products' : '/api/orders'),
        user_agent: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)',
        referer: 'https://example.com',
        'geo.country': 'China',
        'geo.city': actualIndex % 3 === 0 ? 'Beijing' : (actualIndex % 3 === 1 ? 'Shanghai' : 'Guangzhou'),
      };
    });
  };

  // 加载更多数据
  const loadMoreData = useCallback(() => {
    if (loading || !hasMore) return;
    
    setLoading(true);
    
    // 模拟API请求延迟
    setTimeout(() => {
      const newData = generateMockData(tableData.length, pageSize);
      setTableData(prevData => [...prevData, ...newData]);
      
      // 模拟数据上限，设置为500条
      if (tableData.length + newData.length >= 500) {
        setHasMore(false);
      }
      
      setLoading(false);
    }, 500);
  }, [loading, hasMore, tableData.length]);

  // 处理滚动事件检测
  const handleScroll = useCallback((e: React.UIEvent<HTMLDivElement>) => {
    const { scrollTop, clientHeight, scrollHeight } = e.currentTarget;
    // 当滚动到距离底部100px时，加载更多数据
    if (scrollHeight - scrollTop - clientHeight < 100 && !loading && hasMore) {
      loadMoreData();
    }
  }, [loadMoreData, loading, hasMore]);

  // 初始化加载数据
  useEffect(() => {
    setTableData(generateMockData(0, pageSize));
    setHasMore(true);
  }, []);

  // 搜索或时间范围改变时重置数据
  useEffect(() => {
    setTableData(generateMockData(0, pageSize));
    setHasMore(true);
  }, [searchQuery, timeRange, selectedTable]);

  // 生成直方图数据
  const getHistogramOption = (): EChartsOption => {
    const dates = Array(14).fill(null).map((_, i) => `2025-04-${i + 1}`)
    const values = Array(14).fill(null).map(() => Math.floor(Math.random() * 100) + 20)
    
    return {
      grid: {
        top: 20,
        right: 30,
        bottom: 40,
        left: 40,
      },
      xAxis: {
        type: 'category',
        data: dates,
        axisLabel: {
          interval: 2,
          fontSize: 10
        }
      },
      yAxis: {
        type: 'value',
        axisLabel: {
          fontSize: 10
        }
      },
      series: [{
        data: values,
        type: 'bar',
        color: '#1890ff',
        name: '数据条数',
        barWidth: '60%',
        emphasis: {
          itemStyle: {
            color: '#69c0ff'  // 鼠标悬浮时颜色变亮
          }
        }
      }],
      tooltip: {
        trigger: 'axis',
        formatter: '{b}<br/>{a}: {c}'
      },
      dataZoom: [
        {
          type: 'slider',
          show: true,
          xAxisIndex: [0],
          start: 0,
          end: 100,
          height: 15,
          bottom: 5,
          borderColor: 'transparent',
          backgroundColor: '#e6e6e6',
          fillerColor: 'rgba(24, 144, 255, 0.2)',
          handleSize: '70%',
          handleStyle: {
            color: '#1890ff'
          },
          textStyle: {
            color: 'rgba(0, 0, 0, 0.65)',
            fontSize: 10
          },
          brushSelect: true  // 允许刷选
        },
        {
          type: 'inside',
          xAxisIndex: [0],
          start: 0,
          end: 100,
          zoomOnMouseWheel: 'ctrl',  // 按住Ctrl键滚动鼠标滚轮进行缩放
          moveOnMouseMove: true,     // 鼠标移动即可平移
          preventDefaultMouseMove: false
        }
      ]
    }
  }

  // 处理图表事件
  const onChartEvents = {
    datazoom: (params: {
      batch?: Array<{
        startValue?: number;
        endValue?: number;
      }>;
    }) => {
      if (params.batch && params.batch.length > 0) {
        const { startValue, endValue } = params.batch[0]
        
        if (startValue !== undefined && endValue !== undefined) {
          try {
            // 确保startValue和endValue是有效的索引
            const dates = Array(14).fill(null).map((_, i) => `2025-04-${i + 1}`)
            const startIndex = Math.max(0, Math.floor(startValue))
            const endIndex = Math.min(dates.length - 1, Math.floor(endValue))
            
            if (startIndex >= 0 && endIndex < dates.length) {
              const startDate = dates[startIndex]
              const endDate = dates[endIndex]
              
              if (startDate && endDate) {
                setTimeRange([startDate, endDate])
                message.info(`已选择时间范围: ${startDate} 至 ${endDate}`)
              }
            }
          } catch (error) {
            console.error('处理缩放事件时出错:', error)
          }
        }
      }
    }
  }

  // 根据选中的字段生成表格列
  const getTableColumns = () => {
    return selectedFields.map(field => ({
      title: field,
      dataIndex: field,
      key: field,
      render: (text: string | number) => {
        const searchQueryLower = searchQuery.toLowerCase();
        const textStr = String(text);
        
        if (field === 'timestamp') {
          return <span style={{ color: '#1890ff' }}>{textStr}</span>
        }
        if (field === 'status') {
          const status = Number(text);
          return <Tag color={status === 200 ? 'green' : (status === 404 ? 'orange' : 'red')}>{textStr}</Tag>
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
    }))
  }

  // 字段类型对应的图标颜色
  const getFieldTypeColor = (type: string) => {
    switch (type) {
      case 'text': return 'purple'
      case 'keyword': return 'blue'
      case 'number': return 'cyan'
      case 'date': return 'green'
      case 'ip': return 'orange'
      default: return 'default'
    }
  }

  const toggleFieldSelection = (fieldName: string) => {
    if (selectedFields.includes(fieldName)) {
      setLastRemovedField(fieldName)
      setLastAddedField(null)
      setSelectedFields(selectedFields.filter(f => f !== fieldName))
    } else {
      setLastAddedField(fieldName)
      setLastRemovedField(null)
      setSelectedFields([...selectedFields, fieldName])
    }
    
    setTimeout(() => {
      setLastAddedField(null)
      setLastRemovedField(null)
    }, 5000)
  }

  // 处理表选择变化
  const handleTableChange = (value: string) => {
    setSelectedTable(value);
  };

  // 过滤器相关方法
  // 根据字段类型获取操作符选项
  const getOperatorsByFieldType = (fieldType?: string): { label: string; value: FilterOperator }[] => {
    const commonOperators = [
      { label: '是', value: 'is' },
      { label: '不是', value: 'is_not' },
      { label: '存在', value: 'exists' },
      { label: '不存在', value: 'does_not_exist' },
    ];
    
    if (!fieldType) return commonOperators;
    
    switch (fieldType) {
      case 'text':
      case 'keyword':
        return [
          ...commonOperators,
          { label: '包含', value: 'contains' },
          { label: '不包含', value: 'does_not_contain' },
          { label: '是其中之一', value: 'is_one_of' },
          { label: '不是其中之一', value: 'is_not_one_of' },
        ];
      case 'number':
      case 'date':
        return [
          ...commonOperators,
          { label: '大于', value: 'greater_than' },
          { label: '小于', value: 'less_than' },
          { label: '在...之间', value: 'is_between' },
        ];
      default:
        return commonOperators;
    }
  };

  // 获取字段的类型
  const getFieldType = (fieldName: string): string => {
    const field = availableFields.find(f => f.name === fieldName);
    return field?.type || 'keyword';
  };

  // 打开添加过滤器对话框
  const openFilterModal = () => {
    filterForm.resetFields();
    setSelectedFilterField('');
    setSelectedFilterOperator('is');
    setShowFilterModal(true);
  };

  // 处理过滤器字段变化
  const handleFilterFieldChange = (fieldName: string) => {
    setSelectedFilterField(fieldName);
    const fieldType = getFieldType(fieldName);
    const operators = getOperatorsByFieldType(fieldType);
    setSelectedFilterOperator(operators[0]?.value || 'is');
    filterForm.setFieldsValue({ operator: operators[0]?.value || 'is' });
  };

  // 添加过滤器
  const addFilter = (values: any) => {
    const { field, operator, value } = values;
    const fieldType = getFieldType(field);
    
    // 生成随机颜色
    const colors = ['blue', 'green', 'orange', 'red', 'purple', 'cyan', 'magenta', 'gold', 'lime', 'geekblue'];
    const color = colors[Math.floor(Math.random() * colors.length)];
    
    // 创建新过滤器
    const newFilter: Filter = {
      id: `filter-${Date.now()}`,
      field,
      operator,
      value,
      color,
    };
    
    setFilters([...filters, newFilter]);
    setShowFilterModal(false);
    message.success(`已添加过滤器: ${field}`);
  };

  // 删除过滤器
  const removeFilter = (filterId: string) => {
    setFilters(filters.filter(f => f.id !== filterId));
  };

  // 获取过滤器显示文本
  const getFilterDisplayText = (filter: Filter): string => {
    const { field, operator, value } = filter;
    
    switch (operator) {
      case 'is':
        return `${field}: ${value}`;
      case 'is_not':
        return `${field} 不是: ${value}`;
      case 'contains':
        return `${field} 包含: ${value}`;
      case 'does_not_contain':
        return `${field} 不包含: ${value}`;
      case 'exists':
        return `${field} 存在`;
      case 'does_not_exist':
        return `${field} 不存在`;
      case 'is_one_of':
        return `${field} 是: [${Array.isArray(value) ? value.join(', ') : value}]`;
      case 'is_not_one_of':
        return `${field} 不是: [${Array.isArray(value) ? value.join(', ') : value}]`;
      case 'greater_than':
        return `${field} > ${value}`;
      case 'less_than':
        return `${field} < ${value}`;
      case 'is_between':
        if (Array.isArray(value) && value.length === 2) {
          return `${field}: ${value[0]} 至 ${value[1]}`;
        }
        return `${field} 在范围内`;
      default:
        return `${field}: ${value}`;
    }
  };

  // 当时间范围改变时更新 DatePicker
  useEffect(() => {
    if (timeRange) {
      console.log('Selected time range:', timeRange)
    }
  }, [timeRange])

  return (
    <>
    <Layout style={{ background: '#f0f2f5', minHeight: 'calc(100vh - 64px)' }}>
      {/* 搜索区域 */}
      <div style={{ padding: '16px 24px', background: '#fff', boxShadow: '0 1px 4px rgba(0, 21, 41, 0.08)' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
          <Space size="middle" style={{ width: '100%' }}>
            <Space.Compact>
              <AutoComplete
                placeholder="输入搜索内容"
                style={{ width: 1100 }}
                value={searchQuery}
                onChange={setSearchQuery}
                options={searchHistory.map(query => ({
                  value: query,
                  label: query
                }))}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' && searchQuery.trim()) {
                    // 执行搜索并保存历史记录
                    if (!searchHistory.includes(searchQuery.trim())) {
                      const newHistory = [searchQuery.trim(), ...searchHistory].slice(0, 10)
                      setSearchHistory(newHistory)
                      localStorage.setItem('searchHistory', JSON.stringify(newHistory))
                    }
                    // 这里可以添加其他搜索时的逻辑
                    message.info(`正在搜索: ${searchQuery}`)
                  }
                }}
              />
              <Button 
                icon={<SearchOutlined />} 
                type="primary"
                onClick={() => {
                  if (searchQuery.trim()) {
                    // 执行搜索并保存历史记录
                    if (!searchHistory.includes(searchQuery.trim())) {
                      const newHistory = [searchQuery.trim(), ...searchHistory].slice(0, 10)
                      setSearchHistory(newHistory)
                      localStorage.setItem('searchHistory', JSON.stringify(newHistory))
                    }
                    // 这里可以添加其他搜索时的逻辑
                    message.info(`正在搜索: ${searchQuery}`)
                  }
                }}
              />
            </Space.Compact>
            
            <Space.Compact>
              <Dropdown
                menu={{
                  items: [
                    {
                      key: '15m',
                      label: '最近15分钟',
                      onClick: () => {
                        const now = dayjs()
                        setTimeRange([
                          now.subtract(15, 'minute').format('YYYY-MM-DD HH:mm:ss'),
                          now.format('YYYY-MM-DD HH:mm:ss')
                        ])
                        setShowTimePicker(false)
                      }
                    },
                    {
                      key: '1h',
                      label: '最近1小时',
                      onClick: () => {
                        const now = dayjs()
                        setTimeRange([
                          now.subtract(1, 'hour').format('YYYY-MM-DD HH:mm:ss'),
                          now.format('YYYY-MM-DD HH:mm:ss')
                        ])
                        setShowTimePicker(false)
                      }
                    },
                    {
                      key: 'custom',
                      label: '自定义时间',
                      onClick: () => setShowTimePicker(!showTimePicker)
                    }
                  ]
                }}
                trigger={['click']}
              >
                <Button>
                  {timeRange 
                    ? `${dayjs(timeRange[0]).format('YYYY-MM-DD HH:mm:ss')} ~ ${dayjs(timeRange[1]).format('YYYY-MM-DD HH:mm:ss')}`
                    : '最近15分钟'}
                </Button>
              </Dropdown>
              {showTimePicker && (
                <RangePicker
                  showTime={{ format: 'HH:mm:ss' }}
                  format="YYYY-MM-DD HH:mm:ss"
                  style={{ width: 400 }}
                  onOk={values => {
                    if (values && values[0] && values[1]) {
                      setTimeRange([
                        values[0].format('YYYY-MM-DD HH:mm:ss'),
                        values[1].format('YYYY-MM-DD HH:mm:ss')
                      ])
                    }
                    setShowTimePicker(false)
                  }}
                />
              )}
            </Space.Compact>
          </Space>
        </div>
        <div>
          <Space>
            {filters.map(filter => (
              <Tag 
                key={filter.id} 
                color={filter.color} 
                closable 
                onClose={() => removeFilter(filter.id)}
                icon={<FilterOutlined />}
                style={{ display: 'flex', alignItems: 'center', padding: '0 8px' }}
              >
                {getFilterDisplayText(filter)}
              </Tag>
            ))}
            <Button 
              type="link" 
              icon={<PlusOutlined />} 
              onClick={openFilterModal}
            >
              添加过滤器
            </Button>
          </Space>
        </div>
      </div>
      
      {/* 下面为左右结构 */}
      <Layout style={{ padding: '12px 0', background: '#f0f2f5' }}>
        {/* 左侧：字段展示和搜索 */}
        <Sider 
          width={280} 
          theme="light" 
          collapsible 
          collapsed={collapsed} 
          onCollapse={setCollapsed}
          style={{ background: '#fff', marginRight: 16, overflowY: 'auto' }}
        >
          {!collapsed && (
            <>
              <div style={{ padding: '12px 16px', borderBottom: '1px solid #f0f0f0' }}>
                <div>
                  <Select
                    placeholder="选择数据表"
                    style={{ width: '100%' }}
                    value={selectedTable || undefined}
                    onChange={handleTableChange}
                    allowClear
                    showSearch
                  >
                    {availableTables.map((table) => (
                      <Option key={table.name} value={table.name}>{table.name}</Option>
                    ))}
                  </Select>
                </div>
              </div>
              
              <Collapse 
                defaultActiveKey={['available']} 
                ghost
                items={[
                  {
                    key: 'available',
                    label: '可用字段',
                    children: availableFields.map(field => (
                      <div 
                        key={field.name} 
                        style={{ 
                          padding: '8px 16px',
                          display: 'flex',
                          justifyContent: 'space-between',
                          alignItems: 'center',
                          cursor: 'pointer',
                          borderRadius: '4px',
                          background: selectedFields.includes(field.name) ? '#e6f7ff' : 'transparent',
                          marginBottom: 4,
                          transition: 'all 0.3s ease'
                        }}
                        onClick={() => toggleFieldSelection(field.name)}
                        className={`field-selection-item ${lastAddedField === field.name ? 'field-added' : ''} ${lastRemovedField === field.name ? 'field-removed' : ''}`}
                      >
                        <Space>
                          <Tag color={getFieldTypeColor(field.type)} style={{ marginRight: 8 }}>
                            {field.type.substr(0, 1).toUpperCase()}
                          </Tag>
                          {field.name}
                        </Space>
                        {selectedFields.includes(field.name) && (
                          <EyeOutlined style={{ color: '#1890ff' }} />
                        )}
                      </div>
                    ))
                  },
                  {
                    key: 'selected',
                    label: '已选字段',
                    children: selectedFields.map(fieldName => {
                      const field = availableFields.find(f => f.name === fieldName)
                      if (!field) return null
                      
                      return (
                        <div 
                          key={field.name} 
                          style={{ 
                            padding: '8px 16px',
                            display: 'flex',
                            justifyContent: 'space-between',
                            alignItems: 'center',
                            borderRadius: '4px',
                            background: '#e6f7ff',
                            marginBottom: 4
                          }}
                        >
                          <Space>
                            <Tag color={getFieldTypeColor(field.type)} style={{ marginRight: 8 }}>
                              {field.type.substr(0, 1).toUpperCase()}
                            </Tag>
                            {field.name}
                          </Space>
                          <EyeOutlined style={{ color: '#1890ff' }} />
                        </div>
                      )
                    })
                  }
                ]}
              />
            </>
          )}
        </Sider>
        
        {/* 右侧：分为上下结构 */}
        <Layout style={{ background: '#f0f2f5' }}>
          {/* 右侧上方：直方图 */}
          <Content style={{ background: '#fff', marginBottom: 12, display: showHistogram ? 'block' : 'none', flex: 'none', }}>
            <div style={{ padding: '8px 12px', background: '#fff', position: 'relative' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 0 }}>
                <div style={{ fontSize: 11, color: '#666' }}>
                  <InfoCircleOutlined style={{ marginRight: 4 }} />
                  直接在图表上拖拽可选择时间范围
                </div>
                <Button 
                  size="small" 
                  type="text" 
                  icon={<MinusOutlined />} 
                  onClick={() => setShowHistogram(false)}
                  style={{ padding: '0 4px' }}
                />
              </div>
              <ReactECharts 
                option={getHistogramOption()} 
                style={{ height: 150 }} 
                onEvents={onChartEvents}
              />
            </div>
          </Content>
          
          {/* 右侧下方：数据表格 */}
          <Content style={{ background: '#fff', padding: '0 0 24px 0', position: 'relative', flex: 1 }}>
            <div style={{ padding: '8px 12px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid #f0f0f0' }}>
              <div>找到 {3242423} 条记录</div>
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
                  <Tooltip title="表格视图">
                    <Button 
                      type={viewMode === 'table' ? 'primary' : 'default'} 
                      icon={<CompressOutlined />} 
                      onClick={() => setViewMode('table')}
                    />
                  </Tooltip>
                  <Tooltip title="JSON视图">
                    <Button 
                      type={viewMode === 'json' ? 'primary' : 'default'} 
                      icon={<ExpandOutlined />} 
                      onClick={() => setViewMode('json')}
                    />
                  </Tooltip>
                </Space.Compact>
              </Space>
            </div>
            
            {viewMode === 'table' ? (
              <div className="table-container-with-animation" style={{ 
                height: `calc(100vh - ${showHistogram ? 315 : 165}px)`, 
                overflowY: 'auto' 
              }} onScroll={handleScroll} ref={tableContainerRef}>
                <Table 
                  dataSource={tableData} 
                  columns={getTableColumns()} 
                  pagination={false}
                  size="small"
                  expandable={{
                    expandedRowRender: record => (
                      <div style={{ padding: 16 }}>
                        <Tabs defaultActiveKey="json">
                          <TabPane tab="JSON" key="json">
                            <pre style={{ background: '#f6f8fa', padding: 16, borderRadius: 4 }}>
                              {JSON.stringify(record, null, 2)}
                            </pre>
                          </TabPane>
                          <TabPane tab="表格" key="table">
                            <Table 
                              dataSource={Object.entries(record).filter(([key]) => key !== 'key').map(([key, value]) => ({ key, field: key, value }))} 
                              columns={[
                                { title: '字段', dataIndex: 'field', key: 'field' },
                                { title: '值', dataIndex: 'value', key: 'value' }
                              ]} 
                              pagination={false}
                              size="small"
                            />
                          </TabPane>
                        </Tabs>
                      </div>
                    )
                  }}
                  rowKey="key"
                  className="data-table-with-animation"
                  // scroll={{ y: `calc(100vh - ${showHistogram ? 315 : 220}px)` }} // 动态调整滚动区域
                />
                {loading && (
                  <div style={{ textAlign: 'center', padding: 16 }}>
                    <Spin indicator={<LoadingOutlined style={{ fontSize: 24 }} spin />} />
                  </div>
                )}
                {!hasMore && tableData.length > 0 && (
                  <div style={{ textAlign: 'center', padding: 16, color: '#999' }}>
                    已加载全部数据
                  </div>
                )}
              </div>
            ) : (
              <div 
                style={{ 
                  overflowY: 'auto', 
                  height: `calc(100vh - ${showHistogram ? 365 : 215}px)`,
                  padding: '0 16px' 
                }}
                onScroll={handleScroll}
                ref={tableContainerRef}
              >
                {tableData.map(record => (
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
                {loading && (
                  <div style={{ textAlign: 'center', padding: 16 }}>
                    <Spin indicator={<LoadingOutlined style={{ fontSize: 24 }} spin />} />
                  </div>
                )}
                {!hasMore && tableData.length > 0 && (
                  <div style={{ textAlign: 'center', padding: 16, color: '#999' }}>
                    已加载全部数据
                  </div>
                )}
              </div>
            )}
          </Content>
        </Layout>
      </Layout>
      
      <Outlet />
    </Layout>
    
    {/* 添加过滤器对话框 */}
    <Modal
      title="添加过滤器"
      open={showFilterModal}
      onCancel={() => setShowFilterModal(false)}
      onOk={() => filterForm.submit()}
      okText="添加"
      cancelText="取消"
      destroyOnClose
    >
      <Form 
        form={filterForm} 
        layout="vertical" 
        onFinish={addFilter}
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
              <Option key={field.name} value={field.name}>
                <Space>
                  <Tag color={getFieldTypeColor(field.type)} style={{ marginRight: 0 }}>
                    {field.type.substr(0, 1).toUpperCase()}
                  </Tag>
                  {field.name}
                </Space>
              </Option>
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
            onChange={(value) => setSelectedFilterOperator(value as FilterOperator)}
          >
            {selectedFilterField && 
              getOperatorsByFieldType(getFieldType(selectedFilterField)).map(op => (
                <Option key={op.value} value={op.value}>{op.label}</Option>
              ))
            }
            {!selectedFilterField && 
              getOperatorsByFieldType().map(op => (
                <Option key={op.value} value={op.value}>{op.label}</Option>
              ))
            }
          </Select>
        </Form.Item>
        
        {/* 根据不同的操作符类型显示不同的输入控件 */}
        {selectedFilterOperator !== 'exists' && selectedFilterOperator !== 'does_not_exist' && (
          <Form.Item 
            name="value" 
            label="值" 
            rules={[
              { 
                required: ['is', 'is_not', 'contains', 'does_not_contain', 'greater_than', 'less_than'].includes(selectedFilterOperator), 
                message: '请输入值' 
              }
            ]}
          >
            {selectedFilterOperator === 'is_between' ? (
              <div style={{ display: 'flex', gap: 8 }}>
                <Input placeholder="起始值" style={{ flex: 1 }} />
                <Input placeholder="结束值" style={{ flex: 1 }} />
              </div>
            ) : selectedFilterOperator === 'is_one_of' || selectedFilterOperator === 'is_not_one_of' ? (
              <Select
                mode="tags"
                placeholder="输入多个值，按回车键分隔"
                style={{ width: '100%' }}
                tokenSeparators={[',']}
              />
            ) : (
              <Input placeholder="输入值" />
            )}
          </Form.Item>
        )}
      </Form>
    </Modal>
    </>
  )
}

export default HomePage
