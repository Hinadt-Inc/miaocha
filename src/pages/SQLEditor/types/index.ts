import { ExecuteSQLResult } from '../../../api/sql';
import { CSVRowData } from '../utils/editorUtils';

// 图表类型枚举
export enum ChartType {
  Bar = 'bar',
  Line = 'line',
  Pie = 'pie',
}

// 数据源类型
export interface DataSource {
  id: string;
  name: string;
  type: string;
  ip: string;
}

// 查询结果类型
export interface QueryResult extends ExecuteSQLResult {
  columns?: string[];
  rows?: CSVRowData[];
  total?: number;
  executionTimeMs?: number;
  downloadUrl?: string;
  affectedRows?: number;
  status: 'success' | 'error';
  message?: string;
}

// 数据库结构类型
export interface SchemaResult {
  databaseName: string;
  tables: {
    tableName: string;
    tableComment: string;
    columns: {
      columnName: string;
      dataType: string;
      columnComment: string;
      isPrimaryKey: boolean;
      isNullable: boolean;
    }[];
  }[];
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
  theme: 'vs' | 'vs-dark' | 'hc-black' | 'sqlTheme';
  wordWrap: boolean;
  autoComplete: boolean;
  tabSize: number;
  minimap: boolean;
}

// 编辑器设置类型验证函数
export function isValidEditorSettings(obj: unknown): obj is EditorSettings {
  return (
    typeof obj === 'object' &&
    obj !== null &&
    typeof (obj as Record<string, unknown>).fontSize === 'number' &&
    typeof (obj as Record<string, unknown>).theme === 'string' &&
    typeof (obj as Record<string, unknown>).wordWrap === 'boolean' &&
    typeof (obj as Record<string, unknown>).autoComplete === 'boolean' &&
    typeof (obj as Record<string, unknown>).tabSize === 'number' &&
    typeof (obj as Record<string, unknown>).minimap === 'boolean'
  );
}

// 历史记录类型验证函数
export function isValidQueryHistory(obj: unknown): obj is QueryHistory {
  return (
    typeof obj === 'object' &&
    obj !== null &&
    typeof (obj as Record<string, unknown>).id === 'string' &&
    typeof (obj as Record<string, unknown>).sql === 'string' &&
    typeof (obj as Record<string, unknown>).dataSourceId === 'string' && // 确保检查dataSourceId字段
    typeof (obj as Record<string, unknown>).timestamp === 'string' &&
    ((obj as Record<string, unknown>).status === 'success' || (obj as Record<string, unknown>).status === 'error')
  );
}

// 本地存储的键名
export const HISTORY_STORAGE_KEY = 'sql_editor_history';
export const SETTINGS_STORAGE_KEY = 'sql_editor_settings';
// 历史记录最大保存数量
export const MAX_HISTORY_COUNT = 100;
