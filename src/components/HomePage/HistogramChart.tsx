import ReactECharts from 'echarts-for-react';
import { EChartsOption } from 'echarts';
import { message, Button } from 'antd';
import { MinusOutlined } from '@ant-design/icons';
import { useMemo } from 'react';

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
  // 添加直接的调试日志，查看传入的原始数据
  console.log('HistogramChart 接收到的原始数据:', distributionData);
  
  // 按小时聚合数据，将相同小时的数据合并
  const aggregatedData = useMemo(() => {
    if (!distributionData || distributionData.length === 0) {
      console.log('没有分布数据，返回空结果');
      return { hourlyData: [], hourLabels: [], originalData: [] };
    }

    // 当只有一个时间点时，直接使用该时间点
    if (distributionData.length === 1) {
      const timePoint = distributionData[0].timePoint;
      const hour = timePoint.split(' ')[1];
      return {
        hourlyData: [{ hour: timePoint, count: distributionData[0].count }],
        hourLabels: [hour || timePoint],
        originalData: distributionData
      };
    }

    // 打印一些样本数据，帮助调试
    console.log('分布数据样本:', distributionData.slice(0, 3));
    
    const hourMap = new Map<string, number>();
    const originalTimePoints = new Map<string, string>();
    
    // 将数据按小时分组并累加
    distributionData.forEach(item => {
      if (!item || !item.timePoint) {
        console.log('发现无效数据项:', item);
        return;
      }
      
      // 提取日期和时间部分
      const dateParts = item.timePoint.split(' ');
      if (dateParts.length !== 2) {
        console.log('时间格式不正确:', item.timePoint);
        return;
      }
      
      const date = dateParts[0];
      const timeStr = dateParts[1];
      
      // 使用完整时间点作为键，以保留所有唯一时间点
      const timeKey = item.timePoint;
      
      // 累加相同时间点的数量
      const currentCount = hourMap.get(timeKey) || 0;
      hourMap.set(timeKey, currentCount + item.count);
      
      // 保存原始时间点
      if (!originalTimePoints.has(timeKey)) {
        originalTimePoints.set(timeKey, item.timePoint);
      }
    });
    
    // 转换为数组并按时间排序
    const hourlyData = Array.from(hourMap.entries())
      .map(([hour, count]) => ({ hour, count }))
      .sort((a, b) => new Date(a.hour).getTime() - new Date(b.hour).getTime());
    
    // 提取时间标签
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
    
    console.log('聚合后数据统计:', {
      原始数据条数: distributionData.length,
      聚合后时间点数: hourlyData.length,
      hourMap尺寸: hourMap.size
    });
    
    return {
      hourlyData,
      hourLabels,
      originalData
    };
  }, [distributionData]);
  
  const getHistogramOption = (): EChartsOption => {
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
    
    // 检查横坐标数据是否有效
    console.log('聚合后的直方图数据:', hourlyData, hourLabels, values);
    
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
        name: '数量',
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
  };

  const onChartEvents = {
    datazoom: (params: {
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
            
            if (startIndex >= 0 && endIndex < dates.length) {
              const startDate = dates[startIndex];
              const endDate = dates[endIndex];
              
              if (startDate && endDate) {
                onTimeRangeChange([startDate, endDate]);
                message.info(`已选择时间范围: ${startDate} 至 ${endDate}`);
              }
            }
          } catch (error) {
            console.error('处理缩放事件时出错:', error);
          }
        }
      }
    }
  };

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
          option={getHistogramOption()} 
          style={{ height: 150 }} 
          onEvents={onChartEvents}
        />
      </div>
    </div>
  );
};
