import React from 'react';
import { vi } from 'vitest';

// 模拟SchemaTree组件
vi.mock('../../components/SchemaTree', () => ({
  default: ({
    databaseSchema,
    loadingSchema,
    refreshSchema,
    handleTreeNodeDoubleClick,
    handleInsertTable,
    collapsed,
    toggleSider,
  }) => (
    <div data-testid="schema-tree">
      <button data-testid="refresh-schema-btn" onClick={refreshSchema}>
        刷新结构
      </button>
      <button data-testid="toggle-sider-btn" onClick={toggleSider}>
        {collapsed ? '展开' : '收起'}
      </button>
      <div data-testid="schema-content">
        {loadingSchema ? (
          '加载中...'
        ) : (
          <ul>
            {databaseSchema?.tables?.map((table) => (
              <li key={table.tableName} data-testid={`table-${table.tableName}`}>
                <span
                  data-testid={`table-name-${table.tableName}`}
                  onDoubleClick={() => handleTreeNodeDoubleClick(table.tableName)}
                >
                  {table.tableName}
                </span>
                <button
                  data-testid={`insert-table-${table.tableName}`}
                  onClick={() => handleInsertTable(table.tableName, table.columns)}
                >
                  插入表
                </button>
                <ul>
                  {table.columns?.map((col) => (
                    <li
                      key={`${table.tableName}-${col.columnName}`}
                      data-testid={`column-${col.columnName}`}
                    >
                      {col.columnName} ({col.dataType})
                    </li>
                  ))}
                </ul>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  ),
}));

// 模拟QueryEditor组件
vi.mock('../../components/QueryEditor', () => ({
  default: ({
    sqlQuery,
    onChange,
    onEditorMount,
    editorSettings,
    height,
    collapsed,
    onCollapsedChange,
    onHeightChange,
  }) => (
    <div data-testid="query-editor" style={{ height: `${height}px` }}>
      <button data-testid="toggle-editor-collapse" onClick={() => onCollapsedChange(!collapsed)}>
        {collapsed ? '展开编辑器' : '收起编辑器'}
      </button>
      <textarea
        data-testid="sql-textarea"
        value={sqlQuery}
        onChange={(e) => onChange(e.target.value)}
        style={{ width: '100%', height: '80%' }}
      />
      <div>
        <button data-testid="increase-height" onClick={() => onHeightChange(height + 50)}>
          增加高度
        </button>
        <button data-testid="decrease-height" onClick={() => onHeightChange(height - 50)}>
          减少高度
        </button>
      </div>
    </div>
  ),
}));

// 模拟ResultsViewer组件
vi.mock('../../components/ResultsViewer', () => ({
  default: ({ queryResults, loading, downloadResults, formatTableCell }) => (
    <div data-testid="results-viewer">
      {loading ? (
        <div data-testid="loading-results">加载查询结果中...</div>
      ) : (
        <div data-testid="query-results">
          {queryResults?.status === 'error' ? (
            <div data-testid="error-message">{queryResults.message}</div>
          ) : (
            <div>
              <div data-testid="result-stats">
                {queryResults?.rows?.length || 0}行结果
                {queryResults?.executionTimeMs !== undefined &&
                  `，耗时${queryResults.executionTimeMs}ms`}
              </div>
              <table data-testid="results-table">
                <thead>
                  <tr>{queryResults?.columns?.map((col) => <th key={col}>{col}</th>)}</tr>
                </thead>
                <tbody>
                  {queryResults?.rows?.map((row, i) => (
                    <tr key={i} data-testid={`result-row-${i}`}>
                      {queryResults.columns?.map((col) => (
                        <td key={`${i}-${col}`}>{formatTableCell(row[col])}</td>
                      ))}
                    </tr>
                  ))}
                </tbody>
              </table>
              <button data-testid="download-results-btn" onClick={downloadResults}>
                下载CSV
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  ),
}));

// 模拟VisualizationPanel组件
vi.mock('../../components/VisualizationPanel', () => ({
  default: ({ queryResults, chartType, setChartType, xField, setXField, yField, setYField }) => (
    <div data-testid="visualization-panel">
      <div>
        <select
          data-testid="chart-type-select"
          value={chartType}
          onChange={(e) => setChartType(e.target.value as any)}
        >
          <option value="bar">柱状图</option>
          <option value="line">折线图</option>
          <option value="pie">饼图</option>
        </select>
        <select
          data-testid="x-field-select"
          value={xField}
          onChange={(e) => setXField(e.target.value)}
        >
          {queryResults?.columns?.map((col) => (
            <option key={col} value={col}>
              {col}
            </option>
          ))}
        </select>
        <select
          data-testid="y-field-select"
          value={yField}
          onChange={(e) => setYField(e.target.value)}
        >
          {queryResults?.columns?.map((col) => (
            <option key={col} value={col}>
              {col}
            </option>
          ))}
        </select>
      </div>
      <div data-testid="chart-container">
        Chart: {chartType} - X: {xField} - Y: {yField}
      </div>
    </div>
  ),
}));

// 模拟HistoryDrawer组件
vi.mock('../../components/HistoryDrawer', () => ({
  default: ({ visible, onClose, queryHistory, loadFromHistory, copyToClipboard, clearHistory }) => (
    <div data-testid="history-drawer" style={{ display: visible ? 'block' : 'none' }}>
      <button data-testid="close-history-btn" onClick={onClose}>
        关闭
      </button>
      <button data-testid="clear-history-btn" onClick={clearHistory}>
        清除历史
      </button>
      <ul>
        {queryHistory?.map((item) => (
          <li key={item.id} data-testid={`history-item-${item.id}`}>
            <div data-testid={`history-sql-${item.id}`}>{item.sql}</div>
            <div data-testid={`history-status-${item.id}`}>状态: {item.status}</div>
            <button
              data-testid={`load-history-${item.id}`}
              onClick={() => loadFromHistory(item.sql)}
            >
              加载
            </button>
            <button
              data-testid={`copy-history-${item.id}`}
              onClick={() => copyToClipboard(item.sql)}
            >
              复制
            </button>
          </li>
        ))}
      </ul>
    </div>
  ),
}));

// 模拟SettingsDrawer组件
vi.mock('../../components/SettingsDrawer', () => ({
  default: ({ onClose, editorSettings, updateEditorSettings }) => (
    <div data-testid="settings-drawer" style={{ display: editorSettings ? 'block' : 'none' }}>
      <button data-testid="close-settings-btn" onClick={onClose}>
        关闭
      </button>
      <div>
        <label>
          字体大小:
          <input
            data-testid="font-size-input"
            type="number"
            value={editorSettings?.fontSize || 14}
            onChange={(e) =>
              updateEditorSettings({ ...editorSettings, fontSize: parseInt(e.target.value) })
            }
          />
        </label>
      </div>
      <div>
        <label>
          主题:
          <select
            data-testid="theme-select"
            value={editorSettings?.theme || 'vs'}
            onChange={(e) => updateEditorSettings({ ...editorSettings, theme: e.target.value })}
          >
            <option value="vs">亮色</option>
            <option value="vs-dark">暗色</option>
          </select>
        </label>
      </div>
    </div>
  ),
}));

// 模拟EditorHeader组件
vi.mock('../../components/EditorHeader', () => ({
  default: ({
    dataSources,
    selectedSource,
    setSelectedSource,
    executeQuery,
    toggleHistory,
    toggleSettings,
    toggleFullscreen,
  }) => (
    <div data-testid="editor-header">
      <select
        data-testid="datasource-select"
        value={selectedSource}
        onChange={(e) => setSelectedSource(e.target.value)}
      >
        {dataSources.map((source) => (
          <option key={source.id} value={source.id}>
            {source.name} ({source.type})
          </option>
        ))}
      </select>
      <button data-testid="execute-query-btn" onClick={executeQuery}>
        执行
      </button>
      <button data-testid="toggle-history-btn" onClick={toggleHistory}>
        历史
      </button>
      <button data-testid="toggle-settings-btn" onClick={toggleSettings}>
        设置
      </button>
      <button data-testid="toggle-fullscreen-btn" onClick={toggleFullscreen}>
        全屏
      </button>
    </div>
  ),
}));

// 模拟monaco-editor相关
vi.mock('monaco-editor', () => {
  return {
    KeyMod: {
      CtrlCmd: 2048,
    },
    KeyCode: {
      Enter: 13,
    },
    Range: {
      fromPositions: vi
        .fn()
        .mockReturnValue({ startLineNumber: 1, endLineNumber: 1, startColumn: 1, endColumn: 1 }),
    },
    languages: {
      CompletionItemKind: {
        Class: 1,
        Field: 2,
        Keyword: 3,
      },
      registerCompletionItemProvider: vi.fn().mockReturnValue({
        dispose: vi.fn(),
      }),
    },
  };
});

// 模拟@monaco-editor/react
vi.mock('@monaco-editor/react', () => {
  return {
    OnMount: vi.fn(),
  };
});

// 模拟copy-to-clipboard
vi.mock('copy-to-clipboard', () => {
  return {
    default: vi.fn().mockImplementation(() => true),
  };
});

// 模拟utils
vi.mock('../../utils/monacoInit', () => ({
  default: vi.fn(),
}));

vi.mock('../../utils/editorUtils', () => ({
  downloadAsCSV: vi.fn(),
  insertTextToEditor: vi.fn(),
  insertFormattedSQL: vi.fn(),
  getSQLContext: vi.fn().mockReturnValue({
    isSelectQuery: true,
    hasFromClause: true,
    isInSelectClause: false,
    isInFromClause: false,
    isInWhereClause: false,
  }),
  generateColumnList: vi.fn(),
}));

vi.mock('../../utils/formatters', () => ({
  default: vi.fn((value) => value),
}));

export {};
