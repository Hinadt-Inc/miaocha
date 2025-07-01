import React from 'react';
import { Button, Select, Space, Tooltip } from 'antd';
import { PlayCircleOutlined, HistoryOutlined, SettingOutlined, ReloadOutlined } from '@ant-design/icons';

export interface SQLEditorHeaderProps {
  dataSources: Array<{ id: string; name: string; type: string }>;
  selectedSource: string | null;
  onSourceChange: (sourceId: string) => void;
  loadingSchema: boolean;
  loadingDataSources: boolean;
  loadingResults: boolean;
  onExecuteQuery: () => void;
  onToggleHistory: () => void;
  onToggleSettings: () => void;
  sqlQuery: string;
}

/**
 * SQL编辑器头部组件
 * 包含数据源选择、执行按钮等操作
 */
export const SQLEditorHeader: React.FC<SQLEditorHeaderProps> = ({
  dataSources,
  selectedSource,
  onSourceChange,
  loadingSchema,
  loadingDataSources,
  loadingResults,
  onExecuteQuery,
  onToggleHistory,
  onToggleSettings,
  sqlQuery,
}) => {
  return (
    <Space className="editor-header-actions">
      <Select
        placeholder="选择数据源"
        value={selectedSource}
        onChange={onSourceChange}
        loading={loadingDataSources}
        style={{ minWidth: 180 }}
        options={dataSources.map((ds) => ({
          value: ds.id,
          label: `${ds.name} (${ds.type})`,
        }))}
      />

      <Tooltip title="执行查询 (Ctrl+Enter)">
        <Button
          type="primary"
          icon={<PlayCircleOutlined />}
          onClick={onExecuteQuery}
          loading={loadingResults}
          disabled={!selectedSource || !sqlQuery.trim()}
        >
          执行
        </Button>
      </Tooltip>

      <Tooltip title="查看历史">
        <Button icon={<HistoryOutlined />} onClick={onToggleHistory}>
          历史
        </Button>
      </Tooltip>

      <Tooltip title="编辑器设置">
        <Button icon={<SettingOutlined />} onClick={onToggleSettings}>
          设置
        </Button>
      </Tooltip>

      {loadingSchema && (
        <div className="loading-indicator">
          <ReloadOutlined spin />
          <span className="loading-text">加载数据库结构...</span>
        </div>
      )}
    </Space>
  );
};
