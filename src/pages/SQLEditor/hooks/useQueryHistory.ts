import { useState, useEffect } from 'react';
import { message } from 'antd';
import type { QueryHistory } from '@/pages/SQLEditor/types';
import { HISTORY_STORAGE_KEY, MAX_HISTORY_COUNT, isValidQueryHistory } from '@/pages/SQLEditor/types';

// 按数据源构造存储键
const getStorageKey = (dataSourceId: string): string => {
  return `${HISTORY_STORAGE_KEY}_${dataSourceId}`;
};

// 空函数，移除了调试日志输出
const debugListHistoryKeys = () => {
  // 移除调试日志
};

export const useQueryHistory = (selectedSource: string) => {
  const [history, setHistory] = useState<QueryHistory[]>([]);
  const [isHistoryOpen, setIsHistoryOpen] = useState(false);

  // 加载指定数据源的历史记录
  useEffect(() => {
    // 移除调试日志
    debugListHistoryKeys();
    
    if (!selectedSource) {
      return;
    }
    
    // 加载全局历史记录（兼容旧数据）
    const loadGlobalHistory = () => {
      const savedHistory = localStorage.getItem(HISTORY_STORAGE_KEY);
      if (savedHistory) {
        const parsed = JSON.parse(savedHistory) as unknown;
          if (Array.isArray(parsed) && parsed.every(isValidQueryHistory)) {
            // 只过滤当前数据源的记录
            const filtered = parsed.filter(item => item.dataSourceId === selectedSource);
            return filtered;
          }
      }
      return [];
    };
    
    // 加载指定数据源的历史记录
    const loadSourceHistory = () => {
      const sourceKey = getStorageKey(selectedSource);
      const savedHistory = localStorage.getItem(sourceKey);
      if (savedHistory) {
        try {
          const parsed = JSON.parse(savedHistory) as unknown;
          
          // 尝试简单验证并修复数据
          if (Array.isArray(parsed)) {
            // 修复数据并保留有效的记录
            const fixedHistory = parsed
              .filter(item => item && typeof item === 'object')
              .map((item: Partial<QueryHistory>) => {
                // 确保每个记录都有必要的字段
                return {
                  id: item.id ?? Date.now().toString(),
                  sql: item.sql ?? '',
                  dataSourceId: item.dataSourceId ?? selectedSource,
                  executionTime: typeof item.executionTime === 'number' ? item.executionTime : Date.now(),
                  status: (item.status === 'success' || item.status === 'error') ? item.status : 'success',
                  timestamp: item.timestamp ?? new Date().toISOString(),
                  message: item.message
                };
              });
            
            if (fixedHistory.length > 0) {
              // 保存修复后的记录
              localStorage.setItem(sourceKey, JSON.stringify(fixedHistory));
              return fixedHistory;
            }
          }
          
          if (Array.isArray(parsed) && parsed.every(isValidQueryHistory)) {
            return parsed;
          } else {
            // 移除验证失败的详细日志
          }
        } catch (error) {
          // 移除错误日志
          
        }
      }
      return [];
    };
    
    // 优先使用数据源特定的历史记录，如果没有则使用全局历史中过滤的记录
    const sourceHistory = loadSourceHistory();
    if (sourceHistory.length > 0) {
      setHistory(sourceHistory);
    } else {
      const filteredGlobalHistory = loadGlobalHistory();
      if (filteredGlobalHistory.length > 0) {
        // 发现全局历史中有此数据源的记录，迁移到数据源特定存储中
        localStorage.setItem(getStorageKey(selectedSource), JSON.stringify(filteredGlobalHistory));
        setHistory(filteredGlobalHistory);
      } else {
        setHistory([]);
      }
    }
  }, [selectedSource]);

  // 添加新的历史记录
  const addHistory = (sql: string, status: 'success' | 'error', message?: string) => {
    if (!selectedSource) {
      return;
    }
    
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
      // 去重：如果存在相同SQL，则移除旧记录
      const filtered = prev.filter(item => item.sql !== sql);
      const updated = [newItem, ...filtered].slice(0, MAX_HISTORY_COUNT);
      
      // 保存到数据源特定的存储中
      const sourceKey = getStorageKey(selectedSource);
      localStorage.setItem(sourceKey, JSON.stringify(updated));
      
      return updated;
    });
  };

  // 清除历史记录
  const clearHistory = () => {
    if (!selectedSource) return;
    
    setHistory([]);
    // 清除数据源特定的存储
    const sourceKey = getStorageKey(selectedSource);
    localStorage.removeItem(sourceKey);
    message.success('已清除查询历史');
  };

  // 清除所有数据源的历史记录
  const clearAllHistory = () => {
    setHistory([]);
    
    // 查找并清除所有与查询历史相关的存储项
    Object.keys(localStorage).forEach(key => {
      if (key === HISTORY_STORAGE_KEY || key.startsWith(`${HISTORY_STORAGE_KEY}_`)) {
        localStorage.removeItem(key);
      }
    });
    
    message.success('已清除所有数据源的查询历史');
  };

  const toggleHistory = () => {
    setIsHistoryOpen(prev => !prev);
  };

  return {
    history,
    addHistory,
    clearHistory,
    clearAllHistory,
    isHistoryOpen,
    toggleHistory
  };
};
