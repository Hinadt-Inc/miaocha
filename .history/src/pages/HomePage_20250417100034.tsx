import { useState } from 'react'
import { 
  Layout, 
  Input, 
  Select, 
  Space, 
  DatePicker, 
  Button, 
  Menu, 
  Table, 
  Card, 
  Collapse, 
  Tag, 
  Tabs, 
  Tooltip, 
  Divider 
} from 'antd'
import { 
  SearchOutlined, 
  FilterOutlined, 
  DownloadOutlined, 
  SaveOutlined, 
  ReloadOutlined,
  EyeOutlined,
  StarOutlined,
  ExpandOutlined,
  CompressOutlined,
  InfoCircleOutlined,
  PlusOutlined,
  MinusOutlined
} from '@ant-design/icons'
import ReactECharts from 'echarts-for-react'
import { Outlet } from 'react-router-dom'

const { Content, Sider } = Layout
const { RangePicker } = DatePicker
const { Panel } = Collapse
const { TabPane } = Tabs

export default function HomePage() {
  const [collapsed, setCollapsed] = useState(false)
  const [selectedFields, setSelectedFields] = useState<string[]>(['timestamp', 'message', 'host', 'source'])
  const [viewMode, setViewMode] = useState<'table' | 'json'>('table')
  const [searchQuery, setSearchQuery] = useState('')
  const [showHistogram, setShowHistogram] = useState(true)

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
  const getHistogramOption = () => {
    const dates = Array(14).fill(null).map((_, i) => `2025-04-${i + 1}`)
    const values = Array(14).fill(null).map(() => Math.floor(Math.random() * 100) + 20)
    
    return {
      grid: {
        top: 10,
        right: 10,
        bottom: 20,
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
        color: '#1890ff'
      }],
      tooltip: {
        trigger: 'axis'
      }
    }
  }

  // 根据选中的字段生成表格列
  const getTableColumns = () => {
    return selectedFields.map(field => ({
      title: field,
      dataIndex: field,
      key: field,
      render: (text: any, record: any) => {
        if (field === 'timestamp') {
          return <span style={{ color: '#1890ff' }}>{text}</span>
        }
        if (field === 'status') {
          return <Tag color={text === 200 ? 'green' : (text === 404 ? 'orange' : 'red')}>{text}</Tag>
        }
        return text
      }
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
      setSelectedFields(selectedFields.filter(f => f !== fieldName))
    } else {
      setSelectedFields([...selectedFields, fieldName])
    }
  }

  return (
    <Layout style={{ background: '#f0f2f5', minHeight: 'calc(100vh - 64px)' }}>
      <div style={{ padding: '16px 24px', background: '#fff', boxShadow: '0 1px 4px rgba(0, 21, 41, 0.08)' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
          <Space size="middle">
            <Input.Search
              placeholder="搜索查询语句"
              allowClear
              enterButton={<SearchOutlined />}
              size="middle"
              style={{ width: 400 }}
              value={searchQuery}
              onChange={e => setSearchQuery(e.target.value)}
            />
            <RangePicker showTime style={{ width: 400 }} placeholder={['开始时间', '结束时间']} />
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
            <Button type="link" icon={<PlusOutlined />}>添加过滤器</Button>
          </Space>
        </div>
      </div>
      
      {showHistogram && (
        <div style={{ padding: '12px 24px', background: '#fff', margin: '12px 0', position: 'relative' }}>
          <Button 
            size="small" 
            type="text" 
            icon={<MinusOutlined />} 
            style={{ position: 'absolute', right: 10, top: 10, zIndex: 1 }}
            onClick={() => setShowHistogram(false)}
          />
          <ReactECharts option={getHistogramOption()} style={{ height: 100 }} />
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
                        marginBottom: 4
                      }}
                      onClick={() => toggleFieldSelection(field.name)}
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
            />
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
