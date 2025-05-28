import { useState, useCallback } from 'react';
import { message } from 'antd';
import { executeSQL } from '@/api/sql';
import type { QueryResult } from '@/pages/SQLEditor/types';
import * as monaco from 'monaco-editor';
import { getSelectedSQLStatement, isCompleteSQLStatement } from '../utils/editorUtils';

export interface ExecuteQueryOptions {
  datasourceId: string;
  sql: string;
  selectedText?: string;
  editor?: monaco.editor.IStandaloneCodeEditor | null;
}

export const useQueryExecution = (
  selectedSource: string,
): {
  executeQuery: (options: ExecuteQueryOptions) => Promise<QueryResult>;
  executeSelectedQuery: (selectedText: string) => Promise<QueryResult>;
  queryResults: QueryResult | null;
  loading: boolean;
  sqlQuery: string;
  setSqlQuery: React.Dispatch<React.SetStateAction<string>>;
  setQueryResults: React.Dispatch<React.SetStateAction<QueryResult | null>>;
} => {
  const [queryResults, setQueryResults] = useState<QueryResult | null>(null);
  const [loading, setLoading] = useState(false);
  const [sqlQuery, setSqlQuery] = useState('');

  const executeQuery = useCallback(
    async (options?: ExecuteQueryOptions): Promise<QueryResult> => {
      if (!selectedSource) {
        message.warning('请先选择数据源');
        throw new Error('未选择数据源');
      }

      let queryToExecute = sqlQuery;
      if (options?.selectedText && options.editor) {
        // 使用编辑器工具提取选中的完整SQL语句
        queryToExecute = getSelectedSQLStatement(options.editor);
      } else if (options?.selectedText) {
        // 如果没有编辑器实例，使用原始逻辑
        queryToExecute = options.selectedText;
      }

      if (!queryToExecute.trim()) {
        message.warning('请输入SQL查询语句');
        throw new Error('SQL查询语句为空');
      }

      // 确保是完整SQL语句
      if (!isCompleteSQLStatement(queryToExecute)) {
        message.warning('选中的SQL语句不完整');
        throw new Error('选中的SQL语句不完整');
      }

      function parseSelectedSQL(selectedText: string): string {
        // 1. 移除注释
        const withoutComments = selectedText.replace(/--.*$|#.*$|\/\*[\s\S]*?\*\//gm, '');

        // 2. 检查是否是完整语句
        if (
          /^(SELECT|INSERT|UPDATE|DELETE|CREATE|ALTER|DROP|TRUNCATE|EXEC)\b/i.test(withoutComments) &&
          /;[\s]*$/.test(withoutComments)
        ) {
          return withoutComments.trim();
        }

        // 3. 提取第一个完整语句
        const statements = withoutComments
          .split(';')
          .map((s) => s.trim())
          .filter((s) => s.length > 0);

        return statements[0] || withoutComments.trim();
      }
      setLoading(true);
      try {
        const response = await executeSQL({
          datasourceId: selectedSource,
          sql: queryToExecute,
        });

        // 转换rows类型以匹配QueryResult
        const convertedResponse: QueryResult = {
          ...response,
          rows: response.rows?.map((row) => {
            const convertedRow: Record<string, string | number | boolean | null | undefined | object> = {};
            for (const key in row) {
              const value = row[key];
              if (
                typeof value === 'string' ||
                typeof value === 'number' ||
                typeof value === 'boolean' ||
                value === null ||
                value === undefined ||
                (typeof value === 'object' && !Array.isArray(value))
              ) {
                convertedRow[key] = value;
              } else {
                // 对于不兼容的类型，转换为字符串
                convertedRow[key] = JSON.stringify(value);
              }
            }
            return convertedRow;
          }),
        };

        setQueryResults(convertedResponse);
        return convertedResponse;
      } catch (error) {
        console.error('执行查询失败:', error);
        message.error('执行查询失败');
        throw error;
      } finally {
        setLoading(false);
      }
    },
    [selectedSource, sqlQuery],
  );

  const executeQueryInternal = async (query: string): Promise<QueryResult> => {
    setLoading(true);
    try {
      const response = await executeSQL({
        datasourceId: selectedSource,
        sql: query,
      });

      const convertedResponse: QueryResult = {
        ...response,
        rows: response.rows?.map((row) => {
          const convertedRow: Record<string, string | number | boolean | null | undefined | object> = {};
          for (const key in row) {
            const value = row[key];
            if (
              typeof value === 'string' ||
              typeof value === 'number' ||
              typeof value === 'boolean' ||
              value === null ||
              value === undefined ||
              (typeof value === 'object' && !Array.isArray(value))
            ) {
              convertedRow[key] = value;
            } else {
              convertedRow[key] = JSON.stringify(value);
            }
          }
          return convertedRow;
        }),
      };

      setQueryResults(convertedResponse);
      return convertedResponse;
    } catch (error) {
      console.error('执行查询失败:', error);
      message.error('执行查询失败');
      throw error;
    } finally {
      setLoading(false);
    }
  };

  return {
    queryResults,
    setQueryResults,
    loading,
    sqlQuery,
    setSqlQuery,
    executeQuery,
    executeSelectedQuery: (selectedText: string) =>
      executeQuery({
        datasourceId: selectedSource,
        sql: sqlQuery,
        selectedText,
      }),
  };
};
