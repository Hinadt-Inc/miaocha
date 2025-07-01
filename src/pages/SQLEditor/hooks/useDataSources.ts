import { useState, useEffect } from 'react';
import { message } from 'antd';
import { getDataSources } from '../../../api/datasource';
import type { DataSource } from '../../../types/datasourceTypes';

// 本地存储键
const SELECTED_SOURCE_KEY = 'sql_editor_selected_source';

export const useDataSources = () => {
  const [dataSources, setDataSources] = useState<DataSource[]>([]);
  const [selectedSource, setSelectedSource] = useState<string>('');
  const [loading, setLoading] = useState(false);

  // 从本地存储加载选中的数据源
  useEffect(() => {
    const savedSource = localStorage.getItem(SELECTED_SOURCE_KEY);
    if (savedSource) {
      setSelectedSource(savedSource);
    }
  }, []);

  // 获取数据源并设置选中的数据源
  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      try {
        const response = await getDataSources();
        setDataSources(response);

        // 如果有保存的选中数据源，验证它是否存在于当前数据源列表中
        const savedSource = localStorage.getItem(SELECTED_SOURCE_KEY);
        if (savedSource && response.some((source) => source.id === savedSource)) {
          // 如果存在，使用保存的数据源
          setSelectedSource(savedSource);
        } else if (response.length > 0) {
          // 否则使用第一个数据源并保存到本地存储
          setSelectedSource(response[0].id);
          localStorage.setItem(SELECTED_SOURCE_KEY, response[0].id);
        }
      } catch (error) {
        console.error('获取数据源失败:', error);
        message.error('获取数据源失败');
      } finally {
        setLoading(false);
      }
    };
    void fetchData();
  }, []);

  // 自定义设置选中数据源的函数，同时保存到本地存储
  const handleSetSelectedSource = (sourceId: string) => {
    setSelectedSource(sourceId);
    localStorage.setItem(SELECTED_SOURCE_KEY, sourceId);
  };

  return {
    dataSources,
    setDataSources,
    selectedSource,
    setSelectedSource: handleSetSelectedSource,
    loading,
  };
};
