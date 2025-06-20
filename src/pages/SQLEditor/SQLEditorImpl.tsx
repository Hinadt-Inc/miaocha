import { useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react';
import { useDataSources } from './hooks/useDataSources';
import { useQueryExecution } from './hooks/useQueryExecution';
import { useEditorSettings } from './hooks/useEditorSettings';
import { useQueryHistory } from './hooks/useQueryHistory';
import { useDatabaseSchema } from './hooks/useDatabaseSchema';
import { Alert, Button, Card, Layout, message, Space, Tabs, Tooltip, Splitter } from 'antd';
import { CopyOutlined } from '@ant-design/icons';
import { EditorView } from '@codemirror/view';
import copy from 'copy-to-clipboard';
import { debounce } from 'lodash';
import { getEditorText } from './utils/selectionUtils';

// 组件导入
import SchemaTree from './components/SchemaTree';
import QueryEditor from './components/QueryEditor';
import ResultsViewer from './components/ResultsViewer';
import VisualizationPanel from './components/VisualizationPanel';
import HistoryDrawer from './components/HistoryDrawer';
import SettingsDrawer from './components/SettingsDrawer';
import EditorHeader from './components/EditorHeader';
import SnippetSelector from './components/SnippetSelector';

// 工具和类型导入
import { downloadAsCSV, insertTextToEditor } from './utils/editorUtils';
import formatTableCell from './utils/formatters';
import { ChartType, QueryResult } from './types';

// 样式导入
import './SQLEditorPage.less';

const { Content, Sider } = Layout;

/**
 * SQL编辑器主组件实现
 * 包含数据库结构展示、SQL编辑器、查询结果显示和可视化等功能
 */
const SQLEditorImpl: React.FC = () => {
  // 使用编辑器引用，添加类型标注
  const editorRef = useRef<EditorView | null>(null);
  // 这里使用 editorView 来存储 CodeMirror 的视图引用

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
  // 动态计算高度，设置合理的初始值与父元素保持一致
  const [editorHeight, setEditorHeight] = useState<number>(Math.floor(window.innerHeight / 2) - 110);

  // 计算并设置编辑器高度
  useEffect(() => {
    const calculateHeight = () => {
      const windowHeight = window.innerHeight;
      const parentPadding = 110; // 父元素Layout的padding
      const newHeight = Math.floor(windowHeight / 2) - parentPadding;
      setEditorHeight(newHeight);
    };

    // 监听窗口大小变化
    window.addEventListener('resize', calculateHeight);

    return () => {
      window.removeEventListener('resize', calculateHeight);
    };
  }, []);

  // 在DOM更新后立即计算高度，确保Splitter面板大小可用
  useLayoutEffect(() => {
    // 使用requestAnimationFrame确保DOM完全渲染后再获取高度
    const calculateInitialHeight = () => {
      const splitterPanel = document.querySelector('.ant-splitter-panel:first-child');
      if (splitterPanel) {
        const panelHeight = splitterPanel.clientHeight;
        if (panelHeight > 0) {
          // 减去编辑器Card的标题和内部padding高度
          const calculatedHeight = Math.max(panelHeight - 102, 100);
          console.log('初始编辑器高度:', calculatedHeight);
          setEditorHeight(calculatedHeight);
        } else {
          // 如果还没有高度，可能是因为还未完全渲染，稍后再试
          requestAnimationFrame(calculateInitialHeight);
        }
      }
    };

    // 在下一帧计算
    requestAnimationFrame(calculateInitialHeight);
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

        // 直接使用 getEditorText 函数获取已清理的文本
        // 该函数会自动判断是否有选中的文本，并会清理首尾空格和换行符
        let queryToExecute = getEditorText(editorRef.current);
        console.log('准备执行SQL:', queryToExecute);

        // 验证SQL非空
        if (!validateSQL(queryToExecute)) return;

        // 自动添加分号(如果不存在)但保留原始查询格式
        if (queryToExecute && !queryToExecute.endsWith(';')) {
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

    if (!selectedSource || !sqlQuery) {
      message.warning('缺少必要参数');
      return;
    }

    downloadAsCSV(selectedSource, sqlQuery, 'csv')
      .then(() => {
        message.success('下载已开始');
      })
      .catch((error) => {
        console.error('下载失败:', error);
        message.error('下载失败');
      });
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

  // 自动完成在CodeMirror配置中处理

  // SQL片段插入
  const insertSnippet = useCallback((snippet: string) => {
    if (editorRef.current) {
      // 使用CodeMirror的insertTextToEditor工具函数
      insertTextToEditor(editorRef.current, snippet);
      // 插入后聚焦编辑器
      editorRef.current.focus();
    }
  }, []);

  // 使用外部SnippetSelector组件

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
      const safeTableName = tableName.replace(/\W/g, '');

      // 简化逻辑，直接插入表名或生成基础查询语句
      if (columns && columns.length > 0) {
        // 生成字段列表字符串
        const columnNames = columns.map((col) => col.columnName).join(', ');
        insertTextToEditor(editor, `SELECT ${columnNames} FROM ${safeTableName};`);
      } else {
        insertTextToEditor(editor, `SELECT * FROM ${safeTableName};`);
      }
    },
    [],
  );

  // CodeMirror使用自己的补全机制，在codeMirrorInit.ts中配置
  // 此处移除Monaco编辑器的补全配置

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
    if (!sizes || !sizes.length) return;

    // 更新编辑器高度，减去编辑器Card的标题和内部padding高度
    // Card标题高度 + Card内部padding + Card顶部edge
    const titleAndPadding = 102;
    const newHeight = sizes[0] - titleAndPadding;

    // 设置最小高度为100px，确保编辑器始终有可用空间
    setEditorHeight(Math.max(newHeight, 100));

    console.log('更新编辑器高度:', newHeight);
  }, []);

  // 编辑器挂载事件
  const handleEditorDidMount = (view: EditorView) => {
    console.log('编辑器挂载成功', view);
    editorRef.current = view;

    // 使用增强选择支持功能
    import('./utils/selectionHelper').then(({ enhanceSelectionSupport }) => {
      enhanceSelectionSupport(view);

      // 在文档加载完成后确保选择功能可用
      setTimeout(() => {
        console.log('编辑器初始化完成，选择功能已就绪');
      }, 500);
    });

    // 为运行按钮添加鼠标进入事件，在执行前确保选择状态正确
    const runButton = document.querySelector('button[aria-label="执行SQL"]');
    if (runButton) {
      runButton.addEventListener('mouseenter', () => {
        // 在即将执行前优化选择状态
        import('./utils/selectionHelper').then(({ finalizeSelection }) => {
          finalizeSelection(view);
        });
      });
    }
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
                        <SnippetSelector onSelect={insertSnippet} snippets={sqlSnippets} />
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
                    <Tabs
                      activeKey={activeTab}
                      onChange={(key) => setActiveTab(key)}
                      className="results-tabs"
                      items={[
                        {
                          key: 'results',
                          label: '查询结果',
                        },
                        {
                          key: 'visualization',
                          label: '可视化',
                          disabled: !queryResults?.rows?.length || queryResults?.status === 'error',
                        },
                      ]}
                    />
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
