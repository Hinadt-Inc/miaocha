import { PageContainer } from '@ant-design/pro-components';
import { executeSQL, downloadResult, getSchema, ExecuteSQLResult } from '../api/sql';
import { getAllDataSources } from '../api/datasource';
import { useState, useEffect, useRef } from 'react';
import { 
  Input, Select, Button, Space, Table, Tabs, 
  Card, Modal, Form, Drawer, Tree, 
  Tooltip, Badge, Empty, Alert, Tag, Spin, Typography
} from 'antd';
import { useMessage } from '../hooks/useMessage';
import { 
  PlayCircleOutlined, SaveOutlined, HistoryOutlined,
  DownloadOutlined, CopyOutlined, FileTextOutlined,
  DatabaseOutlined, TableOutlined, ClockCircleOutlined,
  ReloadOutlined, FileSearchOutlined,
} from '@ant-design/icons';
import Editor, { OnMount, loader } from '@monaco-editor/react';
import ReactECharts from 'echarts-for-react';
import './SQLEditorPage.less';

// 预加载 Monaco 编辑器
loader.init().then(monaco => {
  // 可以在这里配置编辑器的一些全局选项
  monaco.editor.defineTheme('myTheme', {
    base: 'vs',
    inherit: true,
    rules: [],
    // 把颜色配置的炫一些
    colors: {
      'editor.foreground': '#000000',
      'editor.background': '#F5F5F5',
      'editorCursor.foreground': '#8B0000',
      'editor.lineHighlightBackground': '#FFFAFA',
      'editorLineNumber.foreground': '#008080',
      'editor.selectionBackground': '#ADD8E6',
      'editor.inactiveSelectionBackground': '#D3D3D3'
    }
  })
}).catch(error => {
  console.error('Monaco editor 加载失败:', error);
});

const { Option } = Select;
const { Text } = Typography;

// 本地存储的键名
const HISTORY_STORAGE_KEY = 'sql_editor_history';
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

