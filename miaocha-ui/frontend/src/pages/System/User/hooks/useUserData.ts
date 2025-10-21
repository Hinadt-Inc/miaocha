import { useState, useEffect, useRef, useCallback } from 'react';
import { getUsers } from '@/api/user';
import { getModules } from '@/api/modules';
import type { UserListItem } from '../types';

export const useUserData = () => {
  const [data, setData] = useState<UserListItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchText, setSearchText] = useState('');
  const [moduleList, setModuleList] = useState<{ value: string; label: string }[]>([]);

  const searchTimeoutRef = useRef<number | null>(null);
  const originalDataRef = useRef<UserListItem[]>([]);

  const fetchUsers = useCallback(async () => {
    setLoading(true);
    try {
      const users = await getUsers();
      setData(users);
      // 保存原始数据供搜索使用
      originalDataRef.current = users;
    } finally {
      setLoading(false);
    }
  }, []);

  const fetchModules = useCallback(async () => {
    const modules = await getModules();

    setModuleList(
      modules.map((module) => ({
        value: module.id.toString(),
        label: module.name,
      })),
    );
  }, []);

  // 搜索匹配函数
  const matchesSearchTerms = (user: UserListItem, terms: string[]) => {
    const fields = [user.nickname?.toLowerCase() || '', user.email?.toLowerCase() || ''];
    // 每个 term 必须命中至少一个字段（全局 AND，字段内 OR）
    return terms.every((item) => fields.some((f) => f.includes(item)));
  };

  // 处理搜索（带防抖功能）
  const handleSearch = useCallback(
    (value: string) => {
      setSearchText(value);

      // 清除之前的定时器
      if (searchTimeoutRef.current !== null) {
        window.clearTimeout(searchTimeoutRef.current);
        searchTimeoutRef.current = null;
      }

      // 设置新的定时器，300ms后执行搜索
      searchTimeoutRef.current = window.setTimeout(() => {
        const v = value.trim();
        if (!v) {
          // 如果搜索词为空，则恢复原始数据
          if (originalDataRef.current.length > 0) {
            setData(originalDataRef.current);
          } else {
            // 如果原始数据不存在，则重新加载
            void fetchUsers();
          }
          return;
        }

        // 分词处理，按空格拆分搜索词
        const terms = v.toLowerCase().split(/\s+/).filter(Boolean);

        // 先在当前数据中搜索
        const currentDataFiltered = data.filter((user) => matchesSearchTerms(user, terms));
        // 如果当前数据中有匹配项，直接返回
        if (currentDataFiltered.length > 0) {
          setData(currentDataFiltered);
          return;
        }

        // 如果当前数据中没有匹配项，在原始数据中搜索
        const originalDataFiltered = originalDataRef.current.filter((user) => matchesSearchTerms(user, terms));

        // 设置搜索结果
        setData(originalDataFiltered);
      }, 300); // 300ms防抖延迟
    },
    [data, fetchUsers],
  );

  // 重新加载数据
  const handleReload = useCallback(() => {
    void fetchUsers();
  }, [fetchUsers]);

  // 初始化数据
  useEffect(() => {
    void fetchUsers();
    void fetchModules();
  }, [fetchUsers, fetchModules]);

  // 清理定时器
  useEffect(() => {
    return () => {
      if (searchTimeoutRef.current !== null) {
        window.clearTimeout(searchTimeoutRef.current);
      }
    };
  }, []);

  return {
    data,
    loading,
    searchText,
    moduleList,
    originalDataRef,
    handleSearch,
    handleReload,
    fetchUsers,
  };
};
