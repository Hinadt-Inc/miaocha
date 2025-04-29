import { useState, useEffect } from 'react';
import { message } from 'antd';
import { getDataSources } from '@/api/datasource';
import type { DataSource } from '@/types/datasourceTypes';

export const useDataSources = () => {
  const [dataSources, setDataSources] = useState<DataSource[]>([]);
  const [selectedSource, setSelectedSource] = useState<string>('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      try {
        const response = await getDataSources();
        setDataSources(response);
        if (response.length > 0) {
          setSelectedSource(response[0].id);
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

  return { 
    dataSources,
    setDataSources,
    selectedSource, 
    setSelectedSource,
    loading
  };
};
