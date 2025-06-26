import { useState, useEffect, useRef } from 'react';
import { message } from 'antd';
import { getModules, getModuleDetail } from '@/api/modules';
import { getDataSources } from '@/api/datasource';
import type { DataSource } from '@/types/datasourceTypes';
import type { ModuleData } from '../types';
import { transformModuleData, searchModuleData } from '../utils';

export const useModuleData = () => {
  const [data, setData] = useState<ModuleData[]>([]);
  const [originalData, setOriginalData] = useState<ModuleData[]>([]);
  const [dataSources, setDataSources] = useState<DataSource[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchText, setSearchText] = useState('');

  const searchTimeoutRef = useRef<number | null>(null);
  const [messageApi, contextHolder] = message.useMessage();

  // 获取模块数据
  const fetchModules = async (config?: any) => {
    setLoading(true);
    try {
      const modules = await getModules(config);
      const transformedModules = transformModuleData(modules);
      setData(transformedModules);
      setOriginalData(transformedModules);
    } catch (error) {
      if (error instanceof Error && error.name !== 'CanceledError') {
        messageApi.error('加载模块数据失败');
      }
    } finally {
      setLoading(false);
    }
  };

  // 获取数据源
  const fetchDataSources = async () => {
    try {
      const sources = await getDataSources();
      setDataSources(sources);
    } catch (error) {
      console.error('加载数据源失败:', error);
      messageApi.error('加载数据源失败');
    }
  };

  // 获取模块详情
  const fetchModuleDetail = async (moduleId: number) => {
    try {
      setLoading(true);
      const detail = await getModuleDetail(moduleId);
      return {
        ...detail,
        users: detail.users?.map((user) => ({
          ...user,
          role: (user as any).role || 'USER',
        })),
      };
    } catch (error) {
      messageApi.error('获取模块详情失败');
      throw error;
    } finally {
      setLoading(false);
    }
  };

  // 处理搜索
  const handleSearch = (value: string) => {
    setSearchText(value);

    if (searchTimeoutRef.current !== null) {
      window.clearTimeout(searchTimeoutRef.current);
    }

    searchTimeoutRef.current = window.setTimeout(() => {
      if (!value.trim()) {
        setData(originalData);
        return;
      }

      const filteredData = searchModuleData(originalData, value);
      setData(filteredData);
    }, 300);
  };

  // 重新加载数据
  const handleReload = () => {
    fetchModules().catch(() => {
      messageApi.error('加载模块数据失败');
    });
  };

  // 初始化数据
  useEffect(() => {
    const abortController = new AbortController();

    fetchModules({ signal: abortController.signal }).catch((error: { name: string }) => {
      if (error.name !== 'CanceledError') {
        messageApi.error('加载模块数据失败');
      }
    });

    fetchDataSources().catch(() => {
      messageApi.error('加载数据源失败');
    });

    return () => {
      abortController.abort();
      if (searchTimeoutRef.current !== null) {
        window.clearTimeout(searchTimeoutRef.current);
      }
    };
  }, []);

  return {
    data,
    setData,
    dataSources,
    loading,
    searchText,
    fetchModules,
    fetchModuleDetail,
    handleSearch,
    handleReload,
    messageApi,
    contextHolder,
  };
};
