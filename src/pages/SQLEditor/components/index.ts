// 原有组件导出
export { default as QueryEditor } from './QueryEditor';
export { default as ResultsViewer } from './ResultsViewer';
export { default as VisualizationPanel } from './VisualizationPanel';
export { default as HistoryDrawer } from './HistoryDrawer';
export { default as SettingsDrawer } from './SettingsDrawer';
export { default as SchemaTree } from './SchemaTree';
export { default as EditorHeader } from './EditorHeader';

// 重构后的新组件导出
export { SQLEditorHeader } from './SQLEditorHeader';
export { SQLEditorSidebar } from './SQLEditorSidebar';
export { SQLQueryPanel } from './SQLQueryPanel';
export { SQLResultsPanel } from './SQLResultsPanel';
export { SQLHistoryDrawer } from './SQLHistoryDrawer';
export { SQLSettingsDrawer } from './SQLSettingsDrawer';
export { SQLSnippetSelector } from './SQLSnippetSelector';
