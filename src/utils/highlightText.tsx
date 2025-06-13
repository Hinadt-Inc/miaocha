/*
  高亮文本
  @param {string} text - 文本
  @param {string[]} keywords - 关键词
  @returns {React.ReactNode} - 高亮文本
*/
export const highlightText = (text: string, keywords: string[]) => {
  // 处理keywords，将每项中的中英文字符提取出来
  const extractWords = (expr: string) => {
    // 匹配所有连续的中英文字符串
    return expr.match(/[\u4e00-\u9fa5a-zA-Z0-9]+/g) || [];
  };

  // 扁平化所有关键词
  let flatKeywords: string[] = [];
  if (keywords && keywords.length) {
    keywords.forEach((expr) => {
      flatKeywords.push(...extractWords(expr));
    });
  }
  // 去重
  flatKeywords = Array.from(new Set(flatKeywords)).filter(Boolean);

  // 按长度降序排列，先匹配长的关键词，避免短关键词干扰长关键词的匹配
  flatKeywords.sort((a, b) => b.length - a.length);

  if (!flatKeywords.length) return <span title={text}>{text}</span>;

  let str = String(text);
  flatKeywords.forEach((kwStr) => {
    // 中文单字匹配
    if (/^[\u4e00-\u9fa5]$/.test(kwStr)) {
      str = str.replace(new RegExp(kwStr, 'g'), `<mark>${kwStr}</mark>`);
    } else {
      // 英文匹配，不区分大小写，但保持原始字符串格式
      const escapedKeyword = kwStr.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
      str = str.replace(new RegExp(`(${escapedKeyword})`, 'gi'), '<mark>$1</mark>');
    }
  });
  return <span dangerouslySetInnerHTML={{ __html: str }} title={text} />;
};
