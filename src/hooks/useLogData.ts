import { useState, useEffect, useCallback, useRef, useMemo } from 'react';
import { LogData } from '../types/logDataTypes';
import { searchLogs } from '../api/logs';
import { generateMockData } from '../utils/logDataHelpers';

const PAGE_SIZE = 20;
const MAX_DATA_COUNT = 500;

interface LogDataParams {
  datasourceId: number;
  tableName: string;
  keyword?: string;
  whereSql?: string;
  timeRange?: string;
  pageSize?: number;
  offset?: number;
  fields?: string[];
  startTime?: string;
  endTime?: string;
  timeGrouping?: string;
}

export const useLogData = (params: LogDataParams) => {
  const [tableData, setTableData] = useState<LogData[]>([]);
  const [loading, setLoading] = useState(false);
  const [hasMore, setHasMore] = useState(true);
  
  // 加载更多数据
  const loadMoreData = useCallback(() => {
    if (loading || !hasMore) return;
    
    setLoading(true);
    
    // 模拟API请求延迟
    setTimeout(() => {
      const newData = generateMockData(tableData.length, PAGE_SIZE);
      setTableData(prevData => [...prevData, ...newData]);
      
      // 模拟数据上限
      if (tableData.length + newData.length >= MAX_DATA_COUNT) {
        setHasMore(false);
      }
      
      setLoading(false);
    }, 500);
  }, [loading, hasMore, tableData.length]);

  // 初始化加载数据
  useEffect(() => {
    setTableData(generateMockData(0, PAGE_SIZE));
    setHasMore(true);
  }, []);

  // 搜索条件改变时重置数据
  const prevParams = useRef(params);
  
  useEffect(() => {
    // 深度比较params是否变化
    if (JSON.stringify(params) === JSON.stringify(prevParams.current)) {
      return;
    }
    prevParams.current = params;

    const fetchData = async () => {
      try {
        setLoading(true);
        const result = await searchLogs({
          ...params,
          pageSize: params.pageSize || PAGE_SIZE,
          offset: params.offset || 0
        });
        
        if (result.success) {
          const formattedData = result.rows.map((row, index) => ({
            key: `${index}`,
            timestamp: row.timestamp as string || new Date().toISOString(),
            message: row.message as string || '',
            host: row.host as string || '',
            source: row.source as string || '',
            level: row.level as string || 'INFO',
            ...row
          }));
          setTableData(formattedData);
          setHasMore(result.rows.length >= (params.pageSize || PAGE_SIZE));
        } else {
          // 失败时使用mock数据
          setTableData(generateMockData(0, PAGE_SIZE));
          setHasMore(true);
        }
      } catch (error) {
        console.error('获取日志数据失败:', error);
        setTableData(generateMockData(0, PAGE_SIZE));
        setHasMore(true);
      } finally {
        setLoading(false);
      }
    };
    
    fetchData();
  }, [params]);

  // 从表格数据中生成时间分布数据
  const distributionData = useMemo(() => {
    // 确保有log_time字段
    if (!tableData || tableData.length === 0 || !tableData[0].log_time) {
      return [];
    }

    // 从日志数据中提取时间信息并计数
    const timeCountMap = new Map<string, number>();
    
    tableData.forEach(item => {
      let timePoint = '';
      
      // 判断时间字段的格式
      if (item.log_time) {
        timePoint = item.log_time;
      } else if (item.timestamp) {
        timePoint = item.timestamp;
      } else {
        return; // 跳过没有时间字段的数据
      }
      
      // 如果时间是ISO格式，转换为更友好的格式
      if (timePoint.includes('T')) {
        timePoint = timePoint.replace('T', ' ').substr(0, 19);
      }
      
      // 累加同一时间点的计数
      const count = timeCountMap.get(timePoint) || 0;
      timeCountMap.set(timePoint, count + 1);
    });
    
    // 转换为所需的数据格式并按时间排序
    return Array.from(timeCountMap.entries())
      .map(([timePoint, count]) => ({ timePoint, count }))
      .sort((a, b) => new Date(a.timePoint).getTime() - new Date(b.timePoint).getTime());
  }, [tableData]);

  return {
    tableData,
    loading,
    hasMore,
    loadMoreData,
    distributionData
  };
};
