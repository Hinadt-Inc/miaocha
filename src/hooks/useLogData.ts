import { useState, useEffect, useCallback, useRef, useMemo } from 'react';
import { message } from 'antd';
import { searchLogs, getLogDistribution } from '../api/logs';
import { LogData } from '../types/logDataTypes';
import { throttle, debounce } from '../utils/logDataHelpers';

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
  totalCount?: number; // 改为可选字段
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
  tableData: LogData[];
  loading: boolean;
  hasMore: boolean;
  loadMoreData: () => void;
  resetData: () => void;
  distributionData: DistributionPoint[];
  error: Error | null;
  totalCount: number;
}

// 用于比较两个查询参数对象是否发生实质性变化的工具函数
const hasQueryParamsChanged = (prev: UseLogDataParams, next: UseLogDataParams): boolean => {
  // 只比较会影响查询结果的关键参数
  const relevantKeys: (keyof UseLogDataParams)[] = [
    'datasourceId',
    'tableName',
    'keyword',
    'whereSql',
    'startTime',
    'endTime',
    'timeRange',
    'timeGrouping',
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

// 创建请求签名，用于识别重复请求
const createRequestSignature = (params: any): string => {
  return JSON.stringify(params);
};

export const useLogData = (queryParams: UseLogDataParams): UseLogDataReturn => {
  // console.log('useLogData - 初始查询参数:', queryParams);
  const [tableData, setTableData] = useState<LogData[]>([]);
  const [loading, setLoading] = useState<boolean>(false);
  const [hasMore, setHasMore] = useState<boolean>(true);
  const [offset, setOffset] = useState<number>(0);
  const [distributionData, setDistributionData] = useState<DistributionPoint[]>([]);
  const [error, setError] = useState<Error | null>(null);
  const [currentParams, setCurrentParams] = useState<UseLogDataParams>(queryParams);
  const [totalCount, setTotalCount] = useState<number>(0);

  // 使用 ref 来跟踪上一次的请求，避免重复请求
  const distributionRequestRef = useRef<string>('');
  const searchRequestRef = useRef<string>('');
  const isInitialLoadRef = useRef<boolean>(true);
  const loadingRef = useRef<boolean>(false);

  // 使用ref跟踪状态，避免闭包问题
  const offsetRef = useRef<number>(offset);
  offsetRef.current = offset;

  const hasMoreRef = useRef<boolean>(hasMore);
  hasMoreRef.current = hasMore;

  // useMemo优化构造查询参数，减少不必要的重新创建
  const constructQueryParams = useMemo(() => {
    return {
      ...queryParams,
      offset: offset,
      startTime: queryParams.startTime,
      endTime: queryParams.endTime,
      fields: queryParams.fields,
    };
  }, [queryParams, offset]);

  // 重置数据 - 使用useCallback优化
  const resetData = useCallback(() => {
    setTableData([]);
    setOffset(0);
    setHasMore(true);
    setError(null);
    // 重置请求跟踪信息
    searchRequestRef.current = '';
    isInitialLoadRef.current = true;
  }, []);

  // 参数变化时只在必要时重置数据，使用useMemo和自定义比较函数减少不必要的重置
  useEffect(() => {
    // 使用hasQueryParamsChanged来判断是否需要重置
    if (hasQueryParamsChanged(currentParams, queryParams)) {
      resetData();
      setCurrentParams(queryParams);
    }
  }, [queryParams, currentParams, resetData]);

  // 优化的分布数据加载函数，修复数据格式问题
  const fetchDistribution = useCallback(async () => {
    // 避免无效请求
    if (
      !queryParams.datasourceId ||
      !queryParams.tableName ||
      !queryParams.startTime ||
      !queryParams.endTime
    ) {
      return;
    }

    // 创建请求签名并检查是否重复
    const requestSignature = createRequestSignature({
      datasourceId: queryParams.datasourceId,
      tableName: queryParams.tableName,
      keyword: queryParams.keyword || '',
      whereSql: queryParams.whereSql || '',
      startTime: queryParams.startTime,
      endTime: queryParams.endTime,
      timeGrouping: queryParams.timeGrouping || 'minute',
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
        timeGrouping: queryParams.timeGrouping || 'minute',
      });

      // 确保response是数组且有数据
      if (Array.isArray(response) && response.length > 0) {
        console.log('获取到分布数据:', response.length);
        // 确保每个元素都有必要的属性
        const formattedData = response.map((item) => ({
          timePoint: item.timePoint || new Date().toISOString(),
          count: typeof item.count === 'number' ? item.count : 0,
        }));

        // 只有当请求签名仍然匹配时才更新状态，避免竞态条件
        if (distributionRequestRef.current === requestSignature) {
          setDistributionData(formattedData);
        }
      } else {
        console.warn('分布数据为空或格式不正确:', response);
        if (distributionRequestRef.current === requestSignature) {
          setDistributionData([]);
        }
      }
    } catch (error) {
      console.error('获取日志分布数据失败:', error);
      if (distributionRequestRef.current === requestSignature) {
        setDistributionData([]);
      }
    }
  }, [
    queryParams.datasourceId,
    queryParams.tableName,
    queryParams.keyword,
    queryParams.whereSql,
    queryParams.startTime,
    queryParams.endTime,
    queryParams.timeGrouping,
  ]);

  // 使用防抖来减少分布数据的请求频率
  const debouncedFetchDistribution = useMemo(
    () => debounce(fetchDistribution, 300),
    [fetchDistribution],
  );

  // 仅在关键查询参数变化时加载分布数据
  useEffect(() => {
    debouncedFetchDistribution();
  }, [debouncedFetchDistribution]);

  // 在 useLogData 钩子中添加调试代码，检查分布数据
  useEffect(() => {
    // 添加调试日志，但删除重复的 debouncedFetchDistribution 调用
    console.log('调试 - 当前分布数据状态:', {
      distributionData,
      hasData: distributionData && distributionData.length > 0,
      distributionDataType: typeof distributionData,
    });
  }, [distributionData]);

  // 优化的日志数据加载函数
  const fetchLogData = useCallback(async () => {
    // 避免无效请求或重复加载
    if (
      loadingRef.current ||
      !queryParams.datasourceId ||
      !queryParams.tableName ||
      !queryParams.startTime ||
      !queryParams.endTime
    ) {
      return;
    }

    // 创建请求签名并检查是否重复
    const params = constructQueryParams;
    const requestSignature = createRequestSignature({
      ...params,
      offset: offsetRef.current,
    });

    // 如果与上一次请求相同，则跳过
    if (requestSignature === searchRequestRef.current && !isInitialLoadRef.current) {
      return;
    }

    // 更新请求状态
    searchRequestRef.current = requestSignature;
    loadingRef.current = true;
    setLoading(true);
    setError(null);

    try {
      const response = await searchLogs(params);

      // 避免竞态条件
      if (searchRequestRef.current !== requestSignature) {
        loadingRef.current = false;
        setLoading(false);
        return;
      }

      // 为每条记录添加唯一ID
      const recordsWithId = (response.rows || []).map(
        (record: Record<string, unknown>, index: number) =>
          ({
            ...record,
            key: `${Date.now()}-${offsetRef.current}-${index}`,
            timestamp: record.timestamp || new Date().toISOString(),
            message: record.message || '',
            host: record.host || '',
            source: record.source || '',
            level: record.level || 'info',
          }) as LogData,
      );

      // 根据offset更新表格数据
      if (offsetRef.current === 0) {
        setTableData(recordsWithId);
      } else {
        setTableData((prev) => [...prev, ...recordsWithId]);
      }
      setDistributionData(response.distributionData || []);
      // 更新分页状态和总数
      setHasMore(response.totalCount > offsetRef.current + (queryParams.pageSize || 50));
      setTotalCount(response.totalCount);

      // 完成初始加载
      isInitialLoadRef.current = false;
    } catch (err) {
      if (searchRequestRef.current === requestSignature) {
        setError(err instanceof Error ? err : new Error('加载数据失败'));
        message.error('加载数据失败');
      }
    } finally {
      if (searchRequestRef.current === requestSignature) {
        loadingRef.current = false;
        setLoading(false);
      }
    }
  }, [constructQueryParams, queryParams.pageSize]);

  // 使用节流来减少日志数据的请求频率
  const throttledFetchLogData = useMemo(() => throttle(fetchLogData, 300), [fetchLogData]);

  // 监听关键参数变化和分页变化来加载数据
  useEffect(() => {
    throttledFetchLogData();
  }, [
    throttledFetchLogData,
    offset,
    queryParams.datasourceId,
    queryParams.tableName,
    queryParams.startTime,
    queryParams.endTime,
  ]);

  // 优化的加载更多函数
  const loadMoreData = useCallback(() => {
    if (!loadingRef.current && hasMoreRef.current) {
      setOffset((prev) => prev + (queryParams.pageSize || 50));
    }
  }, [queryParams.pageSize]);

  return {
    tableData,
    loading,
    hasMore,
    loadMoreData,
    resetData,
    distributionData,
    error,
    totalCount,
  };
};
