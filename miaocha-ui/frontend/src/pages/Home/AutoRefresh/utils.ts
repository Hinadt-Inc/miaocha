/**
 * 自动刷新相关的工具函数
 */

/**
 * 格式化剩余时间显示
 * @param ms 剩余时间（毫秒）
 * @param loading 是否正在加载
 * @returns 格式化的时间字符串
 */
export const formatRemainingTime = (ms: number, loading?: boolean): string => {
  if (ms <= 0 || loading) return loading ? '...' : '';
  
  const seconds = Math.ceil(ms / 1000);
  if (seconds < 60) {
    return `${seconds}s`;
  }
  
  const minutes = Math.floor(seconds / 60);
  const remainingSeconds = seconds % 60;
  return remainingSeconds > 0 ? `${minutes}m${remainingSeconds}s` : `${minutes}m`;
};

/**
 * 格式化上次刷新时间
 * @param date 刷新时间
 * @returns 格式化的时间字符串
 */
export const formatLastRefreshTime = (date: Date | null): string => {
  if (!date) return '';
  
  const now = new Date();
  const diff = now.getTime() - date.getTime();
  
  if (diff < 60000) {
    return '刚刚';
  } else if (diff < 3600000) {
    const minutes = Math.floor(diff / 60000);
    return `${minutes}分钟前`;
  } else {
    return date.toLocaleTimeString('zh-CN', { hour12: false });
  }
};

/**
 * 计算进度百分比
 * @param refreshInterval 刷新间隔
 * @param remainingTime 剩余时间
 * @param isAutoRefreshing 是否正在自动刷新
 * @param loading 是否正在加载
 * @returns 进度百分比（0-100）
 */
export const calculateProgressPercent = (
  refreshInterval: number,
  remainingTime: number,
  isAutoRefreshing: boolean,
  loading?: boolean
): number => {
  if (!isAutoRefreshing || refreshInterval <= 0 || loading) return 0;
  return Math.max(0, Math.min(100, ((refreshInterval - remainingTime) / refreshInterval) * 100));
};

/**
 * 生成Tooltip内容
 * @param isAutoRefreshing 是否正在自动刷新
 * @param refreshInterval 刷新间隔
 * @param currentIntervalLabel 当前间隔标签
 * @param loading 是否正在加载
 * @param remainingTime 剩余时间
 * @param lastRefreshTime 上次刷新时间
 * @returns Tooltip内容字符串
 */
export const generateTooltipContent = (
  isAutoRefreshing: boolean,
  refreshInterval: number,
  currentIntervalLabel: string,
  loading?: boolean,
  remainingTime?: number,
  lastRefreshTime?: Date | null
): string => {
  if (!isAutoRefreshing || refreshInterval === 0) {
    return '点击开启自动刷新';
  }
  
  let content = `自动刷新已开启，每${currentIntervalLabel}刷新一次`;
  
  if (loading) {
    content += '\n当前正在加载中...';
  } else if (remainingTime && remainingTime > 0) {
    content += `\n下次刷新: ${formatRemainingTime(remainingTime)}`;
  }
  
  if (lastRefreshTime) {
    content += `\n上次刷新: ${formatLastRefreshTime(lastRefreshTime)}`;
  }
  
  return content;
};
