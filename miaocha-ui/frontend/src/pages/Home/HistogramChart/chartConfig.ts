import { EChartsOption } from 'echarts';
import { colorPrimary } from '@/utils/utils';
import { formatXAxisLabel, calculateBarWidth } from './utils';
import { IAggregatedData } from './types';

/**
 * 创建图表配置选项
 */
export const createChartOption = (
  aggregatedData: IAggregatedData,
  distributionData: any[] | undefined,
  timeGrouping: string = 'auto',
  startTime: string = '',
  endTime: string = '',
): EChartsOption => {
  const dataCount = distributionData?.length || 0;
  const barWidth = calculateBarWidth(dataCount);

  return {
    animation: false, // 禁用动画提升性能
    toolbox: {
      feature: {
        brush: {
          type: ['lineX'],
          title: {
            lineX: '横向选择',
          },
        },
        saveAsImage: {
          pixelRatio: 2,
        },
      },
      right: 10,
      top: 10,
    },
    brush: {
      xAxisIndex: 0,
      brushLink: 'all',
      outOfBrush: {
        colorAlpha: 0.1,
      },
      throttleType: 'debounce',
      throttleDelay: 300,
      removeOnClick: true,
      z: 100,
      brushMode: 'single',
      transformable: false,
      brushType: 'lineX',
    },
    // 提示框组件配置
    tooltip: {
      trigger: 'axis', // 触发类型，axis表示坐标轴触发
    },
    // 直角坐标系网格配置
    grid: {
      top: '5%', // 距离容器上边距
      right: '2%', // 距离容器右边距
      bottom: '7%', // 距离容器下边距，为时间轴留出空间
      left: '2%', // 距离容器左边距
      containLabel: true, // 是否包含坐标轴的标签
    },
    // X轴配置
    xAxis: {
      type: 'category', // 类目轴，适用于离散的类目数据
      data: aggregatedData.labels, // 类目数据
      silent: false,
      splitLine: {
        show: false,
      },
      splitArea: {
        show: false,
      },
      axisLabel: {
        fontSize: 10,
        formatter: (value: string) => formatXAxisLabel(value, timeGrouping, startTime, endTime),
      },
    },
    // Y轴配置
    yAxis: {
      type: 'value', // 数值轴，适用于连续数据
      axisLabel: {
        fontSize: 10,
      },
      splitLine: {
        lineStyle: {
          type: 'dashed', // 虚线类型
        },
      },
      axisLine: {
        lineStyle: {
          width: 0, // 轴线宽度
        },
      },
      splitArea: {
        show: false,
      },
    },
    // 决定图表中显示哪些数据以及以何种形式呈现
    series: [
      {
        name: '日志数量', // 系列名称
        type: 'bar', // 图表类型：柱状图
        data: aggregatedData.values, // 数据数组
        large: true, // 开启大数据量优化
        barWidth: barWidth, // 柱条宽度，动态计算
        itemStyle: {
          color: colorPrimary, // 柱状图填充颜色
          borderRadius: [3, 3, 0, 0], // 柱状图圆角，[左上, 右上, 右下, 左下]
        },
        emphasis: {
          itemStyle: {
            color: '#0029cc', // 鼠标悬停时的颜色
            shadowBlur: 10, // 阴影模糊大小
            shadowColor: 'rgba(0, 56, 255, 0.3)', // 阴影颜色，带透明度
          },
        },
      },
    ],
  };
};
