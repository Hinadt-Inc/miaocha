import { PageContainer } from '@ant-design/pro-components';
import { executeSQL, downloadResult, getSchema, ExecuteSQLResult } from '../api/sql';
import { getAllDataSources } from '../api/datasource';
import { useState, useEffect, useRef, useMemo, useCallback, lazy, Suspense } from 'react';
import { 
  Input, Select, Button, Space, Table, Tabs, 
  Card, Modal, Form, Drawer, Tree, 
  Tooltip, Badge, Empty, Alert, Tag, Spin, Typography,
  Divider, ConfigProvider, Skeleton, App, Checkbox
} from 'antd';
import { useMessage } from '../hooks/useMessage';
import { 
  PlayCircleOutlined, SaveOutlined, HistoryOutlined,
  DownloadOutlined, CopyOutlined, FileTextOutlined,
  DatabaseOutlined, TableOutlined, ClockCircleOutlined,
  ReloadOutlined, FileSearchOutlined, FullscreenOutlined,
  FullscreenExitOutlined, CodeOutlined, PieChartOutlined,
  DeleteOutlined, InfoCircleOutlined, SettingOutlined
} from '@ant-design/icons';
import Editor, { OnMount, loader, Monaco } from '@monaco-editor/react';
import ReactECharts from 'echarts-for-react';
import VirtualList from 'rc-virtual-list';
import debounce from 'lodash/debounce';
import './SQLEditorPage.less';

// 预加载 Monaco 编辑器
loader.config({
  paths: {
    // 修改为更可靠的CDN源
    vs: 'https://cdn.jsdelivr.net/npm/monaco-editor@0.44.0/min/vs'
  },
  // 添加monaco-editor的CSP标头支持
  'vs/nls': {
    availableLanguages: {
      '*': 'zh-cn'
    }
  },
  // 设置一个超时时间
  'vs/editor/editor.main': {
    timeout: 30000 // 30秒超时
  }
});

// 确保编辑器能够正确初始化
try {
  window.MonacoEnvironment = {
    getWorkerUrl: function (_moduleId, label) {
      if (label === 'sql' || label === 'mysql') {
        return '/monaco-editor/sql.worker.js';
      }
      return '/monaco-editor/editor.worker.js';
    }
  };
  
  loader.init()
    .then(monaco => {
      console.log('Monaco editor 加载成功');
      // 可以在这里配置编辑器的一些全局选项
      monaco.editor.defineTheme('sqlTheme', {
        base: 'vs',
        inherit: true,
        rules: [
          { token: 'keyword', foreground: '0000ff', fontStyle: 'bold' },
          { token: 'string', foreground: 'a31515' },
          { token: 'identifier', foreground: '001080' },
          { token: 'comment', foreground: '008000' },
        ],
        colors: {
          'editor.foreground': '#000000',
          'editor.background': '#F5F5F5',
          'editorCursor.foreground': '#8B0000',
          'editor.lineHighlightBackground': '#F8F8F8',
          'editorLineNumber.foreground': '#2B91AF',
          'editor.selectionBackground': '#ADD6FF',
          'editor.inactiveSelectionBackground': '#E5EBF1'
        }
      });
    })
    .catch(error => {
      console.error('Monaco editor 初始化失败:', error);
      // 尝试备用CDN
      loader.config({
        paths: {
          vs: 'https://unpkg.com/monaco-editor@0.44.0/min/vs'
        }
      });
      return loader.init();
    })
    .catch(error => {
      console.error('Monaco editor 备用CDN初始化也失败:', error);
    });
} catch (error) {
  console.error('Monaco editor 加载失败:', error);
}

const { Option } = Select;
const { Text } = Typography;

// 本地存储的键名
const HISTORY_STORAGE_KEY = 'sql_editor_history';
const SETTINGS_STORAGE_KEY = 'sql_editor_settings';
// 历史记录最大保存数量
const MAX_HISTORY_COUNT = 100;

interface DataSource {
  id: string;
  name: string;
  type: string;
  host: string;
}

interface QueryResult extends ExecuteSQLResult {
  columns?: string[];
  rows?: Record<string, unknown>[];
  total?: number;
  executionTimeMs?: number;
  downloadUrl?: string;
  affectedRows?: number;
}

