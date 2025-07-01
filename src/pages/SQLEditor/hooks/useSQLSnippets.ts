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
        label: 'SELECT TOP',
        insertText: 'SELECT TOP ${1:10} * FROM ${2:table_name} ORDER BY ${3:column} DESC;',
        description: '查询前N条记录',
      },
      {
        label: 'GROUP BY聚合',
        insertText:
          'SELECT ${1:column}, COUNT(*) as count\nFROM ${2:table_name}\nGROUP BY ${1:column}\nORDER BY count DESC;',
        description: '分组聚合查询',
      },
      {
        label: 'INNER JOIN',
        insertText: 'SELECT a.*, b.*\nFROM ${1:table_a} a\nINNER JOIN ${2:table_b} b ON a.${3:key} = b.${4:key};',
        description: '内连接查询',
      },
      {
        label: 'LEFT JOIN',
        insertText: 'SELECT a.*, b.*\nFROM ${1:table_a} a\nLEFT JOIN ${2:table_b} b ON a.${3:key} = b.${4:key};',
        description: '左连接查询',
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
        label: 'EXISTS子查询',
        insertText:
          'SELECT *\nFROM ${1:table_a} a\nWHERE EXISTS (\n  SELECT 1 FROM ${2:table_b} b\n  WHERE b.${3:key} = a.${4:key}\n);',
        description: 'EXISTS子查询',
      },
      {
        label: 'IN子查询',
        insertText:
          'SELECT *\nFROM ${1:table_name}\nWHERE ${2:column} IN (\n  SELECT ${3:column} FROM ${4:other_table}\n  WHERE ${5:condition}\n);',
        description: 'IN子查询',
      },
      {
        label: 'CASE WHEN',
        insertText:
          "SELECT *,\n  CASE \n    WHEN ${1:condition1} THEN '${2:value1}'\n    WHEN ${3:condition2} THEN '${4:value2}'\n    ELSE '${5:default_value}'\n  END as ${6:new_column}\nFROM ${7:table_name};",
        description: '条件分支查询',
      },
      {
        label: '窗口函数',
        insertText:
          'SELECT *,\n  ROW_NUMBER() OVER (PARTITION BY ${1:column} ORDER BY ${2:order_column}) as row_num\nFROM ${3:table_name};',
        description: '窗口函数查询',
      },
      {
        label: '插入数据',
        insertText: "INSERT INTO ${1:table_name} (${2:column1}, ${3:column2})\nVALUES ('${4:value1}', '${5:value2}');",
        description: '插入数据模板',
      },
      {
        label: '批量插入',
        insertText:
          "INSERT INTO ${1:table_name} (${2:column1}, ${3:column2})\nVALUES \n  ('${4:value1}', '${5:value2}'),\n  ('${6:value3}', '${7:value4}');",
        description: '批量插入数据',
      },
      {
        label: '更新数据',
        insertText: "UPDATE ${1:table_name}\nSET ${2:column} = '${3:new_value}'\nWHERE ${4:condition};",
        description: '更新数据模板',
      },
      {
        label: '删除数据',
        insertText: 'DELETE FROM ${1:table_name}\nWHERE ${2:condition};',
        description: '删除数据模板',
      },
    ],
    [],
  );

  // SQL函数补全
  const sqlFunctions: SQLFunction[] = useMemo(
    () => [
      // 聚合函数
      { label: 'COUNT', insertText: 'COUNT($1)', detail: '计数函数' },
      { label: 'COUNT_DISTINCT', insertText: 'COUNT(DISTINCT $1)', detail: '不重复计数' },
      { label: 'SUM', insertText: 'SUM($1)', detail: '求和函数' },
      { label: 'AVG', insertText: 'AVG($1)', detail: '平均值函数' },
      { label: 'MAX', insertText: 'MAX($1)', detail: '最大值函数' },
      { label: 'MIN', insertText: 'MIN($1)', detail: '最小值函数' },

      // 字符串函数
      { label: 'CONCAT', insertText: 'CONCAT($1, $2)', detail: '字符串连接' },
      { label: 'SUBSTRING', insertText: 'SUBSTRING($1, $2, $3)', detail: '字符串截取' },
      { label: 'LEFT', insertText: 'LEFT($1, $2)', detail: '左截取字符串' },
      { label: 'RIGHT', insertText: 'RIGHT($1, $2)', detail: '右截取字符串' },
      { label: 'UPPER', insertText: 'UPPER($1)', detail: '转换为大写' },
      { label: 'LOWER', insertText: 'LOWER($1)', detail: '转换为小写' },
      { label: 'TRIM', insertText: 'TRIM($1)', detail: '去除空格' },
      { label: 'LTRIM', insertText: 'LTRIM($1)', detail: '去除左空格' },
      { label: 'RTRIM', insertText: 'RTRIM($1)', detail: '去除右空格' },
      { label: 'LENGTH', insertText: 'LENGTH($1)', detail: '字符串长度' },
      { label: 'REPLACE', insertText: 'REPLACE($1, $2, $3)', detail: '字符串替换' },

      // 日期函数
      { label: 'NOW', insertText: 'NOW()', detail: '当前时间' },
      { label: 'CURDATE', insertText: 'CURDATE()', detail: '当前日期' },
      { label: 'DATE_FORMAT', insertText: "DATE_FORMAT($1, '$2')", detail: '日期格式化' },
      { label: 'DATE_ADD', insertText: 'DATE_ADD($1, INTERVAL $2 $3)', detail: '日期加法' },
      { label: 'DATE_SUB', insertText: 'DATE_SUB($1, INTERVAL $2 $3)', detail: '日期减法' },
      { label: 'DATEDIFF', insertText: 'DATEDIFF($1, $2)', detail: '日期差值' },
      { label: 'YEAR', insertText: 'YEAR($1)', detail: '提取年份' },
      { label: 'MONTH', insertText: 'MONTH($1)', detail: '提取月份' },
      { label: 'DAY', insertText: 'DAY($1)', detail: '提取日期' },

      // 数学函数
      { label: 'ROUND', insertText: 'ROUND($1, $2)', detail: '四舍五入' },
      { label: 'CEIL', insertText: 'CEIL($1)', detail: '向上取整' },
      { label: 'FLOOR', insertText: 'FLOOR($1)', detail: '向下取整' },
      { label: 'ABS', insertText: 'ABS($1)', detail: '绝对值' },
      { label: 'MOD', insertText: 'MOD($1, $2)', detail: '取模运算' },

      // 条件函数
      { label: 'COALESCE', insertText: 'COALESCE($1, $2)', detail: '空值处理' },
      { label: 'NULLIF', insertText: 'NULLIF($1, $2)', detail: '空值判断' },
      { label: 'IFNULL', insertText: 'IFNULL($1, $2)', detail: '空值替换' },
      { label: 'CASE WHEN', insertText: 'CASE WHEN $1 THEN $2 ELSE $3 END', detail: '条件表达式' },

      // 窗口函数
      { label: 'ROW_NUMBER', insertText: 'ROW_NUMBER() OVER (ORDER BY $1)', detail: '行号' },
      { label: 'RANK', insertText: 'RANK() OVER (ORDER BY $1)', detail: '排名' },
      { label: 'DENSE_RANK', insertText: 'DENSE_RANK() OVER (ORDER BY $1)', detail: '密集排名' },
      { label: 'LAG', insertText: 'LAG($1, $2) OVER (ORDER BY $3)', detail: '上一行值' },
      { label: 'LEAD', insertText: 'LEAD($1, $2) OVER (ORDER BY $3)', detail: '下一行值' },

      // 类型转换
      { label: 'CAST', insertText: 'CAST($1 AS $2)', detail: '类型转换' },
      { label: 'CONVERT', insertText: 'CONVERT($1, $2)', detail: '数据转换' },
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
