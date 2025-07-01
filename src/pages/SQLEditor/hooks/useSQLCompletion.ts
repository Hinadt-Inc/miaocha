import { useCallback, useEffect, useRef } from 'react';
import * as monaco from 'monaco-editor';
import { useSQLSnippets } from './useSQLSnippets';
import { getSQLContext } from '../utils/editorUtils';
import { SchemaResult } from '../types';

/**
 * SQL自动补全管理Hook
 * 管理Monaco编辑器的自动补全功能
 */
export const useSQLCompletion = (
  databaseSchema: SchemaResult | null,
  editorRef: React.RefObject<monaco.editor.IStandaloneCodeEditor | null>,
  monacoRef: React.RefObject<typeof monaco | null>,
) => {
  const { sqlFunctions, getSqlKeywords } = useSQLSnippets();
  const completionDisposableRef = useRef<monaco.IDisposable | null>(null);

  // 创建补全建议
  const createCompletionSuggestions = useCallback(
    (
      model: monaco.editor.ITextModel,
      position: monaco.Position,
      schema: SchemaResult,
      monacoInstance: typeof monaco,
    ) => {
      const word = model.getWordUntilPosition(position);
      const range = {
        startLineNumber: position.lineNumber,
        endLineNumber: position.lineNumber,
        startColumn: word.startColumn,
        endColumn: word.endColumn,
      };

      const suggestions: monaco.languages.CompletionItem[] = [];

      // 表和字段补全
      if (schema && schema.tables) {
        schema.tables.forEach((table) => {
          suggestions.push({
            label: table.tableName,
            kind: monacoInstance.languages.CompletionItemKind.Class,
            insertText: table.tableName,
            detail: `表: ${table.tableComment ?? table.tableName}`,
            range,
          });

          if (table.columns && Array.isArray(table.columns)) {
            table.columns.forEach((column) => {
              suggestions.push({
                label: column.columnName,
                kind: monacoInstance.languages.CompletionItemKind.Field,
                insertText: column.columnName,
                detail: `字段: ${column.columnComment ?? column.columnName} (${column.dataType})`,
                range,
              });
            });
          }
        });
      }

      // SQL函数补全
      sqlFunctions.forEach((func) => {
        suggestions.push({
          label: func.label,
          kind: monacoInstance.languages.CompletionItemKind.Function,
          insertText: func.insertText,
          insertTextRules: monacoInstance.languages.CompletionItemInsertTextRule.InsertAsSnippet,
          detail: func.detail,
          range,
        });
      });

      // SQL关键字补全（根据上下文）
      if (editorRef.current) {
        const context = getSQLContext(editorRef.current);
        getSqlKeywords(context).forEach((keyword) => {
          suggestions.push({
            label: keyword,
            kind: monacoInstance.languages.CompletionItemKind.Keyword,
            insertText: keyword,
            detail: 'SQL关键字',
            range,
          });
        });
      }

      return suggestions;
    },
    [sqlFunctions, getSqlKeywords, editorRef],
  );

  // 注册自动补全提供器
  useEffect(() => {
    if (monacoRef.current && databaseSchema) {
      // 清理之前的注册
      if (completionDisposableRef.current) {
        completionDisposableRef.current.dispose();
      }

      // 注册新的补全提供器
      completionDisposableRef.current = monacoRef.current.languages.registerCompletionItemProvider('sql', {
        provideCompletionItems: (model, position) => {
          if (!monacoRef.current || !databaseSchema) return { suggestions: [] };
          const suggestions = createCompletionSuggestions(model, position, databaseSchema, monacoRef.current);
          return { suggestions };
        },
      });
    }

    // 清理函数
    return () => {
      if (completionDisposableRef.current) {
        completionDisposableRef.current.dispose();
        completionDisposableRef.current = null;
      }
    };
  }, [databaseSchema, createCompletionSuggestions]);

  return {
    createCompletionSuggestions,
  };
};