interface SchemaResult {
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

interface QueryHistory {
  id: string;
  sql: string;
  dataSourceId: string;
  executionTime: number;
  status: 'success' | 'error';
  timestamp: string;
  message?: string;
}

interface EditorSettings {
  fontSize: number;
  theme: string;
  wordWrap: boolean;
  autoComplete: boolean;
  tabSize: number;
  minimap: boolean;
}

export default function SQLEditorPage() {
  const message = useMessage();
  // 使用本地存储保存编辑器设置
  const [editorSettings, setEditorSettings] = useState<EditorSettings>(() => {
    try {
      const storedSettings = localStorage.getItem(SETTINGS_STORAGE_KEY);
      return storedSettings ? JSON.parse(storedSettings) : {
        fontSize: 14,
        theme: 'sqlTheme',
        wordWrap: true,
        autoComplete: true,
        tabSize: 2,
        minimap: false
      };
    } catch (error) {
      console.error('读取编辑器设置失败:', error);
      return {
        fontSize: 14,
        theme: 'sqlTheme',
        wordWrap: true,
        autoComplete: true,
        tabSize: 2,
        minimap: false
      };
    }
  });
  
  const [dataSources, setDataSources] = useState<DataSource[]>([]);
  const [selectedDataSource, setSelectedDataSource] = useState<string>('');
  const editorRef = useRef<any>(null);
  const chartRef = useRef<any>(null);

  const [loadingDataSources, setLoadingDataSources] = useState(false);
  const [loadingSchema, setLoadingSchema] = useState(false);

  // 使用useMemo缓存数据源的选项
  const dataSourceOptions = useMemo(() => (
    dataSources.map(ds => (
      <Option key={ds.id} value={ds.id} label={ds.name}>
        <Space>
          <DatabaseOutlined />
          <span>{ds.name}</span>
          <Tag color="blue">{ds.type}</Tag>
        </Space>
      </Option>
    ))
  ), [dataSources]);

  // 加载数据源
  useEffect(() => {
    const fetchDataSources = async () => {
      // 避免在已经加载中时重复请求
      if (loadingDataSources) return;
      
      setLoadingDataSources(true);
      try {
        const sources = await getAllDataSources();
        setDataSources(sources);
        if (sources.length > 0 && !selectedDataSource) {
          setSelectedDataSource(sources[0].id);
        }
      } catch (error) {
        message.error('加载数据源失败');
        console.error('加载数据源失败:', error);
      } finally {
        // 确保无论成功还是失败，都会重置加载状态
        setLoadingDataSources(false);
      }
    };
    
    // 仅在数据源列表为空时加载
    if (dataSources.length === 0) {
      fetchDataSources();
    }
  }, [message, loadingDataSources, dataSources.length, selectedDataSource]);
  
  const [databaseSchema, setDatabaseSchema] = useState<SchemaResult | null>(null);
  const [sqlQuery, setSqlQuery] = useState<string>('');
  const [queryResults, setQueryResults] = useState<QueryResult | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [activeTab, setActiveTab] = useState<string>('results');
  const [queryHistory, setQueryHistory] = useState<QueryHistory[]>(() => {
    try {
      const storedHistory = localStorage.getItem(HISTORY_STORAGE_KEY);
      return storedHistory ? JSON.parse(storedHistory) : [];
    } catch (error) {
      console.error('读取历史记录失败:', error);
      return [];
    }
  });
  const [saveModalVisible, setSaveModalVisible] = useState<boolean>(false);
  const [historyDrawerVisible, setHistoryDrawerVisible] = useState<boolean>(false);
  const [settingsDrawerVisible, setSettingsDrawerVisible] = useState<boolean>(false);
  const [fullscreen, setFullscreen] = useState<boolean>(false);
  
  // 使用useCallback优化切换全屏的函数
  const toggleFullscreen = useCallback(() => {
    setFullscreen(prev => !prev);
  }, []);
  
  const [form] = Form.useForm();
  const [chartType, setChartType] = useState<'bar' | 'line' | 'pie'>('bar');
  const [xField, setXField] = useState<string>('');
  const [yField, setYField] = useState<string>('');

  // 使用debounce优化编辑器内容更新
  const handleSqlChange = useCallback(
    debounce((value: string | undefined) => {
      setSqlQuery(value ?? '');
    }, 300),
    []
  );

  // 编辑器加载完成的回调
  const handleEditorDidMount: OnMount = useCallback((editor, monaco) => {
    editorRef.current = editor;
    
    // 添加快捷键支持
    editor.addCommand(monaco.KeyMod.CtrlCmd | monaco.KeyCode.Enter, () => {
      executeQuery();
    });
  }, []);

  // 加载数据库结构
  const loadSchema = useCallback(async (datasourceId: string) => {
    if (!datasourceId) return null;
    setLoadingSchema(true);
    
    try {
      const schema = await getSchema(datasourceId);
      message.success('数据库结构加载成功');
      setLoadingSchema(false);
      return schema;
    } catch {
      message.error('获取数据库结构失败');
      setLoadingSchema(false);
      return null;
    }
  }, [message]);

  // 数据源变更时加载结构
  useEffect(() => {
    let isMounted = true;
    
    const loadSchemaData = async () => {
      if (!selectedDataSource) return;
      
      try {
        // 避免在已有数据的情况下重复加载
        if (databaseSchema && databaseSchema.tables.length > 0) return;
        
        // 设置加载状态
        setLoadingSchema(true);
        
        // 加载数据库结构
        const schema = await getSchema(selectedDataSource);
        
        // 检查组件是否仍然挂载
        if (isMounted) {
          setDatabaseSchema(schema);
          message.success('数据库结构加载成功');
        }
      } catch (error) {
        if (isMounted) {
          message.error('获取数据库结构失败');
          console.error('加载结构失败:', error);
        }
      } finally {
        if (isMounted) {
          setLoadingSchema(false);
        }
      }
    };
    
    loadSchemaData();
    
    // 清理函数，防止组件卸载后仍然设置状态
    return () => {
      isMounted = false;
    };
  }, [selectedDataSource, message]);

  // 执行查询
  const executeQuery = useCallback(async () => {
    if (!selectedDataSource) {
      message.error('请选择数据源');
      return;
    }

    if (!sqlQuery.trim()) {
      message.error('SQL查询不能为空');
      return;
    }

    setLoading(true);
    setActiveTab('results');
    setQueryResults(null);

    try {
      const result = await executeSQL({
        datasourceId: selectedDataSource,
        sql: sqlQuery
      });

      if (result && result.status !== 'error') {
        const queryResult: QueryResult = {
          queryId: result.queryId,
          columns: result.columns || [],
          rows: result.rows || [],
          total: result.rows?.length ?? 0,
          executionTimeMs: result.executionTimeMs ?? 0,
          status: 'success'
        };
        setQueryResults(queryResult);
        
        const newHistory: QueryHistory = {
          id: result.queryId,
          sql: sqlQuery,
          dataSourceId: selectedDataSource,
          executionTime: queryResult.executionTimeMs || 0,
          status: 'success',
          timestamp: new Date().toLocaleString()
        };
        setQueryHistory(prev => {
          const updatedHistory = [newHistory, ...prev].slice(0, MAX_HISTORY_COUNT);
          localStorage.setItem(HISTORY_STORAGE_KEY, JSON.stringify(updatedHistory));
          return updatedHistory;
        });
        message.success(`查询成功，耗时 ${queryResult.executionTimeMs} ms`);
        
        // 如果有列和数据，自动设置可视化的默认字段
        if (queryResult.columns && queryResult.columns.length >= 2) {
          setXField(queryResult.columns[0]);
          setYField(queryResult.columns[1]);
        }
      } else {
        setQueryResults({
          queryId: result?.queryId || '',
          columns: [],
          rows: [],
          total: 0,
          executionTimeMs: 0,
          status: 'error',
          message: result?.message || '未知错误'
        });
        
        const newHistory: QueryHistory = {
          id: result?.queryId || new Date().getTime().toString(),
          sql: sqlQuery,
          dataSourceId: selectedDataSource,
          executionTime: 0,
          status: 'error',
          message: result?.message || '未知错误',
          timestamp: new Date().toLocaleString()
        };
        setQueryHistory(prev => {
          const updatedHistory = [newHistory, ...prev].slice(0, MAX_HISTORY_COUNT);
          localStorage.setItem(HISTORY_STORAGE_KEY, JSON.stringify(updatedHistory));
          return updatedHistory;
        });
        message.error(`查询失败: ${result?.message || '未知错误'}`);
      }
    } catch (error) {
      setQueryResults({
        queryId: '',
        columns: [],
        rows: [],
        total: 0,
        executionTimeMs: 0,
        status: 'error',
        message: error instanceof Error ? error.message : '请求失败'
      });
      message.error('查询请求失败');
    } finally {
      setLoading(false);
    }
  }, [selectedDataSource, sqlQuery, message]);

  // 下载查询结果
  const downloadResults = useCallback(async () => {
    if (!queryResults || queryResults.status === 'error') {
      message.warning('没有可下载的数据');
      return;
    }

    try {
      const blob = await downloadResult(queryResults.queryId || '');
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `query_results_${new Date().getTime()}.csv`);
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      message.success('下载成功');
    } catch {
      message.error('下载失败');
    }
  }, [queryResults, message]);

