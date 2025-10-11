/**
 * Log 模块的数据管理 Hook
 */

import { useState, useEffect, useMemo, useRef, useCallback } from 'react';
import { ILogTableProps } from '../types';
import { mergeDataArrays } from '../utils';

/**
 * 管理日志数据的 Hook
 * 处理数据累积、加载更多等逻辑
 */
export const useLogData = (getDetailData: any, detailData: any) => {
  const { rows = [], totalCount } = detailData || {};
  const [allRows, setAllRows] = useState<any[]>([]);
  const lastSearchParamsRef = useRef<string>('');

  // 当新数据到达时，将其添加到历史数据中
  useEffect(() => {
    const currentParams = getDetailData.params?.[0] || {};
    const { offset } = currentParams;

    // 生成当前搜索参数的唯一标识（排除offset）
    const searchKey = JSON.stringify({
      ...currentParams,
      offset: undefined, // 排除offset，因为它会随着加载更多而变化
    });

    // 如果是新的搜索（搜索参数发生变化），清空之前的数据
    if (searchKey !== lastSearchParamsRef.current) {
      lastSearchParamsRef.current = searchKey;
      if (offset === 0) {
        // 新搜索，直接使用新数据
        setAllRows(rows);
        return;
      }
    }

    // 合并数据
    const mergedData = mergeDataArrays(allRows, rows, offset);

    // 只有在数据真正发生变化时才更新状态
    if (JSON.stringify(mergedData) !== JSON.stringify(allRows)) {
      setAllRows(mergedData);
    }
  }, [rows, allRows, getDetailData.params]);

  const handleLoadMore = useCallback(() => {
    if (!getDetailData.loading) {
      // 使用当前已加载数据的长度作为offset
      const currentParams = getDetailData.params?.[0] || {};
      getDetailData.run({
        ...currentParams,
        offset: allRows.length,
        pageSize: 50, // 确保每次加载50条
      });
    }
  }, [getDetailData, allRows.length]);

  return {
    allRows,
    totalCount,
    loading: getDetailData?.loading,
    handleLoadMore,
    hasMore: totalCount ? allRows.length < totalCount : false,
  };
};

/**
 * 生成表格属性的 Hook
 * 将所有表格相关的属性组织在一起
 */
export const useTableProps = (
  allRows: any[],
  loading: boolean,
  totalCount: number,
  handleLoadMore: () => void,
  hasMore: boolean,
  otherProps: Partial<ILogTableProps>,
) => {
  return useMemo(() => {
    return {
      data: allRows,
      loading,
      totalCount,
      onLoadMore: handleLoadMore,
      hasMore,
      ...otherProps,
    };
  }, [
    allRows,
    loading,
    totalCount,
    handleLoadMore,
    hasMore,
    otherProps.dynamicColumns,
    otherProps.searchParams,
    otherProps.whereSqlsFromSider,
    otherProps.sqls,
    otherProps.onSearch,
    otherProps.onSortChange,
    otherProps.onChangeColumns,
    otherProps.moduleQueryConfig,
  ]);
};
