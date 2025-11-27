import { useMemo } from 'react';

import { IAggregatedData } from './types';

/**
 * 处理和聚合分布数据的 Hook
 */
export const useAggregatedData = (distributionData: any[] | undefined, data: ILogHistogramData): IAggregatedData => {
  return useMemo(() => {
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
};
