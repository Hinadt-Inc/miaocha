/**
 * 日志图表组件
 * 封装直方图的展示逻辑
 */

import React from 'react';
import HistogramChart from '../../HistogramChart';
import { ILogHistogramData, ILogSearchParams } from '../../types';
import styles from '../styles/LogChart.module.less';

export interface ILogChartProps {
  /** 直方图数据 - 可以是单个对象或数组 */
  data: ILogHistogramData[] | ILogHistogramData | null;
  /** 搜索参数 */
  searchParams: ILogSearchParams;
  /** 搜索回调函数 */
  onSearch: (params: ILogSearchParams) => void;
}

const LogChart: React.FC<ILogChartProps> = ({ data, searchParams, onSearch }) => {
  // 处理数据类型，确保得到单个数据对象
  let chartData: ILogHistogramData | null = null;

  if (Array.isArray(data)) {
    // 如果是数组，取第一个元素
    chartData = data[0] || null;
  } else {
    // 如果是单个对象或null，直接使用
    chartData = data;
  }

  if (!chartData) {
    return <div className={styles.chartContainer}>暂无数据</div>;
  }

  return (
    <div className={styles.chartContainer}>
      <HistogramChart data={chartData} searchParams={searchParams as any} onSearch={onSearch as any} />
    </div>
  );
};

export default LogChart;
