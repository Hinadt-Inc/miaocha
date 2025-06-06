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
  last_7d: {
    label: '最近7天',
    from: () => dayjs().subtract(7, 'day'),
    to: () => dayjs(),
    format: [DATE_FORMAT, DATE_FORMAT],
  },
  last_2week: {
    label: '最近2周',
    from: () => dayjs().subtract(13, 'day'), // 包含今天共14天
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
  this_week: {
    label: '本周',
    from: () => dayjs().startOf('week'),
    to: () => dayjs().endOf('week'),
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
      .format(startOption.isExact ? startOption.format : DATE_FORMAT);
    const end = dayjs()
      .subtract(endOption.number || 0, endOption.unitEN as any)
      .format(endOption.isExact ? endOption.format : DATE_FORMAT);
    target.startTime = start;
    target.endTime = end;
  } else if (type === 'absolute' && range?.length === 2) {
    // 绝对时间
    target.startTime = range[0];
    target.endTime = range[1];
  }
  return target;
};
