import { post, get } from './request';

export const getTableColumns = async (
  datasourceId: string,
  module: string,
): Promise<Array<{ columnName: string; dataType: string }>> => {
  return get(`/api/logs/columns`, {
    params: { datasourceId, module },
  }) as Promise<Array<{ columnName: string; dataType: string }>>;
};

// 执行日志检索
export const searchLogs = (params: ISearchLogsParams) => {
  return post('/api/logs/search', params) as Promise<ISearchLogsResponse>;
};

// 旧

export const getTimeDistribution = async (params: {
  datasourceId: number;
  tableName: string;
  timeField: string;
  startTime?: string;
  endTime?: string;
  interval?: string;
}): Promise<Array<{ time: string; count: number }>> => {
  return get(`/api/logs/time-distribution`, {
    params,
  }) as Promise<Array<{ time: string; count: number }>>;
};
