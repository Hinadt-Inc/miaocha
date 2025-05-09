import ReactECharts from 'echarts-for-react';
import { EChartsOption } from 'echarts';
import { message } from 'antd';
import { useMemo, useEffect } from 'react';

interface HistogramChartProps {
  show: boolean;
  onTimeRangeChange: (range: [string, string]) => void;
  onToggle: () => void;
  distributionData?: Array<{
    timePoint: string;
    count: number;
  }>;
  timeGrouping?: 'minute' | 'hour' | 'day' | 'month';
}

export const HistogramChart = ({ 
  show,
  onTimeRangeChange,  distributionData,
  timeGrouping = 'minute'
}: HistogramChartProps) => {
  // 增强调试日志，检查组件接收到的数据
  useEffect(() => {
    console.log('HistogramChart 详细数据:', {
      show,
      distributionData,
      dataLength: distributionData ? distributionData.length : 0,
      dataValid: distributionData && Array.isArray(distributionData) && distributionData.length > 0,
      firstItem: distributionData && distributionData.length > 0 ? distributionData[0] : null
    });
  }, [show, distributionData]);
  
  // 根据timeGrouping聚合数据
  const aggregatedData = useMemo(() => {
    if (!distributionData || distributionData.length === 0) {
      console.log('没有分布数据，返回空结果');
      return { groupedData: [], groupLabels: [], originalData: [] };
    }

    // 当只有一个时间点时，直接使用该时间点
    if (distributionData.length === 1) {
      const timePoint = distributionData[0].timePoint;
      const count = distributionData[0].count;
      return {
        groupedData: [count],
        groupLabels: [timePoint],
        originalData: [{ timePoint, count }]
      };
    }

    // 按不同粒度聚合数据
    const groupMap = new Map<string, number>();
    const original: Array<{timePoint: string; count: number}> = [];

    // 对数据按时间排序
    const sortedData = [...distributionData].sort((a, b) => {
      return new Date(a.timePoint).getTime() - new Date(b.timePoint).getTime();
    });

    // 聚合数据
    sortedData.forEach(item => {
      const date = new Date(item.timePoint);
      let groupKey = '';
      
      switch(timeGrouping) {
        case 'minute':
          groupKey = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
          break;
        case 'hour':
          groupKey = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:00`;
          break;
        case 'day':
          groupKey = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
          break;
        case 'month':
          groupKey = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
          break;
        default:
          groupKey = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:00`;
      }
      
      if (!groupMap.has(groupKey)) {
        groupMap.set(groupKey, 0);
      }
      
      groupMap.set(groupKey, (groupMap.get(groupKey) || 0) + item.count);
      original.push(item);
    });

    // 转换为数组
    const groupLabels = Array.from(groupMap.keys());
    const groupedData = Array.from(groupMap.values());

    return {
      groupedData,
      groupLabels,
      originalData: original
    };
  }, [distributionData]);

  // 构建图表选项
  const option = useMemo<EChartsOption>(() => ({
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
      type: 'category',
      data: aggregatedData.groupLabels,
      axisTick: {
        alignWithLabel: true
      },
      axisLabel: {
        formatter: (value: string) => {
          // 根据分组粒度调整x轴标签显示格式
          switch(timeGrouping) {
            case 'minute':
              return value.substring(11, 16); // 显示时分
            case 'hour':
              return value.substring(11, 13) + ':00'; // 显示小时
            case 'day':
              return value.substring(5, 10); // 显示月日
            case 'month':
              return value.substring(5, 7) + '月'; // 显示月份
            default:
              return value.substring(11, 16);
          }
        }
      }
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: '日志数量',
        type: 'bar',
        barWidth: '60%',
        data: aggregatedData.groupedData
      }
    ]
  }), [aggregatedData]);

  // 处理图表点击事件
  const handleChartClick = (params: any) => {
    if (params.componentType === 'series') {
      const index = params.dataIndex;
      const selectedGroup = aggregatedData.groupLabels[index];
      
      // 查找对应分组内的第一个和最后一个时间点
      const groupItems = aggregatedData.originalData.filter(item => {
        const date = new Date(item.timePoint);
        let groupKey = '';
        
        switch(timeGrouping) {
          case 'minute':
            groupKey = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
            break;
          case 'hour':
            groupKey = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:00`;
            break;
          case 'day':
            groupKey = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
            break;
          case 'month':
            groupKey = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
            break;
          default:
            groupKey = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:00`;
        }
        
        return groupKey === selectedGroup;
      });
      
      if (groupItems.length > 0) {
        // 对分组内的数据按时间排序
        groupItems.sort((a, b) => new Date(a.timePoint).getTime() - new Date(b.timePoint).getTime());
        
        // 获取时间范围
        const start = groupItems[0].timePoint;
        const end = groupItems[groupItems.length - 1].timePoint;
        
        // 调用回调函数更新时间范围
        onTimeRangeChange([start, end]);
        message.success(`已选择 ${start} 至 ${end} 的数据`);
      }
    }
  };

  // 如果没有数据或显示标志为false，则不显示图表
  if (!show || !distributionData || distributionData.length === 0) {
    return null;
  }

  return (
    <div className="histogram-chart-container">
      <ReactECharts 
        option={option} 
        style={{ height: 200, width: '100%' }} 
        onEvents={{
          'click': handleChartClick
        }}
      />
    </div>
  );
};
