import { useRef, useMemo } from 'react';

import { Empty } from 'antd';
import dayjs from 'dayjs';
import ReactECharts from 'echarts-for-react';

import { useHomeContext } from '../context';
import { useDataInit } from '../hooks/useDataInit';
import { DATE_FORMAT_THOUSOND, debounce } from '../utils';

import { createChartOption } from './chartConfig';

interface Props {
  data: any;
}

const HistogramChart = (props: Props) => {
  const { data } = props;
  const { searchParams, searchParamsRef, distributions, logTableColumns, updateSearchParams } = useHomeContext();
  const { fetchData, refreshFieldDistributions } = useDataInit();
  const { distributionData, timeUnit, timeInterval } = data || {};
  const { timeGrouping = 'auto', startTime = '', endTime = '' } = searchParams;

  const chartRef = useRef<any>(null);

  // 根据timeGrouping聚合数据
  const aggregatedData = useMemo(() => {
    // 转换为数组
    const labels: string[] = [];
    const values: number[] = [];
    distributionData?.forEach((item: any) => {
      labels.push(item.timePoint?.replace('T', ' '));
      values.push(item.count);
    });

    return {
      values,
      labels,
      originalData: data,
    };
  }, [distributionData, data, searchParams]);

  // 构建图表选项
  const option = useMemo(() => {
    return createChartOption(aggregatedData, distributionData, timeGrouping, startTime, endTime);
  }, [aggregatedData, distributionData, distributions, logTableColumns]);

  // 事件处理器
  const handleChartClick = (params: any) => {
    if (params.componentType === 'series' && timeUnit) {
      const { name } = params;
      const startTime = dayjs(name).format(DATE_FORMAT_THOUSOND);
      const endTime = dayjs(name)
        .add(timeInterval || 1, timeUnit as any)
        .format(DATE_FORMAT_THOUSOND);
      // 使用 searchParamsRef.current 获取最新的 searchParams
      const newParams = updateSearchParams({
        ...searchParamsRef.current,
        startTime,
        endTime,
        offset: 0,
        timeRange: `${startTime} ~ ${endTime}`,
      });
      fetchData({
        searchParams: newParams,
      });
      refreshFieldDistributions(newParams);
    }
  };

  // 防抖处理图表点击（200ms）
  const debouncedHandleChartClick = useMemo(() => {
    return debounce(handleChartClick, 300);
  }, [
    handleChartClick,
    timeUnit,
    timeInterval,
    searchParamsRef,
    updateSearchParams,
    fetchData,
    refreshFieldDistributions,
  ]);

  const handleBrushEnd = (params: { areas: { coordRange: [number, number] }[] }) => {
    if (params.areas && params.areas.length > 0 && timeUnit) {
      const [start, end] = params.areas[0].coordRange;

      // 确保索引在有效范围内
      const startIndex = Math.max(0, Math.floor(start));
      const endIndex = Math.min(aggregatedData.labels.length - 1, Math.floor(end));

      // 开始时间：使用选中范围的第一个数据点
      const startTime = dayjs(aggregatedData.labels[startIndex]).format(DATE_FORMAT_THOUSOND);

      // 结束时间：使用选中范围的最后一个数据点，并加上时间间隔以包含完整的时间段
      const endTime = dayjs(aggregatedData.labels[endIndex])
        .add(timeInterval || 1, timeUnit as any)
        .format(DATE_FORMAT_THOUSOND);

      // 使用 searchParamsRef.current 获取最新的 searchParams
      const newParams = updateSearchParams({
        ...searchParamsRef.current,
        startTime,
        endTime,
        offset: 0,
        timeRange: `${startTime} ~ ${endTime}`,
      });
      fetchData({
        searchParams: newParams,
      });
      refreshFieldDistributions(newParams);
    }
  };

  // 防抖处理刷选结束（200ms）
  const debouncedHandleBrushEnd = useMemo(() => debounce(handleBrushEnd, 200), [handleBrushEnd]);

  const resetEvent = () => {
    const chartInstance = chartRef.current;
    if (!chartInstance) return;
    chartInstance.off('click', debouncedHandleChartClick);
    chartInstance.off('brushEnd', debouncedHandleBrushEnd);
    // 绑定事件
    chartInstance.on('click', debouncedHandleChartClick);
    chartInstance.on('brushEnd', debouncedHandleBrushEnd);
  };

  const handleChartReady = (chart: any) => {
    // 设置全局鼠标样式为横向选择
    chart.dispatchAction({
      type: 'takeGlobalCursor',
      key: 'brush',
      brushOption: {
        brushType: 'lineX',
        brushMode: 'single',
      },
    });
    // 启用 brush 组件
    chart.dispatchAction({
      type: 'brush',
      areas: [],
    });
    resetEvent();
  };

  // 如果没有数据或显示标志为false，则不显示图表
  if (!distributionData || distributionData?.length === 0) {
    return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} />;
  }

  return (
    <>
      <ReactECharts
        option={option}
        style={{ height: 160, width: '100%' }}
        onChartReady={(chart) => {
          chartRef.current = chart;
          handleChartReady(chart);
        }}
      />
    </>
  );
};

export { HistogramChart };
export default HistogramChart;
