import React from 'react';
import { Card, Tabs, Button } from 'antd';
import ResultsViewer from './ResultsViewer';
import VisualizationPanel from './VisualizationPanel';
import formatTableCell from '../utils/formatters';
import { ChartType, QueryResult } from '../types';
import styles from '../SQLEditorPage.module.less';

export interface SQLResultsPanelProps {
  queryResults: QueryResult | null;
  loading: boolean;
  activeTab: string;
  onTabChange: (key: string) => void;
  onDownloadResults: () => void;
  chartType: ChartType;
  onChartTypeChange: (type: 'bar' | 'line' | 'pie') => void;
  xField: string;
  onXFieldChange: (field: string) => void;
  yField: string;
  onYFieldChange: (field: string) => void;
  fullscreen: boolean;
}

/**
 * SQL结果面板组件
 * 包含查询结果显示和可视化功能
 */
export const SQLResultsPanel: React.FC<SQLResultsPanelProps> = ({
  queryResults,
  loading,
  activeTab,
  onTabChange,
  onDownloadResults,
  chartType,
  onChartTypeChange,
  xField,
  onXFieldChange,
  yField,
  onYFieldChange,
  fullscreen,
}) => {
  // 添加详细调试日志
  // console.log('SQLResultsPanel render:', {
  //   queryResults,
  //   loading,
  //   activeTab,
  //   hasRows: queryResults?.rows?.length,
  //   hasColumns: queryResults?.columns?.length,
  //   status: queryResults?.status,
  //   message: queryResults?.message,
  //   queryResultsType: typeof queryResults,
  //   isQueryResultsNull: queryResults === null,
  //   isQueryResultsUndefined: queryResults === undefined,
  // });

  const tabItems = [
    {
      key: 'results',
      label: '查询结果',
    },
    {
      key: 'visualization',
      label: '可视化',
      disabled: !queryResults?.rows?.length || queryResults?.status === 'error',
    },
  ];

  return (
    <Card
      className={styles.resultsCard}
      extra={
        activeTab === 'results' && (
          <Button
            aria-label="下载查询结果"
            disabled={!queryResults?.rows?.length}
            type="primary"
            onClick={onDownloadResults}
          >
            下载CSV
          </Button>
        )
      }
      style={{ height: '100%', overflow: 'auto' }}
      title={<Tabs activeKey={activeTab} className={styles.resultsTabs} items={tabItems} onChange={onTabChange} />}
    >
      {activeTab === 'results' ? (
        <ResultsViewer
          downloadResults={onDownloadResults}
          formatTableCell={(value) => formatTableCell(value)}
          loading={loading}
          queryResults={queryResults}
        />
      ) : (
        <VisualizationPanel
          chartType={chartType}
          fullscreen={fullscreen}
          queryResults={queryResults}
          setChartType={onChartTypeChange}
          setXField={onXFieldChange}
          setYField={onYFieldChange}
          xField={xField}
          yField={yField}
        />
      )}
    </Card>
  );
};
