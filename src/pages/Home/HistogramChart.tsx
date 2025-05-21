import { useMemo, useEffect } from 'react';
import ReactECharts from 'echarts-for-react';
import { EChartsOption } from 'echarts';
import { message, Empty } from 'antd';
import { colorPrimary } from '@/utils/utils';

interface IProps {
  data: any;
  // show?: boolean;
  // onTimeRangeChange: (range: [string, string]) => void;
  // onToggle: () => void;
  // distributionData?: Array<{
  //   timePoint: string;
  //   count: number;
  // }>;
  // timeGrouping?: 'minute' | 'hour' | 'day' | 'month';
}

const HistogramChart = (props: IProps) => {
  const { data, show, onTimeRangeChange, distributionData, timeGrouping = 'minute' } = props;

  // 增强调试日志，检查组件接收到的数据
  // useEffect(() => {
  //   console.log('HistogramChart 详细数据:', {
  //     show,
  //     distributionData,
  //     dataLength: distributionData ? distributionData.length : 0,
  //     dataValid: distributionData && Array.isArray(distributionData) && distributionData.length > 0,
  //     firstItem: distributionData && distributionData.length > 0 ? distributionData[0] : null,
  //   });
  // }, [show, distributionData]);

  // 根据timeGrouping聚合数据
  const aggregatedData = useMemo(() => {
    if (!data || data.length === 0) {
      // console.log('没有分布数据，返回空结果');
      return { values: [], labels: [], originalData: [] };
    }

    // 当只有一个时间点时，直接使用该时间点
    // if (distributionData.length === 1) {
    //   const timePoint = distributionData[0].timePoint;
    //   const count = distributionData[0].count;
    //   return {
    //     values: [count],
    //     labels: [timePoint],
    //     originalData: [{ timePoint, count }],
    //   };
    // }

    // 按不同粒度聚合数据
    // const groupMap = new Map<string, number>();
    // const original: Array<{ timePoint: string; count: number }> = [];

    // // 对数据按时间排序
    // const sortedData = [...distributionData].sort((a, b) => {
    //   return new Date(a.timePoint).getTime() - new Date(b.timePoint).getTime();
    // });

    // // 聚合数据
    // sortedData.forEach((item) => {
    //   const date = new Date(item.timePoint);
    //   let groupKey = '';

    //   switch (timeGrouping) {
    //     case 'minute':
    //       groupKey = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
    //       break;
    //     case 'hour':
    //       groupKey = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:00`;
    //       break;
    //     case 'day':
    //       groupKey = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
    //       break;
    //     case 'month':
    //       groupKey = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
    //       break;
    //     default:
    //       groupKey = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:00`;
    //   }

    //   if (!groupMap.has(groupKey)) {
    //     groupMap.set(groupKey, 0);
    //   }

    //   groupMap.set(groupKey, (groupMap.get(groupKey) || 0) + item.count);
    //   original.push(item);
    // });

    // 转换为数组
    const labels: string[] = [];
    const values: number[] = [];
    data.forEach((item: any) => {
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

  // 处理图表点击事件
  const handleChartClick = (params: any) => {
    if (params.componentType === 'series') {
      const index = params.dataIndex;
      const selectedGroup = aggregatedData.labels[index];

      // 查找对应分组内的第一个和最后一个时间点
      const groupItems = aggregatedData.originalData.filter((item) => {
        const date = new Date(item.timePoint);
        let groupKey = '';

        switch (timeGrouping) {
          case 'minute':
            groupKey = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
            break;
          case 'hour':
            groupKey = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:00`;
            break;
          case 'day':
            groupKey = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
            break;
          case 'month':
            groupKey = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
            break;
          default:
            groupKey = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:00`;
        }

        return groupKey === selectedGroup;
      });

      if (groupItems.length > 0) {
        // 对分组内的数据按时间排序
        groupItems.sort((a, b) => new Date(a.timePoint).getTime() - new Date(b.timePoint).getTime());

        // 获取时间范围
        const start = groupItems[0].timePoint;
        const end = groupItems[groupItems.length - 1].timePoint;

        // 调用回调函数更新时间范围
        onTimeRangeChange([start, end]);
        message.success(`已选择 ${start} 至 ${end} 的数据`);
      }
    }
  };

  // 如果没有数据或显示标志为false，则不显示图表
  if (!data || data?.length === 0) {
    return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} />;
  }

  return (
    <ReactECharts
      option={option}
      style={{ height: 180, width: '100%' }}
      // onEvents={{
      //   click: handleChartClick,
      // }}
    />
  );
};

export default HistogramChart;
