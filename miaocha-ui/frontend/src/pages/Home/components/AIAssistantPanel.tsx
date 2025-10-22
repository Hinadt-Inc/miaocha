import React from 'react';
import AIAssistant from '@/components/AIAssistant';

interface IAIAssistantPanelProps {
  searchParams: any;
  state: any;
  searchBarRef: React.MutableRefObject<any>;
  setActiveColumns: (fields: string[]) => void;
  setDetailData: (data: any) => void;
  setSearchParams: (params: any) => void;
  setKeywords: (keywords: string[]) => void;
  setLogTableColumns: (updater: (prev: any) => any) => void;
  executeDataRequest: (params: any) => void;
  getDistributionWithSearchBar: () => void;
  setHistogramData: (data: any) => void;
}

const AIAssistantPanel: React.FC<IAIAssistantPanelProps> = ({
  searchParams,
  state,
  searchBarRef,
  setActiveColumns,
  setDetailData,
  setSearchParams,
  setKeywords,
  setLogTableColumns,
  executeDataRequest,
  getDistributionWithSearchBar,
  setHistogramData,
}) => {
  return (
    <AIAssistant
      searchParams={searchParams as any}
      onFieldSelect={(fields) => {
        setActiveColumns(fields);
      }}
      onLogSearch={(data) => {
        let params = (data as any).searchParams || data;

        if (!params.datasourceId || !params.module) {
          params = {
            ...params,
            datasourceId: params.datasourceId || state.searchParams.datasourceId,
            module: params.module || state.searchParams.module,
          };
        }

        if ((data as any).searchResult) {
          setDetailData((data as any).searchResult);
        }

        setSearchParams(params);

        if (params.keywords && params.keywords.length > 0) {
          setKeywords(params.keywords);
        }

        if (params.whereSqls && params.whereSqls.length > 0) {
          state.setSqls(params.whereSqls);
        } else {
          state.setSqls([]);
        }

        if (searchBarRef.current && params) {
          if (params.startTime && params.endTime && typeof searchBarRef.current.setTimeOption === 'function') {
            const timeOption = {
              label: `${params.startTime} ~ ${params.endTime}`,
              value: `${params.startTime} ~ ${params.endTime}`,
              range: [params.startTime, params.endTime],
              type: 'absolute',
            };
            searchBarRef.current.setTimeOption(timeOption);
          }

          if (params.fields && params.fields.length > 0) {
            setActiveColumns(params.fields);
            setLogTableColumns((prevColumns: any) => {
              return prevColumns.map((column: any) => ({
                ...column,
                selected: params.fields!.includes(column.columnName || ''),
                _createTime: params.fields!.includes(column.columnName || '') ? Date.now() : undefined,
              }));
            });
          }
        }

        if (!(data as any).skipRequest) {
          executeDataRequest(params);

          try {
            const savedSearchParams = localStorage.getItem('searchBarParams');
            const currentParams = savedSearchParams ? JSON.parse(savedSearchParams) : {};
            const updatedParams = {
              ...currentParams,
              ...params,
              datasourceId: params.datasourceId,
              module: params.module,
            };
            localStorage.setItem('searchBarParams', JSON.stringify(updatedParams));
          } catch (error) {
            console.error('Failed to update searchBarParams in localStorage:', error);
          }

          setTimeout(() => {
            getDistributionWithSearchBar();
          }, 100);
        }
      }}
      onTimeRangeChange={(data) => {
        let timeRangeData: any = data as any;
        if (typeof data === 'string') {
          timeRangeData = { timeRange: data };
        }

        if (timeRangeData.histogramData) {
          setHistogramData(timeRangeData.histogramData);
        }

        const newSearchParams = {
          ...searchParams,
          timeRange: timeRangeData.timeRange,
          startTime: timeRangeData.startTime,
          endTime: timeRangeData.endTime,
        };
        setSearchParams(newSearchParams);

        if (!timeRangeData.skipRequest) {
          executeDataRequest(newSearchParams);
        }
      }}
    />
  );
};

export default AIAssistantPanel;
