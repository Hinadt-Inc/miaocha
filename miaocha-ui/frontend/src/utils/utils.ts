export const colorPrimary = '#0038FF'; // 主题色

export const isOverOneDay = (startTime: string, endTime: string) => {
  const start = new Date(startTime).getTime();
  const end = new Date(endTime).getTime();
  return end - start > 24 * 60 * 60 * 1000; // 1天的毫秒数
};
