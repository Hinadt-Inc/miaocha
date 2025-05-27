import dayjs from 'dayjs';
import customParseFormat from 'dayjs/plugin/customParseFormat';
import isSameOrBefore from 'dayjs/plugin/isSameOrBefore';
import isSameOrAfter from 'dayjs/plugin/isSameOrAfter';

import duration from 'dayjs/plugin/duration';

// 扩展 dayjs 功能
dayjs.extend(duration);
dayjs.extend(customParseFormat);
dayjs.extend(isSameOrBefore);
dayjs.extend(isSameOrAfter);

export const DATE_FORMAT = 'YYYY-MM-DD HH:mm:ss';

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
    format: [DATE_FORMAT, DATE_FORMAT],
  },
  last_15m: {
    label: '最近15分钟',
    from: () => dayjs().subtract(15, 'minute'),
    to: () => dayjs(),
    format: [DATE_FORMAT, DATE_FORMAT],
  },
  last_30m: {
    label: '最近30分钟',
    from: () => dayjs().subtract(30, 'minute'),
    to: () => dayjs(),
    format: [DATE_FORMAT, DATE_FORMAT],
  },
  last_1h: {
    label: '最近1小时',
    from: () => dayjs().subtract(1, 'hour'),
    to: () => dayjs(),
    format: [DATE_FORMAT, DATE_FORMAT],
  },
  last_8h: {
    label: '最近8小时',
    from: () => dayjs().subtract(8, 'hour'),
    to: () => dayjs(),
    format: [DATE_FORMAT, DATE_FORMAT],
  },
  last_24h: {
    label: '最近24小时',
    from: () => dayjs().subtract(24, 'hour'),
    to: () => dayjs(),
    format: [DATE_FORMAT, DATE_FORMAT],
  },
  today: {
    label: '今天',
    from: () => dayjs().subtract(0, 'day'),
    to: () => dayjs(),
    format: ['YYYY-MM-DD 00:00:00', 'YYYY-MM-DD 23:59:59'],
  },
  yesterday: {
    label: '昨天',
    from: () => dayjs().subtract(1, 'day').startOf('day'),
    to: () => dayjs().subtract(1, 'day').endOf('day'),
    format: ['YYYY-MM-DD 00:00:00', 'YYYY-MM-DD 23:59:59'],
  },
  last_week: {
    label: '上周',
    from: () => dayjs().subtract(1, 'week').startOf('week'),
    to: () => dayjs().subtract(1, 'week').endOf('week'),
    format: ['YYYY-MM-DD 00:00:00', 'YYYY-MM-DD 23:59:59'],
  },
};

// 相对时间范围
export const RELATIVE_TIME: IRelativeTime[] = [
  { label: '秒前', value: '秒前', unitCN: '秒', format: 'YYYY-MM-DD HH:mm:ss', unitEN: 'second' },
  {
    label: '分钟前',
    value: '分钟前',
    unitCN: '分钟',
    format: 'YYYY-MM-DD HH:mm:00',
    unitEN: 'minute',
  },
  {
    label: '小时前',
    value: '小时前',
    unitCN: '小时',
    format: 'YYYY-MM-DD HH:00:00',
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
    format: 'YYYY-MM-DD HH:mm:00',
    unitEN: 'minute',
  },
  {
    label: '小时后',
    value: '小时后',
    unitCN: '小时',
    format: 'YYYY-MM-DD HH:00:00',
    unitEN: 'hour',
  },
  { label: '天后', value: '天后', unitCN: '天', format: 'YYYY-MM-DD 00:00:00', unitEN: 'day' },
  { label: '周后', value: '周后', unitCN: '周', format: 'YYYY-MM-DD 00:00:00', unitEN: 'week' },
  { label: '月后', value: '月后', unitCN: '月', format: 'YYYY-MM-01 00:00:00', unitEN: 'month' },
  { label: '年后', value: '年后', unitCN: '年', format: 'YYYY-01-01 00:00:00', unitEN: 'year' },
];

// 时间分组
export const TIME_GROUP: Record<string, string> = {
  second: '秒',
  minute: '分钟',
  hour: '小时',
  day: '天',
  auto: '自动',
};

