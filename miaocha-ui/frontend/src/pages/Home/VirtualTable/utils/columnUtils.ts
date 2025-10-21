/**
 * 计算文本宽度的工具函数
 */

/**
 * 计算文本在指定字体下的渲染宽度
 * @param text 要计算的文本
 * @param fontSize 字体大小，默认14px
 * @param fontFamily 字体族，默认Arial
 * @returns 文本宽度（像素）
 */
export const getTextWidth = (
  text: string, 
  fontSize = 14, 
  fontFamily = 'Arial'
): number => {
  const canvas = document.createElement('canvas');
  const context = canvas.getContext('2d');
  if (!context) return text.length * 8; // 降级处理

  context.font = `${fontSize}px ${fontFamily}`;
  const metrics = context.measureText(text);
  return Math.ceil(metrics.width);
};

/**
 * 计算列的自适应宽度
 * @param columnName 列名
 * @param screenWidth 屏幕宽度
 * @param minWidth 最小宽度，默认120px
 * @param maxWidth 最大宽度，默认400px
 * @returns 计算后的列宽度
 */
export const getAutoColumnWidth = (
  columnName: string, 
  screenWidth: number, 
  minWidth = 120, 
  maxWidth = 400
): number => {
  const textWidth = getTextWidth(columnName, 14, '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto');
  // 添加padding和排序图标空间：
  // 左右padding各16px，排序图标20px，删除/移动按钮空间40px，安全余量20px
  const totalWidth = textWidth + 112;
  
  // 在小屏幕上进一步限制最大宽度
  let adjustedMaxWidth = maxWidth;
  
  if (screenWidth < 1200) {
    // 小屏幕上，根据屏幕宽度动态调整最大宽度
    adjustedMaxWidth = Math.min(maxWidth, Math.floor((screenWidth - 300) / 4)); // 留出300px给时间列和操作空间，剩余空间平均分配
  }
  
  return Math.min(Math.max(totalWidth, minWidth), adjustedMaxWidth);
};
