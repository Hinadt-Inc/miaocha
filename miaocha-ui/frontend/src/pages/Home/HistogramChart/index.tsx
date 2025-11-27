import { useRef, useMemo, useCallback, useEffect } from 'react';

import { Empty } from 'antd';
import dayjs from 'dayjs';
import ReactECharts from 'echarts-for-react';

import { useHomeContext } from '../context';
import { useDataInit } from '../hooks/useDataInit';
import { IModuleQueryConfig } from '../types';
import { DATE_FORMAT_THOUSOND } from '../utils';

import { createChartOption } from './chartConfig';

interface Props {
  data: any;
}

const HistogramChart = (props: Props) => {
  const { data } = props;
  const { searchParams, updateSearchParams, moduleQueryConfig } = useHomeContext();
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
  }, [distributionData, data]);

  // 构建图表选项
  const option = useMemo(() => {
    return createChartOption(aggregatedData, distributionData, timeGrouping, startTime, endTime);
  }, [aggregatedData, distributionData, timeGrouping, startTime, endTime]);

  // 事件处理器
  const handleChartClick = useCallback(
    (params: any) => {
      if (params.componentType === 'series' && timeUnit) {
        const { name } = params;
        const newParams = updateSearchParams({
          ...searchParams,
          startTime: dayjs(name).format(DATE_FORMAT_THOUSOND),
          endTime: dayjs(name)
            .add(timeInterval || 1, timeUnit as any)
            .format(DATE_FORMAT_THOUSOND),
          offset: 0,
          timeType: 'absolute',
        });
        delete newParams.timeRange;
        fetchData({
          moduleQueryConfig: moduleQueryConfig as IModuleQueryConfig,
          searchParams: newParams,
        });
        refreshFieldDistributions(newParams);
      }
    },
    [timeUnit, timeInterval, searchParams, updateSearchParams, fetchData, moduleQueryConfig, refreshFieldDistributions],
  );

  const handleBrushEnd = useCallback(
    (params: { areas: { coordRange: [number, number] }[] }) => {
      if (params.areas && params.areas.length > 0 && timeUnit) {
        const [start, end] = params.areas[0].coordRange;
        const startTime = aggregatedData.labels[start];
        const endTime = aggregatedData.labels[end];

        const newParams = updateSearchParams({
          ...searchParams,
          startTime: dayjs(startTime).format(DATE_FORMAT_THOUSOND),
          endTime: dayjs(endTime)
            .add(1, timeUnit as any)
            .format(DATE_FORMAT_THOUSOND),
          offset: 0,
          timeType: 'absolute',
        });
        delete newParams.timeRange;
        fetchData({
          moduleQueryConfig: moduleQueryConfig as IModuleQueryConfig,
          searchParams: newParams,
        });
        refreshFieldDistributions(newParams);
      }
    },
    [
      timeUnit,
      aggregatedData.labels,
      searchParams,
      updateSearchParams,
      fetchData,
      moduleQueryConfig,
      refreshFieldDistributions,
    ],
  );

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
  };

  // 手动管理事件监听器，避免重复绑定
  useEffect(() => {
    const chartInstance = chartRef.current;
    if (!chartInstance) return;

    // 绑定事件
    chartInstance.on('click', handleChartClick);
    chartInstance.on('brushEnd', handleBrushEnd);

    // 清理函数：组件卸载或依赖变化时解绑事件
    return () => {
      chartInstance.off('click', handleChartClick);
      chartInstance.off('brushEnd', handleBrushEnd);
    };
  }, [handleChartClick, handleBrushEnd]);

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
