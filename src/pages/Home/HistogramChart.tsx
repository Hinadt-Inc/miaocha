import { useMemo } from 'react';
import ReactECharts from 'echarts-for-react';
import { EChartsOption } from 'echarts';
import { Empty } from 'antd';
import { colorPrimary } from '@/utils/utils';

interface IProps {
  data: ILogHistogramData[]; // 直方图数据
  searchParams: ILogSearchParams; // 搜索参数
}

const HistogramChart = (props: IProps) => {
  const { data, searchParams } = props;
  const { timeGrouping, timeRange = '' } = searchParams;

  // 根据timeGrouping聚合数据
  const aggregatedData = useMemo(() => {
    // 转换为数组
    const labels: string[] = [];
    const values: number[] = [];
    data?.forEach((item: any) => {
      labels.push(item.timePoint?.replace('T', ' '));
      values.push(item.count);
    });

    return {
      values,
      labels,
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
          fontSize: 10,
          formatter: (value: string) => {
            // 根据时间范围和时间分组动态调整显示格式
            // 当时间范围较大时（如上周、上月），显示更高粒度的格式
            if (timeRange.includes('week') || timeRange.includes('month') || timeRange.includes('year')) {
              switch (timeGrouping) {
                case 'second':
                case 'minute':
                  return timeRange.includes('week') ? value.substring(5, 10) : value.substring(0, 10);
                case 'hour':
                  return value.substring(11, 13) + ':00';
                case 'day':
                  return value.substring(5, 10);
                default:
                  return value.substring(0, 10);
              }
            }

            // 默认按时间分组显示
            switch (timeGrouping) {
              case 'second':
                return value.substring(11, 19);
              case 'minute':
                return value.substring(11, 16);
              case 'hour':
                return value.substring(11, 13) + ':00';
              case 'day':
                return value.substring(5, 10);
              default:
                return value.substring(11, 16);
            }
          },
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
      },
      // 数据系列配置
      series: [
        {
          name: '日志数量', // 系列名称
          type: 'bar', // 图表类型：柱状图
          data: aggregatedData.values, // 数据数组
          barWidth: '20%', // 柱条宽度，相对于类目宽度的百分比
          itemStyle: {
            color: colorPrimary, // 柱状图填充颜色
            borderRadius: [4, 4, 0, 0], // 柱状图圆角，[左上, 右上, 右下, 左下]
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
      // 数据区域缩放组件配置
      // dataZoom: [
      //   {
      //     type: 'slider', // 滑动条型数据缩放组件
      //     xAxisIndex: [0], // 控制第一个x轴
      //     start: 0, // 数据窗口范围的起始百分比
      //     end: 100, // 数据窗口范围的结束百分比
      //     height: 16, // 组件高度
      //     bottom: 10, // 组件离容器底部的距离
      //     backgroundColor: '#f5f5f5', // 滑动条背景色
      //     fillerColor: 'rgba(0, 56, 255, 0.1)', // 选中范围的填充颜色
      //     handleStyle: {
      //       color: '#0038ff', // 滑块手柄的颜色
      //       borderColor: '#0038ff', // 滑块手柄的边框颜色
      //     },
      //     selectedDataBackground: {
      //       lineStyle: {
      //         color: '#0038ff', // 选中范围的线条颜色
      //       },
      //       areaStyle: {
      //         color: '#0038ff', // 选中范围的填充颜色
      //       },
      //     },
      //   },
      // ],
    }),
    [data],
  );

  // 如果没有数据或显示标志为false，则不显示图表
  if (!data || data?.length === 0) {
    return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} />;
  }

  return <ReactECharts option={option} style={{ height: 180, width: '100%' }} />;
};

export default HistogramChart;
