import { useCallback, useEffect, useRef, useState } from 'react';
import { useDataSources } from './hooks/useDataSources';
import { useQueryExecution } from './hooks/useQueryExecution';
import { useEditorSettings } from './hooks/useEditorSettings';
import { useQueryHistory } from './hooks/useQueryHistory';
import { useDatabaseSchema } from './hooks/useDatabaseSchema';
import { Alert, Button, Card, Layout, message, Space, Tabs, Tooltip } from 'antd';
import { CopyOutlined } from '@ant-design/icons';
import { OnMount } from '@monaco-editor/react';
import * as monaco from 'monaco-editor';
import copy from 'copy-to-clipboard';
import { debounce } from 'lodash';

// 组件导入
import SchemaTree from './components/SchemaTree';
import QueryEditor from './components/QueryEditor';
import ResultsViewer from './components/ResultsViewer';
import VisualizationPanel from './components/VisualizationPanel';
import HistoryDrawer from './components/HistoryDrawer';
import SettingsDrawer from './components/SettingsDrawer';
import EditorHeader from './components/EditorHeader';

// 工具和类型导入
import initMonacoEditor from './utils/monacoInit';
import {
  downloadAsCSV,
  insertTextToEditor,
  insertFormattedSQL,
  getSQLContext,
  generateColumnList,
} from './utils/editorUtils';
import formatTableCell from './utils/formatters';
import { ChartType, QueryResult, SchemaResult } from './types';

// 样式导入
import './SQLEditorPage.less';

const { TabPane } = Tabs;
const { Content, Sider } = Layout;

/**
 * SQL编辑器主组件实现
 * 包含数据库结构展示、SQL编辑器、查询结果显示和可视化等功能
 */
