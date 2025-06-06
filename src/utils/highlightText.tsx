/*
  高亮文本
  @param {string} text - 文本
  @param {string[]} keywords - 关键词
  @returns {React.ReactNode} - 高亮文本
*/
export const highlightText = (text: string, keywords: string[]) => {
  // kube-node18 || data/log/hina-cl
  if (!keywords || !keywords.length) return <span title={text}>{text}</span>;

  let str = String(text);
  keywords.forEach((keyword) => {
    const kwStr = String(keyword);
    // 中文单字匹配
    if (/[\u4e00-\u9fa5]/.test(kwStr)) {
      kwStr.split('').forEach((char) => {
        str = str.replace(new RegExp(char, 'g'), `<mark>${char}</mark>`);
      });
    } else {
      str = str.replace(new RegExp(kwStr.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'g'), `<mark>${kwStr}</mark>`);
    }
  });
  return <span dangerouslySetInnerHTML={{ __html: str }} title={text} />;
};
