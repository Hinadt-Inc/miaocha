import { useState, useEffect } from 'react'
import { 
  AutoComplete,
  Dropdown,
  Layout, 
  Input, 
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
  message 
} from 'antd'
import { 
  SearchOutlined, 
  DownloadOutlined, 
  SaveOutlined, 
  ReloadOutlined,
  EyeOutlined,
  ExpandOutlined,
  CompressOutlined,
  InfoCircleOutlined,
  PlusOutlined,
  MinusOutlined
} from '@ant-design/icons'
import ReactECharts from 'echarts-for-react'
import { Outlet } from 'react-router-dom'
import type { EChartsOption } from 'echarts'
import dayjs from 'dayjs'
import './HomePage.less'

const { Content, Sider } = Layout
const { RangePicker } = DatePicker
const { Panel } = Collapse
const { TabPane } = Tabs

export default function HomePage() {
  const [collapsed, setCollapsed] = useState(false)
  const [selectedFields, setSelectedFields] = useState<string[]>(['timestamp', 'message', 'host', 'source'])
  const [viewMode, setViewMode] = useState<'table' | 'json'>('table')
  const [searchQuery, setSearchQuery] = useState('')
  const [searchHistory, setSearchHistory] = useState<string[]>([])
  const [showHistogram, setShowHistogram] = useState(true)
  const [timeRange, setTimeRange] = useState<[string, string] | null>(null)
  const [showTimePicker, setShowTimePicker] = useState(false)
  const [selectedTable, setSelectedTable] = useState<string>('')
  const [selectedField, setSelectedField] = useState<string>('')
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

  // 模拟数据
  const mockData = Array(20).fill(null).map((_, index) => ({
    key: index,
    timestamp: `2025-04-${(index % 14) + 1} ${(index % 24)}:${(index % 60).toString().padStart(2, '0')}:00`,
    message: `这是日志消息 ${index}`,
    host: `server-${index % 5}.example.com`,
    source: index % 3 === 0 ? 'nginx' : (index % 3 === 1 ? 'application' : 'database'),
    status: index % 10 === 0 ? 500 : (index % 5 === 0 ? 404 : 200),
    bytes: Math.floor(Math.random() * 10000),
    response_time: Math.round(Math.random() * 1000) / 10,
    ip: `192.168.1.${index % 256}`,
    method: index % 4 === 0 ? 'POST' : (index % 3 === 0 ? 'PUT' : 'GET'),
    path: index % 3 === 0 ? '/api/users' : (index % 5 === 0 ? '/api/products' : '/api/orders'),
    user_agent: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)',
    referer: 'https://example.com',
    'geo.country': 'China',
    'geo.city': index % 3 === 0 ? 'Beijing' : (index % 3 === 1 ? 'Shanghai' : 'Guangzhou'),
  }))

  // 生成直方图数据
  const getHistogramOption = (): EChartsOption => {
    const dates = Array(14).fill(null).map((_, i) => `2025-04-${i + 1}`)
    const values = Array(14).fill(null).map(() => Math.floor(Math.random() * 100) + 20)
    
    return {
      grid: {
        top: 35,
        right: 40,
        bottom: 60,
        left: 50,
      },
      xAxis: {
        type: 'category',
        data: dates,
        axisLabel: {
          interval: 2
        }
      },
      yAxis: {
        type: 'value'
      },
      series: [{
        data: values,
        type: 'bar',
        color: '#1890ff',
        name: '数据条数',
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
      toolbox: {
        feature: {
          dataZoom: {
            yAxisIndex: 'none',
            title: {
              zoom: '区域缩放',
              back: '还原缩放'
            },
            // 移除自定义图标定义，使用默认图标
            icon: {}
          },
          restore: {
            title: '重置'
          },
          saveAsImage: {
            title: '保存为图片'
          }
        },
        right: 15,
        top: 0
      },
      dataZoom: [
        {
          type: 'slider',
          show: true,
          xAxisIndex: [0],
          start: 0,
          end: 100,
          height: 20,
          bottom: 10,
          borderColor: 'transparent',
          backgroundColor: '#e6e6e6',
          fillerColor: 'rgba(24, 144, 255, 0.2)',
          handleIcon: 'M10.7,11.9v-1.3H9.3v1.3c-4.9,0.3-8.8,4.4-8.8,9.4c0,5,3.9,9.1,8.8,9.4v1.3h1.3v-1.3c4.9-0.3,8.8-4.4,8.8-9.4C19.5,16.3,15.6,12.2,10.7,11.9z M13.3,24.4H6.7V23h6.6V24.4z M13.3,19.6H6.7v-1.4h6.6V19.6z',
          handleSize: '80%',
          handleStyle: {
            color: '#1890ff'
          },
          textStyle: {
            color: 'rgba(0, 0, 0, 0.65)'
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

  // 当时间范围改变时更新 DatePicker
  useEffect(() => {
    if (timeRange) {
      console.log('Selected time range:', timeRange)
    }
  }, [timeRange])

  return (
    <Layout style={{ background: '#f0f2f5', minHeight: 'calc(100vh - 64px)' }}>
      <div style={{ padding: '16px 24px', background: '#fff', boxShadow: '0 1px 4px rgba(0, 21, 41, 0.08)' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
          <Space size="middle">
            <Space.Compact style={{ width: '100%' }}>
              <Select
                placeholder="选择表"
                style={{ width: 120 }}
                value={selectedTable}
                onChange={value => {
                  setSelectedTable(value)
                  setSelectedField('')
                }}
                options={availableTables.map(table => ({
                  label: table.name,
                  value: table.name
                }))}
              />
              <Select
                placeholder="选择字段"
                style={{ width: 120 }}
                value={selectedField}
                onChange={setSelectedField}
                disabled={!selectedTable}
                options={
                  selectedTable 
                    ? availableTables
                        .find(t => t.name === selectedTable)
                        ?.fields.map(field => ({
                          label: field,
                          value: field
                        })) || []
                    : []
                }
              />
              <AutoComplete
                placeholder="输入搜索内容"
                style={{ width: 300 }}
                value={searchQuery}
                onChange={setSearchQuery}
                options={searchHistory.map(query => ({
                  value: query,
                  label: query
                }))}
                onSearch={text => {
                  if (text && !searchHistory.includes(text)) {
                    setSearchHistory(prev => [text, ...prev].slice(0, 10))
                  }
                }}
              />
              <Button icon={<SearchOutlined />} type="primary" />
            </Space.Compact>
            
            <Dropdown
              overlay={
                <Menu>
                  <Menu.Item key="15m" onClick={() => {
                    const now = dayjs()
                    setTimeRange([
                      now.subtract(15, 'minute').format('YYYY-MM-DD HH:mm:ss'),
                      now.format('YYYY-MM-DD HH:mm:ss')
                    ])
                  }}>
                    最近15分钟
                  </Menu.Item>
                  <Menu.Item key="1h" onClick={() => {
                    const now = dayjs()
                    setTimeRange([
                      now.subtract(1, 'hour').format('YYYY-MM-DD HH:mm:ss'),
                      now.format('YYYY-MM-DD HH:mm:ss')
                    ])
                  }}>
                    最近1小时
                  </Menu.Item>
                  <Menu.Item key="custom" onClick={() => setShowTimePicker(true)}>
                    自定义时间
                  </Menu.Item>
                </Menu>
              }
              trigger={['click']}
              visible={showTimePicker}
              onVisibleChange={setShowTimePicker}
            >
              <Button>
                {timeRange 
                  ? `${dayjs(timeRange[0]).format('YYYY-MM-DD HH:mm:ss')} ~ ${dayjs(timeRange[1]).format('YYYY-MM-DD HH:mm:ss')}`
                  : '选择时间段'}
              </Button>
            </Dropdown>
            
            {showTimePicker && (
              <RangePicker
                showTime={{ format: 'HH:mm:ss' }}
                format="YYYY-MM-DD HH:mm:ss"
                style={{ width: 400, position: 'absolute', zIndex: 1000 }}
                onOk={values => {
                  if (values && values[0] && values[1]) {
                    setTimeRange([
                      values[0].format('YYYY-MM-DD HH:mm:ss'),
                      values[1].format('YYYY-MM-DD HH:mm:ss')
                    ])
                  }
                  setShowTimePicker(false)
                }}
                onCancel={() => setShowTimePicker(false)}
              />
            )}
          </Space>
          <Space>
            <Button icon={<ReloadOutlined />}>刷新</Button>
            <Button icon={<SaveOutlined />}>保存</Button>
            <Button icon={<DownloadOutlined />}>导出</Button>
          </Space>
        </div>
        <div>
          <Space>
            <Tag color="blue" closable>来源: nginx</Tag>
            <Tag color="green" closable>状态: 200</Tag>
            {timeRange && (
              <Tag color="purple" closable onClose={() => setTimeRange(null)}>
                时间范围: {timeRange[0]} 至 {timeRange[1]}
              </Tag>
            )}
            <Button type="link" icon={<PlusOutlined />}>添加过滤器</Button>
          </Space>
        </div>
      </div>
      
      {showHistogram && (
        <div style={{ padding: '12px 24px', background: '#fff', margin: '12px 0', position: 'relative' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 5 }}>
            <div style={{ fontSize: 12, color: '#666' }}>
              <InfoCircleOutlined style={{ marginRight: 5 }} />
              提示：直接在图表上拖拽可以选择时间范围
            </div>
            <Button 
              size="small" 
              type="text" 
              icon={<MinusOutlined />} 
              onClick={() => setShowHistogram(false)}
            />
          </div>
          <ReactECharts 
            option={getHistogramOption()} 
            style={{ height: 180 }} 
            onEvents={onChartEvents}
          />
        </div>
      )}
      
      <Layout>
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
                <Input.Search
                  placeholder="搜索字段"
                  size="small"
                />
              </div>
              
              <Collapse defaultActiveKey={['available']} ghost>
                <Panel header="可用字段" key="available">
                  {availableFields.map(field => (
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
                  ))}
                </Panel>
                <Panel header="已选字段" key="selected">
                  {selectedFields.map(fieldName => {
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
                  })}
                </Panel>
              </Collapse>
            </>
          )}
        </Sider>
        
        <Content style={{ padding: '0 0 24px 0', background: '#fff', position: 'relative' }}>
          <div style={{ padding: '12px 16px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid #f0f0f0' }}>
            <div>找到 {mockData.length} 条记录</div>
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
              <Button.Group>
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
              </Button.Group>
            </Space>
          </div>
          
          {viewMode === 'table' ? (
            <div className="table-container-with-animation">
              <Table 
                dataSource={mockData} 
                columns={getTableColumns()} 
                pagination={{ pageSize: 10 }}
                size="middle"
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
              />
            </div>
          ) : (
            <div style={{ overflow: 'auto', height: 'calc(100vh - 280px)' }}>
              {mockData.map(record => (
                <Card 
                  key={record.key} 
                  style={{ margin: '8px 16px', borderLeft: '4px solid #1890ff' }} 
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
        </Content>
      </Layout>
      
      <Outlet />
    </Layout>
  )
}
