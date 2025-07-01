import { useMemo } from 'react';

export interface SQLSnippet {
  label: string;
  insertText: string;
  description: string;
}

export interface SQLFunction {
  label: string;
  insertText: string;
  detail: string;
}

/**
 * SQL片段和函数管理Hook
 * 管理SQL模板、函数等可插入的代码片段
 */
export const useSQLSnippets = () => {
  // SQL常用片段
  const sqlSnippets: SQLSnippet[] = useMemo(
    () => [
      {
        label: '基础SELECT',
        insertText: 'SELECT * FROM ${1:table_name} WHERE ${2:condition};',
        description: '基础查询模板',
      },
      {
        label: 'GROUP BY聚合',
        insertText:
          'SELECT ${1:column}, COUNT(*) as count\nFROM ${2:table_name}\nGROUP BY ${1:column}\nORDER BY count DESC;',
        description: '分组聚合查询',
      },
      {
        label: 'JOIN查询',
        insertText: 'SELECT a.*, b.*\nFROM ${1:table_a} a\nJOIN ${2:table_b} b ON a.${3:key} = b.${4:key};',
        description: '表连接查询',
      },
      {
        label: '条件查询',
        insertText:
          "SELECT *\nFROM ${1:table_name}\nWHERE ${2:column} = '${3:value}'\n  AND ${4:column2} > ${5:value2};",
        description: '多条件查询',
      },
      {
        label: '分页查询',
        insertText: 'SELECT *\nFROM ${1:table_name}\nORDER BY ${2:column}\nLIMIT ${3:10} OFFSET ${4:0};',
        description: '分页查询模板',
      },
      {
        label: '时间范围查询',
        insertText:
          "SELECT *\nFROM ${1:table_name}\nWHERE ${2:date_column} >= '${3:2024-01-01}'\n  AND ${2:date_column} < '${4:2024-02-01}';",
        description: '时间范围过滤',
      },
      {
        label: '插入数据',
        insertText: "INSERT INTO ${1:table_name} (${2:column1}, ${3:column2})\nVALUES ('${4:value1}', '${5:value2}');",
        description: '插入数据模板',
      },
      {
        label: '更新数据',
        insertText: "UPDATE ${1:table_name}\nSET ${2:column} = '${3:new_value}'\nWHERE ${4:condition};",
        description: '更新数据模板',
      },
    ],
    [],
  );

  // SQL函数补全
  const sqlFunctions: SQLFunction[] = useMemo(
    () => [
      { label: 'COUNT', insertText: 'COUNT($1)', detail: '计数函数' },
      { label: 'SUM', insertText: 'SUM($1)', detail: '求和函数' },
      { label: 'AVG', insertText: 'AVG($1)', detail: '平均值函数' },
      { label: 'MAX', insertText: 'MAX($1)', detail: '最大值函数' },
      { label: 'MIN', insertText: 'MIN($1)', detail: '最小值函数' },
      { label: 'CONCAT', insertText: 'CONCAT($1, $2)', detail: '字符串连接' },
      { label: 'SUBSTRING', insertText: 'SUBSTRING($1, $2, $3)', detail: '字符串截取' },
      { label: 'UPPER', insertText: 'UPPER($1)', detail: '转换为大写' },
      { label: 'LOWER', insertText: 'LOWER($1)', detail: '转换为小写' },
      { label: 'TRIM', insertText: 'TRIM($1)', detail: '去除空格' },
      { label: 'DATE_FORMAT', insertText: "DATE_FORMAT($1, '$2')", detail: '日期格式化' },
      { label: 'NOW', insertText: 'NOW()', detail: '当前时间' },
      { label: 'COALESCE', insertText: 'COALESCE($1, $2)', detail: '空值处理' },
      { label: 'CASE WHEN', insertText: 'CASE WHEN $1 THEN $2 ELSE $3 END', detail: '条件表达式' },
    ],
    [],
  );

  // SQL关键字（根据上下文）
  const getSqlKeywords = (context: any) => {
    const baseKeywords = ['SELECT', 'FROM', 'WHERE', 'GROUP BY', 'ORDER BY', 'LIMIT'];

    if (context?.isInSelectClause) {
      return ['DISTINCT', 'AS', ...baseKeywords];
    }
    if (context?.isInFromClause) {
      return ['JOIN', 'LEFT JOIN', 'RIGHT JOIN', 'INNER JOIN', 'CROSS JOIN', ...baseKeywords];
    }
    if (context?.isInWhereClause) {
      return ['AND', 'OR', 'IN', 'EXISTS', 'LIKE', 'BETWEEN', 'IS NULL', 'IS NOT NULL', ...baseKeywords];
    }

    return baseKeywords;
  };

  return {
    sqlSnippets,
    sqlFunctions,
    getSqlKeywords,
  };
};
