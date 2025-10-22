export const colorPrimary = '#0038FF'; // 主题色

/**
 * 判断两个时间点之间的间隔是否超过一天（24小时）
 * @param startTime 开始时间字符串
 * @param endTime 结束时间字符串
 * @returns 超过24小时返回true，否则返回false
 */
export const isOverOneDay = (startTime: string, endTime: string): boolean => {
  const start = new Date(startTime).getTime(); // 开始时间转为毫秒
  const end = new Date(endTime).getTime(); // 结束时间转为毫秒
  return end - start > 24 * 60 * 60 * 1000; // 判断是否超过24小时
};

/**
 * 防抖函数，延迟执行传入的函数，如果在延迟时间内再次调用则重新计时
 * @param func 需要防抖的函数
 * @param wait 延迟时间（毫秒）
 * @param immediate 是否立即执行一次
 * @returns 防抖后的函数
 */
export function debounce<T extends (...args: any[]) => any>(
  func: T,
  wait: number,
  immediate: boolean = false,
): T & { cancel: () => void } {
  let timeout: NodeJS.Timeout | null = null;
  let result: ReturnType<T> | undefined;

  const debounced = function (this: ThisParameterType<T>, ...args: Parameters<T>): ReturnType<T> | undefined {
    if (timeout) {
      clearTimeout(timeout);
    }

    if (immediate) {
      // 如果是立即执行模式
      const callNow = !timeout;
      timeout = setTimeout(() => {
        timeout = null;
      }, wait);

      if (callNow) {
        result = func.apply(this, args);
      }
    } else {
      // 常规模式：延迟执行
      timeout = setTimeout(() => {
        result = func.apply(this, args);
      }, wait);
    }

    return result;
  } as T & { cancel: () => void };

  // 添加取消功能
  debounced.cancel = function () {
    if (timeout) {
      clearTimeout(timeout);
      timeout = null;
    }
  };

  return debounced;
}
