import type { MessageInstance } from 'antd/es/message/interface';

export {};
declare global {
  interface Window {
    messageApi: MessageInstance;
  }
  interface IStatus {
    value?: string;
    label?: string;
    [key: string]: any;
  }

  // 时间组件的提交参数
  interface ILogTimeSubmitParams {
    label?: string; // 显示的标签, 如：'最近7天'
    value?: string; // 值, 如：'last_7d'
    range?: string[]; // 时间范围，如：['2020-01-01 12:26:38', '2020-03-02 12:26:38']
    format?: string[]; // 时间格式，如：['YYYY-MM-DD HH:mm:ss', 'YYYY-MM-DD HH:mm:ss']
    form?: () => void; // dayjs函数
    to?: () => void; // dayjs函数
    type?: 'relative' | 'absolute' | 'quick'; // 时间范围类型，如：'relative', 'absolute', 'quick'
    startOption?: IRelativeTime; // 开始时间选项
    endOption?: IRelativeTime; // 结束时间选项
  }

  // 旧

  // 相对时间
  interface IRelativeTime {
    label: string; // 显示的标签
    value: string; // 后端的值
    unitCN: string; // 单位中文
    unitEN: string; // 单位英文
    format: string; // 时间格式
    isExact?: boolean; // 是否精确到单位
    number?: number; // 数量
  }

  // 字段值分布列表，按数量降序排序
  interface IValueDistributions {
    count: number; // 出现次数
    percentage: number; // 占比百分比
    value: string; // 字段值
  }

  interface IFieldDistributions {
    sampleSize: any; // 样本大小
    fieldName: string; // 字段名称
    nonNullCount: number; // 非空记录数
    nullCount: number; // 空记录数
    totalCount: number; // 总记录数
    uniqueValueCount: number; // 唯一值数量
    valueDistributions: IValueDistributions[];
  }

  // 时间间隔
  interface IIntervalConfig {
    interval: number;
    unit: 'second' | 'minute' | 'hour' | 'day';
  }

  interface ITimeIntervalConfig {
    auto: IIntervalConfig;
    second: IIntervalConfig;
    minute: IIntervalConfig;
    hour?: IIntervalConfig;
    day?: IIntervalConfig;
  }

  interface ITimeInterval {
    last_5m: ITimeIntervalConfig;
    last_15m: ITimeIntervalConfig;
    last_30m: ITimeIntervalConfig;
    last_1h: ITimeIntervalConfig;
    last_8h: ITimeIntervalConfig;
    last_24h: ITimeIntervalConfig;
    today: ITimeIntervalConfig;
    yesterday: ITimeIntervalConfig;
    last_week: ITimeIntervalConfig;
  }
}