  // 从历史记录中加载SQL
  const loadFromHistory = useCallback((historySql: string) => {
    setSqlQuery(historySql);
    // 同时更新编辑器中的内容
    if (editorRef.current) {
      editorRef.current.setValue(historySql);
    }
    setHistoryDrawerVisible(false);
  }, []);

  // 复制SQL到剪贴板
  const copyToClipboard = useCallback((text: string) => {
    navigator.clipboard.writeText(text)
      .then(() => message.success('已复制到剪贴板'))
      .catch(() => message.error('复制失败'));
  }, [message]);

  // 刷新数据库结构
  const refreshSchema = useCallback(() => {
    if (selectedDataSource) {
      loadSchema(selectedDataSource).then(schema => {
        setDatabaseSchema(schema);
      });
    }
  }, [selectedDataSource, loadSchema]);

  // 表格数据格式化展示
  const formatTableCell = useCallback((value: unknown) => {
    if (value === null) return <Text type="secondary">(null)</Text>;
    if (value === undefined) return <Text type="secondary">(undefined)</Text>;
    if (typeof value === 'object') return <Text code>{JSON.stringify(value)}</Text>;
    return value;
  }, []);

  // 将表名拖拽到编辑器
  const handleTreeNodeDoubleClick = useCallback((tableName: string) => {
    if (editorRef.current) {
      const position = editorRef.current.getPosition();
      editorRef.current.executeEdits('', [
        {
          range: {
            startLineNumber: position.lineNumber,
            startColumn: position.column,
            endLineNumber: position.lineNumber,
            endColumn: position.column
          },
          text: tableName
        }
      ]);
      // 也更新 SQL 查询状态
      setSqlQuery(prev => {
        const position = editorRef.current.getPosition();
        const model = editorRef.current.getModel();
        const lines = model.getLinesContent();
        
        // 在当前位置插入表名
        const line = lines[position.lineNumber - 1];
        const newLine = line.substring(0, position.column - 1) + 
                       tableName + 
                       line.substring(position.column - 1);
        lines[position.lineNumber - 1] = newLine;
        return lines.join('\n');
      });
    }
  }, []);

