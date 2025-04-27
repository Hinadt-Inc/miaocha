import { useState } from 'react'
import { Row, Col, Segmented, DatePicker, Skeleton, Space, Table, Card, Button, Dropdown } from 'antd'
import { ArrowUpOutlined, ArrowDownOutlined, ReloadOutlined, DownOutlined } from '@ant-design/icons'
import type { MenuProps } from 'antd'
import type { SegmentedValue } from 'antd/es/segmented'
import type { EChartsOption } from 'echarts-for-react'

// 导入优化的组件
import StatCard from '../components/common/StatCard'
import OptimizedChart from '../components/common/OptimizedChart'
import PageContainer from '../components/common/PageContainer'
import CardGrid from '../components/common/CardGrid'
import AnimatedNumber from '../components/common/AnimatedNumber'

// 导入主题钩子
import { useTheme } from '../providers/ThemeProvider'

const { RangePicker } = DatePicker

// 定义趋势样式类
const upTrendStyle = { color: '#3f8600' };
const downTrendStyle = { color: '#cf1322' };

const DashboardPage = () => {
  const [loading, setLoading] = useState(false)
  const [timeRange, setTimeRange] = useState<SegmentedValue>('week')
  // 使用主题钩子
  const { isDarkMode } = useTheme()

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
      left: 'center',
      textStyle: {
        color: isDarkMode ? 'rgba(255, 255, 255, 0.85)' : 'rgba(0, 0, 0, 0.85)'
      }
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    legend: {
      data: ['销售额', '订单量'],
      top: 'bottom',
      textStyle: {
        color: isDarkMode ? 'rgba(255, 255, 255, 0.65)' : 'rgba(0, 0, 0, 0.65)'
      }
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
        data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
        axisLine: {
          lineStyle: {
            color: isDarkMode ? 'rgba(255, 255, 255, 0.2)' : 'rgba(0, 0, 0, 0.15)'
          }
        },
        axisLabel: {
          color: isDarkMode ? 'rgba(255, 255, 255, 0.65)' : 'rgba(0, 0, 0, 0.65)'
        }
      }
    ],
    yAxis: [
      {
        type: 'value',
        name: '销售额',
        position: 'left',
        axisLine: {
          lineStyle: {
            color: isDarkMode ? 'rgba(255, 255, 255, 0.2)' : 'rgba(0, 0, 0, 0.15)'
          }
        },
        axisLabel: {
          color: isDarkMode ? 'rgba(255, 255, 255, 0.65)' : 'rgba(0, 0, 0, 0.65)'
        },
        splitLine: {
          lineStyle: {
            color: isDarkMode ? 'rgba(255, 255, 255, 0.1)' : 'rgba(0, 0, 0, 0.05)'
          }
        }
      },
      {
        type: 'value',
        name: '订单量',
        position: 'right',
        axisLine: {
          lineStyle: {
            color: isDarkMode ? 'rgba(255, 255, 255, 0.2)' : 'rgba(0, 0, 0, 0.15)'
          }
        },
        axisLabel: {
          color: isDarkMode ? 'rgba(255, 255, 255, 0.65)' : 'rgba(0, 0, 0, 0.65)'
        },
        splitLine: {
          show: false
        }
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
        data: [12000, 13200, 10100, 13400, 19000, 23400, 14000],
        lineStyle: {
          width: 3,
          shadowColor: 'rgba(0, 0, 0, 0.3)',
          shadowBlur: 10,
          shadowOffsetY: 5
        },
        itemStyle: {
          borderWidth: 2
        }
      },
      {
        name: '订单量',
        type: 'bar',
        emphasis: {
          focus: 'series'
        },
        yAxisIndex: 1,
        data: [120, 132, 101, 134, 190, 230, 140],
        itemStyle: {
          borderRadius: 4
        }
      }
    ]
  }

  // 销售类别饼图配置
  const categoryOption: EChartsOption = {
    title: {
      text: '销售类别占比',
      left: 'center',
      textStyle: {
        color: isDarkMode ? 'rgba(255, 255, 255, 0.85)' : 'rgba(0, 0, 0, 0.85)'
      }
    },
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b} : {c} ({d}%)'
    },
    legend: {
      bottom: '0%',
      left: 'center',
      textStyle: {
        color: isDarkMode ? 'rgba(255, 255, 255, 0.65)' : 'rgba(0, 0, 0, 0.65)'
      }
    },
    series: [
      {
        name: '销售额',
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['50%', '50%'],
        itemStyle: {
          borderRadius: 10,
          borderColor: isDarkMode ? '#1f1f1f' : '#fff',
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
        },
        label: {
          color: isDarkMode ? 'rgba(255, 255, 255, 0.85)' : 'rgba(0, 0, 0, 0.85)'
        }
      }
    ]
  }

  // 地区分布图
  const regionOption: EChartsOption = {
    title: {
      text: '销售区域分布',
      left: 'center',
      textStyle: {
        color: isDarkMode ? 'rgba(255, 255, 255, 0.85)' : 'rgba(0, 0, 0, 0.85)'
      }
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
      boundaryGap: [0, 0.01],
      axisLine: {
        lineStyle: {
          color: isDarkMode ? 'rgba(255, 255, 255, 0.2)' : 'rgba(0, 0, 0, 0.15)'
        }
      },
      axisLabel: {
        color: isDarkMode ? 'rgba(255, 255, 255, 0.65)' : 'rgba(0, 0, 0, 0.65)'
      },
      splitLine: {
        lineStyle: {
          color: isDarkMode ? 'rgba(255, 255, 255, 0.1)' : 'rgba(0, 0, 0, 0.05)'
        }
      }
    },
    yAxis: {
      type: 'category',
      data: ['北京', '上海', '广州', '深圳', '杭州', '成都', '武汉', '其他'],
      axisLine: {
        lineStyle: {
          color: isDarkMode ? 'rgba(255, 255, 255, 0.2)' : 'rgba(0, 0, 0, 0.15)'
        }
      },
      axisLabel: {
        color: isDarkMode ? 'rgba(255, 255, 255, 0.65)' : 'rgba(0, 0, 0, 0.65)'
      }
    },
    series: [
      {
        name: '销售额',
        type: 'bar',
        data: [18203, 23489, 29034, 10498, 12380, 14000, 13050, 17000],
        itemStyle: {
          borderRadius: [0, 4, 4, 0],
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 1,
            y2: 0,
            colorStops: [
              { offset: 0, color: '#1677ff' },
              { offset: 1, color: '#69c0ff' }
            ]
          }
        }
      }
    ]
  }

  // 页面标题区域的额外内容
  const titleExtra = (
    <Space wrap>
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
  );

  return (
    <PageContainer 
      title="数据仪表盘"
      extra={titleExtra}
    >
      <Skeleton loading={loading} active paragraph={{ rows: 16 }}>
        {/* 使用新的卡片网格和统计卡片组件 */}
        <CardGrid>
          <StatCard
            title="总销售额"
            value={<AnimatedNumber value={112893} formatter={(val) => `¥${val.toFixed(2)}`} />}
            icon={<span>¥</span>}
            trend="up"
          />
          <StatCard
            title="订单数量"
            value={<AnimatedNumber value={1893} />}
            icon={<span>N</span>}
            trend="up"
          />
          <StatCard
            title="访问量"
            value={<AnimatedNumber value={8846} />}
            icon={<span>V</span>}
            trend="up"
          />
          <StatCard
            title="转化率"
            value={<AnimatedNumber value={21.8} formatter={(val) => `${val.toFixed(1)}%`} />}
            icon={<span>R</span>}
            trend="down"
          />
        </CardGrid>

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
              className="hoverable"
            >
              <OptimizedChart 
                option={salesTrendOption} 
                style={{ height: 340 }} 
              />
            </Card>
          </Col>
          <Col xs={24} md={8}>
            <Card 
              title="销售类别占比" 
              className="hoverable"
            >
              <OptimizedChart 
                option={categoryOption} 
                style={{ height: 340 }} 
              />
            </Card>
          </Col>
        </Row>

        <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
          <Col xs={24} md={12}>
            <Card 
              title="区域销售分布" 
              className="hoverable"
            >
              <OptimizedChart 
                option={regionOption} 
                style={{ height: 340 }} 
              />
            </Card>
          </Col>
          <Col xs={24} md={12}>
            <Card 
              title="销售排行榜" 
              className="hoverable"
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
    </PageContainer>
  )
}

