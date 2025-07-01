import React from 'react';
import { Layout } from 'antd';
import {
  SQLEditorHeader,
  SQLEditorSidebar,
  SQLQueryPanel,
  SQLResultsPanel,
  SQLHistoryDrawer,
  SQLSettingsDrawer,
} from './components';
import { useSQLEditorState, useSQLEditorActions, useEditorLayout } from './hooks';
import './SQLEditorPage.less';

const { Content, Sider } = Layout;

/**
 * SQL编辑器主页面
 * 重构后的模块化架构，将原有的复杂组件拆分为多个独立模块
 */
const SQLEditorPage: React.FC = () => {
  // 使用重构后的hooks管理状态和操作
  const editorState = useSQLEditorState();
  const editorActions = useSQLEditorActions(editorState);
  const layoutConfig = useEditorLayout();

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

    // 编辑器相关
    editorSettings,
    editorHeight,

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
    setEditorHeight,

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
    handleTreeNodeDoubleClick,
    handleInsertField,
    handleInsertTable,
  } = editorActions;

  const { siderWidth, siderCollapsed, setSiderCollapsed } = layoutConfig;

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
      <Sider
        width={siderWidth}
        theme="light"
        className="sider-container"
        collapsible
        collapsed={siderCollapsed}
        onCollapse={setSiderCollapsed}
        trigger={null}
      >
        <SQLEditorSidebar
          databaseSchema={databaseSchema}
          loadingSchema={loadingSchema}
          refreshSchema={() => fetchDatabaseSchema(selectedSource)}
          onTreeNodeDoubleClick={handleTreeNodeDoubleClick}
          onInsertTable={handleInsertTable}
          onInsertField={handleInsertField}
          fullscreen={fullscreen}
          collapsed={siderCollapsed}
          onToggle={() => setSiderCollapsed(!siderCollapsed)}
        />
      </Sider>

      <Layout className="layout-inner">
        <Content className="content-container">
          <div className="editor-results-layout">
            <div className="query-panel-container">
              <SQLQueryPanel
                sqlQuery={sqlQuery}
                onChange={handleSqlQueryChange}
                onEditorMount={handleEditorDidMount}
                editorSettings={editorSettings}
                height={editorHeight}
                onHeightChange={setEditorHeight}
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
            </div>

            <div className="results-panel-container">
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
            </div>
          </div>
        </Content>
      </Layout>

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
