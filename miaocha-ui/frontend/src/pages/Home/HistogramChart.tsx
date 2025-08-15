import { useMemo, useRef } from 'react';
import ReactECharts from 'echarts-for-react';
import { EChartsOption } from 'echarts';
import { Empty } from 'antd';
import dayjs from 'dayjs';
import { colorPrimary, isOverOneDay } from '@/utils/utils';
import { DATE_FORMAT_THOUSOND } from './utils';

interface IProps {
  data: ILogHistogramData | null; // 直方图数据
  searchParams: ILogSearchParams; // 搜索参数
  onSearch: (params: ILogSearchParams) => void; // 搜索回调函数
}

const HistogramChart = (props: IProps) => {
  const { data, searchParams, onSearch } = props;
  const { distributionData, timeUnit, timeInterval } = data || {};
  const { timeGrouping = 'auto', startTime = '', endTime = '' } = searchParams;
  const chartRef = useRef<any>(null);

  // 添加调试日志
  console.log('📊 HistogramChart接收到的props.data:', data);
  console.log('📊 HistogramChart解构的distributionData:', distributionData);
  console.log('📊 distributionData类型和长度:', {
    type: typeof distributionData,
    isArray: Array.isArray(distributionData),
    length: distributionData?.length,
    firstItem: distributionData?.[0],
  });

  // 根据timeGrouping聚合数据
  const aggregatedData = useMemo(() => {
    console.log('📊 aggregatedData计算开始, distributionData:', distributionData);

    // 如果没有数据，返回空数组
    if (!distributionData) {
      console.log('📊 没有distributionData, 返回空数组');
      return {
        values: [],
        labels: [],
        originalData: null,
      };
    }

    // 检查distributionData是否为数组
    if (!Array.isArray(distributionData)) {
      console.log('📊 distributionData不是数组:', typeof distributionData);
      return {
        values: [],
        labels: [],
        originalData: null,
      };
    }

    console.log('📊 开始转换distributionData, 长度:', distributionData.length);

    // 转换为数组
    const labels: string[] = [];
    const values: number[] = [];
    distributionData.forEach((item: any, index: number) => {
      console.log(`📊 处理第${index}个数据点:`, item);
      labels.push(item.timePoint?.replace('T', ' '));
      values.push(item.count);
    });

    console.log('📊 转换完成, labels:', labels);
    console.log('📊 转换完成, values:', values);

    return {
      values,
      labels,
      originalData: data,
    };
  }, [distributionData, data]);

  // 构建图表选项
  const option = useMemo<EChartsOption>(() => {
    const dataCount = distributionData?.length || 0;
    let barWidth;
    if (dataCount === 1) {
      barWidth = '5%';
    } else if (dataCount === 2) {
      barWidth = '10%';
    } else if (dataCount <= 10) {
      barWidth = '30%';
    } else if (dataCount <= 15) {
      barWidth = '60%';
    } else if (dataCount <= 31) {
      barWidth = '80%';
    } else {
      barWidth = '90%';
    }
    return {
      // dataZoom: [
      //   {
      //     type: 'inside', // 鼠标在坐标系范围内滚轮滚动
      //     startValue: dataZoom[0],
      //     endValue: dataZoom[100],
      //   },
      //   {
      //     type: 'slider',
      //     startValue: dataZoom[0],
      //     endValue: dataZoom[100],
      //     height: 14, // 组件高度
      //     bottom: 7, // 组件离容器底部的距离
      //     backgroundColor: '#f5f5f5', // 滑动条背景色
      //     fillerColor: 'rgba(0, 56, 255, 0.1)', // 选中范围的填充颜色
      //     handleStyle: {
      //       color: colorPrimary, // 滑块手柄的颜色
      //       borderColor: colorPrimary, // 滑块手柄的边框颜色
      //     },
      //     selectedDataBackground: {
      //       lineStyle: {
      //         color: colorPrimary, // 选中范围的线条颜色
      //       },
      //       areaStyle: {
      //         color: colorPrimary, // 选中范围的填充颜色
      //       },
      //     },
      //     // 修改滑动条背景文字（如果启用）
      //     textStyle: {
      //       fontSize: 9, // 某些版本可能不支持，需检查 ECharts 版本
      //     },
      //     labelFormatter: function (value) {
      //       const timeStr = data[value].timePoint.replace('T', ' ');
      //       // 将日期和时间用换行符分隔
      //       return timeStr.replace(/\s/, '\n');
      //     },
      //   },
      // ],
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
        // axisPointer: {
        //   type: 'shadow',
        //   shadowStyle: {
        //     color: colorPrimary, // 可选：设置边框颜色
        //     opacity: 0.2, // 可选：设置边框透明度
        //   },
        // },
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
          formatter: (value: string) => {
            // 是否超过1天
            const overOneDay = isOverOneDay(startTime, endTime);
            // 时间分组显示
            switch (timeGrouping) {
              // value: 2 0 2 5 - 0 5 - 1 5    0  0  :  0  0  :  0  0
              //        0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19
              case 'minute':
                return value.substring(overOneDay ? 0 : 11, 16); // 显示时分
              case 'hour':
                return value.substring(overOneDay ? 0 : 11, 13) + ':00'; // 显示小时
              case 'day':
                return value.substring(overOneDay ? 0 : 5, 10); // 显示日期
              default:
                return value; // 默认显示完整时间
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
  }, [distributionData, timeGrouping, startTime, endTime]);

  // 如果没有数据或显示标志为false，则不显示图表
  if (!distributionData || distributionData?.length === 0) {
    return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} />;
  }

  // 处理图表点击事件
  const handleChartClick = (params: any) => {
    if (params.componentType === 'series' && timeUnit && timeInterval) {
      const { name } = params;
      const newParams = {
        ...searchParams,
        startTime: dayjs(name).format(DATE_FORMAT_THOUSOND),
        endTime: dayjs(name)
          .add(timeInterval, timeUnit as any)
          .format(DATE_FORMAT_THOUSOND),
        offset: 0,
      };
      delete newParams.timeRange;
      onSearch(newParams);
    }
  };

  // 如果没有数据，显示空状态
  if (!data || !distributionData || distributionData.length === 0) {
    return (
      <div style={{ height: '160px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <Empty description="暂无直方图数据" />
      </div>
    );
  }

  return (
    <ReactECharts
      option={option}
      onEvents={{
        click: handleChartClick,
        brushEnd: (params: { areas: Array<{ coordRange: [number, number] }> }) => {
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
        },
        // mousemove: (params: { componentType: string }) => {
        //   if (!chartRef.current) return;

        //   if (params.componentType === 'series') {
        //     // 鼠标在柱子上，禁用 brush，启用点击
        //     chartRef.current.dispatchAction({
        //       type: 'takeGlobalCursor',
        //       key: 'default',
        //       cursor: 'pointer',
        //     });
        //   }
        // },
        // mouseout: () => {
        //   if (!chartRef.current) return;

        //   // 鼠标离开任何区域时，恢复横向选择模式
        //   chartRef.current.dispatchAction({
        //     type: 'takeGlobalCursor',
        //     key: 'brush',
        //     brushOption: {
        //       brushType: 'lineX',
        //       brushMode: 'single',
        //     },
        //   });
        // },
      }}
      onChartReady={(chart) => {
        chartRef.current = chart;
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
      }}
      style={{ height: 160, width: '100%' }}
    />
  );
};

export default HistogramChart;
