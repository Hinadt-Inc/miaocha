import React from 'react';
import { Button, Select, Space, Tooltip } from 'antd';
import { PlayCircleOutlined, HistoryOutlined, SettingOutlined, ReloadOutlined } from '@ant-design/icons';
import styles from '../SQLEditorPage.module.less';

export interface SQLEditorHeaderProps {
  dataSources: { id: string; name: string; type: string }[];
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
    <Space className={styles.editorHeaderActions}>
      <Select
        loading={loadingDataSources}
        options={dataSources.map((ds) => ({
          value: ds.id,
          label: `${ds.name} (${ds.type})`,
        }))}
        placeholder="选择数据源"
        style={{ minWidth: 180 }}
        value={selectedSource}
        onChange={onSourceChange}
      />

      <Tooltip title="执行查询 (Ctrl+Enter)">
        <Button
          disabled={!selectedSource || !sqlQuery.trim()}
          icon={<PlayCircleOutlined />}
          loading={loadingResults}
          type="primary"
          onClick={onExecuteQuery}
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
        <div className={styles.loadingIndicator}>
          <ReloadOutlined spin />
          <span className={styles.loadingText}>加载数据库结构...</span>
        </div>
      )}
    </Space>
  );
};