export default function SQLEditorPage() {
  const message = useMessage();
  const [dataSources, setDataSources] = useState<DataSource[]>([]);
  const [selectedDataSource, setSelectedDataSource] = useState<string>('');
  const editorRef = useRef<any>(null);

  const [loadingDataSources, setLoadingDataSources] = useState(false);
  const [loadingSchema, setLoadingSchema] = useState(false);

  // 加载数据源
  useEffect(() => {
    if (loadingDataSources || dataSources.length > 0) return;
    
    const fetchDataSources = async () => {
      setLoadingDataSources(true);
      try {
        const sources = await getAllDataSources();
        setDataSources(sources);
        if (sources.length > 0) {
          setSelectedDataSource(sources[0].id);
        }
      } catch {
        message.error('加载数据源失败');
      } finally {
        setLoadingDataSources(false);
      }
    };
    fetchDataSources();
  }, [loadingDataSources, dataSources.length]);
  
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
  const [mode, setMode] = useState<'normal' | 'nest' | 'fullscreen'>('normal');
  
  const toggleMode = () => {
    setMode(prev => {
      if (prev === 'normal') return 'nest';
      if (prev === 'nest') return 'fullscreen';
      return 'normal';
    });
  };
  
  const [form] = Form.useForm();

  // 编辑器加载完成的回调
  const handleEditorDidMount: OnMount = (editor) => {
    editorRef.current = editor;
  };

  // 加载数据库结构
  const loadSchema = async (datasourceId: string) => {
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
  };

  // 数据源变更时加载结构
  useEffect(() => {
    if (selectedDataSource) {
      loadSchema(selectedDataSource).then(schema => {
        setDatabaseSchema(schema);
      });
    } else {
      setDatabaseSchema(null);
    }
  }, [selectedDataSource]);

  // 执行查询
  const executeQuery = async () => {
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
          executionTime: result.executionTimeMs ?? 0,
          status: 'success'
        };
        setQueryResults(queryResult);
        
        const newHistory: QueryHistory = {
          id: result.queryId,
          sql: sqlQuery,
          dataSourceId: selectedDataSource,
          executionTime: queryResult.executionTime || 0,
          status: 'success',
          timestamp: new Date().toLocaleString()
        };
        setQueryHistory(prev => {
          const updatedHistory = [newHistory, ...prev].slice(0, MAX_HISTORY_COUNT);
          localStorage.setItem(HISTORY_STORAGE_KEY, JSON.stringify(updatedHistory));
          return updatedHistory;
        });
        message.success(`查询成功，耗时 ${queryResult.executionTime} ms`);
      } else {
        setQueryResults({
          queryId: result?.queryId || '',
          columns: [],
          rows: [],
          total: 0,
          executionTime: 0,
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
        executionTime: 0,
        status: 'error',
        message: error instanceof Error ? error.message : '请求失败'
      });
      message.error('查询请求失败');
    } finally {
      setLoading(false);
    }
  };

  // 下载查询结果
  const downloadResults = async () => {
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
  };

  // 从历史记录中加载SQL
  const loadFromHistory = (historySql: string) => {
    setSqlQuery(historySql);
    setHistoryDrawerVisible(false);
  };

  // 复制SQL到剪贴板
  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text)
      .then(() => message.success('已复制到剪贴板'))
      .catch(() => message.error('复制失败'));
  };

  // 刷新数据库结构
  const refreshSchema = () => {
    if (selectedDataSource) {
      loadSchema(selectedDataSource).then(schema => {
        setDatabaseSchema(schema);
      });
    }
  };

  // 表格数据格式化展示
  const formatTableCell = (value: unknown) => {
    if (value === null) return <Text type="secondary">(null)</Text>;
    if (value === undefined) return <Text type="secondary">(undefined)</Text>;
    if (typeof value === 'object') return <Text code>{JSON.stringify(value)}</Text>;
    return value;
  };

  // 将表名拖拽到编辑器
  const handleTreeNodeDoubleClick = (tableName: string) => {
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
    }
  };

  // 插入表名和字段
  const handleInsertTable = (tableName: string, columns: Array<{ columnName: string }>) => {
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
  };

  return (
    <PageContainer className="sql-editor-page">
      <Card className="sql-editor-card">
        <Space className="toolbar" wrap size="middle" style={{ marginBottom: 16 }}>
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
          >
            {dataSources.map(ds => (
              <Option key={ds.id} value={ds.id} label={ds.name}>
                <Space>
                  <DatabaseOutlined />
                  <span>{ds.name}</span>
                  <Tag color="blue">{ds.type}</Tag>
                </Space>
              </Option>
            ))}
          </Select>

          <Tooltip title="执行查询 (Ctrl+Enter)" open={mode === 'nest' || mode === 'fullscreen'}>
            <Button 
              type="primary" 
              icon={<PlayCircleOutlined />}
              onClick={executeQuery}
              loading={loading}
            >
              执行
            </Button>
          </Tooltip>

          {/* <Tooltip title="切换模式" open={mode === 'nest' || mode === 'fullscreen'}>
            <Button 
              icon={mode === 'fullscreen' ? <FullscreenExitOutlined /> : <FullscreenOutlined />}
              onClick={toggleMode}
            >
              {mode === 'normal' ? '普通' : mode === 'nest' ? '嵌套' : '全屏'}
            </Button>
          </Tooltip> */}

          <Tooltip title="保存查询" open={mode === 'nest' || mode === 'fullscreen'}>
            <Button 
              icon={<SaveOutlined />}
              onClick={() => setSaveModalVisible(true)}
            >
              保存
            </Button>
          </Tooltip>

            <Tooltip title="查询历史记录" open={mode === 'nest' || mode === 'fullscreen'}>
              <Badge count={queryHistory.length} size="small">
                <Button 
                  icon={<HistoryOutlined />}
                  onClick={() => setHistoryDrawerVisible(true)}
                >
                  历史记录
                </Button>
              </Badge>
            </Tooltip>

          <Tooltip title="下载查询结果为CSV" open={mode === 'nest' || mode === 'fullscreen'}>
            <Button 
              icon={<DownloadOutlined />}
              onClick={downloadResults}
              disabled={!queryResults || queryResults.status === 'error' || !queryResults.rows?.length}
            >
              下载结果
            </Button>
          </Tooltip>
        </Space>

        <div style={{ display: 'flex', height: 'calc(100vh - 180px)' }}>
          {/* 左侧树形结构 */}
          <div style={{ width: 300, marginRight: 16 }}>
            <Card 
              title={
                <Space>
                  <FileSearchOutlined />
                  <span>数据库结构</span>
                  <Tooltip title="刷新数据库结构" open={mode === 'nest' || mode === 'fullscreen'}>
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
              style={{ height: '100%', overflow: 'auto' }}
            >
              {loadingSchema ? (
                <div style={{ textAlign: 'center', padding: '40px 0' }}>
                  <Spin tip="加载中..." />
                </div>
              ) : databaseSchema ? (
                <Tree
                  showLine
                  defaultExpandAll={false}
                  titleRender={(node: { key: string; title: string }) => {
                    const isTable = node.key.indexOf('-') === -1;
                    return (
                      <div 
                        onDoubleClick={() => isTable && handleTreeNodeDoubleClick(node.key)}
                        style={{ display: 'flex', alignItems: 'center' }}
                      >
                        {isTable ? <TableOutlined style={{ marginRight: 8 }} /> : 
                          <span style={{ width: 16, display: 'inline-block', marginRight: 8 }}></span>}
                        <Tooltip title={node.title} open={mode === 'nest' || mode === 'fullscreen'}>
                          <span className="tree-node-title" style={{ maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis' }}>
                            {node.title}
                          </span>
                        </Tooltip>
                        {isTable && (
                          <Tooltip title="插入表和字段" open={mode === 'nest' || mode === 'fullscreen'}>
                            <CopyOutlined 
                              style={{ marginLeft: 8, cursor: 'pointer', opacity: 0.6 }}
                              onClick={(e) => {
                                e.stopPropagation();
                                const table = databaseSchema.tables.find(t => t.tableName === node.key);
                                if (table) {
                                  handleInsertTable(table.tableName, table.columns);
                                }
                              }}
                            />
                          </Tooltip>
                        )}
                      </div>
                    );
                  }}
                  treeData={databaseSchema.tables.map(table => ({
                    title: `${table.tableName}${table.tableComment ? ` (${table.tableComment})` : ''}`,
                    key: table.tableName,
                    children: table.columns.map(column => ({
                      title: `${column.columnName} ${column.isPrimaryKey ? '🔑 ' : ''}(${column.dataType})${column.columnComment ? ` - ${column.columnComment}` : ''}`,
                      key: `${table.tableName}-${column.columnName}`,
                      isLeaf: true
                    }))
                  }))}
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
          <div style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
            <div className="editor-container" style={{ height: '40%', marginBottom: 16 }}>
              <Editor
                language="sql"
                value={sqlQuery}
                onChange={value => setSqlQuery(value ?? '')}
                onMount={handleEditorDidMount}
                options={{
                  minimap: { enabled: false },
                  scrollBeyondLastLine: false,
                  folding: true,
                  lineNumbers: 'on',
                  wordWrap: 'on',
                  automaticLayout: true,
                  fontFamily: '"SFMono-Regular", Consolas, "Liberation Mono", Menlo, monospace',
                  fontSize: 14
                }}
              />
            </div>
            <div className="results-container" style={{ flex: 1 }}>
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
                          <div style={{ textAlign: 'center', padding: '80px 0' }}>
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
                                      <Text>耗时: {queryResults.executionTime} ms</Text>
                                      <Text>总行数: {queryResults.rows.length}</Text>
                                    </Space>
                                  </div>
                                  <Table
                                    columns={queryResults.columns?.map(col => ({
                                      title: col,
                                      dataIndex: col,
                                      key: col,
                                      render: formatTableCell,
                                      ellipsis: {
                                        showTitle: false,
                                      },
                                      width: 150,
                                    }))}
                                    dataSource={queryResults.rows.map((row, index) => ({
                                      ...row,
                                      key: `row-${index}`
                                    }))}
                                    pagination={{
                                      pageSize: 20,
                                      showSizeChanger: true,
                                      showTotal: (total) => `共 ${total} 行`
                                    }}
                                    scroll={{ x: 'max-content', y: 250 }}
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
                        <span>可视化</span>
                      </span>
                    ),
                    children: (
                      <div>
                        {queryResults && queryResults.status === 'success' && queryResults.rows && queryResults.rows.length > 0 ? (
                          <>
                            <div className="chart-controls" style={{ marginBottom: 16 }}>
                              <Form layout="inline">
                                <Form.Item label="图表类型">
                                  <Select defaultValue="bar" style={{ width: 120 }}>
                                    <Option value="bar">柱状图</Option>
                                    <Option value="line">折线图</Option>
                                    <Option value="pie">饼图</Option>
                                  </Select>
                                </Form.Item>
                                <Form.Item label="X轴">
                                  <Select style={{ width: 120 }} placeholder="选择字段">
                                    {queryResults.columns?.map(col => (
                                      <Option key={col} value={col}>{col}</Option>
                                    ))}
                                  </Select>
                                </Form.Item>
                                <Form.Item label="Y轴">
                                  <Select style={{ width: 120 }} placeholder="选择字段">
                                    {queryResults.columns?.map(col => (
                                      <Option key={col} value={col}>{col}</Option>
                                    ))}
                                  </Select>
                                </Form.Item>
                              </Form>
                            </div>
                            <ReactECharts 
                              option={{
                                title: { text: '查询结果可视化' },
                                tooltip: {},
                                xAxis: { type: 'category', data: [] },
                                yAxis: { type: 'value' },
                                series: [{ type: 'bar', data: [] }]
                              }} 
                              style={{ height: 400 }}
                            />
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
          style={{ padding: '12px' }}
        >
          <div className="history-list">
            {queryHistory.length > 0 ? (
              queryHistory.map(history => (
                <div 
                  key={history.id} 
                  className={`history-item ${history.status === 'error' ? 'error-history' : ''}`}
                  onClick={() => loadFromHistory(history.sql)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' || e.key === ' ') {
                      loadFromHistory(history.sql);
                    }
                  }}
                  style={{ 
                    cursor: 'pointer',
                    background: 'none',
                    border: 'none',
                    width: '100%',
                    textAlign: 'left',
                    padding: 0
                  }}
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
              ))
            ) : (
              <Empty description="暂无查询历史记录" />
            )}
          </div>
        </Drawer>
      </Card>
    </PageContainer>
  );
}
