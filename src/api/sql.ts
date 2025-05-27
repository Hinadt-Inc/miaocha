import { request, get, post } from './request';

export interface ExecuteSQLParams {
  datasourceId: string;
  sql: string;
  exportResult?: boolean;
  exportFormat?: 'csv' | 'xlsx' | 'json';
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
  downloadUrl?: string;
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

export interface QueryHistoryItem {
  id: number;
  userId: number;
  userEmail: string;
  datasourceId: number;
  tableName: string;
  sqlQuery: string;
  hasResultFile: boolean;
  downloadUrl: string;
  createTime: string;
}

export interface QueryHistoryResult {
  pageNum: number;
  pageSize: number;
  total: number;
  pages: number;
  records: QueryHistoryItem[];
}

/**
 * 查询SQL执行历史
 * @param params 查询参数
 * @returns 历史记录列表
 */
export async function queryHistory(params: {
  pageNum: number;
  pageSize: number;
  datasourceId?: number;
  tableName?: string;
  queryKeyword?: string;
}): Promise<QueryHistoryResult> {
  const query = new URLSearchParams();
  query.append('pageNum', params.pageNum.toString());
  query.append('pageSize', params.pageSize.toString());
  if (params.datasourceId) query.append('datasourceId', params.datasourceId.toString());
  if (params.tableName) query.append('tableName', params.tableName);
  if (params.queryKeyword) query.append('queryKeyword', params.queryKeyword);
  return get<QueryHistoryResult>(`/api/sql/history?${query.toString()}`);
}

/**
 * 下载查询结果
 * @param queryId 查询ID
 * @returns 文件下载流
 */
export async function downloadSqlResult(url: string): Promise<Blob> {
  return request<Blob>({
    url,
    responseType: 'blob',
  });
}
