import dayjs from 'dayjs';
import customParseFormat from 'dayjs/plugin/customParseFormat';
import isSameOrBefore from 'dayjs/plugin/isSameOrBefore';
import isSameOrAfter from 'dayjs/plugin/isSameOrAfter';
import duration from 'dayjs/plugin/duration';

import { ILogTimeSubmitParams } from '../types';
import { QUICK_RANGES, DATE_FORMAT_THOUSOND } from './constants';

// 扩展 dayjs 功能
dayjs.extend(duration);
dayjs.extend(customParseFormat);
dayjs.extend(isSameOrBefore);
dayjs.extend(isSameOrAfter);

/**
 * 格式化时间字符串，处理毫秒部分不足三位的情况
 * @param timeString 原始时间字符串
 * @returns 格式化后的时间字符串
 */
export const formatTimeString = (timeString: string): string => {
  try {
    if (!timeString) {
      return '';
    }

    let formattedTimeString = timeString;

    // 如果时间字符串没有毫秒部分，添加 .000
    if (!formattedTimeString.includes('.')) {
      formattedTimeString += '.000';
    } else {
      // 处理毫秒部分不足三位的情况
      // 例如：2025-08-05T13:12:07.6 -> 2025-08-05T13:12:07.600
      // 例如：2025-08-05T13:12:07.67 -> 2025-08-05T13:12:07.670
      const millisecondsRegex = /\.(\d{1,2})$/;
      const millisecondsMatch = millisecondsRegex.exec(formattedTimeString);
      if (millisecondsMatch) {
        const milliseconds = millisecondsMatch[1];
        const paddedMs = milliseconds.padEnd(3, '0');
        formattedTimeString = formattedTimeString.replace(millisecondsRegex, `.${paddedMs}`);
      }
    }

    const timeValue = dayjs(formattedTimeString);
    return timeValue.isValid() ? timeValue.format(DATE_FORMAT_THOUSOND) : timeString;
  } catch (error) {
    console.warn('时间格式化失败:', error, timeString);
    return timeString;
  }
};

/**
 * 获取当前时间范围
 * @param timeOption 时间选项参数
 * @returns 包含开始时间和结束时间的对象
 */
export const getLatestTime = (timeOption: ILogTimeSubmitParams) => {
  const { type, value, startOption, endOption, range } = timeOption;
  const target: any = {};

  if (!value) return target;

  // 快捷选择
  if (type === 'quick' && value && QUICK_RANGES[value]) {
    const current = QUICK_RANGES[value];
    target.startTime = current.from().format(current.format[0]);
    target.endTime = current.to().format(current.format[1]);
  } else if (type === 'relative' && startOption && endOption) {
    // 相对时间
    const start = dayjs()
      .subtract(startOption.number || 0, startOption.unitEN as any)
      .format(startOption.isExact ? startOption.format : DATE_FORMAT_THOUSOND);
    const end = dayjs()
      .subtract(endOption.number || 0, endOption.unitEN as any)
      .format(endOption.isExact ? endOption.format : DATE_FORMAT_THOUSOND);
    target.startTime = start;
    target.endTime = end;
  } else if (type === 'absolute' && range?.length === 2) {
    // 绝对时间
    target.startTime = range[0];
    target.endTime = range[1];
  }

  return target;
};

/**
 * 验证时间范围是否有效
 * @param startTime 开始时间
 * @param endTime 结束时间
 * @returns 是否有效
 */
export const isValidTimeRange = (startTime: string, endTime: string): boolean => {
  if (!startTime || !endTime) return false;

  const start = dayjs(startTime);
  const end = dayjs(endTime);

  return start.isValid() && end.isValid() && start.isBefore(end);
};

/**
 * 获取时间显示文本
 * @param timeOption 时间选项
 * @returns 显示文本
 */
export const getTimeDisplayText = (timeOption: ILogTimeSubmitParams): string => {
  if (!timeOption?.type) return '';

  const { type, label, range, value } = timeOption;

  switch (type) {
    case 'quick':
      return label || '';
    case 'absolute':
      return range ? range.join(' ~ ') : '';
    case 'relative':
      return label || value || '';
    default:
      return '';
  }
};
