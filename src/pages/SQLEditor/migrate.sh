#!/bin/bash

# SQL编辑器重构迁移脚本
# 用于在原有实现和重构版本之间切换

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
EDITOR_DIR="$SCRIPT_DIR"

echo "SQL编辑器重构迁移工具"
echo "======================="

# 检查当前使用的版本
check_current_version() {
    if grep -q "SQLEditorImpl" "$EDITOR_DIR/SQLEditorPage.tsx"; then
        echo "当前使用: 原有实现 (SQLEditorImpl)"
        return 0
    elif grep -q "useSQLEditorState" "$EDITOR_DIR/SQLEditorPage.tsx"; then
        echo "当前使用: 重构版本 (模块化架构)"
        return 1
    else
        echo "未知版本状态"
        return 2
    fi
}

# 切换到重构版本
switch_to_refactored() {
    echo "正在切换到重构版本..."
    
    # 备份当前文件
    cp "$EDITOR_DIR/SQLEditorPage.tsx" "$EDITOR_DIR/SQLEditorPage.backup.tsx"
    
    # 创建重构版本的内容
    cat > "$EDITOR_DIR/SQLEditorPage.tsx" << 'EOF'
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
 * SQL编辑器主页面 - 重构版本
 * 使用模块化架构，提供更好的可维护性和扩展性
 */
const SQLEditorPage: React.FC = () => {
  const editorState = useSQLEditorState();
  const editorActions = useSQLEditorActions(editorState);
  const layoutConfig = useEditorLayout();

  const {
    dataSources,
    selectedSource,
    loadingDataSources,
    databaseSchema,
    loadingSchema,
    queryResults,
    sqlQuery,
    loadingResults,
    activeTab,
    chartType,
    xField,
    yField,
    fullscreen,
    historyDrawerVisible,
    settingsDrawerVisible,
    editorSettings,
    editorHeight,
    queryHistory,
    pagination,
  } = editorState;

  const {
    setSelectedSource,
    setSqlQuery,
    executeQuery,
    setActiveTab,
    setChartType,
    setXField,
    setYField,
    setHistoryDrawerVisible,
    setSettingsDrawerVisible,
    setEditorHeight,
    saveEditorSettings,
    handleEditorDidMount,
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

  const { siderWidth, siderCollapsed, setSiderCollapsed, handleSplitterDrag } = layoutConfig;

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
          <SQLQueryPanel
            sqlQuery={sqlQuery}
            onChange={setSqlQuery}
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
            onSplitterResize={handleSplitterDrag}
          />

          <SQLResultsPanel
            queryResults={queryResults}
            loading={loadingResults}
            activeTab={activeTab}
            onTabChange={setActiveTab}
            onDownloadResults={handleDownloadResults}
            chartType={chartType}
            onChartTypeChange={setChartType}
            xField={xField}
            onXFieldChange={setXField}
            yField={yField}
            onYFieldChange={setYField}
            fullscreen={fullscreen}
          />
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
EOF

    echo "✅ 已切换到重构版本"
    echo "⚠️  备份文件: SQLEditorPage.backup.tsx"
}

# 切换到原有实现
switch_to_original() {
    echo "正在切换到原有实现..."
    
    # 备份当前文件
    cp "$EDITOR_DIR/SQLEditorPage.tsx" "$EDITOR_DIR/SQLEditorPage.backup.tsx"
    
    # 创建原有实现的内容
    cat > "$EDITOR_DIR/SQLEditorPage.tsx" << 'EOF'
import React from 'react';
import SQLEditorImpl from './SQLEditorImpl';

/**
 * SQL编辑器主页面 - 原有实现
 * 使用SQLEditorImpl组件，保持原有功能稳定
 */
const SQLEditorPage: React.FC = () => {
  return <SQLEditorImpl />;
};

export default SQLEditorPage;
EOF

    echo "✅ 已切换到原有实现"
    echo "⚠️  备份文件: SQLEditorPage.backup.tsx"
}

# 恢复备份
restore_backup() {
    if [ -f "$EDITOR_DIR/SQLEditorPage.backup.tsx" ]; then
        echo "正在恢复备份..."
        cp "$EDITOR_DIR/SQLEditorPage.backup.tsx" "$EDITOR_DIR/SQLEditorPage.tsx"
        echo "✅ 已恢复备份"
    else
        echo "❌ 未找到备份文件"
    fi
}

# 显示状态
show_status() {
    echo ""
    echo "当前状态:"
    check_current_version
    
    if [ -f "$EDITOR_DIR/SQLEditorPage.backup.tsx" ]; then
        echo "备份文件: 存在"
    else
        echo "备份文件: 不存在"
    fi
    
    echo ""
    echo "重构进度:"
    echo "✅ Hooks模块: 已完成"
    echo "✅ 组件模块: 已完成"  
    echo "✅ 文档: 已完成"
    echo "🔄 功能验证: 进行中"
    echo "⏳ 全面切换: 待定"
}

# 主菜单
show_menu() {
    echo ""
    echo "请选择操作:"
    echo "1) 查看当前状态"
    echo "2) 切换到重构版本"
    echo "3) 切换到原有实现" 
    echo "4) 恢复备份"
    echo "5) 退出"
    echo ""
    read -p "输入选项 (1-5): " choice
    
    case $choice in
        1) show_status ;;
        2) switch_to_refactored ;;
        3) switch_to_original ;;
        4) restore_backup ;;
        5) echo "退出"; exit 0 ;;
        *) echo "无效选项" ;;
    esac
}

# 主循环
while true; do
    show_menu
    echo ""
    read -p "按回车继续..."
done