// 模拟数据 - 销售排行榜
const topProducts = [
  {
    key: '1',
    rank: 1,
    name: '高端智能手机 Pro Max',
    sales: 1245,
    trend: <span style={upTrendStyle}><ArrowUpOutlined /> 12%</span>
  },
  {
    key: '2',
    rank: 2,
    name: '智能手表 Series 5',
    sales: 983,
    trend: <span style={upTrendStyle}><ArrowUpOutlined /> 8%</span>
  },
  {
    key: '3',
    rank: 3,
    name: '无线蓝牙耳机',
    sales: 873,
    trend: <span style={downTrendStyle}><ArrowDownOutlined /> 2%</span>
  },
  {
    key: '4',
    rank: 4,
    name: '平板电脑 Air',
    sales: 654,
    trend: <span style={upTrendStyle}><ArrowUpOutlined /> 15%</span>
  },
  {
    key: '5',
    rank: 5,
    name: '智能音响',
    sales: 538,
    trend: <span style={downTrendStyle}><ArrowDownOutlined /> 5%</span>
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
    sorter: (a: {sales: number}, b: {sales: number}) => a.sales - b.sales,
  },
  {
    title: '趋势',
    dataIndex: 'trend',
    key: 'trend',
    align: 'right' as const,
  },
]

export default DashboardPage
