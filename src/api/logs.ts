import { post, get } from './request';
import type { DistributionPoint } from '../types/logDataTypes';

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

// 添加日志分布数据获取函数，与 useLogData.ts 中使用的函数签名保持一致
export const getLogDistribution = async (params: {
  datasourceId: number;
  tableName: string;
  keyword?: string;
  whereSql?: string;
  startTime?: string;
  endTime?: string;
  timeGrouping?: string;
}): Promise<DistributionPoint[]> => {
  try {
    // 预处理whereSql，确保它是有效的SQL条件
    let processedWhereSql = params.whereSql;
    
    // 如果whereSql不是有效的SQL条件格式，则将其转换为搜索条件
    if (processedWhereSql && !/\s*[A-Za-z0-9_]+\s*(=|>|<|>=|<=|LIKE|IN|IS|BETWEEN)\s*/.test(processedWhereSql)) {
      // 如果不是SQL条件格式，则清空whereSql
      processedWhereSql = '';
      
      // 如果keyword为空，可以将whereSql的值转移到keyword
      if (!params.keyword && params.whereSql) {
        params = {
          ...params,
          keyword: params.whereSql
        };
      }
    }
    
    // 使用已有的 getTimeDistribution 函数，但转换参数和返回值
    // const response = await get(`/api/logs/time-distribution`, {
    //   params: {
    //     datasourceId: params.datasourceId,
    //     tableName: params.tableName,
    //     timeField: 'log_time', // 假设使用 log_time 字段作为时间字段
    //     startTime: params.startTime,
    //     endTime: params.endTime,
    //     interval: params.timeGrouping || 'minute',
    //     keyword: params.keyword,
    //     whereSql: processedWhereSql
    //   }
    // });
    
    // 转换结果格式以符合 DistributionPoint 类型
    // if (Array.isArray(response)) {
    //   return response.map(item => ({
    //     timePoint: item.time,
    //     count: item.count
    //   }));
    // }
    
    return [];
  } catch (error) {
    console.error('Failed to get log distribution:', error);
    return [];
  }
};
