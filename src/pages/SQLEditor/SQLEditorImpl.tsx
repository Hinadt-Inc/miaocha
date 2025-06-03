import { useCallback, useEffect, useRef, useState } from 'react';
import { useDataSources } from './hooks/useDataSources';
import { useQueryExecution } from './hooks/useQueryExecution';
import { useEditorSettings } from './hooks/useEditorSettings';
import { useQueryHistory } from './hooks/useQueryHistory';
import { useDatabaseSchema } from './hooks/useDatabaseSchema';
import { Alert, Button, Card, Layout, message, Space, Tabs, Tooltip, Splitter, Dropdown, Menu } from 'antd';
import { CopyOutlined } from '@ant-design/icons';
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

  const { history: queryHistory, pagination, handlePaginationChange } = useQueryHistory(selectedSource);

  // 本地UI状态
  const [activeTab, setActiveTab] = useState<string>('results');
  const [chartType, setChartType] = useState<ChartType>(ChartType.Bar);
  const [xField, setXField] = useState<string>('');
  const [yField, setYField] = useState<string>('');
  const [fullscreen, setFullscreen] = useState<boolean>(false);
  const [historyDrawerVisible, setHistoryDrawerVisible] = useState(false);
  const [settingsDrawerVisible, setSettingsDrawerVisible] = useState(false);
  const [editorHeight, setEditorHeight] = useState<number>(0); // 动态计算高度

  // 计算并设置编辑器高度
  useEffect(() => {
    const calculateHeight = () => {
      const windowHeight = window.innerHeight;
      const parentPadding = 110; // 父元素Layout的padding
      const newHeight = Math.floor(windowHeight / 2) - parentPadding;
      setEditorHeight(newHeight);
    };

    // 初始计算
    calculateHeight();

    // 监听窗口大小变化
    window.addEventListener('resize', calculateHeight);

    return () => {
      window.removeEventListener('resize', calculateHeight);
    };
  }, []);

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

    // 允许执行部分SQL语句
    return true;
  };

  // 使用防抖的查询执行
  const executeQueryDebounced = useCallback(
    debounce(() => {
      try {
        if (!selectedSource) {
          message.warning('请先选择数据源');
          return;
        }

        // 优先使用选中文本，没有选中则使用全部内容
        let queryToExecute = '';
        if (editorRef.current) {
          const selection = editorRef.current.getSelection();
          const model = editorRef.current.getModel();
          if (model) {
            queryToExecute = selection && !selection.isEmpty() ? model.getValueInRange(selection) : model.getValue();
          }
        }

        // 验证SQL非空
        if (!validateSQL(queryToExecute)) return;

        // 自动添加分号(如果不存在)但保留原始查询格式
        if (queryToExecute.trim() && !queryToExecute.trim().endsWith(';')) {
          queryToExecute = queryToExecute + ';';
        }

        console.log('准备执行SQL:', {
          datasourceId: selectedSource,
          sql: queryToExecute,
          selectedText: queryToExecute,
        });

        setActiveTab('results');
        console.log('执行SQL参数:', {
          datasourceId: selectedSource,
          sql: queryToExecute,
          selectedText: queryToExecute,
        });

        executeQueryOriginal({
          datasourceId: selectedSource,
          sql: queryToExecute,
          selectedText: queryToExecute,
          editor: editorRef.current,
        })
          .then((results: QueryResult) => {
            console.log('查询结果:', results);
            // 后端会自动记录成功查询历史
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
            console.error('执行查询失败 - 完整错误信息:', {
              error: error,
              stack: error.stack,
              selectedSource,
              query: queryToExecute,
              timestamp: new Date().toISOString(),
            });
            message.error(`执行查询失败: ${error.message}`);
          });
      } catch (error) {
        console.error('执行查询过程中发生未捕获的异常:', error);
        message.error('执行查询时发生未知错误');
      }
    }, 300),
    [executeQueryOriginal, selectedSource, sqlQuery, setActiveTab, setXField, setYField],
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
      if (!selectedSource || !sqlQuery) {
        message.warning('缺少必要参数');
        return;
      }

      downloadAsCSV(selectedSource, sqlQuery, 'csv');
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

  // SQL常用片段
  const sqlSnippets = [
    {
      label: '基础SELECT',
      insertText: 'SELECT * FROM ${1:table_name} WHERE ${2:condition};',
      description: '基础查询模板',
    },
    {
      label: 'GROUP BY聚合',
      insertText:
        'SELECT ${1:column}, COUNT(*) as count\nFROM ${2:table_name}\nGROUP BY ${1:column}\nORDER BY count DESC;',
      description: '分组聚合查询',
    },
    // 可扩展更多模板...
  ];

  // SQL函数补全
  const sqlFunctions = [
    { label: 'COUNT', insertText: 'COUNT($1)', detail: '计数函数' },
    { label: 'SUM', insertText: 'SUM($1)', detail: '求和函数' },
    { label: 'AVG', insertText: 'AVG($1)', detail: '平均值函数' },
    { label: 'MAX', insertText: 'MAX($1)', detail: '最大值函数' },
    { label: 'MIN', insertText: 'MIN($1)', detail: '最小值函数' },
    { label: 'CONCAT', insertText: 'CONCAT($1, $2)', detail: '字符串连接' },
  ];

  // SQL关键字上下文补全
  const getSqlKeywords = (context: any) => {
    const baseKeywords = ['SELECT', 'FROM', 'WHERE', 'GROUP BY', 'ORDER BY', 'LIMIT'];
    if (context?.isInSelectClause) {
      return ['DISTINCT', 'AS', ...baseKeywords];
    }
    if (context?.isInFromClause) {
      return ['JOIN', 'LEFT JOIN', 'RIGHT JOIN', 'INNER JOIN', 'CROSS JOIN', ...baseKeywords];
    }
    if (context?.isInWhereClause) {
      return ['AND', 'OR', 'IN', 'EXISTS', 'LIKE', 'BETWEEN', 'IS NULL', 'IS NOT NULL', ...baseKeywords];
    }
    return baseKeywords;
  };

  // SQL片段插入
  const insertSnippet = useCallback((snippet: string) => {
    if (editorRef.current && monacoRef.current) {
      editorRef.current.focus();
      editorRef.current.trigger('keyboard', 'type', { text: '' });
      // 支持snippet格式
      monacoRef.current.editor.getModels()[0]?.applyEdits([
        {
          range: editorRef.current.getSelection() || editorRef.current.getModel()!.getFullModelRange(),
          text: '',
        },
      ]);
      editorRef.current.trigger('snippet', 'insertSnippet', { snippet });
    }
  }, []);

  // 将SnippetSelector移到组件外部并传递props
  const SnippetSelector: React.FC<{ onSelect: (snippet: string) => void }> = ({ onSelect }) => (
    <Dropdown
      menu={{
        items: sqlSnippets.map((snippet) => ({
          key: snippet.label,
          label: snippet.label,
          onClick: () => onSelect(snippet.insertText),
        })),
      }}
      placement="bottomLeft"
    >
      <Button size="small" style={{ marginLeft: 8 }}>
        插入SQL模板
      </Button>
    </Dropdown>
  );

  // 用useCallback包裹回调，减少不必要的重渲染
  const handleTreeNodeDoubleClick = useCallback((tableName: string) => {
    if (editorRef.current) {
      insertTextToEditor(editorRef.current, tableName);
    }
  }, []);

  const handleInsertTable = useCallback(
    (
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
      const safeTableName = tableName.replace(/[^\w\d_]/g, '');
      const sqlContext = getSQLContext(editor);
      try {
        if (!sqlContext.isSelectQuery || editor.getModel()?.getValue().trim() === '') {
          if (columns.length > 0) {
            const fieldList = generateColumnList(columns, {
              addComments: true,
              indentSize: 4,
              multiline: true,
            });
            insertTextToEditor(editor, `SELECT\n${fieldList}\nFROM ${safeTableName};`);
          } else {
            insertTextToEditor(editor, `SELECT * FROM ${safeTableName};`);
          }
          return;
        }
        if (sqlContext.isInSelectClause) {
          const fieldList =
            columns.length > 0 ? generateColumnList(columns, { multiline: false, addComments: false }) : '*';
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
        if (sqlContext.isSelectQuery && !sqlContext.hasFromClause) {
          const model = editor.getModel();
          if (model) {
            const lastLine = model.getLineCount();
            const lastColumn = model.getLineMaxColumn(lastLine);
            const position = { lineNumber: lastLine, column: lastColumn };
            const text = model.getValue();
            const needsSpace = /[^\s,]$/m.test(text);
            const prefix = needsSpace ? ' ' : '';
            editor.executeEdits('insert-from-clause', [
              {
                range: monaco.Range.fromPositions(position, position),
                text: `${prefix}FROM ${safeTableName};`,
              },
            ]);
          }
          return;
        }
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
            if (/FROM\s+[\w\d_]+(\s*,\s*[\w\d_]+)*\s*$/i.test(textBeforeCursor)) {
              const needsComma = !/,\s*$/.test(textBeforeCursor);
              const prefix = needsComma ? ', ' : '';
              insertTextToEditor(editor, `${prefix}${safeTableName}`);
            } else if (/FROM\s*$/i.test(textBeforeCursor)) {
              insertTextToEditor(editor, ` ${safeTableName}`);
            } else {
              insertTextToEditor(editor, ` ${safeTableName}`);
            }
          } else {
            insertTextToEditor(editor, safeTableName);
          }
          return;
        }
        if (sqlContext.isSelectQuery && sqlContext.hasFromClause && !sqlContext.isInWhereClause) {
          const model = editor.getModel();
          if (model) {
            const text = model.getValue();
            const fromIndex = text.toUpperCase().indexOf('FROM');
            const whereIndex = text.toUpperCase().indexOf('WHERE');
            const groupByIndex = text.toUpperCase().indexOf('GROUP BY');
            const orderByIndex = text.toUpperCase().indexOf('ORDER BY');
            const limitIndex = text.toUpperCase().indexOf('LIMIT');
            let insertPos = -1;
            if (whereIndex > fromIndex) insertPos = whereIndex;
            else if (groupByIndex > fromIndex) insertPos = groupByIndex;
            else if (orderByIndex > fromIndex) insertPos = orderByIndex;
            else if (limitIndex > fromIndex) insertPos = limitIndex;
            if (insertPos > -1) {
              const positionInModel = model.getPositionAt(insertPos);
              const position = {
                lineNumber: positionInModel.lineNumber,
                column: positionInModel.column,
              };
              editor.executeEdits('insert-join-clause', [
                {
                  range: monaco.Range.fromPositions(position, position),
                  text: `\nJOIN ${safeTableName} ON \n`,
                },
              ]);
            } else {
              const lastLine = model.getLineCount();
              const lastColumn = model.getLineMaxColumn(lastLine);
              const position = { lineNumber: lastLine, column: lastColumn };
              const lastChar = text.trim().charAt(text.trim().length - 1);
              const removeSemicolon = lastChar === ';';
              let joinText = `\nJOIN ${safeTableName} ON `;
              if (removeSemicolon) {
                const textWithoutSemicolon = text.trim().slice(0, -1);
                joinText = textWithoutSemicolon + joinText;
              }
              editor.executeEdits('insert-join-clause', [
                { range: monaco.Range.fromPositions(position, position), text: joinText },
              ]);
            }
          }
          return;
        }
        insertTextToEditor(editor, safeTableName);
      } catch (error) {
        console.error('SQL插入错误:', error);
        insertTextToEditor(editor, safeTableName);
      }
    },
    [],
  );

  // 补全建议函数用useCallback包裹
  const createCompletionSuggestions = useCallback(
    (
      model: monaco.editor.ITextModel,
      position: monaco.Position,
      schema: SchemaResult | { error: string },
      monaco: typeof import('monaco-editor'),
    ) => {
      const word = model.getWordUntilPosition(position);
      const range = {
        startLineNumber: position.lineNumber,
        endLineNumber: position.lineNumber,
        startColumn: word.startColumn,
        endColumn: word.endColumn,
      };
      const suggestions: monaco.languages.CompletionItem[] = [];
      const isSchemaResult = (s: unknown): s is SchemaResult => {
        return (
          !!s &&
          typeof s === 'object' &&
          'tables' in s &&
          Array.isArray((s as any).tables) &&
          (s as any).tables.every(
            (table: unknown) =>
              table &&
              typeof table === 'object' &&
              'tableName' in table &&
              'tableComment' in table &&
              'columns' in table,
          )
        );
      };
      // 表和字段补全
      if (isSchemaResult(schema)) {
        schema.tables.forEach((table) => {
          suggestions.push({
            label: table.tableName,
            kind: monaco.languages.CompletionItemKind.Class,
            insertText: table.tableName,
            detail: `表: ${table.tableComment ?? table.tableName}`,
            range,
          });
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
      // SQL函数补全
      sqlFunctions.forEach((func) => {
        suggestions.push({
          label: func.label,
          kind: monaco.languages.CompletionItemKind.Function,
          insertText: func.insertText,
          insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
          detail: func.detail,
          range,
        });
      });
      // SQL关键字补全（根据上下文）
      const context = getSQLContext(editorRef.current!);
      getSqlKeywords(context).forEach((keyword) => {
        suggestions.push({
          label: keyword,
          kind: monaco.languages.CompletionItemKind.Keyword,
          insertText: keyword,
          detail: 'SQL关键字',
          range,
        });
      });
      return suggestions;
    },
    [editorRef],
  );

  // 注册编辑器自动完成
  useEffect(() => {
    if (monacoRef.current && databaseSchema) {
      const completionDisposable = monacoRef.current.languages.registerCompletionItemProvider('sql', {
        provideCompletionItems: (model, position) => {
          if (!monacoRef.current) return { suggestions: [] };
          const suggestions = createCompletionSuggestions(model, position, databaseSchema, monacoRef.current);
          return { suggestions };
        },
      });
      return () => {
        completionDisposable.dispose();
      };
    }
  }, [databaseSchema, createCompletionSuggestions]);

  // 修改为自定义封装函数，解决类型问题
  const handleSetChartType = (type: 'bar' | 'line' | 'pie') => {
    setChartType(type as ChartType);
  };

  // 添加侧边栏折叠切换函数
  const toggleSider = useCallback(() => {
    setSiderCollapsed((prev) => !prev);
  }, []);

  // 处理Splitter拖动事件
  const handleSplitterDrag = useCallback((sizes: number[]) => {
    setEditorHeight(sizes[0] - 102); // 更新编辑器高度
  }, []);

  // 编辑器挂载事件
  const handleEditorDidMount = (editor: monaco.editor.IStandaloneCodeEditor, monacoInstance: typeof monaco) => {
    editorRef.current = editor;
    monacoRef.current = monacoInstance;
    // 添加快捷键：Ctrl+Enter 执行查询
    editor.addCommand(monacoInstance.KeyMod.CtrlCmd | monacoInstance.KeyCode.Enter, executeQueryDebounced);
  };

  return (
    <>
      <Layout style={{ height: '100vh', padding: '10px' }}>
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
            <Splitter layout="vertical" onResize={handleSplitterDrag}>
              <Splitter.Panel>
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
                        <SnippetSelector onSelect={insertSnippet} />
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
                  />
                </Card>
              </Splitter.Panel>
              <Splitter.Panel>
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
                  style={{ marginTop: '10px', height: 'calc(100% - 10px)', overflow: 'auto' }}
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
              </Splitter.Panel>
            </Splitter>
          </Content>
        </Layout>
      </Layout>

      <HistoryDrawer
        visible={historyDrawerVisible}
        onClose={() => setHistoryDrawerVisible(false)}
        queryHistory={queryHistory}
        loadFromHistory={loadFromHistory}
        copyToClipboard={copyToClipboard}
        pagination={pagination}
        onPaginationChange={handlePaginationChange}
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
