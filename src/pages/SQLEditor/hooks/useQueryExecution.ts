import { useState, useCallback } from 'react';
import { message } from 'antd';
import { executeSQL } from '@/api/sql';
import type { QueryResult } from '@/pages/SQLEditor/types';

export const useQueryExecution = (selectedSource: string) => {
  const [queryResults, setQueryResults] = useState<QueryResult | null>(null);
  const [loading, setLoading] = useState(false);
  const [sqlQuery, setSqlQuery] = useState('');

  const executeQuery = useCallback(async (): Promise<QueryResult> => {
    if (!selectedSource) {
      message.warning('请先选择数据源');
      throw new Error('未选择数据源');
    }
    if (!sqlQuery.trim()) {
      message.warning('请输入SQL查询语句');
      throw new Error('SQL查询语句为空');
    }

    setLoading(true);
    try {
      const response = await executeSQL({
        datasourceId: selectedSource,
        sql: sqlQuery,
        exportResult: true,
        exportFormat: 'xlsx',
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
  }, [selectedSource, sqlQuery]);

  return {
    queryResults,
    setQueryResults,
    loading,
    sqlQuery,
    setSqlQuery,
    executeQuery,
  };
};
