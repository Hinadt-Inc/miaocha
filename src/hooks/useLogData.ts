import { useState, useEffect, useCallback, useRef } from 'react';
import { message } from 'antd';
import { searchLogs, getLogDistribution } from '../api/logs';

// 添加必要的类型定义
// LogRecord类型定义
export interface LogRecord {
  _id?: string; // 前端生成的唯一ID
  [key: string]: unknown; // 其他字段
}

// 日志分布点类型定义
export interface DistributionPoint {
  timePoint: string;
  count: number;
}

interface UseLogDataParams {
  datasourceId: number;
  tableName: string;
  keyword: string;
  whereSql: string;
  timeRange?: string;
  timeGrouping?: string;
  pageSize: number;
  offset: number;
  fields: string[];
  startTime?: string;
  endTime?: string;
}

interface UseLogDataReturn {
  tableData: LogRecord[];
  loading: boolean;
  hasMore: boolean;
  loadMoreData: () => void;
  resetData: () => void;
  distributionData: DistributionPoint[];
  error: Error | null;
}

// 用于比较两个查询参数对象是否发生实质性变化的工具函数
const hasQueryParamsChanged = (prev: UseLogDataParams, next: UseLogDataParams): boolean => {
  // 只比较会影响查询结果的关键参数
  const relevantKeys: (keyof UseLogDataParams)[] = [
    'datasourceId', 'tableName', 'keyword', 'whereSql', 
    'startTime', 'endTime', 'timeRange', 'timeGrouping'
  ];
  
  for (const key of relevantKeys) {
    if (prev[key] !== next[key]) {
      // 特殊处理 fields 数组，只关心内容是否相同，不关心顺序
      if (key === 'fields') {
        const prevFields = [...prev.fields].sort();
        const nextFields = [...next.fields].sort();
        if (prevFields.length !== nextFields.length) return true;
        for (let i = 0; i < prevFields.length; i++) {
          if (prevFields[i] !== nextFields[i]) return true;
        }
      } else {
        return true;
      }
    }
  }
  return false;
};

export const useLogData = (queryParams: UseLogDataParams): UseLogDataReturn => {
  const [tableData, setTableData] = useState<LogRecord[]>([]);
  const [loading, setLoading] = useState<boolean>(false);
  const [hasMore, setHasMore] = useState<boolean>(true);
  const [offset, setOffset] = useState<number>(0);
  const [distributionData, setDistributionData] = useState<DistributionPoint[]>([]);
  const [error, setError] = useState<Error | null>(null);
  const [currentParams, setCurrentParams] = useState<UseLogDataParams>(queryParams);
  
  // 使用 ref 来跟踪上一次的请求，避免重复请求
  const distributionRequestRef = useRef<string>('');
  const searchRequestRef = useRef<string>('');

  const constructQueryParams = useCallback(() => {
    return {
      ...queryParams,
      offset: offset,
      startTime: queryParams.startTime,
      endTime: queryParams.endTime,
      fields: queryParams.fields,
    };
  }, [queryParams, offset]);

  // 重置数据
  const resetData = useCallback(() => {
    setTableData([]);
    setOffset(0);
    setHasMore(true);
    setError(null);
  }, []);

  // 参数变化时重置数据，使用自定义比较函数，避免不必要的重置
  useEffect(() => {
    if (hasQueryParamsChanged(currentParams, queryParams)) {
      resetData();
      setCurrentParams(queryParams);
    }
  }, [queryParams, currentParams, resetData]);

  // 加载分布数据，添加防止重复请求的逻辑
  useEffect(() => {
    const fetchDistribution = async () => {
      if (!queryParams.datasourceId || !queryParams.tableName || !queryParams.startTime || !queryParams.endTime) {
        return;
      }

      // 创建一个请求签名，用于识别重复请求
      const requestSignature = JSON.stringify({
        datasourceId: queryParams.datasourceId,
        tableName: queryParams.tableName,
        keyword: queryParams.keyword || '',
        whereSql: queryParams.whereSql || '',
        startTime: queryParams.startTime,
        endTime: queryParams.endTime,
        timeGrouping: queryParams.timeGrouping || 'minute'
      });

      // 如果与上一次请求相同，则跳过
      if (requestSignature === distributionRequestRef.current) {
        return;
      }

      // 更新当前请求签名
      distributionRequestRef.current = requestSignature;

      try {
        // 获取日志分布数据
        const response = await getLogDistribution({
          datasourceId: queryParams.datasourceId,
          tableName: queryParams.tableName,
          keyword: queryParams.keyword,
          whereSql: queryParams.whereSql,
          startTime: queryParams.startTime,
          endTime: queryParams.endTime,
          timeGrouping: queryParams.timeGrouping || 'minute'
        });
        setDistributionData(response);
      } catch (error) {
        console.error('获取日志分布数据失败:', error);
        setDistributionData([]);
      }
    };

    // 只在查询参数变化且有必要时才执行
    fetchDistribution();
  }, [queryParams]); // 简化依赖项，避免重复请求

  // 初始加载和分页加载，添加防止重复请求的逻辑
  useEffect(() => {
    const fetchData = async () => {
      // 必要参数校验
      if (!queryParams.datasourceId || !queryParams.tableName || !queryParams.startTime || !queryParams.endTime) {
        return;
      }

      // 创建一个请求签名，用于识别重复请求
      const params = constructQueryParams();
      const requestSignature = JSON.stringify({
        ...params,
        offset: offset
      });

      // 如果与上一次请求相同，则跳过
      if (requestSignature === searchRequestRef.current) {
        return;
      }

      // 更新当前请求签名
      searchRequestRef.current = requestSignature;

      setLoading(true);
      setError(null);

      try {
        const response = await searchLogs(params);
        console.log('查询结果:', response);
        if (offset === 0) {
          // 为每条记录添加唯一ID
          const recordsWithId = (response.rows || []).map((record: Record<string, unknown>, index: number) => ({
            ...record,
            _id: `${Date.now()}-${index}`
          }));
          setTableData(recordsWithId);
          setDistributionData(response.distributionData || []);
        } else {
          // 为每条记录添加唯一ID，并确保与已有记录ID不重复
          const recordsWithId = (response.rows || []).map((record: Record<string, unknown>, index: number) => ({
            ...record,
            _id: `${Date.now()}-${offset}-${index}`
          }));
          setTableData(prev => [...prev, ...recordsWithId]);
        }
        
        setHasMore(response.totalCount > (offset + (queryParams.pageSize || 50)));
        setLoading(false);
        
      } catch (err) {
        setError(err instanceof Error ? err : new Error('加载数据失败'));
        setLoading(false);
        message.error('加载数据失败');
      }
    };

    fetchData();
  }, [
    offset, 
    constructQueryParams,
    queryParams.datasourceId, 
    queryParams.tableName, 
    queryParams.startTime,
    queryParams.endTime
  ]);

  // 加载更多数据
  const loadMoreData = useCallback(() => {
    if (!loading && hasMore) {
      setOffset(prev => prev + (queryParams.pageSize || 50));
    }
  }, [loading, hasMore, queryParams.pageSize]);

  return {
    tableData,
    loading,
    hasMore,
    loadMoreData,
    resetData,
    distributionData,
    error
  };
};
