import React, { useCallback, useEffect, useRef, useState } from 'react';
import { useDataSources } from './hooks/useDataSources';
import { useQueryExecution } from './hooks/useQueryExecution';
import { useEditorSettings } from './hooks/useEditorSettings';
import { useQueryHistory } from './hooks/useQueryHistory';
import { useDatabaseSchema } from './hooks/useDatabaseSchema';
import { 
  Alert,
  Button, 
  Card, 
  Layout,
  message, 
  Space, 
  Tabs,
  Tooltip,
  Empty
} from 'antd';
import { 
  CopyOutlined, 
} from '@ant-design/icons';
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
import PageContainer from '../../components/common/PageContainer';

// 工具和类型导入
import initMonacoEditor from './utils/monacoInit';
import { downloadAsCSV, insertTextToEditor } from './utils/editorUtils';
import formatTableCell from './utils/formatters';
import { 
  ChartType,
  QueryResult,
  SchemaResult,
} from './types';

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
  const { 
    dataSources,
    selectedSource, 
    setSelectedSource,
    loading: loadingDataSources 
  } = useDataSources();

  const { 
    databaseSchema,
    loadingSchema,
    fetchDatabaseSchema 
  } = useDatabaseSchema(selectedSource);

  const { 
    queryResults, 
    sqlQuery, 
    setSqlQuery,
    loading: loadingResults,
    executeQuery: executeQueryOriginal 
  } = useQueryExecution(selectedSource);

  const { 
    settings: editorSettings,
    saveSettings,
  } = useEditorSettings();

  const { 
    history: queryHistory,
    addHistory,
    clearHistory,
  } = useQueryHistory(selectedSource);

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
    [executeQueryOriginal, selectedSource, sqlQuery, setActiveTab, setXField, setYField, addHistory]
  );
  
  // 初始化
  useEffect(() => {
    // 初始化Monaco编辑器
    initMonacoEditor();
    
    // 全屏模式快捷键
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'F11') {
        e.preventDefault();
        setFullscreen(prev => !prev);
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
    }[]
  ) => {
    if (editorRef.current) {
      const selection = editorRef.current.getSelection();
      const isAtStart = selection?.startColumn === 1 && selection?.startLineNumber === 1;
      
      // 如果光标在开头，插入SELECT语句
      if (isAtStart) {
        insertTextToEditor(editorRef.current, `SELECT * FROM ${tableName}`);
      } 
      // 如果已有SELECT语句，插入字段列表
      else if (sqlQuery?.toUpperCase().includes('SELECT')) {
        const columnList = columns.map(col => col.columnName).join(',\n    ');
        insertTextToEditor(editorRef.current, columnList);
      }
      // 其他情况插入表名
      else {
        insertTextToEditor(editorRef.current, tableName);
      }
    }
  };
  
  // 编辑器挂载事件
  const handleEditorDidMount: OnMount = (editor, monacoInstance) => {
    editorRef.current = editor;
    monacoRef.current = monacoInstance;
    
    // 添加快捷键：Ctrl+Enter 执行查询
    editor.addCommand(
      monacoInstance.KeyMod.CtrlCmd | monacoInstance.KeyCode.Enter,
      executeQueryDebounced
    );
    
    // 不返回清理函数，因为Monaco编辑器自己会处理资源释放
  };

  // 创建补全建议
  const createCompletionSuggestions = (
    model: monaco.editor.ITextModel, 
    position: monaco.Position,
    schema: SchemaResult | { error: string },
    monaco: typeof import('monaco-editor')
  ): monaco.languages.CompletionItem[] => {
    const word = model.getWordUntilPosition(position);
    const range = {
      startLineNumber: position.lineNumber,
      endLineNumber: position.lineNumber,
      startColumn: word.startColumn,
      endColumn: word.endColumn
    };
    
    const suggestions: monaco.languages.CompletionItem[] = [];
    
    // 类型谓词函数检查schema是否为SchemaResult
    const isSchemaResult = (s: unknown): s is SchemaResult => {
      return !!s && 
        typeof s === 'object' && 
        'tables' in s && 
        Array.isArray(s.tables) &&
        s.tables.every((table: unknown) => 
          table && 
          typeof table === 'object' && 
          'tableName' in table && 
          'tableComment' in table &&
          'columns' in table
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
          range
        });
        
        // 添加列建议
        if (table.columns && Array.isArray(table.columns)) {
          table.columns.forEach((column) => {
            suggestions.push({
              label: column.columnName,
              kind: monaco.languages.CompletionItemKind.Field,
              insertText: column.columnName,
              detail: `字段: ${column.columnComment ?? column.columnName} (${column.dataType})`,
              range
            });
          });
        }
      });
    }
    
    // 添加SQL关键字
    ['SELECT', 'FROM', 'WHERE', 'GROUP BY', 'ORDER BY', 'LIMIT', 'JOIN', 'LEFT JOIN', 'INNER JOIN'].forEach(keyword => {
      suggestions.push({
        label: keyword,
        kind: monaco.languages.CompletionItemKind.Keyword,
        insertText: keyword,
        detail: 'SQL关键字',
        range
      });
    });
    
    return suggestions;
  };
  
  // 注册编辑器自动完成
  useEffect(() => {
    if (monacoRef.current && databaseSchema) {
      // 移除旧的自动完成提供者
      const completionDisposable = monacoRef.current.languages.registerCompletionItemProvider('sql', {
        provideCompletionItems: (model, position) => {
          if (!monacoRef.current) return { suggestions: [] };
          const suggestions = createCompletionSuggestions(
            model, 
            position, 
            databaseSchema, 
            monacoRef.current
          );
          return { suggestions };
        }
      });
      
      // 组件卸载时清理
      return () => {
        completionDisposable.dispose();
      };
    }
  }, [databaseSchema]);

  // 修改为自定义封装函数，解决类型问题
  const handleSetChartType = (type: "bar" | "line" | "pie") => {
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

  return (
    <PageContainer title="">
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
        toggleFullscreen={() => setFullscreen(prev => !(prev))}
        sqlQuery={sqlQuery}
        fullscreen={fullscreen}
      />

      <Layout className={`layout-content ${fullscreen ? 'fullscreen' : ''}`}>
        <Sider 
          width={250} 
          theme="light" 
          className="sider-container"
        >
          <SchemaTree
            databaseSchema={databaseSchema}
            loadingSchema={loadingSchema}
            refreshSchema={() => {
              void fetchDatabaseSchema(selectedSource);
            }}
            handleTreeNodeDoubleClick={handleTreeNodeDoubleClick}
            handleInsertTable={handleInsertTable}
            fullscreen={fullscreen}
          />
        </Sider>
        
        <Layout className="layout-inner">
          <Content className="content-container">
            <div className="editor-results-container">
              <Card
                title={
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
                }
                className="editor-card"
              >
                <QueryEditor
                  sqlQuery={sqlQuery}
                  onChange={(value) => setSqlQuery(value ?? '')}
                  onEditorMount={handleEditorDidMount}
                  editorSettings={editorSettings}
                  height={editorHeight}
                  minHeight={100}
                  maxHeight={800}
                  collapsed={editorCollapsed}
                  onCollapsedChange={handleEditorCollapsedChange}
                  onHeightChange={handleEditorHeightChange}
                />
              </Card>
              <Card
                title={
                  <Tabs
                    activeKey={activeTab}
                    onChange={(key) => setActiveTab(key)}
                    className="results-tabs"
                  >
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
                    formatTableCell={formatTableCell}
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
        fullscreen={fullscreen}
      />

      <SettingsDrawer
        visible={settingsDrawerVisible}
        onClose={() => setSettingsDrawerVisible(false)}
        editorSettings={editorSettings}
        updateEditorSettings={saveSettings}
      />
    </PageContainer>
  );
};

export default SQLEditorImpl;
