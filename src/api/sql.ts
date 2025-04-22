import { request, get, post } from './request';

export interface ExecuteSQLParams {
  datasourceId: string;
  sql: string;
}

export interface ExecuteSQLResult {
  queryId: string;
  status: 'success' | 'error';
  message?: string;
  executionTime?: number;
  columns?: string[];
  rows?: Record<string, unknown>[];
  total?: number;
  executionTimeMs?: number;
}

export interface SchemaResult {
  databaseName: string;
  tables: Array<{
    tableName: string;
    tableComment: string;
    columns: Array<{
      columnName: string;
      dataType: string;
      columnComment: string;
      isPrimaryKey: boolean;
      isNullable: boolean;
    }>;
  }>;
}

/**
 * 执行SQL查询
 * @param params 执行参数
 * @returns 执行结果
 */
export async function executeSQL(params: ExecuteSQLParams): Promise<ExecuteSQLResult> {
  return post<ExecuteSQLResult>('/api/sql/execute', params);
}

/**
 * 获取数据库结构
 * @param datasourceId 数据源ID
 * @returns 数据库结构
 */
export async function getSchema(datasourceId: string): Promise<SchemaResult> {
  return get<SchemaResult>(`/api/sql/schema/${datasourceId}`);
}

/**
 * 下载查询结果
 * @param queryId 查询ID
 * @returns 文件下载流
 */
export async function downloadResult(queryId: string): Promise<Blob> {
  return request<Blob>({
    url: `/api/sql/result/${queryId}`,
    responseType: 'blob'
  });
}
