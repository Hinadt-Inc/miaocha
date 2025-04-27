import { ExecuteSQLResult } from '../../../api/sql';

// 数据源类型
export interface DataSource {
  id: string;
  name: string;
  type: string;
  host: string;
}

// 查询结果类型
export interface QueryResult extends ExecuteSQLResult {
  columns?: string[];
  rows?: Record<string, unknown>[];
  total?: number;
  executionTimeMs?: number;
  downloadUrl?: string;
  affectedRows?: number;
}

// 数据库结构类型
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

// 查询历史记录类型
export interface QueryHistory {
  id: string;
  sql: string;
  dataSourceId: string;
  executionTime: number;
  status: 'success' | 'error';
  timestamp: string;
  message?: string;
}

// 编辑器设置类型
export interface EditorSettings {
  fontSize: number;
  theme: string;
  wordWrap: boolean;
  autoComplete: boolean;
  tabSize: number;
  minimap: boolean;
}

// 本地存储的键名
export const HISTORY_STORAGE_KEY = 'sql_editor_history';
export const SETTINGS_STORAGE_KEY = 'sql_editor_settings';
// 历史记录最大保存数量
export const MAX_HISTORY_COUNT = 100;