import { useCallback, useMemo } from 'react';
import * as monaco from 'monaco-editor';
import { useSQLSnippets } from './useSQLSnippets';

export interface SQLCompletionContext {
  text: string;
  position: monaco.Position;
  model: monaco.editor.ITextModel;
}

/**
 * SQL智能补全Hook
 * 提供基于上下文的SQL补全功能
 */
export const useSQLCompletion = (databaseSchema?: any) => {
  const { sqlFunctions, getSqlKeywords } = useSQLSnippets();

  // SQL关键字定义
  const sqlKeywords = useMemo(
    () => [
      // 基础查询
      'SELECT',
      'FROM',
      'WHERE',
      'ORDER BY',
      'GROUP BY',
      'HAVING',
      'LIMIT',
      'OFFSET',
      // 连接
      'JOIN',
      'INNER JOIN',
      'LEFT JOIN',
      'RIGHT JOIN',
      'FULL JOIN',
      'CROSS JOIN',
      'ON',
      // 数据操作
      'INSERT',
      'INTO',
      'VALUES',
      'UPDATE',
      'SET',
      'DELETE',
      // 数据定义
      'CREATE',
      'TABLE',
      'DROP',
      'ALTER',
      'INDEX',
      'VIEW',
      // 条件和逻辑
      'AND',
      'OR',
      'NOT',
      'IN',
      'EXISTS',
      'BETWEEN',
      'LIKE',
      'IS',
      'NULL',
      // 聚合和窗口
      'DISTINCT',
      'ALL',
      'AS',
      'CASE',
      'WHEN',
      'THEN',
      'ELSE',
      'END',
      // 数据类型
      'VARCHAR',
      'INT',
      'INTEGER',
      'DECIMAL',
      'DATE',
      'DATETIME',
      'TIMESTAMP',
      'TEXT',
      'BOOLEAN',
      // 约束
      'PRIMARY KEY',
      'FOREIGN KEY',
      'UNIQUE',
      'NOT NULL',
      'DEFAULT',
      'CHECK',
    ],
    [],
  );

  // 分析当前的SQL上下文
  const analyzeContext = useCallback((context: SQLCompletionContext) => {
    const { text, position } = context;
    const lines = text.split('\n');
    const currentLine = lines[position.lineNumber - 1];
    const textBeforePosition = currentLine.substring(0, position.column - 1);
    const textBefore = text.substring(0, text.indexOf(currentLine) + textBeforePosition.length);

    // 简单的SQL语法分析
    const upperText = textBefore.toUpperCase();
    const lastWord = textBeforePosition.trim().split(/\s+/).pop()?.toUpperCase() || '';

    return {
      isInSelectClause: upperText.includes('SELECT') && !upperText.includes('FROM'),
      isInFromClause: upperText.includes('FROM') && !upperText.includes('WHERE'),
      isInWhereClause: upperText.includes('WHERE'),
      isAfterJoin: ['JOIN', 'INNER', 'LEFT', 'RIGHT', 'FULL'].includes(lastWord),
      isAfterOn: lastWord === 'ON',
      isAfterSelect: lastWord === 'SELECT',
      isAfterFrom: lastWord === 'FROM',
      isAfterWhere: lastWord === 'WHERE',
      lastWord,
      textBefore: upperText,
    };
  }, []);

  // 获取表名补全
  const getTableSuggestions = useCallback(
    (range: monaco.IRange) => {
      if (!databaseSchema?.tables) return [];

      return databaseSchema.tables.map((table: any) => ({
        label: table.name,
        kind: monaco.languages.CompletionItemKind.Class,
        insertText: table.name,
        detail: `表: ${table.comment || ''}`,
        documentation: `表 ${table.name}，包含 ${table.columns?.length || 0} 个列`,
        range,
      }));
    },
    [databaseSchema],
  );

  // 获取列名补全
  const getColumnSuggestions = useCallback(
    (range: monaco.IRange, tableName?: string) => {
      if (!databaseSchema?.tables) return [];

      let columns: any[] = [];

      if (tableName) {
        // 特定表的列
        const table = databaseSchema.tables.find((t: any) => t.name.toLowerCase() === tableName.toLowerCase());
        columns = table?.columns || [];
      } else {
        // 所有表的列
        columns = databaseSchema.tables.flatMap((table: any) =>
          (table.columns || []).map((col: any) => ({ ...col, tableName: table.name })),
        );
      }

      return columns.map((column: any) => ({
        label: column.tableName ? `${column.tableName}.${column.name}` : column.name,
        kind: monaco.languages.CompletionItemKind.Field,
        insertText: column.name,
        detail: `${column.type} - ${column.comment || ''}`,
        documentation: `列: ${column.name} (${column.type})${column.comment ? ` - ${column.comment}` : ''}`,
        range,
      }));
    },
    [databaseSchema],
  );

  // 获取关键字补全
  const getKeywordSuggestions = useCallback(
    (range: monaco.IRange, context: any) => {
      const contextKeywords = getSqlKeywords(context);
      const allKeywords = [...new Set([...sqlKeywords, ...contextKeywords])];

      return allKeywords.map((keyword) => ({
        label: keyword,
        kind: monaco.languages.CompletionItemKind.Keyword,
        insertText: keyword + ' ',
        detail: 'SQL关键字',
        range,
      }));
    },
    [sqlKeywords, getSqlKeywords],
  );

  // 获取函数补全
  const getFunctionSuggestions = useCallback(
    (range: monaco.IRange) => {
      return sqlFunctions.map((func) => ({
        label: func.label,
        kind: monaco.languages.CompletionItemKind.Function,
        insertText: func.insertText,
        detail: func.detail,
        documentation: `SQL函数: ${func.detail}`,
        insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
        range,
      }));
    },
    [sqlFunctions],
  );

  // 主要的补全提供器
  const provideCompletionItems = useCallback(
    (
      model: monaco.editor.ITextModel,
      position: monaco.Position,
    ): monaco.languages.ProviderResult<monaco.languages.CompletionList> => {
      const word = model.getWordUntilPosition(position);
      const range: monaco.IRange = {
        startLineNumber: position.lineNumber,
        endLineNumber: position.lineNumber,
        startColumn: word.startColumn,
        endColumn: word.endColumn,
      };

      const text = model.getValue();
      const context = analyzeContext({ text, position, model });

      let suggestions: monaco.languages.CompletionItem[] = [];

      // 根据上下文提供不同的补全
      if (context.isAfterFrom || context.isAfterJoin) {
        // FROM 或 JOIN 后面，提供表名
        suggestions.push(...getTableSuggestions(range));
      } else if (context.isAfterSelect || context.isInSelectClause) {
        // SELECT 后面，提供列名和函数
        suggestions.push(...getColumnSuggestions(range));
        suggestions.push(...getFunctionSuggestions(range));
      } else if (context.isInWhereClause || context.isAfterOn) {
        // WHERE 或 ON 条件中，提供列名
        suggestions.push(...getColumnSuggestions(range));
      }

      // 总是提供关键字和函数补全
      suggestions.push(...getKeywordSuggestions(range, context));

      // 如果没有特定的上下文补全，提供函数补全
      if (suggestions.length === sqlKeywords.length) {
        suggestions.push(...getFunctionSuggestions(range));
      }

      return {
        suggestions: suggestions.slice(0, 50), // 限制建议数量
      };
    },
    [
      analyzeContext,
      getTableSuggestions,
      getColumnSuggestions,
      getKeywordSuggestions,
      getFunctionSuggestions,
      sqlKeywords,
    ],
  );

  // 注册补全提供器
  const registerCompletionProvider = useCallback(() => {
    return monaco.languages.registerCompletionItemProvider('sql', {
      provideCompletionItems,
      triggerCharacters: ['.', ' ', '(', ','],
    });
  }, [provideCompletionItems]);

  return {
    provideCompletionItems,
    registerCompletionProvider,
    analyzeContext,
  };
};
