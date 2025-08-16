import dayjs from 'dayjs';
import { ITimeStatus, IRelativeTime } from '../types';

// 日期格式常量
export const DATE_FORMAT = 'YYYY-MM-DD HH:mm:ss';
export const DATE_FORMAT_THOUSOND = 'YYYY-MM-DD HH:mm:ss.SSS';

// 时间范围预设选项
export const QUICK_RANGES: Record<string, ITimeStatus> = {
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

// 相对时间范围配置
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

// 时间分组选项
export const TIME_GROUP: Record<string, string> = {
  second: '秒',
  minute: '分钟',
  hour: '小时',
  day: '天',
  auto: '自动',
};
