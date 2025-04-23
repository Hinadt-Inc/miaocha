import ReactECharts from 'echarts-for-react';
import { EChartsOption } from 'echarts';
import { message, Button } from 'antd';
import { MinusOutlined } from '@ant-design/icons';

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
    
    const dates = distributionData.map(item => item.timePoint);
    const values = distributionData.map(item => item.count);
    
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
            color: '#69c0ff'
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
            const dates = distributionData.map(item => item.timePoint);
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
