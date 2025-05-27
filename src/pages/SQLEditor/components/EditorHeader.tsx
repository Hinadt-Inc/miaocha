import React from 'react';
import { Button, Select, Space, Tooltip } from 'antd';
import { HistoryOutlined, PlayCircleOutlined, SettingOutlined } from '@ant-design/icons';
import { DataSource } from '../types';

const { Option } = Select;

interface EditorHeaderProps {
  dataSources: DataSource[];
  selectedSource: string;
  setSelectedSource: (value: string) => void;
  loadingSchema: boolean;
  loadingDataSources: boolean;
  loadingResults: boolean;
  executeQuery: () => void;
  toggleHistory: () => void;
  toggleSettings: () => void;
  sqlQuery: string;
}

/**
 * SQL编辑器头部组件
 * 包含数据源选择器、执行按钮、历史、设置和全屏按钮
 */
const EditorHeader: React.FC<EditorHeaderProps> = ({
  dataSources,
  selectedSource,
  setSelectedSource,
  loadingSchema,
  loadingDataSources,
  loadingResults,
  executeQuery,
  toggleHistory,
  toggleSettings,
  sqlQuery,
}) => {
  return (
    <div className="sql-editor-header">
      <div className="sql-editor-actions">
        <Space>
          <Select
            placeholder="选择数据源"
            style={{ width: 200 }}
            value={selectedSource}
            onChange={setSelectedSource}
            disabled={loadingSchema}
            loading={loadingDataSources}
            aria-label="数据源选择"
          >
            {dataSources.map((source) => (
              <Option key={source.id} value={source.id}>
                {source.name} ({source.type})
              </Option>
            ))}
          </Select>
          <Tooltip title="执行查询 (Ctrl+Enter)">
            <Button
              type="primary"
              icon={<PlayCircleOutlined />}
              onClick={executeQuery}
              loading={loadingResults}
              disabled={!selectedSource || !sqlQuery.trim()}
              aria-label="执行SQL查询"
            >
              执行
            </Button>
          </Tooltip>
          <Button icon={<HistoryOutlined />} onClick={toggleHistory} aria-label="查看查询历史">
            历史
          </Button>
          <Button icon={<SettingOutlined />} onClick={toggleSettings} aria-label="编辑器设置">
            设置
          </Button>
        </Space>
      </div>
    </div>
  );
};

export default EditorHeader;
