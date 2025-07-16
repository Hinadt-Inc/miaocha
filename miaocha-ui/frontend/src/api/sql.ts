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

// 向后兼容的完整结构类型
export interface LegacySchemaResult {
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

// 主要使用的扩展结构类型（支持按需加载）
export interface SchemaResult {
  databaseName: string;
  tables: Array<{
    tableName: string;
    tableComment: string;
    columns?: Array<{
      columnName: string;
      dataType: string;
      columnComment: string;
      isPrimaryKey: boolean;
      isNullable: boolean;
    }>;
    isLoaded?: boolean;
    isLoading?: boolean;
  }>;
}

// 新增：数据库表列表DTO
export interface DatabaseTableListDTO {
  databaseName: string;
  tables: Array<{
    tableName: string;
    tableComment: string;
  }>;
}

// 新增：单个表结构DTO
export interface TableSchemaDTO {
  tableName: string;
  tableComment: string;
  columns: Array<{
    columnName: string;
    dataType: string;
    columnComment: string;
    isPrimaryKey: boolean;
    isNullable: boolean;
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
 * 获取数据库结构（向后兼容接口）
 * @param datasourceId 数据源ID
 * @returns 完整数据库结构
 * @deprecated 建议使用 getDatabaseTables 和 getTableSchema 的组合
 */
export async function getLegacySchema(datasourceId: string): Promise<LegacySchemaResult> {
  return get<LegacySchemaResult>(`/api/sql/schema/${datasourceId}`);
}

/**
 * 获取数据库结构（新版本，返回扩展格式）
 * 优先使用快速表列表，按需加载详情
 * @param datasourceId 数据源ID
 * @returns 扩展的数据库结构
 */
export async function getSchema(datasourceId: string): Promise<SchemaResult> {
  // 先获取表列表
  const tableList = await getDatabaseTables(datasourceId);
  
  // 转换为扩展格式
  return {
    databaseName: tableList.databaseName,
    tables: tableList.tables.map(table => ({
      tableName: table.tableName,
      tableComment: table.tableComment,
      isLoaded: false,
      isLoading: false,
    })),
  };
}

/**
 * 获取数据库表列表（不包含字段信息）- 新增接口
 * @param datasourceId 数据源ID
 * @returns 数据库表列表
 */
export async function getDatabaseTables(datasourceId: string): Promise<DatabaseTableListDTO> {
  return get<DatabaseTableListDTO>(`/api/sql/tables/${datasourceId}`);
}

/**
 * 获取单个表的结构信息 - 新增接口
 * @param datasourceId 数据源ID
 * @param tableName 表名
 * @returns 表结构信息
 */
export async function getTableSchema(datasourceId: string, tableName: string): Promise<TableSchemaDTO> {
  return get<TableSchemaDTO>(`/api/sql/table-schema/${datasourceId}?tableName=${encodeURIComponent(tableName)}`);
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
export async function downloadSqlResult(url: string): Promise<{ data: Blob }> {
  return request<{ data: Blob }>({
    url,
    responseType: 'blob',
  });
}
