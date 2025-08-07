/**
 * 自动刷新组件的类型定义
 */

export interface AutoRefreshProps {
  onRefresh: () => void; // 刷新回调函数
  loading?: boolean; // 是否正在加载
  disabled?: boolean; // 是否禁用
}

export interface RefreshInterval {
  label: string;
  value: number; // 毫秒
  disabled?: boolean;
}

export interface AutoRefreshState {
  isAutoRefreshing: boolean; // 是否开启自动刷新
  refreshInterval: number; // 刷新间隔（毫秒）
  remainingTime: number; // 剩余时间（毫秒）
  lastRefreshTime: Date | null; // 上次刷新时间
  isPaused: boolean; // 是否暂停（用于loading期间）
}
