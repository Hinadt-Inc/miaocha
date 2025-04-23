import { post, get } from './request';

interface SearchLogsParams {
  datasourceId: number;
  tableName: string;
  keyword?: string;
  whereSql?: string;
  startTime?: string;
  endTime?: string;
  timeRange?: string;
  timeGrouping?: string;
  pageSize?: number;
  offset?: number;
  fields?: string[];
}

interface SearchLogsResult {
  success: boolean;
  errorMessage?: string;
  executionTimeMs: number;
  columns: string[];
  rows: Record<string, unknown>[];
  totalCount: number;
  distributionData?: Array<{
    timePoint: string;
    count: number;
  }>;
}

export const searchLogs = async (params: SearchLogsParams): Promise<SearchLogsResult> => {
  return post('/api/logs/search', params);
};

export const getTableColumns = async (datasourceId: string, tableName: string): Promise<Array<{columnName: string; dataType: string}>> => {
  return get(`/api/logs/columns`, {
    params: { datasourceId, tableName }
  }) as Promise<Array<{columnName: string; dataType: string}>>;
};

export const getTimeDistribution = async (params: {
  datasourceId: number;
  tableName: string;
  timeField: string;
  startTime?: string;
  endTime?: string;
  interval?: string;
}): Promise<Array<{time: string; count: number}>> => {
  return get(`/api/logs/time-distribution`, {
    params
  }) as Promise<Array<{time: string; count: number}>>;
};
