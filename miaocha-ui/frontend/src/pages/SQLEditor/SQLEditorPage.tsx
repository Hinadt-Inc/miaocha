import React from 'react';
import { Layout, Splitter } from 'antd';
import {
  SQLEditorHeader,
  SQLEditorSidebar,
  SQLQueryPanel,
  SQLResultsPanel,
  SQLHistoryDrawer,
  SQLSettingsDrawer,
} from './components';
import { useSQLEditorState, useSQLEditorActions } from './hooks';
import styles from './SQLEditorPage.module.less';

const { Content } = Layout;

/**
 * SQL编辑器主页面
 * 重构后的模块化架构，将原有的复杂组件拆分为多个独立模块
 */
const SQLEditorPage: React.FC = () => {
  // 使用重构后的hooks管理状态和操作
  const editorState = useSQLEditorState();
  const editorActions = useSQLEditorActions(editorState);

  const {
    // 数据源相关
    dataSources,
    selectedSource,
    loadingDataSources,

    // 数据库结构相关
    databaseSchema,
    loadingSchema,

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
    handleDownloadResults,
    loadFromHistory,
    copyToClipboard,
    handlePaginationChange,
    insertSnippet,
    handleInsertField,
    handleInsertTable,
  } = editorActions;

  // 处理SQL查询状态更新的包装函数
  const handleSqlQueryChange = (value: string | undefined) => {
    setSqlQuery(value ?? '');
  };

  // 处理图表类型变化的包装函数
  const handleChartTypeChange = (type: 'bar' | 'line' | 'pie') => {
    setChartType(type as any);
  };

  return (
    <Layout style={{ height: '100vh', padding: '10px' }}>
      <Splitter style={{ height: '100%' }}>
        {/* 数据库结构侧边栏 */}
        <Splitter.Panel defaultSize={280} min={200} className={styles.databaseSchemaPanel} resizable>
          <div className={styles.siderContainer}>
            <SQLEditorSidebar
              databaseSchema={databaseSchema}
              loadingSchema={loadingSchema}
              refreshSchema={() => fetchDatabaseSchema(selectedSource)}
              onInsertTable={handleInsertTable}
              onInsertField={handleInsertField}
              collapsed={false}
              onToggle={() => {}}
            />
          </div>
        </Splitter.Panel>

        {/* 主编辑器区域 */}
        <Splitter.Panel className={styles.mainEditorPanel} resizable>
          <Layout className={styles.layoutInner}>
            <Content className={styles.contentContainer}>
              <Splitter layout="vertical" style={{ height: '100%' }}>
                {/* 查询编辑器区域 - 简化高度管理，使用100% */}
                <Splitter.Panel defaultSize="35%" min={200} className={styles.queryPanelContainer}>
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

export default SQLEditorPage;