  // 插入表名和字段
  const handleInsertTable = useCallback((tableName: string, columns: Array<{ columnName: string }>) => {
    if (!editorRef.current) return;
    
    const columnList = columns.map(c => c.columnName).join(', ');
    const snippet = `SELECT ${columnList} FROM ${tableName}`;
    
    const position = editorRef.current.getPosition();
    editorRef.current.executeEdits('', [
      {
        range: {
          startLineNumber: position.lineNumber,
          startColumn: position.column,
          endLineNumber: position.lineNumber,
          endColumn: position.column
        },
        text: snippet
      }
    ]);
    
    // 更新 state 中的 sqlQuery
    setSqlQuery(prev => {
      const model = editorRef.current.getModel();
      return model.getValue();
    });
  }, []);

  // 修改编辑器设置
  const updateEditorSettings = useCallback((newSettings: Partial<EditorSettings>) => {
    setEditorSettings(prev => {
      const updatedSettings = { ...prev, ...newSettings };
      localStorage.setItem(SETTINGS_STORAGE_KEY, JSON.stringify(updatedSettings));
      return updatedSettings;
    });
  }, []);

  // 使用 useMemo 创建图表选项
  const chartOption = useMemo(() => {
    if (!queryResults || queryResults.status !== 'success' || 
        !queryResults.rows || !queryResults.rows.length || 
        !xField || !yField) {
      return {
        title: { text: '暂无数据' },
        tooltip: {},
        xAxis: { type: 'category', data: [] },
        yAxis: { type: 'value' },
        series: [{ type: 'bar', data: [] }]
      };
    }

    const xData = queryResults.rows.map(row => String(row[xField] || ''));
    const yData = queryResults.rows.map(row => {
      const value = row[yField];
      return typeof value === 'number' ? value : parseFloat(String(value)) || 0;
    });

    if (chartType === 'pie') {
      return {
        title: { text: '查询结果可视化' },
        tooltip: { trigger: 'item', formatter: '{a} <br/>{b}: {c} ({d}%)' },
        legend: { orient: 'vertical', right: 10, top: 'center' },
        series: [
          {
            name: '数据',
            type: 'pie',
            radius: ['50%', '70%'],
            avoidLabelOverlap: false,
            label: { show: false },
            emphasis: {
              label: { show: true, fontSize: '16', fontWeight: 'bold' }
            },
            labelLine: { show: false },
            data: xData.map((name, index) => ({ value: yData[index], name }))
          }
        ]
      };
    }

    return {
      title: { text: '查询结果可视化' },
      tooltip: { trigger: 'axis' },
      toolbox: {
        feature: {
          saveAsImage: { title: '保存为图片' }
        }
      },
      xAxis: { type: 'category', data: xData },
      yAxis: { type: 'value' },
      series: [{ 
        name: yField, 
        type: chartType, 
        data: yData,
        label: { show: true, position: 'top' }
      }]
    };
  }, [queryResults, xField, yField, chartType]);

