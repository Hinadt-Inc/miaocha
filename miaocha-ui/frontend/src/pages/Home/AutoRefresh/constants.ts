import { RefreshInterval } from './types';

/**
 * 刷新间隔选项配置
 */
export const REFRESH_INTERVALS: RefreshInterval[] = [
  { label: '关闭', value: 0, disabled: false },
  { label: '5秒', value: 5000, disabled: false },
  { label: '10秒', value: 10000, disabled: false },
  { label: '30秒', value: 30000, disabled: false },
  { label: '1分钟', value: 60000, disabled: false },
  { label: '5分钟', value: 300000, disabled: false },
  { label: '15分钟', value: 900000, disabled: false },
  { label: '30分钟', value: 1800000, disabled: false },
  { label: '1小时', value: 3600000, disabled: false },
];

/**
 * 默认配置
 */
export const DEFAULT_CONFIG = {
  COUNTDOWN_INTERVAL: 1000, // 倒计时间隔（毫秒）
  RESTART_DELAY: 100, // 重启延迟（毫秒）
} as const;
