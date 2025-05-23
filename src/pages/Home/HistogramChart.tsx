import { useMemo } from 'react';
import ReactECharts from 'echarts-for-react';
import { EChartsOption } from 'echarts';
import { colorPrimary } from '@/utils/utils';

interface IProps {
  data: ISearchLogsResponse['distributionData'];
  timeGrouping?: 'minute' | 'hour' | 'day' | 'month';
}

const HistogramChart = (props: IProps) => {
  const { data, timeGrouping = 'minute' } = props;

  // 根据timeGrouping聚合数据
  const aggregatedData = useMemo(() => {
    if (!data || data.length === 0) {
      return { values: [], labels: [], originalData: [] };
    }

    // 转换为数组
    const labels: string[] = [];
    const values: number[] = [];
    data.forEach((item) => {
      labels.push(item.timePoint?.replace('T', ' '));
      values.push(item.count);
    });

    return {
      values,
      labels,
      originalData: data,
    };
  }, [data]);

  // 构建图表选项
  const option = useMemo<EChartsOption>(
    () => ({
      // 提示框组件配置
      tooltip: {
        trigger: 'axis', // 触发类型，axis表示坐标轴触发
      },
      // 直角坐标系网格配置
      grid: {
        top: '5%', // 距离容器上边距
        right: '2%', // 距离容器右边距
        bottom: '0%', // 距离容器下边距，为时间轴留出空间
        left: '2%', // 距离容器左边距
        containLabel: true, // 是否包含坐标轴的标签
      },
      // X轴配置
      xAxis: {
        type: 'category', // 类目轴，适用于离散的类目数据
        data: aggregatedData.labels, // 类目数据
        axisLabel: {
          formatter: (value: string) => {
            // 标签格式化函数
            switch (timeGrouping) {
              case 'minute':
                return value.substring(11, 16); // 显示时分
              case 'hour':
                return value.substring(11, 13) + ':00'; // 显示小时
              case 'day':
                return value.substring(5, 10); // 显示月日
              case 'month':
                return value.substring(5, 7) + '月'; // 显示月份
              default:
                return value.substring(11, 16);
            }
          },
        },
      },
      // Y轴配置
      yAxis: {
        type: 'value', // 数值轴，适用于连续数据
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
      },
      // 数据系列配置
      series: [
        {
          name: '日志数量', // 系列名称
          type: 'bar', // 图表类型：柱状图
          data: aggregatedData.values, // 数据数组
          barWidth: '40%', // 柱条宽度，相对于类目宽度的百分比
          itemStyle: {
            color: colorPrimary, // 柱状图填充颜色
            borderRadius: [8, 8, 0, 0], // 柱状图圆角，[左上, 右上, 右下, 左下]
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
    }),
    [data],
  );

  // 如果没有数据或显示标志为false，则不显示图表
  if (!data || data?.length === 0) {
    return null;
  }

  return <ReactECharts option={option} style={{ height: 180, width: '100%' }} />;
};

export default HistogramChart;
