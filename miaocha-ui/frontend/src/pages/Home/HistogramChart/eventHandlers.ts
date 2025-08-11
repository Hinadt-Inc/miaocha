import dayjs from 'dayjs';
import { DATE_FORMAT_THOUSOND } from '../utils';
import { IAggregatedData } from './types';

/**
 * 创建图表点击事件处理器
 */
export const createChartClickHandler = (
  searchParams: ILogSearchParams,
  timeUnit: string | undefined,
  timeInterval: number | undefined,
  onSearch: (params: ILogSearchParams) => void,
) => {
  return (params: any) => {
    if (params.componentType === 'series' && timeUnit) {
      const { name } = params;
      const newParams = {
        ...searchParams,
        startTime: dayjs(name).format(DATE_FORMAT_THOUSOND),
        endTime: dayjs(name)
          .add(timeInterval || 1, timeUnit as any)
          .format(DATE_FORMAT_THOUSOND),
        offset: 0,
      };
      delete newParams.timeRange;
      onSearch(newParams);
    }
  };
};

/**
 * 创建画刷结束事件处理器
 */
export const createBrushEndHandler = (
  searchParams: ILogSearchParams,
  timeUnit: string | undefined,
  aggregatedData: IAggregatedData,
  onSearch: (params: ILogSearchParams) => void,
) => {
  return (params: { areas: Array<{ coordRange: [number, number] }> }) => {
    if (params.areas && params.areas.length > 0 && timeUnit) {
      const [start, end] = params.areas[0].coordRange;
      const startTime = aggregatedData.labels[start];
      const endTime = aggregatedData.labels[end];

      const newParams = {
        ...searchParams,
        startTime: dayjs(startTime).format(DATE_FORMAT_THOUSOND),
        endTime: dayjs(endTime)
          .add(1, timeUnit as any)
          .format(DATE_FORMAT_THOUSOND),
        offset: 0,
      };
      delete newParams.timeRange;
      onSearch(newParams);
    }
  };
};

/**
 * 图表就绪时的处理函数
 */
export const handleChartReady = (chart: any) => {
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
