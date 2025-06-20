import { useState, useCallback } from 'react';
import { message } from 'antd';
import { executeSQL } from '@/api/sql';
import type { QueryResult } from '@/pages/SQLEditor/types';
import { EditorView } from '@codemirror/view';
import { getSQLContext } from '../utils/codeMirrorUtils';
import { isCompleteSQLStatement } from '../utils/editorUtils';

export interface ExecuteQueryOptions {
  datasourceId: string;
  sql: string;
  selectedText?: string;
  editor?: EditorView | null;
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

      let queryToExecute = options?.sql || sqlQuery;
      if (options?.selectedText && options.editor) {
        // 使用 CodeMirror 工具获取 SQL 上下文
        const selectedSQL = getSQLContext(options.editor);
        if (selectedSQL.trim()) {
          queryToExecute = selectedSQL;
        }
      } else if (options?.selectedText) {
        // 如果没有编辑器实例，使用原始逻辑
        queryToExecute = options.selectedText;
      }

      if (!queryToExecute.trim()) {
        console.error('SQL查询验证失败 - 详细信息:', {
          sqlQuery,
          options,
          selectedSource,
          timestamp: new Date().toISOString(),
        });
        message.warning('请输入有效的SQL查询语句');
        throw new Error('SQL查询语句为空');
      }

      // 确保是完整SQL语句
      if (!isCompleteSQLStatement(queryToExecute)) {
        message.warning('选中的SQL语句不完整');
        throw new Error('选中的SQL语句不完整');
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