// 范围间隔枚举
const timeInterval: ITimeInterval = {
  last_5m: {
    auto: {
      interval: 10,
      unit: 'second',
    },
    second: {
      interval: 5,
      unit: 'second',
    },
    minute: {
      interval: 1,
      unit: 'minute',
    },
  },
  last_15m: {
    auto: {
      interval: 30,
      unit: 'second',
    },
    second: {
      interval: 5,
      unit: 'second',
    },
    minute: {
      interval: 1,
      unit: 'minute',
    },
  },
  last_30m: {
    auto: {
      interval: 1,
      unit: 'minute',
    },
    second: {
      interval: 10,
      unit: 'second',
    },
    minute: {
      interval: 1,
      unit: 'minute',
    },
  },
  last_1h: {
    auto: {
      interval: 5,
      unit: 'minute',
    },
    second: {
      interval: 30,
      unit: 'second',
    },
    minute: {
      interval: 1,
      unit: 'minute',
    },
  },
  last_8h: {
    auto: {
      interval: 5,
      unit: 'minute',
    },
    second: {
      interval: 1,
      unit: 'minute',
    },
    minute: {
      interval: 1,
      unit: 'minute',
    },
    hour: {
      interval: 1,
      unit: 'hour',
    },
  },
  last_24h: {
    auto: {
      interval: 30,
      unit: 'minute',
    },
    second: {
      interval: 10,
      unit: 'minute',
    },
    minute: {
      interval: 10,
      unit: 'minute',
    },
    hour: {
      interval: 1,
      unit: 'hour',
    },
  },
  today: {
    auto: {
      interval: 30,
      unit: 'minute',
    },
    second: {
      interval: 10,
      unit: 'minute',
    },
    minute: {
      interval: 10,
      unit: 'minute',
    },
    hour: {
      interval: 1,
      unit: 'hour',
    },
  },
  yesterday: {
    auto: {
      interval: 30,
      unit: 'minute',
    },
    second: {
      interval: 10,
      unit: 'minute',
    },
    minute: {
      interval: 10,
      unit: 'minute',
    },
    hour: {
      interval: 1,
      unit: 'hour',
    },
  },
  last_week: {
    auto: {
      interval: 3,
      unit: 'hour',
    },
    second: {
      interval: 1,
      unit: 'hour',
    },
    minute: {
      interval: 1,
      unit: 'hour',
    },
    hour: {
      interval: 1,
      unit: 'hour',
    },
    day: {
      interval: 1,
      unit: 'day',
    },
  },
};
export const getTimeRangeCategory = (timeRange: any) => {
  const [startTimeStr, endTimeStr] = timeRange;

  // 解析时间字符串
  const startTime = dayjs(startTimeStr, 'YYYY-MM-DD HH:mm:ss');
  const endTime = dayjs(endTimeStr, 'YYYY-MM-DD HH:mm:ss');

  // 当前时间
  const now = dayjs();

  // 计算时间差（毫秒）
  const durationMs = endTime.diff(startTime);
  const durationMinutes = dayjs.duration(durationMs).asMinutes(); // 转换为分钟

  // 判断是否是过去的时间段（结束时间早于当前时间）
  if (durationMinutes <= 5) {
    return {
      label: 'last_5m',
      value: timeInterval.last_5m,
    };
  } else if (durationMinutes <= 15) {
    return {
      label: 'last_15m',
      value: timeInterval.last_15m,
    };
  } else if (durationMinutes <= 30) {
    return {
      label: 'last_30m',
      value: timeInterval.last_30m,
    };
  } else if (durationMinutes <= 60) {
    return {
      label: 'last_1h',
      value: timeInterval.last_1h,
    };
  } else if (durationMinutes <= 8 * 60) {
    return {
      label: 'last_8h',
      value: timeInterval.last_8h,
    };
  } else if (durationMinutes <= 24 * 60 && !startTimeStr.endsWith('00:00:00') && !endTimeStr.endsWith('23:59:59')) {
    return {
      label: 'last_24h',
      value: timeInterval.last_24h,
    };
  }

  const isToday = () => {
    return (
      startTime.isSame(startTime.startOf('day')) && // 确保开始时间是当天开始
      endTime.isSame(endTime.endOf('day').millisecond(0)) && // 确保结束时间是当天结束
      startTime.isSame(now, 'day') && // 确保是今天
      startTime.isSame(endTime, 'day') // 确保开始和结束是同一天
    );
  };

  // 判断是否是昨天
  const isYesterday = () => {
    const yesterdayStart = now.subtract(1, 'day').startOf('day');
    const yesterdayEnd = now.subtract(1, 'day').endOf('day');
    return (
      startTime.isSame(yesterdayStart.startOf('day')) &&
      endTime.isSame(yesterdayEnd.endOf('day').millisecond(0)) &&
      startTime.isSameOrAfter(yesterdayStart) &&
      endTime.isSameOrBefore(yesterdayEnd)
    );
  };

  // 判断是否是上周
  const isLastWeek = () => {
    const lastWeekStart = now.subtract(1, 'week').startOf('week'); // 上周一 00:00:00
    const lastWeekEnd = lastWeekStart.endOf('week'); // 上周日 23:59:59

    return (
      startTime.isSame(startTime.startOf('day')) && // 确保开始时间是那天的开始
      endTime.isSame(endTime.endOf('day').millisecond(0)) && // 确保结束时间是那天的结束
      startTime.isSameOrAfter(lastWeekStart) && // 确保开始时间不早于上周开始
      endTime.isSameOrBefore(lastWeekEnd) && // 确保结束时间不晚于上周结束
      startTime.isSame(endTime, 'week') // 确保开始和结束时间在同一周
    );
  };

  if (isToday()) {
    return {
      label: 'today',
      value: timeInterval.today,
    };
  } else if (isYesterday()) {
    return {
      label: 'yesterday',
      value: timeInterval.yesterday,
    };
  } else if (isLastWeek()) {
    return {
      label: 'last_week',
      value: timeInterval.last_week,
    };
  }
  return {
    label: 'last_week',
    value: timeInterval.last_week,
  };
};
