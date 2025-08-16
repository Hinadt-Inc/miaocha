import { useRef, useMemo } from 'react';
import ReactECharts from 'echarts-for-react';
import { Empty } from 'antd';
import { IHistogramChartProps } from './types';
import { useAggregatedData } from './dataProcessor';
import { createChartOption } from './chartConfig';
import { createChartClickHandler, createBrushEndHandler, handleChartReady } from './eventHandlers';

const HistogramChart = (props: IHistogramChartProps) => {
  const { data, searchParams, onSearch } = props;
  const { distributionData, timeUnit, timeInterval } = data || {};
  const { timeGrouping = 'auto', startTime = '', endTime = '' } = searchParams;
  const chartRef = useRef<any>(null);

  // 根据timeGrouping聚合数据
  const aggregatedData = useAggregatedData(distributionData, data);

  // 构建图表选项
  const option = useMemo(() => {
    return createChartOption(aggregatedData, distributionData, timeGrouping, startTime, endTime);
  }, [aggregatedData, distributionData, timeGrouping, startTime, endTime]);

  // 如果没有数据或显示标志为false，则不显示图表
  if (!distributionData || distributionData?.length === 0) {
    return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} />;
  }

  // 创建事件处理器
  const handleChartClick = createChartClickHandler(searchParams, timeUnit, timeInterval, onSearch);

  const handleBrushEnd = createBrushEndHandler(searchParams, timeUnit, aggregatedData, onSearch);

  return (
    <ReactECharts
      option={option}
      onEvents={{
        click: handleChartClick,
        brushEnd: handleBrushEnd,
      }}
      onChartReady={(chart) => {
        chartRef.current = chart;
        handleChartReady(chart);
      }}
      style={{ height: 160, width: '100%' }}
    />
  );
};

export { HistogramChart };
export default HistogramChart;
