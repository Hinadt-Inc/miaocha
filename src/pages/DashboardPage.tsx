import { useState } from 'react'
import { Layout, Card, Row, Col, Statistic, Button, Dropdown, Segmented, DatePicker, Typography, Skeleton } from 'antd'
import { ArrowUpOutlined, ArrowDownOutlined, ReloadOutlined, DownOutlined } from '@ant-design/icons'
import ReactECharts from 'echarts-for-react'
import type { MenuProps } from 'antd'
import type { SegmentedValue } from 'antd/es/segmented'
import type { EChartsOption } from 'echarts-for-react'

const { Content } = Layout
const { Title, Text } = Typography
const { RangePicker } = DatePicker

const DashboardPage = () => {
  const [loading, setLoading] = useState(false)
  const [timeRange, setTimeRange] = useState<SegmentedValue>('week')

  // 模拟加载数据
  const refreshData = () => {
    setLoading(true)
    setTimeout(() => {
      setLoading(false)
    }, 1500)
  }

  const dateRangeItems: MenuProps['items'] = [
    {
      key: '1',
      label: '今日',
    },
    {
      key: '2',
      label: '本周',
    },
    {
      key: '3',
      label: '本月',
    },
    {
      key: '4',
      label: '本季度',
    },
    {
      key: '5',
      label: '本年',
    },
    {
      key: '6',
      label: '自定义',
    },
  ]

  // 销售趋势图表配置
  const salesTrendOption: EChartsOption = {
    title: {
      text: '销售趋势',
      left: 'center'
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    legend: {
      data: ['销售额', '订单量'],
      top: 'bottom'
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '12%',
      top: '15%',
      containLabel: true
    },
    xAxis: [
      {
        type: 'category',
        data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
      }
    ],
    yAxis: [
      {
        type: 'value',
        name: '销售额',
        position: 'left'
      },
      {
        type: 'value',
        name: '订单量',
        position: 'right'
      }
    ],
    series: [
      {
        name: '销售额',
        type: 'line',
        smooth: true,
        emphasis: {
          focus: 'series'
        },
        data: [12000, 13200, 10100, 13400, 19000, 23400, 14000]
      },
      {
        name: '订单量',
        type: 'bar',
        emphasis: {
          focus: 'series'
        },
        yAxisIndex: 1,
        data: [120, 132, 101, 134, 190, 230, 140]
      }
    ]
  }

  // 销售类别饼图配置
  const categoryOption: EChartsOption = {
    title: {
      text: '销售类别占比',
      left: 'center'
    },
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b} : {c} ({d}%)'
    },
    legend: {
      bottom: '0%',
      left: 'center'
    },
    series: [
      {
        name: '销售额',
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['50%', '50%'],
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        data: [
          { value: 1048, name: '服装' },
          { value: 735, name: '电子' },
          { value: 580, name: '家居' },
          { value: 484, name: '食品' },
          { value: 300, name: '其他' }
        ],
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }
    ]
  }

  // 地区分布图
  const regionOption: EChartsOption = {
    title: {
      text: '销售区域分布',
      left: 'center'
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'value',
      boundaryGap: [0, 0.01]
    },
    yAxis: {
      type: 'category',
      data: ['北京', '上海', '广州', '深圳', '杭州', '成都', '武汉', '其他']
    },
    series: [
      {
        name: '销售额',
        type: 'bar',
        data: [18203, 23489, 29034, 10498, 12380, 14000, 13050, 17000]
      }
    ]
  }

  return (
    <Content>
      <div className="dashboard-header" style={{ marginBottom: 24 }}>
        <Row gutter={[16, 16]} align="middle">
          <Col xs={24} sm={12}>
            <Title level={3} style={{ margin: 0 }}>数据仪表盘</Title>
            <Text type="secondary">查看关键业务指标和分析数据</Text>
          </Col>
          <Col xs={24} sm={12} style={{ textAlign: 'right' }}>
            <Space>
              <Segmented
                options={['今日', '本周', '本月', '本季度']}
                value={timeRange}
                onChange={setTimeRange}
              />
              <RangePicker />
              <Button
                type="primary"
                icon={<ReloadOutlined />}
                loading={loading}
                onClick={refreshData}
              >
                刷新数据
              </Button>
            </Space>
          </Col>
        </Row>
      </div>

      <Skeleton loading={loading} active paragraph={{ rows: 16 }}>
        {/* 核心指标统计卡片 */}
        <Row gutter={[16, 16]}>
          <Col xs={24} sm={12} md={6}>
            <Card bordered={false} hoverable>
              <Statistic
                title="总销售额"
                value={112893}
                precision={2}
                valueStyle={{ color: '#3f8600' }}
                prefix="¥"
                suffix={
                  <span style={{ fontSize: 14, marginLeft: 8 }}>
                    <ArrowUpOutlined /> 8.2%
                  </span>
                }
              />
              <div style={{ marginTop: 8 }}>
                <Text type="secondary">
                  日销售额 ¥12,423
                </Text>
              </div>
            </Card>
          </Col>
          <Col xs={24} sm={12} md={6}>
            <Card bordered={false} hoverable>
              <Statistic
                title="订单数量"
                value={1893}
                valueStyle={{ color: '#0958d9' }}
                suffix={
                  <span style={{ fontSize: 14, marginLeft: 8 }}>
                    <ArrowUpOutlined /> 12.5%
                  </span>
                }
              />
              <div style={{ marginTop: 8 }}>
                <Text type="secondary">
                  日订单量 258
                </Text>
              </div>
            </Card>
          </Col>
          <Col xs={24} sm={12} md={6}>
            <Card bordered={false} hoverable>
              <Statistic
                title="访问量"
                value={8846}
                valueStyle={{ color: '#722ed1' }}
                suffix={
                  <span style={{ fontSize: 14, marginLeft: 8 }}>
                    <ArrowUpOutlined /> 32.7%
                  </span>
                }
              />
              <div style={{ marginTop: 8 }}>
                <Text type="secondary">
                  日访问量 1,234
                </Text>
              </div>
            </Card>
          </Col>
          <Col xs={24} sm={12} md={6}>
            <Card bordered={false} hoverable>
              <Statistic
                title="转化率"
                value={21.8}
                precision={1}
                valueStyle={{ color: '#cf1322' }}
                suffix="%"
                prefix={
                  <span style={{ fontSize: 14, marginRight: 8 }}>
                    <ArrowDownOutlined />
                  </span>
                }
              />
              <div style={{ marginTop: 8 }}>
                <Text type="secondary">
                  较上周下降 2.3%
                </Text>
              </div>
            </Card>
          </Col>
        </Row>

        {/* 图表区域 */}
        <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
          <Col xs={24} md={16}>
            <Card 
              title="销售趋势分析" 
              extra={
                <Segmented
                  options={['周', '月', '季']}
                  size="small"
                />
              }
              bordered={false}
            >
              <ReactECharts option={salesTrendOption} style={{ height: 340 }} />
            </Card>
          </Col>
          <Col xs={24} md={8}>
            <Card title="销售类别占比" bordered={false}>
              <ReactECharts option={categoryOption} style={{ height: 340 }} />
            </Card>
          </Col>
        </Row>

        <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
          <Col xs={24} md={12}>
            <Card title="区域销售分布" bordered={false}>
              <ReactECharts option={regionOption} style={{ height: 340 }} />
            </Card>
          </Col>
          <Col xs={24} md={12}>
            <Card 
              title="销售排行榜" 
              bordered={false}
              extra={
                <Dropdown menu={{ items: dateRangeItems }}>
                  <Button type="text">
                    <Space>
                      本月
                      <DownOutlined />
                    </Space>
                  </Button>
                </Dropdown>
              }
            >
              <Table
                dataSource={topProducts}
                columns={topProductColumns}
                pagination={false}
                size="small"
              />
            </Card>
          </Col>
        </Row>
      </Skeleton>
    </Content>
  )
}

