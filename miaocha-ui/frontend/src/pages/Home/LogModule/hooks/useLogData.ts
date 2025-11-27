/**
 * Log 模块的数据管理 Hook
 */

import { useState, useEffect, useMemo } from 'react';

import { ILogTableProps } from '../types';
import { mergeDataArrays } from '../utils';

/**
 * 管理日志数据的 Hook
 * 处理数据累积、加载更多等逻辑
 */
export const useLogData = (getDetailData: any, detailData: any) => {
  const { rows = [], totalCount } = detailData || {};
  const [allRows, setAllRows] = useState<any[]>([]);

  // 当新数据到达时，将其添加到历史数据中
  useEffect(() => {
    const { offset } = getDetailData.params?.[0] || {};
    setAllRows((prevRows) => mergeDataArrays(prevRows, rows, offset));
  }, [rows]);

  const handleLoadMore = () => {
    if (!getDetailData.loading) {
      getDetailData.run({
        ...getDetailData.params?.[0],
        offset: allRows.length || 0,
      });
    }
  };

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
