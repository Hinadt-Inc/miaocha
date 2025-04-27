import React, { useEffect, useRef, useState } from 'react';
import { 
  Button, 
  Card, 
  Col, 
  Divider, 
  Layout,
  message, 
  Row, 
  Select, 
  Space, 
  Tabs,
  Tooltip,
  Typography 
} from 'antd';
import { 
  CopyOutlined, 
  ExpandOutlined, 
  FullscreenExitOutlined, 
  HistoryOutlined, 
  PlayCircleOutlined, 
  SaveOutlined, 
  SettingOutlined, 
  SyncOutlined 
} from '@ant-design/icons';
import { OnMount, useMonaco } from '@monaco-editor/react';
import * as monaco from 'monaco-editor';
import { v4 as uuidv4 } from 'uuid';
import dayjs from 'dayjs';
import copy from 'copy-to-clipboard';

// API 导入
import { getDataSources } from '../../api/datasource';
import { executeSQL, getSchema } from '../../api/sql';

// 组件导入
import SchemaTree from './components/SchemaTree';
import QueryEditor from './components/QueryEditor';
import ResultsViewer from './components/ResultsViewer';
import VisualizationPanel from './components/VisualizationPanel';
import HistoryDrawer from './components/HistoryDrawer';
import SettingsDrawer from './components/SettingsDrawer';
import PageContainer from '../../components/common/PageContainer';

// 工具和类型导入
import initMonacoEditor from './utils/monacoInit';
import formatTableCell from './utils/formatters';
import { 
  DataSource, 
  EditorSettings, 
  HISTORY_STORAGE_KEY, 
  MAX_HISTORY_COUNT,
  QueryHistory, 
  QueryResult, 
  SchemaResult, 
  SETTINGS_STORAGE_KEY 
} from './types';

// 样式导入
import './SQLEditorPage.less';

const { Option } = Select;
const { TabPane } = Tabs;
const { Title } = Typography;
const { Content, Sider } = Layout;

// 默认编辑器设置
const DEFAULT_EDITOR_SETTINGS: EditorSettings = {
  fontSize: 14,
  theme: 'vs',
  wordWrap: true,
  autoComplete: true,
  tabSize: 2,
  minimap: true
};

/**
 * SQL编辑器主组件实现
 * 包含数据库结构展示、SQL编辑器、查询结果显示和可视化等功能
 */