  // 使用useMemo缓存表格列定义
  const tableColumns = useMemo(() => {
    return queryResults?.columns?.map(col => ({
      title: col,
      dataIndex: col,
      key: col,
      render: formatTableCell,
      ellipsis: {
        showTitle: false,
      },
      width: 150,
    })) || [];
  }, [queryResults?.columns, formatTableCell]);

  // 树形结构数据
  const treeData = useMemo(() => {
    if (!databaseSchema) return [];
    
    return databaseSchema.tables.map(table => ({
      title: `${table.tableName}${table.tableComment ? ` (${table.tableComment})` : ''}`,
      key: table.tableName,
      children: table.columns.map(column => ({
        title: `${column.columnName} ${column.isPrimaryKey ? '🔑 ' : ''}(${column.dataType})${column.columnComment ? ` - ${column.columnComment}` : ''}`,
        key: `${table.tableName}-${column.columnName}`,
        isLeaf: true
      }))
    }));
  }, [databaseSchema]);

  // 渲染树节点标题
  const renderTreeNodeTitle = useCallback((node: { key: string; title: string }) => {
    const isTable = node.key.indexOf('-') === -1;
    return (
      <div 
        className="tree-node-wrapper"
        onDoubleClick={() => isTable && handleTreeNodeDoubleClick(node.key)}
      >
        {isTable ? <TableOutlined className="tree-table-icon" /> : 
          <span className="tree-spacer"></span>}
        <Tooltip title={node.title}>
          <span className="tree-node-title">
            {node.title}
          </span>
        </Tooltip>
        {isTable && (
          <Tooltip title="插入表和字段">
            <CopyOutlined 
              className="tree-copy-icon"
              onClick={(e) => {
                e.stopPropagation();
                const table = databaseSchema?.tables.find(t => t.tableName === node.key);
                if (table) {
                  handleInsertTable(table.tableName, table.columns);
                }
              }}
            />
          </Tooltip>
        )}
      </div>
    );
  }, [databaseSchema, handleInsertTable, handleTreeNodeDoubleClick]);

  // 创建副标题，显示所选择的数据源和数据库信息
  const renderSubtitle = useMemo(() => {
    if (!selectedDataSource || !databaseSchema) return '';
    
    const dataSource = dataSources.find(ds => ds.id === selectedDataSource);
    if (!dataSource) return '';
    
    return (
      <Space>
        <DatabaseOutlined />
        <span>{dataSource.name}</span>
        <Tag color="purple">{databaseSchema.databaseName}</Tag>
        <Tag color="blue">{dataSource.type}</Tag>
      </Space>
    );
  }, [selectedDataSource, databaseSchema, dataSources]);

