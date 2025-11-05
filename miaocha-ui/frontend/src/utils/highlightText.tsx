/*
  高亮文本（安全版）
  @description 以非侵入方式对普通文本进行关键词高亮；当文本为 XML/HTML 字符串时，保持原样渲染，确保与后端返回完全一致。
  @param {string} text - 原始文本
  @param {string[]} keywords - 关键词数组（可为空、去重后处理）
  @returns {React.ReactNode} - 可渲染的 React 节点
*/
export const highlightText = (text: string, keywords: string[]) => {
  // 统一转为字符串，避免 null/undefined 导致报错
  const raw = String(text ?? '');

  // 判定是否为 XML/HTML：包含典型标记即可认为是标记文本
  // 规则：含有 \n 与成对尖括号、或以 '<?xml'、'<![CDATA['、'<xml' 开头
  const looksLikeXml = /<\?xml|<!\[CDATA\[|<xml|<[^>]+>/i.test(raw);

  // XML/HTML 文本：直接按原样输出，使用 pre-wrap 保留换行与空格
  if (looksLikeXml) {
    return (
      <span style={{ whiteSpace: 'pre-wrap' }} title={raw}>
        {raw}
      </span>
    );
  }

  // 普通文本高亮处理
  const validKeywords = Array.from(new Set((keywords || []).filter(Boolean)));
  if (validKeywords.length === 0) {
    return (
      <span style={{ whiteSpace: 'pre-wrap' }} title={raw}>
        {raw}
      </span>
    );
  }

  // 转义正则特殊字符
  const escapeReg = (kw: string) => kw.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  // 关键词按长度降序，避免短词抢先匹配
  const sortedKeywords = [...validKeywords].sort((a, b) => b.length - a.length);
  const pattern = sortedKeywords.map(escapeReg).join('|');
  const regex = new RegExp(pattern, 'gi');

  const nodes: React.ReactNode[] = [];
  let lastIndex = 0;
  let match: RegExpExecArray | null;

  while ((match = regex.exec(raw)) !== null) {
    const start = match.index;
    const end = start + match[0].length;

    // 先推入未匹配部分
    if (lastIndex < start) {
      nodes.push(raw.slice(lastIndex, start));
    }

    // 推入高亮部分（保留原始大小写）
    nodes.push(
      <mark key={`hl-${start}-${end}`}>{match[0]}</mark>
    );

    lastIndex = end;
  }

  // 剩余尾部文本
  if (lastIndex < raw.length) {
    nodes.push(raw.slice(lastIndex));
  }

  return (
    <span style={{ whiteSpace: 'pre-wrap' }} title={raw}>
      {nodes}
    </span>
  );
};
