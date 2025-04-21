import { useState, useEffect, useCallback } from 'react';
import { LogData } from '../types/logDataTypes';
import { generateMockData } from '../utils/logDataHelpers';

const PAGE_SIZE = 20;
const MAX_DATA_COUNT = 500;

export const useLogData = (searchQuery: string) => {
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
  useEffect(() => {
    setTableData(generateMockData(0, PAGE_SIZE));
    setHasMore(true);
  }, [searchQuery]);

  return {
    tableData,
    loading,
    hasMore,
    loadMoreData
  };
};
