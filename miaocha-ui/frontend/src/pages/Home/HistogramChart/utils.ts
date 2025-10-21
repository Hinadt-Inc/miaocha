import { isOverOneDay } from '@/utils/utils';

/**
 * 根据时间分组格式化 X 轴标签
 */
export const formatXAxisLabel = (
  value: string,
  timeGrouping: string = 'auto',
  startTime: string = '',
  endTime: string = '',
): string => {
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
};

/**
 * 根据数据量计算柱状图宽度
 */
export const calculateBarWidth = (dataCount: number): string => {
  if (dataCount === 1) {
    return '5%';
  } else if (dataCount === 2) {
    return '10%';
  } else if (dataCount <= 10) {
    return '30%';
  } else if (dataCount <= 15) {
    return '60%';
  } else if (dataCount <= 31) {
    return '80%';
  } else {
    return '90%';
  }
};