const SQLEditorImpl: React.FC = () => {
  // 使用编辑器引用，添加类型标注
  const editorRef = useRef<monaco.editor.IStandaloneCodeEditor | null>(null);
  const monacoRef = useRef<typeof monaco | null>(null);
  // 这里使用 _monaco 作为前缀，表示此变量仅用于类型注解

  // 使用自定义hooks管理状态
  const { dataSources, selectedSource, setSelectedSource, loading: loadingDataSources } = useDataSources();

  const { databaseSchema, loadingSchema, fetchDatabaseSchema } = useDatabaseSchema(selectedSource);

  const {
    queryResults,
    sqlQuery,
    setSqlQuery,
    loading: loadingResults,
    executeQuery: executeQueryOriginal,
  } = useQueryExecution(selectedSource);

  const { settings: editorSettings, saveSettings } = useEditorSettings();

  const { history: queryHistory, addHistory, clearHistory, clearAllHistory } = useQueryHistory(selectedSource);

  // 本地UI状态
  const [activeTab, setActiveTab] = useState<string>('results');
  const [chartType, setChartType] = useState<ChartType>(ChartType.Bar);
  const [xField, setXField] = useState<string>('');
  const [yField, setYField] = useState<string>('');
  const [fullscreen, setFullscreen] = useState<boolean>(false);
  const [historyDrawerVisible, setHistoryDrawerVisible] = useState(false);
  const [settingsDrawerVisible, setSettingsDrawerVisible] = useState(false);

  // 添加编辑器高度和收起状态
  const [editorHeight, setEditorHeight] = useState(300);
  const [editorCollapsed, setEditorCollapsed] = useState(false);

  // 添加侧边栏收起状态
  const [siderCollapsed, setSiderCollapsed] = useState(false);
  // 添加侧边栏宽度设置 - 收起时宽度变小
  const siderWidth = siderCollapsed ? 80 : 250;

  // 检查SQL语法有效性
  const validateSQL = (query: string): boolean => {
    if (!query.trim()) {
      message.warning('请输入SQL查询语句');
      return false;
    }
    return true;
  };

  // 使用防抖的查询执行
  const executeQueryDebounced = useCallback(
    debounce(() => {
      if (!selectedSource) {
        message.warning('请先选择数据源');
        return;
      }

      if (!validateSQL(sqlQuery)) return;

      setActiveTab('results');
      executeQueryOriginal()
        .then((results: QueryResult) => {
          // 添加到查询历史记录
          addHistory(sqlQuery, 'success');

          if (results?.rows?.length && results?.columns) {
            setXField(results.columns[0]);
            const numericColumn = results.columns.find((col: string) => {
              const sampleValue = results.rows?.[0][col];
              return typeof sampleValue === 'number';
            });
            setYField(numericColumn ?? results.columns[1] ?? results.columns[0]);
          }
        })
        .catch((error: Error) => {
          // 添加失败的查询到历史记录
          addHistory(sqlQuery, 'error', error.message);
          console.error('执行查询失败:', error);
        });
    }, 300),
    [executeQueryOriginal, selectedSource, sqlQuery, setActiveTab, setXField, setYField, addHistory],
  );

  // 初始化
  useEffect(() => {
    // 初始化Monaco编辑器
    initMonacoEditor();

    // 全屏模式快捷键
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'F11') {
        e.preventDefault();
        setFullscreen((prev) => !prev);
      } else if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
        e.preventDefault();
        executeQueryDebounced();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [executeQueryDebounced]);

  // 当选择新的数据源时，自动获取数据库结构
  useEffect(() => {
    if (selectedSource) {
      // 添加 void 操作符防止 Promise 未处理警告
      void fetchDatabaseSchema(selectedSource);
    }
  }, [selectedSource, fetchDatabaseSchema]);

  // 下载查询结果为 CSV
  const handleDownloadResults = () => {
    if (!queryResults?.rows?.length || !queryResults.columns) {
      message.warning('没有可下载的结果');
      return;
    }

    try {
      downloadAsCSV(queryResults.rows, queryResults.columns);
      message.success('下载已开始');
    } catch (error) {
      console.error('下载失败:', error);
      message.error('下载失败');
    }
  };

  // 从历史记录加载查询
  const loadFromHistory = (historySql: string) => {
    setSqlQuery(historySql);
    setHistoryDrawerVisible(false);
  };

  // 复制到剪贴板
  const copyToClipboard = (text: string) => {
    copy(text);
    message.success('已复制到剪贴板');
  };

  // 处理树节点双击事件
  const handleTreeNodeDoubleClick = (tableName: string) => {
    if (editorRef.current) {
      insertTextToEditor(editorRef.current, tableName);
    }
  };

  // 插入表和字段到编辑器
  const handleInsertTable = (
    tableName: string,
    columns: {
      columnName: string;
      dataType: string;
      columnComment: string;
      isPrimaryKey: boolean;
      isNullable: boolean;
    }[],
  ) => {
    if (!editorRef.current) return;

    const editor = editorRef.current;

    // 规范化表名，防止SQL注入
    const safeTableName = tableName.replace(/[^\w\d_]/g, '');

    // 获取当前SQL上下文
    const sqlContext = getSQLContext(editor);

    try {
      // 场景1: 编辑器为空或光标在开头 - 创建完整的查询
      if (!sqlContext.isSelectQuery || editor.getModel()?.getValue().trim() === '') {
        // 如果有字段，创建完整的带字段列表的查询
        if (columns.length > 0) {
          const fieldList = generateColumnList(columns, {
            addComments: true,
            indentSize: 4,
            multiline: true,
          });
          insertTextToEditor(editor, `SELECT\n${fieldList}\nFROM ${safeTableName};`);
        } else {
          // 如果没有字段信息，创建简单的全字段查询
          insertTextToEditor(editor, `SELECT * FROM ${safeTableName};`);
        }
        return;
      }

      // 场景2: 在SELECT子句中 - 添加字段
      if (sqlContext.isInSelectClause) {
        const fieldList =
          columns.length > 0 ? generateColumnList(columns, { multiline: false, addComments: false }) : '*';

        // 获取当前选择位置前的文本，检查是否需要添加逗号
        const model = editor.getModel();
        const selection = editor.getSelection();

        if (model && selection) {
          const textBeforeCursor = model.getValueInRange({
            startLineNumber: 1,
            startColumn: 1,
            endLineNumber: selection.startLineNumber,
            endColumn: selection.startColumn,
          });

          const needsComma = !/,\s*$/.test(textBeforeCursor) && textBeforeCursor.trim() !== 'SELECT';
          const prefix = needsComma ? ', ' : '';

          insertFormattedSQL(editor, `${prefix}${fieldList}`, { addComma: false });
        } else {
          insertFormattedSQL(editor, fieldList, { addComma: false });
        }
        return;
      }

      // 场景3: 有SELECT但无FROM - 添加FROM子句
      if (sqlContext.isSelectQuery && !sqlContext.hasFromClause) {
        const model = editor.getModel();
        if (model) {
          const lastLine = model.getLineCount();
          const lastColumn = model.getLineMaxColumn(lastLine);
          const position = { lineNumber: lastLine, column: lastColumn };

          // 检查最后一个字符是否需要添加空格
          const text = model.getValue();
          const needsSpace = /[^\s,]$/m.test(text);
          const prefix = needsSpace ? ' ' : '';

          // 创建编辑操作插入到特定位置
          editor.executeEdits('insert-from-clause', [
            {
              range: monaco.Range.fromPositions(position, position),
              text: `${prefix}FROM ${safeTableName};`,
            },
          ]);
        }
        return;
      }

      // 场景4: 在FROM子句中 - 添加表名
      if (sqlContext.isInFromClause) {
        const model = editor.getModel();
        const selection = editor.getSelection();

        if (model && selection) {
          const textBeforeCursor = model.getValueInRange({
            startLineNumber: 1,
            startColumn: 1,
            endLineNumber: selection.startLineNumber,
            endColumn: selection.startColumn,
          });

          // 检查是否已经有表名，需要添加JOIN或逗号
          if (/FROM\s+[\w\d_]+(\s*,\s*[\w\d_]+)*\s*$/i.test(textBeforeCursor)) {
            // 如果有表名，添加逗号分隔的表名
            const needsComma = !/,\s*$/.test(textBeforeCursor);
            const prefix = needsComma ? ', ' : '';
            insertTextToEditor(editor, `${prefix}${safeTableName}`);
          } else if (/FROM\s*$/i.test(textBeforeCursor)) {
            // 如果刚好在FROM后面，直接添加表名
            insertTextToEditor(editor, ` ${safeTableName}`);
          } else {
            // 默认情况下，先添加空格再添加表名
            insertTextToEditor(editor, ` ${safeTableName}`);
          }
        } else {
          insertTextToEditor(editor, safeTableName);
        }
        return;
      }

      // 场景5: 已有完整查询，添加JOIN子句
      if (sqlContext.isSelectQuery && sqlContext.hasFromClause && !sqlContext.isInWhereClause) {
        const model = editor.getModel();
        if (model) {
          // 找到FROM子句后面的位置
          const text = model.getValue();
          const fromIndex = text.toUpperCase().indexOf('FROM');

          // 在FROM子句后找到一个合适的位置
          const whereIndex = text.toUpperCase().indexOf('WHERE');
          const groupByIndex = text.toUpperCase().indexOf('GROUP BY');
          const orderByIndex = text.toUpperCase().indexOf('ORDER BY');
          const limitIndex = text.toUpperCase().indexOf('LIMIT');

          // 确定JOIN应该插入的位置
          let insertPos = -1;
          if (whereIndex > fromIndex) insertPos = whereIndex;
          else if (groupByIndex > fromIndex) insertPos = groupByIndex;
          else if (orderByIndex > fromIndex) insertPos = orderByIndex;
          else if (limitIndex > fromIndex) insertPos = limitIndex;

          if (insertPos > -1) {
            // 在子句前插入JOIN
            const positionInModel = model.getPositionAt(insertPos);
            const position = {
              lineNumber: positionInModel.lineNumber,
              column: positionInModel.column,
            };

            // 添加一个JOIN子句
            editor.executeEdits('insert-join-clause', [
              {
                range: monaco.Range.fromPositions(position, position),
                text: `\nJOIN ${safeTableName} ON \n`,
              },
            ]);
          } else {
            // 在查询末尾添加JOIN
            const lastLine = model.getLineCount();
            const lastColumn = model.getLineMaxColumn(lastLine);
            const position = { lineNumber: lastLine, column: lastColumn };

            // 检查最后一个字符是否为分号
            const lastChar = text.trim().charAt(text.trim().length - 1);
            const removeSemicolon = lastChar === ';' ? true : false;

            let joinText = `\nJOIN ${safeTableName} ON `;
            if (removeSemicolon) {
              // 移除最后的分号然后添加JOIN
              const textWithoutSemicolon = text.trim().slice(0, -1);
              joinText = textWithoutSemicolon + joinText;
            }

            // 创建编辑操作
            editor.executeEdits('insert-join-clause', [
              { range: monaco.Range.fromPositions(position, position), text: joinText },
            ]);
          }
        }
        return;
      }

      // 场景6: 默认情况 - 直接插入表名
      insertTextToEditor(editor, safeTableName);
    } catch (error) {
      console.error('SQL插入错误:', error);
      // 出错时的简单回退方案：直接插入表名
      insertTextToEditor(editor, safeTableName);
    }
  };

  // 编辑器挂载事件
  const handleEditorDidMount: OnMount = (editor, monacoInstance) => {
    editorRef.current = editor;
    monacoRef.current = monacoInstance;

    // 添加快捷键：Ctrl+Enter 执行查询
    editor.addCommand(monacoInstance.KeyMod.CtrlCmd | monacoInstance.KeyCode.Enter, executeQueryDebounced);

    // 不返回清理函数，因为Monaco编辑器自己会处理资源释放
  };

  // 创建补全建议
  const createCompletionSuggestions = (
    model: monaco.editor.ITextModel,
    position: monaco.Position,
    schema: SchemaResult | { error: string },
    monaco: typeof import('monaco-editor'),
  ): monaco.languages.CompletionItem[] => {
    const word = model.getWordUntilPosition(position);
    const range = {
      startLineNumber: position.lineNumber,
      endLineNumber: position.lineNumber,
      startColumn: word.startColumn,
      endColumn: word.endColumn,
    };

    const suggestions: monaco.languages.CompletionItem[] = [];

    // 类型谓词函数检查schema是否为SchemaResult
    const isSchemaResult = (s: unknown): s is SchemaResult => {
      return (
        !!s &&
        typeof s === 'object' &&
        'tables' in s &&
        Array.isArray(s.tables) &&
        s.tables.every(
          (table: unknown) =>
            table && typeof table === 'object' && 'tableName' in table && 'tableComment' in table && 'columns' in table,
        )
      );
    };

    // 添加表建议
    if (isSchemaResult(schema)) {
      schema.tables.forEach((table) => {
        suggestions.push({
          label: table.tableName,
          kind: monaco.languages.CompletionItemKind.Class,
          insertText: table.tableName,
          detail: `表: ${table.tableComment ?? table.tableName}`,
          range,
        });

        // 添加列建议
        if (table.columns && Array.isArray(table.columns)) {
          table.columns.forEach((column) => {
            suggestions.push({
              label: column.columnName,
              kind: monaco.languages.CompletionItemKind.Field,
              insertText: column.columnName,
              detail: `字段: ${column.columnComment ?? column.columnName} (${column.dataType})`,
              range,
            });
          });
        }
      });
    }

    // 添加SQL关键字
    ['SELECT', 'FROM', 'WHERE', 'GROUP BY', 'ORDER BY', 'LIMIT', 'JOIN', 'LEFT JOIN', 'INNER JOIN'].forEach(
      (keyword) => {
        suggestions.push({
          label: keyword,
          kind: monaco.languages.CompletionItemKind.Keyword,
          insertText: keyword,
          detail: 'SQL关键字',
          range,
        });
      },
    );

    return suggestions;
  };

  // 注册编辑器自动完成
  useEffect(() => {
    if (monacoRef.current && databaseSchema) {
      // 移除旧的自动完成提供者
      const completionDisposable = monacoRef.current.languages.registerCompletionItemProvider('sql', {
        provideCompletionItems: (model, position) => {
          if (!monacoRef.current) return { suggestions: [] };
          const suggestions = createCompletionSuggestions(model, position, databaseSchema, monacoRef.current);
          return { suggestions };
        },
      });

      // 组件卸载时清理
      return () => {
        completionDisposable.dispose();
      };
    }
  }, [databaseSchema]);

  // 修改为自定义封装函数，解决类型问题
  const handleSetChartType = (type: 'bar' | 'line' | 'pie') => {
    setChartType(type as ChartType);
  };

  // 处理编辑器高度改变
  const handleEditorHeightChange = (height: number) => {
    setEditorHeight(height);
  };

  // 处理编辑器收起状态改变
  const handleEditorCollapsedChange = (collapsed: boolean) => {
    setEditorCollapsed(collapsed);
  };

  // 添加侧边栏折叠切换函数
  const toggleSider = useCallback(() => {
    setSiderCollapsed((prev) => !prev);
  }, []);

  return (
    <>
      <Layout style={{ height: '100vh' }}>
        <Sider
          width={siderWidth}
          theme="light"
          className="sider-container"
          collapsible
          collapsed={siderCollapsed}
          onCollapse={setSiderCollapsed}
          trigger={null}
        >
          {/* 正确使用React.memo方式 */}
          <SchemaTree
            databaseSchema={databaseSchema}
            loadingSchema={loadingSchema}
            refreshSchema={() => {
              void fetchDatabaseSchema(selectedSource);
            }}
            handleTreeNodeDoubleClick={handleTreeNodeDoubleClick}
            handleInsertTable={handleInsertTable}
            fullscreen={fullscreen}
            collapsed={siderCollapsed}
            toggleSider={toggleSider}
          />
        </Sider>

        <Layout className="layout-inner">
          <Content className="content-container">
            <div className="editor-results-container">
              <Card
                hoverable={false}
                title={
                  <div className="editor-header-container">
                    <Space>
                      <span>SQL 查询</span>
                      <Tooltip title="复制 SQL">
                        <Button
                          type="text"
                          size="small"
                          icon={<CopyOutlined />}
                          onClick={() => copyToClipboard(sqlQuery)}
                          disabled={!sqlQuery.trim()}
                          aria-label="复制SQL语句"
                        />
                      </Tooltip>
                    </Space>
                    <EditorHeader
                      dataSources={dataSources}
                      selectedSource={selectedSource}
                      setSelectedSource={setSelectedSource}
                      loadingSchema={loadingSchema}
                      loadingDataSources={loadingDataSources}
                      loadingResults={loadingResults}
                      executeQuery={executeQueryDebounced}
                      toggleHistory={() => setHistoryDrawerVisible(true)}
                      toggleSettings={() => setSettingsDrawerVisible(true)}
                      sqlQuery={sqlQuery}
                    />
                  </div>
                }
                className="editor-card"
              >
                <QueryEditor
                  sqlQuery={sqlQuery}
                  onChange={(value) => setSqlQuery(value ?? '')}
                  onEditorMount={handleEditorDidMount}
                  editorSettings={editorSettings}
                  height={editorHeight}
                  collapsed={editorCollapsed}
                  onCollapsedChange={handleEditorCollapsedChange}
                  onHeightChange={handleEditorHeightChange}
                />
              </Card>
              <Card
                title={
                  <Tabs activeKey={activeTab} onChange={(key) => setActiveTab(key)} className="results-tabs">
                    <TabPane tab="查询结果" key="results" />
                    <TabPane
                      tab="可视化"
                      key="visualization"
                      disabled={!queryResults?.rows?.length || queryResults?.status === 'error'}
                    />
                  </Tabs>
                }
                className="results-card"
                extra={
                  activeTab === 'results' && (
                    <Button
                      type="primary"
                      onClick={handleDownloadResults}
                      disabled={!queryResults?.rows?.length}
                      aria-label="下载查询结果"
                    >
                      下载CSV
                    </Button>
                  )
                }
              >
                {queryResults?.status === 'error' && (
                  <Alert
                    message="查询失败"
                    description={queryResults.message}
                    type="error"
                    showIcon
                    style={{ marginBottom: 16 }}
                  />
                )}

                {activeTab === 'results' ? (
                  <ResultsViewer
                    queryResults={queryResults}
                    loading={loadingResults}
                    downloadResults={handleDownloadResults}
                    formatTableCell={(value) => formatTableCell(value)}
                  />
                ) : (
                  <VisualizationPanel
                    queryResults={queryResults}
                    chartType={chartType}
                    setChartType={handleSetChartType}
                    xField={xField}
                    setXField={setXField}
                    yField={yField}
                    setYField={setYField}
                    fullscreen={fullscreen}
                  />
                )}
              </Card>
            </div>
          </Content>
        </Layout>
      </Layout>

      <HistoryDrawer
        visible={historyDrawerVisible}
        onClose={() => setHistoryDrawerVisible(false)}
        queryHistory={queryHistory}
        loadFromHistory={loadFromHistory}
        copyToClipboard={copyToClipboard}
        clearHistory={clearHistory}
        clearAllHistory={clearAllHistory}
        fullscreen={fullscreen}
      />

      <SettingsDrawer
        onClose={() => setSettingsDrawerVisible(false)}
        editorSettings={editorSettings}
        updateEditorSettings={saveSettings}
        visible={settingsDrawerVisible}
      />
    </>
  );
};

export default SQLEditorImpl;
