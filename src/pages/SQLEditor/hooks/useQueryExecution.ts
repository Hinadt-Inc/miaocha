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
        sql: sqlQuery
      });
      setQueryResults(response);
      return response;
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
    executeQuery
  };
};
