import { useState, useEffect } from 'react';
import { message } from 'antd';
import type { QueryHistoryItem } from '../../../api/sql';
import { queryHistory } from '../../../api/sql';

interface UseQueryHistoryReturn {
  history: QueryHistoryItem[];
  loading: boolean;
  pagination: {
    pageNum: number;
    pageSize: number;
    total: number;
  };
  filters: {
    tableName: string;
    queryKeyword: string;
  };
  isHistoryOpen: boolean;
  loadHistory: (pageNum?: number, pageSize?: number) => Promise<void>;
  handlePaginationChange: (page: number, pageSize: number) => void;
  handleFilterChange: (newFilters: { tableName?: string; queryKeyword?: string }) => void;
  toggleHistory: () => void;
  clearHistory: () => void;
  clearAllHistory: () => void;
}

export const useQueryHistory = (selectedSource: string): UseQueryHistoryReturn => {
  const [history, setHistory] = useState<QueryHistoryItem[]>([]);
  const [isHistoryOpen, setIsHistoryOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState({
    pageNum: 1,
    pageSize: 10,
    total: 0,
  });
  const [filters, setFilters] = useState({
    tableName: '',
    queryKeyword: '',
  });

  // 加载历史记录
  const loadHistory = async (pageNum = 1, pageSize = 10) => {
    if (!selectedSource) return;

    setLoading(true);
    try {
      const result = await queryHistory({
        pageNum,
        pageSize,
        datasourceId: Number(selectedSource),
        tableName: filters.tableName,
        queryKeyword: filters.queryKeyword,
      });

      setHistory(result.records);
      setPagination((prev) => ({
        ...prev,
        pageNum: result.pageNum,
        pageSize: result.pageSize,
        total: result.total,
      }));
    } catch (error) {
      message.error('加载查询历史失败');
    } finally {
      setLoading(false);
    }
  };

  // 初始化加载和筛选条件变化时重新加载
  useEffect(() => {
    loadHistory();
  }, [selectedSource, filters]);

  // 处理分页变化
  const handlePaginationChange = (page: number, pageSize: number) => {
    loadHistory(page, pageSize);
  };

  // 处理筛选变化
  const handleFilterChange = (newFilters: { tableName?: string; queryKeyword?: string }) => {
    setFilters((prev) => ({ ...prev, ...newFilters }));
  };

  // 清除历史记录
  const clearHistory = () => {
    message.warning('清除历史记录功能需要后端支持');
  };

  // 清除所有历史记录
  const clearAllHistory = () => {
    message.warning('清除所有历史记录功能需要后端支持');
  };

  const toggleHistory = () => {
    setIsHistoryOpen((prev) => !prev);
  };

  return {
    history,
    loading,
    pagination,
    filters,
    isHistoryOpen,
    loadHistory,
    handlePaginationChange,
    handleFilterChange,
    toggleHistory,
    clearHistory,
    clearAllHistory,
  };
};
