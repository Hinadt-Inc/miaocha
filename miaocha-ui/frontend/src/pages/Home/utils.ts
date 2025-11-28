import dayjs, { ManipulateType } from 'dayjs';
import customParseFormat from 'dayjs/plugin/customParseFormat';
import duration from 'dayjs/plugin/duration';
import isSameOrAfter from 'dayjs/plugin/isSameOrAfter';
import isSameOrBefore from 'dayjs/plugin/isSameOrBefore';

import { QueryConfig } from '@/api/modules';
import { IRelativeTimeState } from '@/components/TimePicker';

// 扩展 dayjs 功能
dayjs.extend(duration);
dayjs.extend(customParseFormat);
dayjs.extend(isSameOrBefore);
dayjs.extend(isSameOrAfter);

export const DATE_FORMAT = 'YYYY-MM-DD HH:mm:ss';
export const DATE_FORMAT_THOUSOND = 'YYYY-MM-DD HH:mm:ss.SSS';

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

// 日志常用字段和值
export const LOG_FIELDS: IStatus[] = [
  { label: 'service', value: 'service', example: 'hina-cloud-engine' },
  { label: 'level', value: 'level', example: 'INFO, ERROR, WARN' },
  { label: 'logger', value: 'logger', example: 'c.h.c.e.c.i.HttpHeadInterceptor' },
  { label: 'method', value: 'method', example: 'printHeaders, sendKafka, onMessage' },
  { label: 'thread', value: 'thread', example: 'http-nio-21102-exec-5' },
  { label: 'reqType', value: 'marker.reqType', example: 'EXECUTE, CALL' },
  { label: 'msg', value: 'msg', example: 'POST /gather, GET /ha' },
  { label: 'logId', value: 'logId', example: '4a5bed50-5993-47e2-97bb-4cf6c4f57ce9' },
];

// 日志常用查询模板
export const LOG_TEMPLATES: IStatus[] = [
  {
    name: '错误日志',
    query: "level = 'ERROR'",
    description: '查询所有错误级别日志',
  },
  {
    name: 'API请求日志',
    query: "logger LIKE 'c.h.c.engine.endpoint.web%'",
    description: '查询所有API请求相关日志',
  },
  {
    name: 'Kafka消息处理',
    query: "method = 'onMessage' AND msg LIKE '%批量拉取%'",
    description: '查询Kafka消息处理日志',
  },
  {
    name: '微信用户请求',
    query: "msg LIKE '%MicroMessenger%' OR msg LIKE '%WeChat%'",
    description: '查询来自微信用户的请求',
  },
];

// 时间范围预设选项
export const QUICK_RANGES: Record<string, IStatus> = {
  last_5m: {
    label: '最近5分钟',
    from: () => dayjs().subtract(5, 'minute'),
    to: () => dayjs(),
    format: [DATE_FORMAT_THOUSOND, DATE_FORMAT_THOUSOND],
  },
  last_15m: {
    label: '最近15分钟',
    from: () => dayjs().subtract(15, 'minute'),
    to: () => dayjs(),
    format: [DATE_FORMAT_THOUSOND, DATE_FORMAT_THOUSOND],
  },
  last_30m: {
    label: '最近30分钟',
    from: () => dayjs().subtract(30, 'minute'),
    to: () => dayjs(),
    format: [DATE_FORMAT_THOUSOND, DATE_FORMAT_THOUSOND],
  },
  last_1h: {
    label: '最近1小时',
    from: () => dayjs().subtract(1, 'hour'),
    to: () => dayjs(),
    format: [DATE_FORMAT_THOUSOND, DATE_FORMAT_THOUSOND],
  },
  last_8h: {
    label: '最近8小时',
    from: () => dayjs().subtract(8, 'hour'),
    to: () => dayjs(),
    format: [DATE_FORMAT_THOUSOND, DATE_FORMAT_THOUSOND],
  },
  last_24h: {
    label: '最近24小时',
    from: () => dayjs().subtract(24, 'hour'),
    to: () => dayjs(),
    format: [DATE_FORMAT_THOUSOND, DATE_FORMAT_THOUSOND],
  },
  last_7d: {
    label: '最近7天',
    from: () => dayjs().subtract(7, 'day'),
    to: () => dayjs(),
    format: [DATE_FORMAT_THOUSOND, DATE_FORMAT_THOUSOND],
  },
  last_2week: {
    label: '最近2周',
    from: () => dayjs().subtract(13, 'day'), // 包含今天共14天
    to: () => dayjs(),
    format: [DATE_FORMAT_THOUSOND, DATE_FORMAT_THOUSOND],
  },
  today: {
    label: '今天',
    from: () => dayjs().startOf('day'),
    to: () => dayjs().endOf('day'),
    format: [DATE_FORMAT_THOUSOND, DATE_FORMAT_THOUSOND],
  },
  yesterday: {
    label: '昨天',
    from: () => dayjs().subtract(1, 'day').startOf('day'),
    to: () => dayjs().subtract(1, 'day').endOf('day'),
    format: [DATE_FORMAT_THOUSOND, DATE_FORMAT_THOUSOND],
  },
  this_week: {
    label: '本周',
    from: () => dayjs().startOf('week'),
    to: () => dayjs().endOf('week'),
    format: [DATE_FORMAT_THOUSOND, DATE_FORMAT_THOUSOND],
  },

  last_week: {
    label: '上周',
    from: () => dayjs().subtract(1, 'week').startOf('week'),
    to: () => dayjs().subtract(1, 'week').endOf('week'),
    format: [DATE_FORMAT_THOUSOND, DATE_FORMAT_THOUSOND],
  },
};

