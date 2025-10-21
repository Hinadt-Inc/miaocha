/**
 * 数据处理相关工具函数
 */

/**
 * 处理SQL语句，根据SQL语法提取字段名和值
 * @param sqls SQL语句数组
 * @returns 提取的关键词数组
 */
export const extractSqlKeywords = (sqls: string[]): string[] => {
  if (!sqls || sqls.length === 0) return [];

  const extractKeywords = (sql: string): string[] => {
    const keywords: string[] = [];

    // 匹配字段 = 值的模式，支持单引号、双引号或无引号
    const patterns = [
      // 字段名 = '值' 或 字段名='值'
      /([a-zA-Z_][a-zA-Z0-9_.]*)\s*=\s*'([^']*)'/g,
      // 字段名 = "值" 或 字段名="值"
      /([a-zA-Z_][a-zA-Z0-9_.]*)\s*=\s*"([^"]*)"/g,
      // 字段名 = 值 (无引号，匹配到空格、AND、OR或结尾)
      /([a-zA-Z_][a-zA-Z0-9_.]*)\s*=\s*([^\s'"()]+)(?=\s|$|and|or|\))/gi,
    ];

    patterns.forEach((pattern) => {
      let match;
      while ((match = pattern.exec(sql)) !== null) {
        const fieldName = match[1].trim();
        const value = match[2].trim();

        if (fieldName) keywords.push(fieldName);
        if (value) keywords.push(value);
      }
    });

    return keywords;
  };

  return Array.from(
    new Set(
      sqls
        .map((sql) => extractKeywords(sql))
        .flat()
        .filter((keyword) => keyword.length > 0),
    ),
  );
};

/**
 * 处理搜索关键词格式化
 * @param keywords 原始关键词数组
 * @returns 格式化后的关键词数组
 */
export const formatSearchKeywords = (keywords: string[]): string[] => {
  if (!keywords || keywords.length === 0) return [];

  /**
   * 关键词转换规则函数
   * @param input 输入的字符串
   * @returns 转换后的关键词数组
   */
  function convertKeywords(input: string) {
    // 移除首尾空格
    const trimmed = input.trim();

    // 如果输入为空，返回空数组
    if (!trimmed) {
      return [];
    }

    // 定义所有支持的分隔符，按优先级排序
    const separators = ['&&', '||', ' and ', ' AND ', ' or ', ' OR '];

    // 查找第一个匹配的分隔符
    for (const separator of separators) {
      if (trimmed.includes(separator)) {
        return trimmed
          .split(separator)
          .map((item) => item.trim().replace(/^['"]|['"]$/g, '')) // 移除首尾的单引号或双引号
          .filter((item) => item.length > 0);
      }
    }

    // 单个关键词的情况，移除引号
    return [trimmed.replace(/^['"]|['"]$/g, '')];
  }

  // 处理所有关键词并去重
  const allKeywords = keywords
    .map((keyword: string) => convertKeywords(keyword))
    .flat()
    .filter((keyword: string) => keyword.length > 0);

  return Array.from(new Set(allKeywords));
};

/**
 * 生成记录内容的hash，用于匹配记录
 * @param record 记录对象
 * @param timeField 时间字段名
 * @returns 记录的唯一hash值
 */
export const generateRecordHash = (record: any, timeField: string): string => {
  // 使用时间字段和部分关键字段生成唯一标识，确保唯一性
  const identifyingFields = [timeField, 'host', 'source', 'log_offset'];
  const hashParts = identifyingFields
    .filter((field) => record[field] !== undefined && record[field] !== null)
    .map((field) => `${field}:${String(record[field])}`);

  // 如果基本字段不够唯一，添加更多字段
  if (hashParts.length < 2) {
    const additionalFields = Object.keys(record).slice(0, 5);
    additionalFields.forEach((field) => {
      if (!identifyingFields.includes(field) && record[field] !== undefined) {
        hashParts.push(`${field}:${String(record[field]).substring(0, 100)}`);
      }
    });
  }

  return hashParts.join('|');
};
