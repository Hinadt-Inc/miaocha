import React, { useCallback } from 'react';
import { Layout, Splitter } from 'antd';
import {
  SQLEditorHeader,
  SQLQueryPanel,
  SQLResultsPanel,
  SQLHistoryDrawer,
  SQLSettingsDrawer,
  OptimizedSQLEditorSidebar,
} from './components';
import { useOptimizedSQLEditorState, useOptimizedSQLEditorActions } from './hooks';
import styles from './SQLEditorPage.module.less';

const { Content } = Layout;

/**
 * 优化的SQL编辑器主页面
 * 使用新的接口拆分架构，支持快速加载表列表和按需获取表详情
 */
const OptimizedSQLEditorPage: React.FC = () => {
  // 使用优化后的hooks管理状态和操作
  const editorState = useOptimizedSQLEditorState();
  const editorActions = useOptimizedSQLEditorActions(editorState);

  const {
    // 数据源相关
    dataSources,
    selectedSource,
    loadingDataSources,

    // 优化的数据库结构相关
    databaseSchema,
    loadingSchema,
    loadingTables,
    fetchTableSchema,

    // 查询相关
    queryResults,
    sqlQuery,
    loadingResults,

    // UI状态
    activeTab,
    chartType,
    xField,
    yField,
    fullscreen,
    historyDrawerVisible,
    settingsDrawerVisible,

    // 编辑器设置
    editorSettings,

    // 查询历史
    queryHistory,
    pagination,
  } = editorState;

  const {
    // 数据源操作
    setSelectedSource,

    // 查询操作
    setSqlQuery,
    executeQuery,

    // UI操作
    setActiveTab,
    setChartType,
    setXField,
    setYField,
    setHistoryDrawerVisible,
    setSettingsDrawerVisible,

    // 编辑器操作
    saveEditorSettings,
    handleEditorDidMount,

    // 其他操作
    fetchDatabaseSchema,
    fetchDatabaseTables,
    handleDownloadResults,
    loadFromHistory,
    copyToClipboard,
    handlePaginationChange,
    insertSnippet,
    handleInsertField,
    handleInsertTable,
  } = editorActions;

  // 处理SQL查询状态更新的包装函数 - 使用useCallback优化性能
  const handleSqlQueryChange = useCallback(
    (value: string | undefined) => {
      const newValue = value ?? '';
      // 避免不必要的状态更新
      if (newValue !== sqlQuery) {
        setSqlQuery(newValue);
      }
    },
    [sqlQuery, setSqlQuery],
  );

  // 处理图表类型变化的包装函数
  const handleChartTypeChange = (type: 'bar' | 'line' | 'pie') => {
    setChartType(type as any);
  };

  // 处理表结构按需加载 - 修复：正确传递fetchTableSchema的当前引用
  const handleFetchTableSchema = useCallback(
    async (tableName: string) => {
      if (selectedSource) {
        return await fetchTableSchema(selectedSource, tableName);
      }
      return null;
    },
    [selectedSource, fetchTableSchema] // 修复：添加fetchTableSchema依赖，确保使用最新的函数引用
  );

  // 处理数据库结构刷新 - 修复：使用快速表列表加载而不是完整结构加载
  const handleRefreshSchema = useCallback(() => {
    if (selectedSource) {
      fetchDatabaseTables(selectedSource);
    }
  }, [selectedSource, fetchDatabaseTables]); // 修复：使用fetchDatabaseTables进行快速刷新

  return (
    <Layout style={{ height: '100vh', padding: '10px' }}>
      <Splitter style={{ height: '100%' }}>
        {/* 优化的数据库结构侧边栏 */}
        <Splitter.Panel defaultSize={280} min={200} max={500} className={styles.databaseSchemaPanel}>
          <div className={styles.siderContainer}>
            <OptimizedSQLEditorSidebar
              databaseSchema={databaseSchema}
              loadingSchema={loadingSchema}
              loadingTables={loadingTables}
              refreshSchema={handleRefreshSchema}
              fetchTableSchema={handleFetchTableSchema}
              selectedSource={selectedSource}
              onInsertTable={handleInsertTable}
              onInsertField={handleInsertField}
              collapsed={false}
              onToggle={() => {}}
            />
          </div>
        </Splitter.Panel>

        {/* 主编辑器区域 */}
        <Splitter.Panel className={styles.mainEditorPanel}>
          <Layout className={styles.layoutInner}>
            <Content className={styles.contentContainer}>
              <Splitter layout="vertical" style={{ height: '100%' }}>
                {/* 查询编辑器区域 - 简化高度管理，使用100% */}
                <Splitter.Panel defaultSize="35%" min="25%" className={styles.queryPanelContainer}>
                  <SQLQueryPanel
                    sqlQuery={sqlQuery}
                    onChange={handleSqlQueryChange}
                    onEditorMount={handleEditorDidMount}
                    editorSettings={editorSettings}
                    height="100%"
                    onHeightChange={() => {}} // 空函数，保持接口兼容
                    onInsertSnippet={insertSnippet}
                    onCopyToClipboard={() => copyToClipboard(sqlQuery)}
                    header={
                      <SQLEditorHeader
                        dataSources={dataSources}
                        selectedSource={selectedSource}
                        onSourceChange={setSelectedSource}
                        loadingSchema={loadingSchema}
                        loadingDataSources={loadingDataSources}
                        loadingResults={loadingResults}
                        onExecuteQuery={executeQuery}
                        onToggleHistory={() => setHistoryDrawerVisible(true)}
                        onToggleSettings={() => setSettingsDrawerVisible(true)}
                        sqlQuery={sqlQuery}
                      />
                    }
                  />
                </Splitter.Panel>

                {/* 查询结果区域 - 获得更多可用空间 */}
                <Splitter.Panel className={styles.resultsPanelContainer}>
                  <SQLResultsPanel
                    queryResults={queryResults}
                    loading={loadingResults}
                    activeTab={activeTab}
                    onTabChange={setActiveTab}
                    onDownloadResults={handleDownloadResults}
                    chartType={chartType}
                    onChartTypeChange={handleChartTypeChange}
                    xField={xField}
                    onXFieldChange={setXField}
                    yField={yField}
                    onYFieldChange={setYField}
                    fullscreen={fullscreen}
                  />
                </Splitter.Panel>
              </Splitter>
            </Content>
          </Layout>
        </Splitter.Panel>
      </Splitter>

      <SQLHistoryDrawer
        visible={historyDrawerVisible}
        onClose={() => setHistoryDrawerVisible(false)}
        queryHistory={queryHistory}
        onLoadFromHistory={loadFromHistory}
        onCopyToClipboard={copyToClipboard}
        pagination={pagination}
        onPaginationChange={handlePaginationChange}
      />

      <SQLSettingsDrawer
        visible={settingsDrawerVisible}
        onClose={() => setSettingsDrawerVisible(false)}
        editorSettings={editorSettings}
        onUpdateSettings={saveEditorSettings}
      />
    </Layout>
  );
};

export default OptimizedSQLEditorPage;
