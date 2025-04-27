import ReactECharts from 'echarts-for-react';
import { EChartsOption } from 'echarts';
import { message, Button } from 'antd';
import { MinusOutlined } from '@ant-design/icons';
import { useMemo, useCallback, memo } from 'react';

interface HistogramChartProps {
  show: boolean;
  onTimeRangeChange: (range: [string, string]) => void;
  onToggle: () => void;
  distributionData?: Array<{
    timePoint: string;
    count: number;
  }>;
}

// 使用 memo 优化组件重渲染性能
export const HistogramChart = memo(({ 
  show,
  onTimeRangeChange,
  onToggle,
  distributionData = []
}: HistogramChartProps) => {
  // 按小时聚合数据，使用 useMemo 缓存计算结果
  const aggregatedData = useMemo(() => {
    if (!distributionData || distributionData.length === 0) {
      return { hourlyData: [], hourLabels: [], originalData: [] };
    }

    // 当只有一个时间点时，直接使用该时间点
    if (distributionData.length === 1) {
      const timePoint = distributionData[0].timePoint;
      // 处理带T的ISO格式或带空格的常规格式
      const formattedTimePoint = timePoint.replace('T', ' ');
      const parts = formattedTimePoint.split(' ');
      const hour = parts.length > 1 ? parts[1] : timePoint;
      return {
        hourlyData: [{ hour: formattedTimePoint, count: distributionData[0].count }],
        hourLabels: [hour],
        originalData: [{ timePoint: formattedTimePoint, count: distributionData[0].count }]
      };
    }
    
    // 使用 Map 优化数据聚合
    const hourMap = new Map<string, number>();
    const originalTimePoints = new Map<string, string>();
    
    // 预先检查数据格式，避免在循环中重复判断
    const hasValidFormat = distributionData.every(item => 
      item && item.timePoint && (item.timePoint.includes(' ') || item.timePoint.includes('T'))
    );
    
    // 将数据按小时分组并累加
    for (const item of distributionData) {
      if (!item || !item.timePoint) continue;
      
      // 处理带T的ISO格式或带空格的常规格式
      const timePoint = item.timePoint.replace('T', ' ');
      
      // 提取日期和时间部分 - 仅当数据格式有效时进行
      if (!hasValidFormat) continue;
      
      const dateParts = timePoint.split(' ');
      if (dateParts.length !== 2) continue;
      
      // 使用完整时间点作为键，以保留所有唯一时间点
      const timeKey = timePoint;
      
      // 累加相同时间点的数量
      hourMap.set(timeKey, (hourMap.get(timeKey) || 0) + item.count);
      
      // 保存原始时间点
      if (!originalTimePoints.has(timeKey)) {
        originalTimePoints.set(timeKey, timePoint);
      }
    }
    
    // 转换为数组并按时间排序 - 使用更高效的排序方法
    const hourlyData = [];
    for (const [hour, count] of hourMap.entries()) {
      hourlyData.push({ hour, count });
    }
    
    // 优化排序性能
    hourlyData.sort((a, b) => {
      // 直接比较字符串可能比创建Date对象更快
      return a.hour < b.hour ? -1 : a.hour > b.hour ? 1 : 0;
    });
    
    // 提取时间标签 - 使用映射而不是重复遍历
    const hourLabels = hourlyData.map(item => {
      // 只显示时间部分，不显示日期
      const timePart = item.hour.split(' ')[1];
      return timePart || item.hour; // 如果分割失败返回完整字符串
    });
    
    // 创建原始数据映射，用于缩放事件
    const originalData = hourlyData.map(item => ({
      timePoint: originalTimePoints.get(item.hour) || item.hour,
      count: item.count
    }));
    
    return {
      hourlyData,
      hourLabels,
      originalData
    };
  }, [distributionData]);
  
  // 记忆化图表配置
  const chartOption = useMemo((): EChartsOption => {
    if (!distributionData || distributionData.length === 0) {
      return {
        title: {
          text: '加载数据中...',
          left: 'center',
          top: 'center',
          textStyle: {
            color: '#999',
            fontSize: 14
          }
        },
        xAxis: { show: false },
        yAxis: { show: false }
      };
    }
    
    const { hourlyData, hourLabels } = aggregatedData;
    const values = hourlyData.map(item => item.count);
    
    return {
      grid: {
        top: 20,
        right: 30,
        bottom: 40,
        left: 50,
      },
      xAxis: {
        type: 'category',
        data: hourLabels,
        name: 'log_time',
        nameLocation: 'middle',
        nameGap: 25,
        axisLabel: {
          interval: 'auto',
          fontSize: 10,
          rotate: 30
        }
      },
      yAxis: {
        type: 'value',
        nameLocation: 'middle',
        nameGap: 30,
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
            color: '#69c0ff'
          }
        }
      }],
      tooltip: {
        trigger: 'axis',
        formatter: function(params: any) {
          const param = params[0];
          const dataIndex = param.dataIndex;
          if (dataIndex >= 0 && dataIndex < hourlyData.length) {
            const fullTime = hourlyData[dataIndex].hour;
            const parts = fullTime.split(' ');
            const date = parts.length > 0 ? parts[0] : '';
            const time = parts.length > 1 ? parts[1] : fullTime;
            return `日期: ${date}<br/>时间: ${time}<br/>数量: ${param.value}`;
          }
          return `时间: ${param.name}<br/>数量: ${param.value}`;
        }
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
          brushSelect: true
        },
        {
          type: 'inside',
          xAxisIndex: [0],
          start: 0,
          end: 100,
          zoomOnMouseWheel: 'ctrl',
          moveOnMouseMove: true,
          preventDefaultMouseMove: false
        }
      ]
    };
  }, [distributionData, aggregatedData]);

  // 使用 useCallback 记忆缩放事件处理函数
  const handleDataZoom = useCallback((params: {
    batch?: Array<{
      startValue?: number;
      endValue?: number;
    }>;
  }) => {
    if (!distributionData || distributionData.length === 0) return;
    
    if (params.batch && params.batch.length > 0) {
      const { startValue, endValue } = params.batch[0];
      
      if (startValue !== undefined && endValue !== undefined) {
        try {
          const dates = aggregatedData.originalData.map(item => item.timePoint);
          const startIndex = Math.max(0, Math.floor(startValue));
          const endIndex = Math.min(dates.length - 1, Math.floor(endValue));
          
          if (startIndex >= 0 && endIndex < dates.length && startIndex <= endIndex) {
            const startDate = dates[startIndex];
            const endDate = dates[endIndex];
            
            if (startDate && endDate) {
              onTimeRangeChange([startDate, endDate]);
              // 使用较轻量的消息通知方式
              message.success({
                content: `已选择时间范围`,
                duration: 1,
                style: { marginTop: '5vh' }
              });
            }
          }
        } catch (error) {
          console.error('处理缩放事件时出错:', error);
        }
      }
    }
  }, [distributionData, aggregatedData, onTimeRangeChange]);

  // 缓存事件对象，避免重复创建
  const chartEvents = useMemo(() => ({
    datazoom: handleDataZoom
  }), [handleDataZoom]);

  if (!show) return null;

  return (
    <div style={{ background: '#fff', marginBottom: 12, flex: 'none' }}>
      <div style={{ padding: '8px 12px', position: 'relative' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 0 }}>
          <div style={{ fontSize: 11, color: '#666' }}>
            直接在图表上拖拽可选择时间范围
          </div>
          <Button 
            size="small" 
            type="text" 
            icon={<MinusOutlined />} 
            onClick={onToggle}
            style={{ padding: '0 4px' }}
          />
        </div>
        <ReactECharts 
          option={chartOption} 
          style={{ height: 150 }} 
          onEvents={chartEvents}
          notMerge={true}
          lazyUpdate={true}
        />
      </div>
    </div>
  );
});
