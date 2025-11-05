import { useRef, useEffect, useState } from 'react';

import type { EChartsOption, ECharts } from 'echarts';
import { init, getInstanceByDom } from 'echarts';

import { debounce } from '@/utils/utils';

interface OptimizedChartProps {
  option: EChartsOption;
  style?: React.CSSProperties;
  className?: string;
  theme?: string | object | undefined;
  loading?: boolean;
  notMerge?: boolean;
  showLoading?: boolean;
  loadingOption?: object;
  onEvents?: Record<string, (params: any) => void>;
}

const OptimizedChart: React.FC<OptimizedChartProps> = ({
  option,
  style = { height: '300px' },
  className = '',
  theme,
  notMerge = false,
  showLoading = false,
  loading = false,
  loadingOption = {},
  onEvents = {},
}) => {
  const chartRef = useRef<HTMLDivElement>(null);
  const [chartInstance, setChartInstance] = useState<ECharts | null>(null);

  // 初始化图表实例
  useEffect(() => {
    // 保证 DOM 存在
    if (!chartRef.current) return;

    // 检查是否已经有图表实例
    const chart = chartRef.current.getAttribute('_echarts_instance_')
      ? getInstanceByDom(chartRef.current)
      : init(chartRef.current, theme);

    // 存储图表实例
    setChartInstance(chart ?? null);

    // 处理 resize 事件
    const resizeHandler = debounce(() => {
      chart?.resize();
    }, 100);

    window.addEventListener('resize', resizeHandler);

    return () => {
      window.removeEventListener('resize', resizeHandler);
      // 在组件卸载时销毁图表实例
      chart?.dispose();
    };
  }, [theme]);

  // 处理图表选项更新
  useEffect(() => {
    if (!chartInstance) return;

    // 显示加载状态
    if (showLoading || loading) {
      chartInstance.showLoading(loadingOption);
    } else {
      chartInstance.hideLoading();
    }

    // 设置图表选项
    chartInstance.setOption(option, { notMerge });
  }, [chartInstance, option, notMerge, showLoading, loading, loadingOption]);

  // 处理事件绑定
  useEffect(() => {
    if (!chartInstance) return;

    // 清除已有事件
    const registeredEvents = Object.keys(onEvents);
    registeredEvents.forEach((eventName) => {
      chartInstance.off(eventName);
    });

    // 注册新事件
    registeredEvents.forEach((eventName) => {
      chartInstance.on(eventName, onEvents[eventName]);
    });

    return () => {
      // 在事件更新前清除事件
      registeredEvents.forEach((eventName) => {
        chartInstance.off(eventName);
      });
    };
  }, [chartInstance, onEvents]);

  return <div ref={chartRef} className={className} style={style} />;
};

export default OptimizedChart;
