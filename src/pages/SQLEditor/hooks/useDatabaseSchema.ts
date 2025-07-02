import { useState, useCallback, useEffect } from 'react';
import { message } from 'antd';
import { getSchema } from '../../../api/sql';
import type { SchemaResult } from '../types';

/**
 * 管理数据库结构的自定义钩子
 * @param initialSelectedSource 初始选中的数据源ID
 */
export const useDatabaseSchema = (initialSelectedSource?: string) => {
  const [databaseSchema, setDatabaseSchema] = useState<SchemaResult | null>(null);
  const [loadingSchema, setLoadingSchema] = useState<boolean>(false);

  /**
   * 获取数据库结构
   * @param sourceId 数据源ID
   */
  const fetchDatabaseSchema = useCallback(async (sourceId?: string) => {
    const dataSourceId = sourceId ?? initialSelectedSource;

    if (!dataSourceId) {
      // 清空之前的schema
      setDatabaseSchema(null);
      return null;
    }

    setLoadingSchema(true);
    try {
      const response = await getSchema(dataSourceId);
      if (response) {
        setDatabaseSchema(response);
        return response;
      }
      return null;
    } catch (error) {
      console.error('获取数据库结构失败:', error);
      message.error('获取数据库结构失败');
      return null;
    } finally {
      setLoadingSchema(false);
    }
  }, []); // 移除 initialSelectedSource 依赖，避免循环更新

  // 自动加载数据库结构
  useEffect(() => {
    if (initialSelectedSource) {
      fetchDatabaseSchema(initialSelectedSource);
    } else {
      // 如果没有选中数据源，清空schema
      setDatabaseSchema(null);
    }
  }, [initialSelectedSource]); // 只依赖 initialSelectedSource，移除 fetchDatabaseSchema

  return {
    databaseSchema,
    setDatabaseSchema,
    loadingSchema,
    fetchDatabaseSchema,
  };
};
