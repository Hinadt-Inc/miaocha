/**
 * TimePicker 组件相关类型定义
 */
import type { Dayjs } from 'dayjs';

// 时间状态接口
export interface ITimeStatus {
  value?: string;
  label?: string;
  //  from: () => dayjs().subtract(1, 'week').startOf('week'),
  //     to: () => dayjs().subtract(1, 'week').endOf('week'),
  from?: () => Dayjs;
  to?: () => Dayjs;
  format?: string[];
  [key: string]: unknown;
}

// 时间组件的提交参数
export interface ILogTimeSubmitParams {
  label?: string; // 显示的标签, 如：'最近7天'
  value?: string; // 值, 如：'last_7d'
  range?: string[]; // 时间范围，如：['2020-01-01 12:26:38', '2020-03-02 12:26:38']
  format?: string[]; // 时间格式，如：['YYYY-MM-DD HH:mm:ss', 'YYYY-MM-DD HH:mm:ss']
  from?: () => Dayjs; // dayjs函数
  to?: () => Dayjs; // dayjs函数
  type?: 'relative' | 'absolute' | 'quick'; // 时间范围类型，如：'relative', 'absolute', 'quick'
  startOption?: IRelativeTime; // 开始时间选项
  endOption?: IRelativeTime; // 结束时间选项
}

// 相对时间配置
export interface IRelativeTime {
  label: string; // 显示的标签
  value: string; // 后端的值
  unitCN: string; // 单位中文
  unitEN: string; // 单位英文
  format: string; // 时间格式
  isExact?: boolean; // 是否精确到单位
  number?: number; // 数量
}

// TimePicker 主组件 Props
export interface ITimePickerProps {
  activeTab: string; // 选中的选项卡
  setActiveTab: (tab: string) => void; // 设置选中的选项卡
  onSubmit: (params: ILogTimeSubmitParams) => void; // 提交时间
  currentTimeOption?: ILogTimeSubmitParams; // 当前选择的时间选项
}

// 相对时间组件 Props
export interface IRelativeTimePickerProps {
  onSubmit: (params: ILogTimeSubmitParams) => void; // 提交时间
  currentTimeOption?: ILogTimeSubmitParams; // 当前选择的时间选项
}

// 快速选择组件 Props
export interface IQuickTimePickerProps {
  selectedTag: string;
  onTagChange: (value: string) => void;
}

// 绝对时间组件 Props
export interface IAbsoluteTimePickerProps {
  onSubmit: (params: ILogTimeSubmitParams) => void;
  currentTimeOption?: ILogTimeSubmitParams; // 当前选择的时间选项
}

// 相对时间内部状态
export interface IRelativeTimeState extends IRelativeTime {
  number: number; // 数字框的值
  isExact: boolean; // 是否精确到秒
}
