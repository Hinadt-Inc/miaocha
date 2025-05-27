import { useMemo, useState } from 'react';
import ReactECharts from 'echarts-for-react';
import { EChartsOption } from 'echarts';
import { Empty, message } from 'antd';
import dayjs from 'dayjs';
import { colorPrimary, isOverOneDay } from '@/utils/utils';
import { getTimeRangeCategory, DATE_FORMAT } from './utils';

interface IProps {
  data: ILogHistogramData[]; // 直方图数据
  searchParams: ILogSearchParams; // 搜索参数
  onSearch: (params: ILogSearchParams) => void; // 搜索回调函数
}

const HistogramChart = (props: IProps) => {
  const { data, searchParams, onSearch } = props;
  const [dataZoom, setDataZoom] = useState<number[]>([0, 100]);
  const [messageApi, contextHolder] = message.useMessage();
  const { timeGrouping = 'auto', startTime = '', endTime = '' } = searchParams;

  // 根据timeGrouping聚合数据
  const aggregatedData = useMemo(() => {
    // 转换为数组
    const labels: string[] = [];
    const values: number[] = [];
    data?.forEach((item: any) => {
      labels.push(item.timePoint?.replace('T', ' '));
      values.push(item.count);
    });
    setDataZoom([0, 100]);
    return {
      values,
      labels,
    };
  }, [data]);

  // 构建图表选项
  const option = useMemo<EChartsOption>(
    () => ({
      dataZoom: [
        {
          type: 'inside', // 鼠标在坐标系范围内滚轮滚动
          startValue: dataZoom[0],
          endValue: dataZoom[100],
        },
        {
          type: 'slider',
          startValue: dataZoom[0],
          endValue: dataZoom[100],
          height: 14, // 组件高度
          bottom: 7, // 组件离容器底部的距离
          backgroundColor: '#f5f5f5', // 滑动条背景色
          fillerColor: 'rgba(0, 56, 255, 0.1)', // 选中范围的填充颜色
          handleStyle: {
            color: colorPrimary, // 滑块手柄的颜色
            borderColor: colorPrimary, // 滑块手柄的边框颜色
          },
          selectedDataBackground: {
            lineStyle: {
              color: colorPrimary, // 选中范围的线条颜色
            },
            areaStyle: {
              color: colorPrimary, // 选中范围的填充颜色
            },
          },
          // 修改滑动条背景文字（如果启用）
          textStyle: {
            fontSize: 9, // 某些版本可能不支持，需检查 ECharts 版本
          },
          labelFormatter: function (value) {
            const timeStr = data[value].timePoint.replace('T', ' ');
            // 将日期和时间用换行符分隔
            return timeStr.replace(/\s/, '\n');
          },
        },
      ],
      animation: false, // 禁用动画提升性能
      toolbox: {
        feature: {
          dataZoom: {
            yAxisIndex: false,
          },
          saveAsImage: {
            pixelRatio: 2,
          },
        },
      },
      // 提示框组件配置
      tooltip: {
        trigger: 'axis', // 触发类型，axis表示坐标轴触发
        axisPointer: {
          type: 'shadow',
          shadowStyle: {
            color: colorPrimary, // 可选：设置边框颜色
            opacity: 0.2, // 可选：设置边框透明度
          },
        },
      },
      // 直角坐标系网格配置
      grid: {
        top: '5%', // 距离容器上边距
        right: '4.5%', // 距离容器右边距
        bottom: '14%', // 距离容器下边距，为时间轴留出空间
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
              // value: 2 0 2 5 - 0 5 - 1 5    0   0 :  0  0   :  0  0
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
    }),
    [data, dataZoom],
  );

  // 如果没有数据或显示标志为false，则不显示图表
  if (!data || data?.length === 0) {
    return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} />;
  }

  // 处理图表点击事件
  const handleChartClick = (params: any) => {
    if (params.componentType === 'series') {
      const { name } = params;
      const data = getTimeRangeCategory([startTime, endTime]);
      console.log('【打印日志】,data =======>', data);
      const timeConfig = data.value as ITimeIntervalConfig;
      const intervalConfig = timeConfig[timeGrouping as keyof ITimeIntervalConfig] as IIntervalConfig;
      if (!intervalConfig) {
        messageApi.warning('已达最小粒度，请切换时间分组');
        return;
      }
      const { interval, unit } = intervalConfig;

      // 绝对时间
      //   {
      //     "label": "2025-05-06 00:07:00 ~ 2025-05-23 05:04:26",
      //     "value": "2025-05-06 00:07:00 ~ 2025-05-23 05:04:26",
      //     "range": [
      //         "2025-05-06 00:07:00",
      //         "2025-05-23 05:04:26"
      //     ]
      // }
      const newParams = {
        ...searchParams,
        startTime: dayjs(name).format(DATE_FORMAT),
        endTime: dayjs(name).add(interval, unit).format(DATE_FORMAT),
        offset: 0,
      };
      delete newParams.timeRange;
      onSearch(newParams);
    }
  };

  return (
    <>
      {contextHolder}
      <ReactECharts
        option={option}
        onEvents={{
          click: handleChartClick,
        }}
        style={{ height: 160, width: '100%' }}
      />
    </>
  );
};

export default HistogramChart;
