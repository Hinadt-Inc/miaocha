/*
  高亮文本
  @param {string} text - 文本
  @param {string[]} keywords - 关键词
  @returns {React.ReactNode} - 高亮文本
*/
export const highlightText = (text: string, keywords: string[]) => {
  // 过滤掉空值并去重
  const validKeywords = Array.from(new Set(keywords.filter(Boolean)));

  // 按长度降序排列，先匹配长的关键词，避免短关键词干扰长关键词的匹配
  validKeywords.sort((a, b) => b.length - a.length);

  if (!validKeywords.length) return <span title={text}>{text}</span>;

  let str = String(text);
  const placeholders: string[] = [];
  let placeholderIndex = 0;

  validKeywords.forEach((keyword) => {
    // 转义特殊正则字符
    const escapedKeyword = keyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');

    // 不区分大小写匹配，但保持原始字符串格式
    const regex = new RegExp(`(${escapedKeyword})`, 'gi');

    str = str.replace(regex, (match) => {
      const placeholder = `__PLACEHOLDER_${placeholderIndex}__`;
      placeholders[placeholderIndex] = `<mark>${match}</mark>`;
      placeholderIndex++;
      return placeholder;
    });
  });

  // 恢复占位符为实际的高亮标签
  placeholders.forEach((replacement, index) => {
    str = str.replace(`__PLACEHOLDER_${index}__`, replacement);
  });

  return <span dangerouslySetInnerHTML={{ __html: ['null', 'undefined'].includes(str) ? '' : str }} title={text} />;
};
