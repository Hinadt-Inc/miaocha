// 保留的原有组件（仍被新组件使用）
export { default as QueryEditor } from './QueryEditor';
export { default as ResultsViewer } from './ResultsViewer';
export { default as VisualizationPanel } from './VisualizationPanel';
export { default as HistoryDrawer } from './HistoryDrawer';
export { default as SettingsDrawer } from './SettingsDrawer';

// 主要组件导出 - 使用优化版本
export { SQLEditorHeader } from './SQLEditorHeader';
export { OptimizedSQLEditorSidebar as SQLEditorSidebar } from './OptimizedSQLEditorSidebar';
export { SQLQueryPanel } from './SQLQueryPanel';
export { SQLResultsPanel } from './SQLResultsPanel';
export { SQLHistoryDrawer } from './SQLHistoryDrawer';
export { SQLSettingsDrawer } from './SQLSettingsDrawer';
export { SQLSnippetSelector } from './SQLSnippetSelector';

// 直接导出优化的组件（供需要时使用）
export { default as OptimizedSchemaTree } from './OptimizedSchemaTree';
export { OptimizedSQLEditorSidebar } from './OptimizedSQLEditorSidebar';

// 保留原版本以备兼容性需要
export { default as VirtualizedSchemaTree } from './VirtualizedSchemaTree';
export { SQLEditorSidebar as LegacySQLEditorSidebar } from './SQLEditorSidebar';
