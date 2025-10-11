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
