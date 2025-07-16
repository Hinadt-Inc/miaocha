// 保留的原有hooks（被新hooks内部使用）
export { useDataSources } from './useDataSources';
export { useEditorSettings } from './useEditorSettings';
export { useQueryExecution } from './useQueryExecution';
export { useQueryHistory } from './useQueryHistory';

// 主要的hooks导出 - 使用优化版本
export { useOptimizedDatabaseSchema as useDatabaseSchema } from './useOptimizedDatabaseSchema';
export { useOptimizedSQLEditorState as useSQLEditorState } from './useOptimizedSQLEditorState';
export { useOptimizedSQLEditorActions as useSQLEditorActions } from './useOptimizedSQLEditorActions';

// 工具类hooks
export { useSQLSnippets } from './useSQLSnippets';
export { useSQLCompletion } from './useSQLCompletion';

// 直接导出优化版本（供需要时使用）
export { useOptimizedDatabaseSchema } from './useOptimizedDatabaseSchema';
export { useOptimizedSQLEditorState } from './useOptimizedSQLEditorState';
export { useOptimizedSQLEditorActions } from './useOptimizedSQLEditorActions';