// 相对时间范围
export const RELATIVE_TIME: IRelativeTime[] = [
  { label: '秒前', value: '秒前', unitCN: '秒', format: 'YYYY-MM-DD HH:mm:ss', unitEN: 'second' },
  {
    label: '分钟前',
    value: '分钟前',
    unitCN: '分钟',
    format: 'YYYY-MM-DD HH:mm:00.000',
    unitEN: 'minute',
  },
  {
    label: '小时前',
    value: '小时前',
    unitCN: '小时',
    format: 'YYYY-MM-DD HH:00:00.000',
    unitEN: 'hour',
  },
  { label: '天前', value: '天前', unitCN: '天', format: 'YYYY-MM-DD 00:00:00', unitEN: 'day' },
  { label: '周前', value: '周前', unitCN: '周', format: 'YYYY-MM-DD 00:00:00', unitEN: 'week' },
  { label: '月前', value: '月前', unitCN: '月', format: 'YYYY-MM-01 00:00:00', unitEN: 'month' },
  { label: '年前', value: '年前', unitCN: '年', format: 'YYYY-01-01 00:00:00', unitEN: 'year' },
  { label: '秒后', value: '秒后', unitCN: '秒', format: 'YYYY-MM-DD HH:mm:ss', unitEN: 'second' },
  {
    label: '分钟后',
    value: '分钟后',
    unitCN: '分钟',
    format: 'YYYY-MM-DD HH:mm:00.000',
    unitEN: 'minute',
  },
  {
    label: '小时后',
    value: '小时后',
    unitCN: '小时',
    format: 'YYYY-MM-DD HH:00:00.000',
    unitEN: 'hour',
  },
  { label: '天后', value: '天后', unitCN: '天', format: 'YYYY-MM-DD 00:00:00.000', unitEN: 'day' },
  { label: '周后', value: '周后', unitCN: '周', format: 'YYYY-MM-DD 00:00:00.000', unitEN: 'week' },
  { label: '月后', value: '月后', unitCN: '月', format: 'YYYY-MM-01 00:00:00.000', unitEN: 'month' },
  { label: '年后', value: '年后', unitCN: '年', format: 'YYYY-01-01 00:00:00.000', unitEN: 'year' },
];

// 时间分组
export const TIME_GROUP: Record<string, string> = {
  second: '秒',
  minute: '分钟',
  hour: '小时',
  day: '天',
  auto: '自动',
};