// 模拟数据 - 销售排行榜
const topProducts = [
  {
    key: '1',
    rank: 1,
    name: '高端智能手机 Pro Max',
    sales: 1245,
    trend: <span style={{color: '#3f8600'}}><ArrowUpOutlined /> 12%</span>
  },
  {
    key: '2',
    rank: 2,
    name: '智能手表 Series 5',
    sales: 983,
    trend: <span style={{color: '#3f8600'}}><ArrowUpOutlined /> 8%</span>
  },
  {
    key: '3',
    rank: 3,
    name: '无线蓝牙耳机',
    sales: 873,
    trend: <span style={{color: '#cf1322'}}><ArrowDownOutlined /> 2%</span>
  },
  {
    key: '4',
    rank: 4,
    name: '平板电脑 Air',
    sales: 654,
    trend: <span style={{color: '#3f8600'}}><ArrowUpOutlined /> 15%</span>
  },
  {
    key: '5',
    rank: 5,
    name: '智能音响',
    sales: 538,
    trend: <span style={{color: '#cf1322'}}><ArrowDownOutlined /> 5%</span>
  },
]

// 表格列定义
const topProductColumns = [
  {
    title: '排名',
    dataIndex: 'rank',
    key: 'rank',
    width: 80,
  },
  {
    title: '产品名称',
    dataIndex: 'name',
    key: 'name',
  },
  {
    title: '销售量',
    dataIndex: 'sales',
    key: 'sales',
    sorter: (a, b) => a.sales - b.sales,
  },
  {
    title: '趋势',
    dataIndex: 'trend',
    key: 'trend',
    align: 'right' as const,
  },
]

import { Space, Table } from 'antd'

export default DashboardPage
