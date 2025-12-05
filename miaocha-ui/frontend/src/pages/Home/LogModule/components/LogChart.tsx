/**
 * 日志图表组件
 * 封装直方图的展示逻辑
 */

import React from 'react';

import { useHomeContext } from '../../context';
import HistogramChart from '../../HistogramChart';
import styles from '../styles/LogChart.module.less';

const LogChart: React.FC = () => {
  const { histogramData } = useHomeContext();
  // 处理数据类型，确保得到单个数据对象
  let chartData: ILogHistogramResponse | null = null;

  if (Array.isArray(histogramData)) {
    // 如果是数组，取第一个元素
    chartData = histogramData[0] || null;
  } else {
    // 如果是单个对象或null，直接使用
    chartData = histogramData;
  }

  if (!chartData) {
    return <div className={styles.chartContainer}>暂无数据</div>;
  }

  return (
    <div className={styles.chartContainer}>
      <HistogramChart data={chartData} />
    </div>
  );
};

export default LogChart;
