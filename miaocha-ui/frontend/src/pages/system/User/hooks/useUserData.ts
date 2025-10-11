import { useState, useEffect, useRef } from 'react';
import { getUsers } from '@/api/user';
import { getModules } from '@/api/modules';
import type { AxiosRequestConfig } from 'axios';
import type { UserData } from '../components';

export const useUserData = () => {
  const [data, setData] = useState<UserData[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchText, setSearchText] = useState('');
  const [moduleList, setModuleList] = useState<Array<{ value: string; label: string }>>([]);
  const searchTimeoutRef = useRef<number | null>(null);
  const originalDataRef = useRef<UserData[]>([]);

  // 清理定时器
  useEffect(() => {
    return () => {
      if (searchTimeoutRef.current !== null) {
        window.clearTimeout(searchTimeoutRef.current);
      }
    };
  }, []);

  // 初始化数据
  useEffect(() => {
    const abortController = new AbortController();
    fetchUsers({ signal: abortController.signal }).catch((error: Error) => {
      if (error.name !== 'CanceledError') {
        // API 错误已由全局错误处理器处理，这里不再重复处理
      }
    });
    fetchModules();
    return () => abortController.abort();
  }, []);

  const fetchUsers = async (config?: AxiosRequestConfig) => {
    setLoading(true);
    try {
      const users = (await getUsers(config)) as UserData[];
      setData(users);
      // 保存原始数据供搜索使用
      originalDataRef.current = users;
    } catch (error) {
      if (error instanceof Error && error.name === 'CanceledError') {
        // 请求被取消，不需要显示错误
        return;
      }
      // API 错误已由全局错误处理器处理，这里不再重复处理
    } finally {
      setLoading(false);
    }
  };

  const fetchModules = async () => {
    try {
      const modules = await getModules();
      setModuleList(
        modules.map((module) => ({
          value: module.id.toString(),
          label: module.name,
        })),
      );
    } catch {
      // API 错误已由全局错误处理器处理，这里不再重复处理
    }
  };

  // 搜索匹配函数
  const matchesSearchTerms = (user: UserData, searchTerms: string[]) => {
    if (searchTerms.length === 0) return true;
    const fields = [user.nickname?.toLowerCase() || '', user.email?.toLowerCase() || ''];
    // 每个 term 必须命中至少一个字段（全局 AND，字段内 OR）
    return searchTerms.every((term) => fields.some((f) => f.includes(term)));
  };

  // 处理搜索（带防抖功能）
  const handleSearch = (value: string) => {
    console.log(444, value);
    setSearchText(value);

    // 清除之前的定时器
    if (searchTimeoutRef.current !== null) {
      window.clearTimeout(searchTimeoutRef.current);
      searchTimeoutRef.current = null;
    }

    // 设置新的定时器，300ms后执行搜索
    searchTimeoutRef.current = window.setTimeout(() => {
      if (!value.trim()) {
        // 如果搜索词为空，则恢复原始数据
        if (originalDataRef.current.length > 0) {
          setData(originalDataRef.current);
        } else {
          // 如果原始数据不存在，则重新加载
          fetchUsers().catch(() => {
            // API 错误已由全局错误处理器处理，这里不再重复处理
          });
        }
        return;
      }

      // 去除特殊字符（如中文输入法中的单引号等）
      const cleanValue = value.replace(/['"]/g, '');

      // 分词处理，按空格拆分搜索词
      const searchTerms = cleanValue
        .toLowerCase()
        .split(/\s+/)
        .filter((term) => term);

      // 先在当前数据中搜索
      const currentDataFiltered = data.filter((user) => matchesSearchTerms(user, searchTerms));

      // 如果当前数据中有匹配项，直接返回
      if (currentDataFiltered.length > 0) {
        setData(currentDataFiltered);
        return;
      }

      // 如果当前数据中没有匹配项，在原始数据中搜索
      const originalDataFiltered = originalDataRef.current.filter((user) => matchesSearchTerms(user, searchTerms));

      // 设置搜索结果
      setData(originalDataFiltered);
    }, 300); // 300ms防抖延迟
  };

  // 重新加载数据
  const handleReload = () => {
    fetchUsers().catch(() => {
      // API 错误已由全局错误处理器处理，这里不再重复处理
    });
  };

  return {
    data,
    setData,
    loading,
    searchText,
    moduleList,
    originalDataRef,
    handleSearch,
    handleReload,
    fetchUsers,
  };
};
