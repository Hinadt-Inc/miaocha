/**
 * 搜索输入相关的自定义钩子
 */

import { useState, useCallback } from 'react';

import { ISearchInputState } from '../types';

export const useSearchInput = () => {
  const [searchState, setSearchState] = useState<ISearchInputState>({
    keyword: '',
    sql: '',
  });

  // 处理关键词输入变化
  const changeKeyword = useCallback((value: string) => {
    setSearchState((prev) => ({
      ...prev,
      keyword: value || '',
    }));
  }, []);

  // 处理SQL输入变化
  const changeSql = useCallback((value: string) => {
    setSearchState((prev) => ({
      ...prev,
      sql: value || '',
    }));
  }, []);

  // 清空输入框
  const clearInputs = useCallback(() => {
    setSearchState({
      keyword: '',
      sql: '',
    });
  }, []);

  return {
    searchState,
    changeKeyword,
    changeSql,
    clearInputs,
  };
};