  return (
    <PageContainer 
      className="sql-editor-page" 
      title="SQL 查询编辑器"
      subTitle={renderSubtitle}
      extra={[
        <Button key="settings" icon={<SettingOutlined />} onClick={() => setSettingsDrawerVisible(true)}>
          设置
        </Button>,
        <Button key="fullscreen" icon={fullscreen ? <FullscreenExitOutlined /> : <FullscreenOutlined />} onClick={toggleFullscreen}>
          {fullscreen ? '退出全屏' : '全屏'}
        </Button>
      ]}
    >
      <Card 
        className="sql-editor-card" 
        style={fullscreen ? { position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, margin: 0, zIndex: 1000, borderRadius: 0 } : {}}
      >
        <Space className="toolbar" wrap size="middle">
          <Select
            style={{ width: 300 }}
            placeholder="选择数据源"
            value={selectedDataSource}
            onChange={setSelectedDataSource}
            loading={loadingDataSources}
            disabled={loadingDataSources}
            dropdownStyle={{ maxHeight: 400, overflow: 'auto' }}
            optionLabelProp="label"
            optionFilterProp="label"
            showSearch
          >
            {dataSourceOptions}
          </Select>

          <Tooltip title="执行查询 (Ctrl+Enter)">
            <Button 
              type="primary" 
              icon={<PlayCircleOutlined />}
              onClick={executeQuery}
              loading={loading}
            >
              执行
            </Button>
          </Tooltip>

          <Tooltip title="保存查询">
            <Button 
              icon={<SaveOutlined />}
              onClick={() => setSaveModalVisible(true)}
            >
              保存
            </Button>
          </Tooltip>

          <Tooltip title="查询历史记录">
            <Badge count={queryHistory.length} size="small">
              <Button 
                icon={<HistoryOutlined />}
                onClick={() => setHistoryDrawerVisible(true)}
              >
                历史记录
              </Button>
            </Badge>
          </Tooltip>

          <Tooltip title="下载查询结果为CSV">
            <Button 
              icon={<DownloadOutlined />}
              onClick={downloadResults}
              disabled={!queryResults || queryResults.status === 'error' || !queryResults.rows?.length}
            >
              下载结果
            </Button>
          </Tooltip>
        </Space>

        <div className="main-container" style={{ height: fullscreen ? 'calc(100vh - 130px)' : undefined }}>
          {/* 左侧树形结构 */}
          <div className="schema-tree-container">
            <Card 
              title={
                <Space>
                  <FileSearchOutlined />
                  <span>数据库结构</span>
                  <Tooltip title="刷新数据库结构">
                    <Button 
                      type="text" 
                      size="small" 
                      icon={<ReloadOutlined />} 
                      onClick={refreshSchema}
                      loading={loadingSchema}
                    />
                  </Tooltip>
                </Space>
              } 
              style={{ height: '100%' }}
              bodyStyle={{ padding: '12px 8px', height: 'calc(100% - 57px)', overflowY: 'auto' }}
            >
              {loadingSchema ? (
                <div className="loading-spinner">
                  <Spin tip="加载中..." />
                </div>
              ) : databaseSchema ? (
                <Tree
                  showLine
                  defaultExpandAll={false}
                  titleRender={renderTreeNodeTitle}
                  treeData={treeData}
                  height={fullscreen ? window.innerHeight - 250 : undefined}
                  virtual={treeData.length > 100} // 大数据量时启用虚拟滚动
                />
              ) : (
                <Empty 
                  description="请选择数据源获取数据库结构" 
                  image={Empty.PRESENTED_IMAGE_SIMPLE} 
                />
              )}
            </Card>
          </div>

          {/* 右侧SQL编辑器和结果 */}
          <div className="editor-results-container">
            <div className="editor-wrapper">
              <Editor
                language="sql"
                value={sqlQuery}
                onChange={handleSqlChange}
                onMount={handleEditorDidMount}
                theme={editorSettings.theme}
                options={{
                  minimap: { enabled: editorSettings.minimap },
                  scrollBeyondLastLine: false,
                  folding: true,
                  lineNumbers: 'on',
                  wordWrap: editorSettings.wordWrap ? 'on' : 'off',
                  automaticLayout: true,
                  fontFamily: '"SFMono-Regular", Consolas, "Liberation Mono", Menlo, monospace',
                  fontSize: editorSettings.fontSize,
                  tabSize: editorSettings.tabSize,
                  quickSuggestions: editorSettings.autoComplete,
                  suggestOnTriggerCharacters: editorSettings.autoComplete
                }}
              />
            </div>
            <div className="results-container">
              <Tabs 
                activeKey={activeTab} 
                onChange={setActiveTab}
                type="card"
                items={[
                  {
                    key: 'results',
                    label: (
                      <span>
                        <FileTextOutlined />
                        <span>查询结果</span>
                      </span>
                    ),
                    children: (
                      <div style={{overflow: 'hidden'}}>
                        {loading ? (
                          <div className="query-results-spinner">
                            <Spin tip="执行查询中..." size="large" />
                          </div>
                        ) : queryResults ? (
                          <div>
                            {queryResults.status === 'success' ? (
                              queryResults.rows && queryResults.rows.length > 0 ? (
                                <div>
                                  <div className="results-header">
                                    <Space>
                                      <Badge status="success" text={<Text strong className="success-text">查询成功</Text>} />
                                      <Text>耗时: {queryResults.executionTimeMs} ms</Text>
                                      <Text>总行数: {queryResults.total || queryResults.rows.length}</Text>
                                    </Space>
                                    <Button 
                                      type="text" 
                                      icon={<DownloadOutlined />}
                                      onClick={downloadResults}
                                    >
                                      下载 CSV
                                    </Button>
                                  </div>
                                  <Table
                                    className="results-table"
                                    columns={tableColumns}
                                    dataSource={queryResults.rows.map((row, index) => ({
                                      ...row,
                                      key: `row-${index}`
                                    }))}
                                    pagination={{
                                      pageSize: 20,
                                      showSizeChanger: true,
                                      showTotal: (total) => `共 ${total} 行`,
                                      pageSizeOptions: ['10', '20', '50', '100']
                                    }}
                                    scroll={{ x: 'max-content', y: fullscreen ? window.innerHeight - 350 : 250 }}
                                    size="small"
                                    bordered
                                  />
                                </div>
                              ) : (
                                <Empty description="查询未返回数据" />
                              )
                            ) : (
                              <Alert
                                message="查询执行失败"
                                description={queryResults.message}
                                type="error"
                                showIcon
                              />
                            )}
                          </div>
                        ) : (
                          <Empty description="请执行查询获取结果" />
                        )}
                      </div>
                    )
                  },
                  {
                    key: 'visualization',
                    label: (
                      <span>
                        <PieChartOutlined />
                        <span>可视化</span>
                      </span>
                    ),
                    children: (
                      <div>
                        {queryResults && queryResults.status === 'success' && queryResults.rows && queryResults.rows.length > 0 ? (
                          <>
                            <div className="chart-controls">
                              <Form layout="inline">
                                <Form.Item label="图表类型">
                                  <Select 
                                    value={chartType}
                                    onChange={value => setChartType(value)}
                                    style={{ width: 120 }}
                                  >
                                    <Option value="bar">柱状图</Option>
                                    <Option value="line">折线图</Option>
                                    <Option value="pie">饼图</Option>
                                  </Select>
                                </Form.Item>
                                <Form.Item label="X轴/名称">
                                  <Select 
                                    value={xField}
                                    onChange={setXField}
                                    style={{ width: 120 }} 
                                    placeholder="选择字段"
                                  >
                                    {queryResults.columns?.map(col => (
                                      <Option key={col} value={col}>{col}</Option>
                                    ))}
                                  </Select>
                                </Form.Item>
                                <Form.Item label="Y轴/值">
                                  <Select 
                                    value={yField}
                                    onChange={setYField}
                                    style={{ width: 120 }} 
                                    placeholder="选择字段"
                                  >
                                    {queryResults.columns?.map(col => (
                                      <Option key={col} value={col}>{col}</Option>
                                    ))}
                                  </Select>
                                </Form.Item>
                              </Form>
                            </div>
                            <div className="chart-container">
                              <ReactECharts 
                                ref={chartRef}
                                option={chartOption}
                                style={{ height: fullscreen ? window.innerHeight - 450 : 400 }}
                                opts={{ renderer: 'canvas' }}
                              />
                            </div>
                          </>
                        ) : (
                          <Empty description="需要有查询结果才能创建可视化" />
                        )}
                      </div>
                    )
                  }
                ]}
              />
            </div>
          </div>
        </div>

        <Modal
          title="保存查询"
          open={saveModalVisible}
          onCancel={() => setSaveModalVisible(false)}
          onOk={() => {
            form.validateFields().then(values => {
              // 这里应该有保存查询的逻辑
              message.success(`已保存查询 "${values.name}"`);
              setSaveModalVisible(false);
              form.resetFields();
            });
          }}
        >
          <Form form={form} layout="vertical">
            <Form.Item name="name" label="查询名称" rules={[{ required: true, message: '请输入查询名称' }]}>
              <Input placeholder="我的查询" />
            </Form.Item>
            <Form.Item name="description" label="描述">
              <Input.TextArea placeholder="查询的用途或说明" rows={3} />
            </Form.Item>
          </Form>
        </Modal>

        <Drawer
          title={
            <Space>
              <HistoryOutlined />
              <span>查询历史</span>
              <Tag color="blue">{queryHistory.length} 条记录</Tag>
            </Space>
          }
          width={600}
          open={historyDrawerVisible}
          onClose={() => setHistoryDrawerVisible(false)}
        >
          <div className="history-list">
            {queryHistory.length > 0 ? (
              <VirtualList
                data={queryHistory}
                height={fullscreen ? window.innerHeight - 120 : 500}
                itemHeight={120}
                itemKey="id"
              >
                {(history) => (
                  <div 
                    key={history.id} 
                    className={`history-item ${history.status === 'error' ? 'error-history' : ''}`}
                    onClick={() => loadFromHistory(history.sql)}
                  >
                    <div className="history-item-header">
                      <Space>
                        <ClockCircleOutlined style={{ color: '#1890ff' }}/>
                        <Text>{history.timestamp}</Text>
                      </Space>
                      <Space>
                        {history.status === 'success' ? (
                          <Badge status="success" text={<Text className="history-success">成功</Text>} />
                        ) : (
                          <Badge status="error" text={<Text className="history-error">失败</Text>} />
                        )}
                        {history.status === 'success' && (
                          <Text type="secondary">耗时: {history.executionTime} ms</Text>
                        )}
                        <Button 
                          type="text" 
                          size="small" 
                          icon={<CopyOutlined />} 
                          onClick={(e) => {
                            e.stopPropagation();
                            copyToClipboard(history.sql);
                          }}
                        />
                      </Space>
                    </div>
                    <div className="history-sql">{history.sql}</div>
                    {history.status === 'error' && history.message && (
                      <Alert 
                        message={history.message} 
                        type="error" 
                        showIcon 
                        style={{ marginTop: 8 }} 
                      />
                    )}
                  </div>
                )}
              </VirtualList>
            ) : (
              <Empty description="暂无查询历史记录" />
            )}
          </div>
        </Drawer>

        <Drawer
          title={
            <Space>
              <SettingOutlined />
              <span>编辑器设置</span>
            </Space>
          }
          width={400}
          open={settingsDrawerVisible}
          onClose={() => setSettingsDrawerVisible(false)}
        >
          <Form layout="vertical">
            <Form.Item label="字体大小">
              <Select 
                value={editorSettings.fontSize} 
                onChange={value => updateEditorSettings({ fontSize: value })}
              >
                {[12, 14, 16, 18, 20].map(size => (
                  <Option key={size} value={size}>{size}px</Option>
                ))}
              </Select>
            </Form.Item>
            <Form.Item label="主题">
              <Select 
                value={editorSettings.theme} 
                onChange={value => updateEditorSettings({ theme: value })}
              >
                <Option value="vs">浅色</Option>
                <Option value="vs-dark">深色</Option>
                <Option value="sqlTheme">SQL 主题</Option>
              </Select>
            </Form.Item>
            <Form.Item label="Tab 大小">
              <Select 
                value={editorSettings.tabSize} 
                onChange={value => updateEditorSettings({ tabSize: value })}
              >
                {[2, 4, 8].map(size => (
                  <Option key={size} value={size}>{size} 空格</Option>
                ))}
              </Select>
            </Form.Item>
            <Form.Item>
              <Space direction="vertical" style={{ width: '100%' }}>
                <Checkbox 
                  checked={editorSettings.wordWrap} 
                  onChange={e => updateEditorSettings({ wordWrap: e.target.checked })}
                >
                  自动换行
                </Checkbox>
                <Checkbox 
                  checked={editorSettings.autoComplete} 
                  onChange={e => updateEditorSettings({ autoComplete: e.target.checked })}
                >
                  自动完成
                </Checkbox>
                <Checkbox 
                  checked={editorSettings.minimap} 
                  onChange={e => updateEditorSettings({ minimap: e.target.checked })}
                >
                  显示缩略图
                </Checkbox>
              </Space>
            </Form.Item>
          </Form>
        </Drawer>
      </Card>
    </PageContainer>
  );
}
