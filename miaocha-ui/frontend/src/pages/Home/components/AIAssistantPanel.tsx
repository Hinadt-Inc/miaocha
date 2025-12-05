import React, { useCallback } from 'react';

import AIAssistant from '@/components/AIAssistant';

import { useHomeContext } from '../context';
import { useDataInit } from '../hooks/useDataInit';
import { formatTimeString } from '../utils';

const AIAssistantPanel: React.FC = () => {
  const {
    searchParams,
    logTableColumns,
    moduleQueryConfig,
    setLogTableColumns,
    updateSearchParams,
    setDetailData,
    setHistogramData,
  } = useHomeContext();
  const { fetchData, refreshFieldDistributions } = useDataInit();

  const handleLogSearch = useCallback(
    (data: any) => {
      const params = (data as any).searchParams;

      const result: ILogSearchParams = { ...searchParams };
      if (!params.module) {
        result.module = params.module || searchParams.module;
      }

      if (params.fields && params.fields.length > 0) {
        const commonFields = logTableColumns
          .map((item: any) => item.columnName)
          ?.filter((item: any) => !item.includes('.'));
        const newFields = Array.from(new Set([...commonFields, ...params.fields]));
        result.fields = newFields;

        const newLogTableColumns = logTableColumns.map((item: any) => ({
          ...item,
          selected: [moduleQueryConfig?.timeField || 'log_time', ...params.fields].includes(item.columnName),
        }));
        setLogTableColumns(newLogTableColumns);
      }

      if (params.keywords && params.keywords.length > 0) {
        result.keywords = params.keywords;
      }

      if (params.whereSqls && params.whereSqls.length > 0) {
        result.whereSqls = params.whereSqls;
      }

      if (params.sortFields && params.sortFields.length > 0) {
        result.sortFields = params.sortFields;
      }

      if (params.timeRange) {
        result.timeRange = params.timeRange;
      } else if (params.startTime && params.endTime) {
        result.timeRange = `${params.startTime} ~ ${params.endTime}`;
      }
      updateSearchParams(result);

      if (!(data as any).skipRequest) {
        fetchData({ searchParams: result });
      } else if (data.searchResult) {
        const { rows } = data.searchResult;
        const timeField = moduleQueryConfig?.timeField || 'log_time';

        // 为每条记录添加唯一ID并格式化时间字段
        (rows || []).forEach((item: any, index: number) => {
          if (item[timeField]) {
            item[timeField] = formatTimeString(item[timeField] as string);
          }
          item._originalSource = { ...item };
          item._key = `${Date.now()}_${index}`;
        });
        setDetailData(data.searchResult);
      }
      refreshFieldDistributions();
    },
    [searchParams, logTableColumns, moduleQueryConfig, setLogTableColumns],
  );

  return (
    <AIAssistant
      searchParams={searchParams as any}
      onLogSearch={handleLogSearch}
      onTimeRangeChange={(data) => {
        if (data.histogramData) {
          setHistogramData(data.histogramData);
        }
      }}
    />
  );
};

export default AIAssistantPanel;