// 获取当前时间
export const getLatestTime = (timeOption: ILogTimeSubmitParams) => {
  const { type, value, startOption, endOption, range } = timeOption;
  const target: any = {};
  if (!value) return target;

  // 快捷选择
  if (type === 'quick' && QUICK_RANGES[value]) {
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

// 动态确定时间字段的逻辑
export const determineTimeField = (availableColumns: ILogColumnsResponse[], moduleQueryConfig: QueryConfig): string => {
  const availableFieldNames = availableColumns.map((col) => col.columnName).filter(Boolean) as string[];
  // 优先使用配置的时间字段
  if (moduleQueryConfig?.timeField && availableFieldNames.includes(moduleQueryConfig.timeField)) {
    return moduleQueryConfig.timeField;
  }

  // 如果配置的时间字段不存在，按优先级查找常见时间字段
  const commonTimeFields = ['logs_timestamp', 'log_time', 'timestamp', 'time', '@timestamp'];
  for (const timeField of commonTimeFields) {
    if (availableFieldNames.includes(timeField)) {
      return timeField;
    }
  }

  // 如果都没找到，尝试查找包含time关键字的字段
  const timeRelatedField = availableFieldNames.find(
    (field) => field?.toLowerCase().includes('time') || field?.toLowerCase().includes('timestamp'),
  );
  if (timeRelatedField) {
    return timeRelatedField;
  }

  if (availableFieldNames.length > 0 && availableFieldNames[0]) {
    return availableFieldNames[0];
  }

  return '';
};

export const formatSqlKey = (sql: string) => {
  return sql.replace(/\s+/g, '');
};

export const deduplicateAndDeleteWhereSqls = (sqls: string[], deleteSql?: string) => {
  const seen = new Map<string, string>();
  const deleteKey = formatSqlKey(deleteSql || '');
  sqls.forEach((sql) => {
    // 去掉所有空格作为key
    const normalizedKey = formatSqlKey(sql || '');
    if (deleteKey && deleteKey === normalizedKey) return;
    if (!seen.has(normalizedKey)) {
      seen.set(normalizedKey, sql);
    }
  });
  return Array.from(seen.values());
};

/**
 * 防抖函数
 * @param func 需要防抖的函数
 * @param wait 延迟时间（毫秒）
 * @returns 防抖后的函数
 */
export const debounce = <T extends (...args: any[]) => any>(
  func: T,
  wait: number,
): ((...args: Parameters<T>) => void) => {
  let timeout: NodeJS.Timeout | null = null;

  return (...args: Parameters<T>) => {
    if (timeout) {
      clearTimeout(timeout);
    }

    timeout = setTimeout(() => {
      func(...args);
      timeout = null;
    }, wait);
  };
};

/**
 * 解析 timeRange 字符串，返回标准的时间选项对象
 * @param timeRange 时间范围字符串，支持三种模式：
 *   1. quick模式：如 'last_5m', 'today' 等
 *   2. relative模式：如 '23秒前 ~ 2秒前', '现在 ~ 现在'
 *   3. absolute模式：如 '2025-11-28 00:00:00 ~ 2025-11-30 00:00:00'
 * @returns ILogTimeSubmitParams 时间选项对象
 */
export const parseTimeRange = (timeRange?: string): ILogTimeSubmitParams => {
  if (!timeRange) {
    // 默认返回最近15分钟
    const defaultRange = QUICK_RANGES['last_15m'];
    return {
      range: [defaultRange.from().format(defaultRange.format[0]), defaultRange.to().format(defaultRange.format[1])],
      label: defaultRange.label,
      value: 'last_15m',
      type: 'quick',
    };
  }

  // 1. 检查是否为 quick 模式
  if (QUICK_RANGES[timeRange] || !timeRange.includes('~')) {
    const quickRange = QUICK_RANGES[timeRange] || QUICK_RANGES['last_15m'];
    return {
      range: [quickRange.from().format(quickRange.format[0]), quickRange.to().format(quickRange.format[1])],
      label: quickRange.label,
      value: timeRange,
      type: 'quick',
    };
  }

  const [startPart, endPart] = timeRange.split('~').map((s) => s.trim());

  // 2. 检查是否为 absolute 模式（包含完整日期时间格式）
  // absolute 格式示例：'2025-11-28 00:00:00' 或 '2025-11-28 00:00:00.000'
  const absoluteTimeRegex = /^\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}(\.\d{3})?$/;
  if (absoluteTimeRegex.test(startPart) && absoluteTimeRegex.test(endPart)) {
    // 格式化为带毫秒的格式
    const startTime = dayjs(startPart).format(DATE_FORMAT_THOUSOND);
    const endTime = dayjs(endPart).format(DATE_FORMAT_THOUSOND);
    return {
      range: [startTime, endTime],
      label: timeRange,
      value: timeRange,
      type: 'absolute',
    };
  }

  // 3. relative 模式
  // relative 格式示例：'23秒前 ~ 2秒前', '现在 ~ 现在', '1分钟前(精确到分钟) ~ 现在'
  const parseRelativePart = (part: string): IRelativeTimeState | null => {
    // 处理 "现在" 的情况，等同于 "0秒前"
    if (part === '现在') {
      const relativeItem = RELATIVE_TIME.find((item) => item.value === '秒前');
      if (relativeItem) {
        return {
          ...relativeItem,
          number: 0,
          isExact: false,
        };
      }
      return null;
    }

    // 检查是否包含精确标记
    const exactMatch = part.match(/\(精确到(.+?)\)/);
    const isExact = !!exactMatch;
    const cleanPart = part.replace(/\(精确到.+?\)/, '').trim();

    // 解析数字和单位
    // 格式：数字 + 单位（秒前/分钟前/小时前等）
    const match = cleanPart.match(/^(\d+)(.+)$/);
    if (!match) return null;

    const number = parseInt(match[1], 10);
    const unit = match[2]; // 如：秒前、分钟前、小时前等

    // 在 RELATIVE_TIME 中查找对应的配置
    const relativeItem = RELATIVE_TIME.find((item) => item.value === unit);
    if (!relativeItem) return null;

    return {
      ...relativeItem,
      number,
      isExact,
    };
  };

  const getTimeText = (option: IRelativeTimeState): string => {
    const now = dayjs();
    const { number, unitEN, isExact, label, format } = option;

    const unit = unitEN as ManipulateType;
    const fmt = isExact ? format : DATE_FORMAT_THOUSOND;

    if (number === 0 && unitEN === 'second') {
      return now.format(fmt);
    }

    if (label.endsWith('前')) {
      return now.subtract(number, unit).format(fmt);
    }
    // 其余情况均视为「后」
    return now.add(number, unit).format(fmt);
  };

  const startOption = parseRelativePart(startPart);
  const endOption = parseRelativePart(endPart);

  if (startOption && endOption) {
    return {
      range: [getTimeText(startOption), getTimeText(endOption)],
      label: timeRange,
      value: timeRange,
      type: 'relative',
      startOption,
      endOption,
    };
  }

  // 无法解析，返回默认值
  const defaultRange = QUICK_RANGES['last_15m'];
  return {
    range: [defaultRange.from().format(defaultRange.format[0]), defaultRange.to().format(defaultRange.format[1])],
    label: defaultRange.label,
    value: 'last_15m',
    type: 'quick',
  };
};
