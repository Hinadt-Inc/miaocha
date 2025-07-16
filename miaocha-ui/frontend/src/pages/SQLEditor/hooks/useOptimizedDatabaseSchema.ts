import { useState, useCallback, useEffect } from 'react';
import { message } from 'antd';
import { getDatabaseTables, getTableSchema, getSchema } from '../../../api/sql';
import type { ExtendedSchemaResult, DatabaseTableList, TableSchema } from '../types';

/**
 * 优化的数据库结构管理钩子
 * 支持快速加载表列表和按需加载表详细信息
 * @param initialSelectedSource 初始选中的数据源ID
 */
export const useOptimizedDatabaseSchema = (initialSelectedSource?: string) => {
  const [databaseSchema, setDatabaseSchema] = useState<ExtendedSchemaResult | null>(null);
  const [loadingSchema, setLoadingSchema] = useState<boolean>(false);
  const [loadingTables, setLoadingTables] = useState<Set<string>>(new Set());

  /**
   * 快速获取数据库表列表（不包含字段信息）
   * @param sourceId 数据源ID
   */
  const fetchDatabaseTables = useCallback(async (sourceId: string) => {
    if (!sourceId) {
      setDatabaseSchema(null);
      return null;
    }

    setLoadingSchema(true);
    try {
      const response: DatabaseTableList = await getDatabaseTables(sourceId);
      if (response) {
        const extendedSchema: ExtendedSchemaResult = {
          databaseName: response.databaseName,
          tables: response.tables.map(table => ({
            tableName: table.tableName,
            tableComment: table.tableComment,
            isLoaded: false,
            isLoading: false,
          })),
        };
        setDatabaseSchema(extendedSchema);
        return extendedSchema;
      }
      return null;
    } catch (error) {
      console.error('获取数据库表列表失败:', error);
      message.error('获取数据库表列表失败');
      return null;
    } finally {
      setLoadingSchema(false);
    }
  }, []); // 强制要求传入sourceId参数，避免内部使用initialSelectedSource造成依赖问题

  /**
   * 获取单个表的详细结构信息
   * @param sourceId 数据源ID
   * @param tableName 表名
   */
  const fetchTableSchema = useCallback(async (sourceId: string, tableName: string) => {
    if (!sourceId || !tableName) {
      return null;
    }

    // 设置该表为加载状态
    setLoadingTables(prev => new Set([...prev, tableName]));
    setDatabaseSchema(prev => {
      if (!prev) return prev;
      return {
        ...prev,
        tables: prev.tables.map(table => 
          table.tableName === tableName 
            ? { ...table, isLoading: true }
            : table
        ),
      };
    });

    try {
      const response: TableSchema = await getTableSchema(sourceId, tableName);
      if (response) {
        // 更新数据库结构，添加该表的详细信息
        setDatabaseSchema(prev => {
          if (!prev) return prev;
          return {
            ...prev,
            tables: prev.tables.map(table => 
              table.tableName === tableName 
                ? { 
                    ...table, 
                    columns: response.columns,
                    isLoaded: true,
                    isLoading: false,
                  }
                : table
            ),
          };
        });
        return response;
      }
      return null;
    } catch (error) {
      console.error(`获取表 ${tableName} 结构失败:`, error);
      message.error(`获取表 ${tableName} 结构失败`);
      // 失败时移除加载状态
      setDatabaseSchema(prev => {
        if (!prev) return prev;
        return {
          ...prev,
          tables: prev.tables.map(table => 
            table.tableName === tableName 
              ? { ...table, isLoading: false }
              : table
          ),
        };
      });
      return null;
    } finally {
      setLoadingTables(prev => {
        const newSet = new Set(prev);
        newSet.delete(tableName);
        return newSet;
      });
    }
  }, []); // 移除依赖，使函数引用稳定

  /**
   * 兼容原有接口：获取完整数据库结构
   * @param sourceId 数据源ID
   */
  const fetchDatabaseSchema = useCallback(async (sourceId?: string) => {
    const dataSourceId = sourceId ?? initialSelectedSource;

    if (!dataSourceId) {
      setDatabaseSchema(null);
      return null;
    }

    setLoadingSchema(true);
    try {
      const response = await getSchema(dataSourceId);
      if (response) {
        const extendedSchema: ExtendedSchemaResult = {
          databaseName: response.databaseName,
          tables: response.tables.map(table => ({
            tableName: table.tableName,
            tableComment: table.tableComment,
            columns: table.columns,
            isLoaded: true,
            isLoading: false,
          })),
        };
        setDatabaseSchema(extendedSchema);
        return extendedSchema;
      }
      return null;
    } catch (error) {
      console.error('获取数据库结构失败:', error);
      message.error('获取数据库结构失败');
      return null;
    } finally {
      setLoadingSchema(false);
    }
  }, [initialSelectedSource]);

  /**
   * 检查表是否已加载详细信息
   */
  const isTableLoaded = useCallback((tableName: string): boolean => {
    if (!databaseSchema) return false;
    const table = databaseSchema.tables.find(t => t.tableName === tableName);
    return table?.isLoaded || false;
  }, [databaseSchema]);

  /**
   * 检查表是否正在加载
   */
  const isTableLoading = useCallback((tableName: string): boolean => {
    if (!databaseSchema) return false;
    const table = databaseSchema.tables.find(t => t.tableName === tableName);
    return table?.isLoading || false;
  }, [databaseSchema]);

  /**
   * 获取表的列信息
   */
  const getTableColumns = useCallback((tableName: string) => {
    if (!databaseSchema) return [];
    const table = databaseSchema.tables.find(t => t.tableName === tableName);
    return table?.columns || [];
  }, [databaseSchema]);

  // 自动加载数据库表列表（快速模式）
  useEffect(() => {
    if (initialSelectedSource) {
      fetchDatabaseTables(initialSelectedSource);
    } else {
      setDatabaseSchema(null);
    }
  }, [initialSelectedSource]); // 移除fetchDatabaseTables依赖，避免循环更新

  return {
    databaseSchema,
    loadingSchema,
    loadingTables,
    fetchDatabaseTables,
    fetchTableSchema,
    fetchDatabaseSchema, // 兼容原有接口
    isTableLoaded,
    isTableLoading,
    getTableColumns,
  };
};
