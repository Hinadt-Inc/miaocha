import ReactECharts from 'echarts-for-react';
import { EChartsOption } from 'echarts';
import { message, Button } from 'antd';
import { MinusOutlined } from '@ant-design/icons';
import { useMemo, useEffect } from 'react';

interface HistogramChartProps {
  show: boolean;
  onTimeRangeChange: (range: [string, string]) => void;
  onToggle: () => void;
  distributionData?: Array<{
    timePoint: string;
    count: number;
  }>;
}

export const HistogramChart = ({ 
  show,
  onTimeRangeChange,
  onToggle,
  distributionData
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
  
  // 按小时聚合数据，将相同小时的数据合并
  const aggregatedData = useMemo(() => {
    if (!distributionData || distributionData.length === 0) {
      console.log('没有分布数据，返回空结果');
      return { hourlyData: [], hourLabels: [], originalData: [] };
    }

    // 当只有一个时间点时，直接使用该时间点
    if (distributionData.length === 1) {
      const timePoint = distributionData[0].timePoint;
      const count = distributionData[0].count;
      return {
        hourlyData: [count],
        hourLabels: [timePoint],
        originalData: [{ timePoint, count }]
      };
    }

    // 按小时聚合数据
    const hourMap = new Map();
    const original = [];

    // 对数据按时间排序
    const sortedData = [...distributionData].sort((a, b) => {
      return new Date(a.timePoint).getTime() - new Date(b.timePoint).getTime();
    });

    // 聚合数据
    sortedData.forEach(item => {
      const date = new Date(item.timePoint);
      const hourKey = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:00`;
      
      if (!hourMap.has(hourKey)) {
        hourMap.set(hourKey, 0);
      }
      
      hourMap.set(hourKey, hourMap.get(hourKey) + item.count);
      original.push(item);
    });

    // 转换为数组
    const hourLabels = Array.from(hourMap.keys());
    const hourlyData = Array.from(hourMap.values());

    return {
      hourlyData,
      hourLabels,
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
      data: aggregatedData.hourLabels,
      axisTick: {
        alignWithLabel: true
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
        data: aggregatedData.hourlyData
      }
    ]
  }), [aggregatedData]);

  // 处理图表点击事件
  const handleChartClick = (params: any) => {
    if (params.componentType === 'series') {
      const index = params.dataIndex;
      const selectedHour = aggregatedData.hourLabels[index];
      
      // 查找对应小时内的第一个和最后一个时间点
      const hourItems = aggregatedData.originalData.filter(item => {
        const date = new Date(item.timePoint);
        const hourKey = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:00`;
        return hourKey === selectedHour;
      });
      
      if (hourItems.length > 0) {
        // 对小时内的数据按时间排序
        hourItems.sort((a, b) => new Date(a.timePoint).getTime() - new Date(b.timePoint).getTime());
        
        // 获取时间范围
        const start = hourItems[0].timePoint;
        const end = hourItems[hourItems.length - 1].timePoint;
        
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