const SQLEditorImpl: React.FC = () => {
  // Monaco编辑器相关状态
  const monacoRef = useRef<typeof monaco | null>(null);
  const editorRef = useRef<monaco.editor.IStandaloneCodeEditor | null>(null);
  const monaco = useMonaco();
  
  // 数据相关状态
  const [dataSources, setDataSources] = useState<DataSource[]>([]);
  const [selectedSource, setSelectedSource] = useState<string>('');
  const [databaseSchema, setDatabaseSchema] = useState<SchemaResult | null>(null);
  const [loadingSchema, setLoadingSchema] = useState<boolean>(false);
  const [sqlQuery, setSqlQuery] = useState<string>('');
  const [queryResults, setQueryResults] = useState<QueryResult | null>(null);
  const [loadingResults, setLoadingResults] = useState<boolean>(false);
  const [activeTab, setActiveTab] = useState<string>('results');
  
  // 可视化相关状态
  const [chartType, setChartType] = useState<'bar' | 'line' | 'pie'>('bar');
  const [xField, setXField] = useState<string>('');
  const [yField, setYField] = useState<string>('');
  
  // 抽屉和全屏相关状态
  const [historyDrawerVisible, setHistoryDrawerVisible] = useState<boolean>(false);
  const [settingsDrawerVisible, setSettingsDrawerVisible] = useState<boolean>(false);
  const [fullscreen, setFullscreen] = useState<boolean>(false);
  const [collapsed, setCollapsed] = useState<boolean>(false);
  
  // 历史记录和设置
  const [queryHistory, setQueryHistory] = useState<QueryHistory[]>([]);
  const [editorSettings, setEditorSettings] = useState<EditorSettings>(DEFAULT_EDITOR_SETTINGS);
  
  // 初始化
  useEffect(() => {
    // 初始化Monaco编辑器
    initMonacoEditor();
    
    // 加载数据源
    fetchDataSources();
    
    // 加载历史记录和设置
    loadFromLocalStorage();
    
    // 全屏模式快捷键
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'F11') {
        e.preventDefault();
        setFullscreen(prev => !prev);
      }
    };
    
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);
  
  // 加载本地存储的历史记录和设置
  const loadFromLocalStorage = () => {
    try {
      // 历史记录
      const savedHistory = localStorage.getItem(HISTORY_STORAGE_KEY);
      if (savedHistory) {
        setQueryHistory(JSON.parse(savedHistory));
      }
      
      // 编辑器设置
      const savedSettings = localStorage.getItem(SETTINGS_STORAGE_KEY);
      if (savedSettings) {
        setEditorSettings(JSON.parse(savedSettings));
      }
    } catch (error) {
      console.error('读取本地存储失败:', error);
    }
  };
  
  // 保存历史记录到本地存储
  const saveHistoryToLocalStorage = (history: QueryHistory[]) => {
    try {
      localStorage.setItem(HISTORY_STORAGE_KEY, JSON.stringify(history));
    } catch (error) {
      console.error('保存历史记录失败:', error);
    }
  };
  
  // 更新编辑器设置
  const updateEditorSettings = (settings: Partial<EditorSettings>) => {
    const newSettings = { ...editorSettings, ...settings };
    setEditorSettings(newSettings);
    try {
      localStorage.setItem(SETTINGS_STORAGE_KEY, JSON.stringify(newSettings));
    } catch (error) {
      console.error('保存设置失败:', error);
    }
  };
  
  // 获取数据源列表
  const fetchDataSources = async () => {
    try {
      const response = await getDataSources();
      if (response) {
        setDataSources(response);
        // 如果有数据源，默认选择第一个并自动获取数据库结构
        if (response.length > 0) {
          const sourceId = response[0].id;
          console.log('自动选择数据源:', sourceId);
          setSelectedSource(sourceId);
          // 确保状态更新后再调用
          setTimeout(() => {
            console.log('开始自动获取数据库结构...');
            fetchDatabaseSchema().catch(e => {
              console.error('自动获取数据库结构失败:', e);
              message.error('自动获取数据库结构失败');
            });
          }, 0);
        }
      }
    } catch (error) {
      console.error('获取数据源失败:', error);
      message.error('获取数据源失败');
    }
  };
  
  // 获取数据库结构
  const fetchDatabaseSchema = async () => {
    if (!selectedSource) {
      message.warning('请先选择数据源');
      return;
    }
    
    setLoadingSchema(true);
    try {
      const response = await getSchema(selectedSource);
      if (response) {
        setDatabaseSchema(response);
      }
    } catch (error) {
      console.error('获取数据库结构失败:', error);
      message.error('获取数据库结构失败');
    } finally {
      setLoadingSchema(false);
    }
  };
  
  // 执行SQL查询
  const executeQuery = async () => {
    if (!selectedSource) {
      message.warning('请先选择数据源');
      return;
    }
    
    if (!sqlQuery.trim()) {
      message.warning('请输入SQL查询语句');
      return;
    }
    
    setLoadingResults(true);
    setQueryResults(null);
    setActiveTab('results');
    
    const startTime = performance.now();
    
    try {
      const response = await executeSQL({
        datasourceId: selectedSource,
        sql: sqlQuery,
        exportResult: false,
        exportFormat: 'xlsx'
      });
      const executionTime = Math.round(performance.now() - startTime);
      
      // 保存到历史记录
      const historyItem: QueryHistory = {
        id: uuidv4(),
        sql: sqlQuery,
        dataSourceId: selectedSource,
        executionTime,
        status: response.status,
        timestamp: dayjs().format('YYYY-MM-DD HH:mm:ss'),
        message: response.message
      };
      
      const newHistory = [historyItem, ...queryHistory];
      if (newHistory.length > MAX_HISTORY_COUNT) {
        newHistory.pop();
      }
      
      setQueryHistory(newHistory);
      saveHistoryToLocalStorage(newHistory);
      
      // 设置查询结果
      const resultsWithTime = {
        ...response,
        executionTimeMs: executionTime
      };
      
      setQueryResults(resultsWithTime);
      
      // 如果查询成功且有结果，设置默认的可视化字段
      console.log('查询结果:', response);
      if (response.rows && response.rows.length > 0 && response.columns) {
        // 默认选择第一列作为 X 轴
        setXField(response.columns[0]);
        
        // 尝试找到一个数值类型的列作为 Y 轴
        const numericColumn = response.columns.find(col => {
          const sampleValue = response.rows?.[0][col];
          return typeof sampleValue === 'number';
        });
        
        setYField(numericColumn || response.columns[1] || response.columns[0]);
      }
    } catch (error) {
      console.error('执行查询失败:', error);
      const errorMessage = error instanceof Error ? error.message : '未知错误';
      
      // 保存错误到历史记录
      const historyItem: QueryHistory = {
        id: uuidv4(),
        sql: sqlQuery,
        dataSourceId: selectedSource,
        executionTime: 0,
        status: 'error',
        timestamp: dayjs().format('YYYY-MM-DD HH:mm:ss'),
        message: errorMessage
      };
      
      const newHistory = [historyItem, ...queryHistory];
      if (newHistory.length > MAX_HISTORY_COUNT) {
        newHistory.pop();
      }
      
      setQueryHistory(newHistory);
      saveHistoryToLocalStorage(newHistory);
      
      // 设置错误结果
      setQueryResults({
        status: 'error',
        message: errorMessage
      });
      
      message.error('执行查询失败');
    } finally {
      setLoadingResults(false);
    }
  };
  
  // 下载查询结果为 CSV
  const downloadResults = () => {
    if (!queryResults || !queryResults.rows || !queryResults.columns) {
      message.warning('没有可下载的结果');
      return;
    }
    
    try {
      // 构造 CSV 内容
      const header = queryResults.columns.join(',');
      const rows = queryResults.rows.map(row => {
        return queryResults.columns?.map(col => {
          const value = row[col];
          if (value === null || value === undefined) return '';
          if (typeof value === 'string') return `"${value.replace(/"/g, '""')}"`;
          return String(value);
        }).join(',');
      });
      
      const csvContent = [header, ...rows].join('\n');
      
      // 创建 Blob 对象
      const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
      
      // 创建下载链接
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `query_results_${dayjs().format('YYYYMMDD_HHmmss')}.csv`);
      document.body.appendChild(link);
      
      // 触发下载
      link.click();
      
      // 清理
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
      
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
  
  // 插入表名到编辑器
  const handleTreeNodeDoubleClick = (tableName: string) => {
    insertTextToEditor(tableName);
  };
  
  // 插入表和字段到编辑器
  const handleInsertTable = (tableName: string, columns: Array<{ columnName: string }>) => {
    if (editorRef.current) {
      const selection = editorRef.current.getSelection();
      const isAtStart = selection?.startColumn === 1 && selection?.startLineNumber === 1;
      
      // 如果光标在开头，插入SELECT语句
      if (isAtStart) {
        insertTextToEditor(`SELECT * FROM ${tableName}`);
      } 
      // 如果已有SELECT语句，插入字段列表
      else if (sqlQuery.toUpperCase().includes('SELECT')) {
        const columnList = columns.map(col => col.columnName).join(',\n    ');
        insertTextToEditor(columnList);
      }
      // 其他情况插入表名
      else {
        insertTextToEditor(tableName);
      }
    }
  };
  
  // 向编辑器插入文本
  const insertTextToEditor = (text: string) => {
    if (editorRef.current) {
      const selection = editorRef.current.getSelection();
      const id = { major: 1, minor: 1 };
      const op = { identifier: id, range: selection, text, forceMoveMarkers: true };
      editorRef.current.executeEdits('insert-text', [op]);
    }
  };
  
  // 编辑器挂载事件
  const handleEditorDidMount: OnMount = (editor, monaco) => {
    editorRef.current = editor;
    monacoRef.current = monaco;
    
    // 添加快捷键：Ctrl+Enter 执行查询
    editor.addCommand(monaco.KeyMod.CtrlCmd | monaco.KeyCode.Enter, executeQuery);
    
    // 添加智能提示
    if (databaseSchema) {
      // 添加表名和字段的自动完成
      databaseSchema.tables.forEach(table => {
        monaco.languages.registerCompletionItemProvider('sql', {
          provideCompletionItems: () => {
            return {
              suggestions: [
                {
                  label: table.tableName,
                  kind: monaco.languages.CompletionItemKind.Class,
                  insertText: table.tableName,
                  detail: `表: ${table.tableComment || table.tableName}`
                },
                ...table.columns.map(column => ({
                  label: column.columnName,
                  kind: monaco.languages.CompletionItemKind.Field,
                  insertText: column.columnName,
                  detail: `字段: ${column.columnComment || column.columnName} (${column.dataType})`
                }))
              ]
            };
          }
        });
      });
    }
  };

  return (
    <PageContainer>
      <div className="sql-editor-header">
        <div className="sql-editor-title">
          <Title level={4}>SQL 编辑器</Title>
        </div>
        <div className="sql-editor-actions">
          <Space>
            <Select
              placeholder="选择数据源"
              style={{ width: 200 }}
              value={selectedSource}
              onChange={value => {
                setSelectedSource(value);
                setDatabaseSchema(null);
              }}
              disabled={loadingSchema}
            >
              {dataSources.map(source => (
                <Option key={source.id} value={source.id}>
                  {source.name} ({source.type})
                </Option>
              ))}
            </Select>
            <Button
              icon={<SyncOutlined />}
              onClick={fetchDatabaseSchema}
              loading={loadingSchema}
              disabled={!selectedSource}
            >
              获取数据库结构
            </Button>
            <Tooltip title="执行查询 (Ctrl+Enter)">
              <Button
                type="primary"
                icon={<PlayCircleOutlined />}
                onClick={executeQuery}
                loading={loadingResults}
                disabled={!selectedSource}
              >
                执行
              </Button>
            </Tooltip>
            <Tooltip title="查询历史">
              <Button
                icon={<HistoryOutlined />}
                onClick={() => setHistoryDrawerVisible(true)}
              >
                历史
              </Button>
            </Tooltip>
            <Tooltip title="编辑器设置">
              <Button
                icon={<SettingOutlined />}
                onClick={() => setSettingsDrawerVisible(true)}
              >
                设置
              </Button>
            </Tooltip>
            <Tooltip title={fullscreen ? '退出全屏 (F11)' : '全屏模式 (F11)'}>
              <Button
                icon={fullscreen ? <FullscreenExitOutlined /> : <ExpandOutlined />}
                onClick={() => setFullscreen(prev => !prev)}
              />
            </Tooltip>
          </Space>
        </div>
      </div>

      <Divider style={{ margin: '8px 0' }} />

      <Layout className={`layout-content ${fullscreen ? 'fullscreen' : ''}`}>
        <Sider 
          width={280} 
          theme="light" 
          className="sider-container"
        >
          <SchemaTree
            databaseSchema={databaseSchema}
            loadingSchema={loadingSchema}
            refreshSchema={fetchDatabaseSchema}
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
                      />
                    </Tooltip>
                    <Tooltip title="保存为模板">
                      <Button
                        type="text"
                        size="small"
                        icon={<SaveOutlined />}
                        onClick={() => message.info('保存模板功能即将推出')}
                      />
                    </Tooltip>
                  </Space>
                }
                className="query-editor-card"
              >
                <QueryEditor
                  sqlQuery={sqlQuery}
                  onChange={value => setSqlQuery(value || '')}
                  onEditorMount={handleEditorDidMount}
                  editorSettings={editorSettings}
                />
              </Card>

              <div className="table-header">
                <div className="results-tabs">
                  <Tabs
                    activeKey={activeTab}
                    onChange={setActiveTab}
                    animated={false}
                    size="small"
                    tabBarStyle={{ marginBottom: 0 }}
                  >
                    <TabPane tab="查询结果" key="results" />
                    <TabPane tab="可视化" key="visualization" disabled={!queryResults} />
                  </Tabs>
                </div>
                <div className="view-controls">
                  {queryResults && queryResults.status === 'success' && 
                   queryResults.rows && queryResults.rows.length > 0 && (
                    <Space>
                      <span>找到 <b>{queryResults.rows.length}</b> 条记录</span>
                      <Button size="small" onClick={downloadResults}>下载CSV</Button>
                    </Space>
                  )}
                </div>
              </div>

              <div className="results-container">
                {activeTab === 'results' ? (
                  <ResultsViewer
                    loading={loadingResults}
                    queryResults={queryResults}
                    downloadResults={downloadResults}
                    formatTableCell={formatTableCell}
                    fullscreen={fullscreen}
                  />
                ) : (
                  <VisualizationPanel
                    queryResults={queryResults}
                    chartType={chartType}
                    setChartType={setChartType}
                    xField={xField}
                    setXField={setXField}
                    yField={yField}
                    setYField={setYField}
                    fullscreen={fullscreen}
                  />
                )}
              </div>
            </div>
          </Content>
        </Layout>
      </Layout>

      {/* 历史记录抽屉 */}
      <HistoryDrawer
        visible={historyDrawerVisible}
        onClose={() => setHistoryDrawerVisible(false)}
        queryHistory={queryHistory}
        loadFromHistory={loadFromHistory}
        copyToClipboard={copyToClipboard}
        fullscreen={fullscreen}
      />

      {/* 设置抽屉 */}
      <SettingsDrawer
        visible={settingsDrawerVisible}
        onClose={() => setSettingsDrawerVisible(false)}
        editorSettings={editorSettings}
        updateEditorSettings={updateEditorSettings}
      />
    </PageContainer>
  );
};

export default SQLEditorImpl;
