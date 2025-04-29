import { useState, useEffect } from 'react';
import { message } from 'antd';
import type { QueryHistory } from '@/pages/SQLEditor/types';
import { HISTORY_STORAGE_KEY, MAX_HISTORY_COUNT } from '@/pages/SQLEditor/types';

export const isValidQueryHistory = (item: unknown): item is QueryHistory => {
  return !!item && typeof item === 'object' &&
    'id' in item && typeof item.id === 'string' &&
    'sql' in item && typeof item.sql === 'string' &&
    'dataSourceId' in item && typeof item.dataSourceId === 'string' &&
    'timestamp' in item && typeof item.timestamp === 'string';
};

export const useQueryHistory = (selectedSource: string) => {
  const [history, setHistory] = useState<QueryHistory[]>([]);
  const [isHistoryOpen, setIsHistoryOpen] = useState(false);

  useEffect(() => {
    const savedHistory = localStorage.getItem(HISTORY_STORAGE_KEY);
    if (savedHistory) {
      try {
        const parsed = JSON.parse(savedHistory) as unknown;
        if (Array.isArray(parsed) && parsed.every(isValidQueryHistory)) {
          setHistory(parsed);
        }
      } catch (error) {
        console.error('解析查询历史失败:', error);
        message.error('加载查询历史失败');
      }
    }
  }, []);

  const isValidQueryHistory = (item: unknown): item is QueryHistory => {
    return !!item && typeof item === 'object' &&
      'id' in item && typeof item.id === 'string' &&
      'sql' in item && typeof item.sql === 'string' &&
      'dataSourceId' in item && typeof item.dataSourceId === 'string' &&
      'timestamp' in item && typeof item.timestamp === 'string';
  };

  const addHistory = (sql: string, status: 'success' | 'error', message?: string) => {
    const newItem: QueryHistory = {
      id: Date.now().toString(),
      sql,
      dataSourceId: selectedSource,
      executionTime: Date.now(),
      status,
      timestamp: new Date().toISOString(),
      message
    };

    setHistory(prev => {
      const updated = [newItem, ...prev].slice(0, MAX_HISTORY_COUNT);
      localStorage.setItem(HISTORY_STORAGE_KEY, JSON.stringify(updated));
      return updated;
    });
  };

  const clearHistory = () => {
    setHistory([]);
    localStorage.removeItem(HISTORY_STORAGE_KEY);
    message.success('已清除查询历史');
  };

  const toggleHistory = () => {
    setIsHistoryOpen(prev => !prev);
  };

  return {
    history,
    addHistory,
    clearHistory,
    isHistoryOpen,
    toggleHistory
  };
};
