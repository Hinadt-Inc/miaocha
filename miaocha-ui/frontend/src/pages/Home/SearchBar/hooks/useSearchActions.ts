/**
 * 搜索逻辑相关的自定义钩子
 */

import { useCallback } from 'react';
import { getLatestTime } from '../../utils';
import { ITimeOption } from '../../types';

interface UseSearchActionsProps {
  keywords: string[];
  setKeywords: (keywords: string[]) => void;
  sqls: string[];
  setSqls: (sqls: string[]) => void;
  setWhereSqlsFromSider: (value: any) => void;
  setTimeOption: (value: any) => void;
  timeOption: ITimeOption;
  onRemoveSql?: (sql: string) => void;
  changeKeyword?: (keyword: string) => void; // 设置关键词到输入框的方法
  changeSql?: (sql: string) => void; // 设置SQL到输入框的方法
}

export const useSearchActions = ({
  keywords,
  setKeywords,
  sqls,
  setSqls,
  setWhereSqlsFromSider: _setWhereSqlsFromSider,
  setTimeOption,
  timeOption,
  onRemoveSql,
  changeKeyword,
  changeSql,
}: UseSearchActionsProps) => {
  // 处理点击关键词标签
  const handleClickKeyword = useCallback(
    (item: string) => {
      // 将关键词设置到输入框进行编辑，同时移除原标签
      if (changeKeyword) {
        changeKeyword(item);
      }
      setKeywords(keywords.filter((keyword: string) => keyword !== item));
    },
    [changeKeyword, keywords, setKeywords],
  );

  // 处理点击SQL标签
  const handleClickSql = useCallback(
    (item: string) => {
      // 将SQL设置到输入框进行编辑，同时移除原标签
      if (changeSql) {
        changeSql(item);
      }
      setSqls(sqls.filter((sql: string) => sql !== item));
      // 通知父组件删除该SQL并重新搜索
      if (onRemoveSql) {
        onRemoveSql(item);
      }
      // 注意：不在这里调用getDistributionWithSearchBar，因为Home组件的handleRemoveSql会处理
    },
    [changeSql, sqls, setSqls, onRemoveSql],
  );

  // 处理删除关键词
  const handleCloseKeyword = useCallback(
    (item: string) => {
      setKeywords(keywords.filter((keyword: string) => keyword !== item));
    },
    [keywords, setKeywords],
  );

  // 处理删除SQL
  const handleCloseSql = useCallback(
    (item: string) => {
      // 直接删除标签并重新搜索
      setSqls(sqls.filter((sql: string) => sql !== item));
      // 调用删除回调通知父组件重新搜索
      if (onRemoveSql) {
        onRemoveSql(item);
      }
      // 注意：不在这里调用getDistributionWithSearchBar，因为Home组件的handleRemoveSql会处理
    },
    [sqls, setSqls, onRemoveSql],
  );

  // 处理搜索提交
  const handleSubmit = useCallback(
    (keyword: string, sql: string, clearInputs: () => void) => {
      const keywordTrim = String(keyword || '')?.trim();
      const sqlTrim = String(sql || '')?.trim();

      // 添加到关键词列表
      if (keywordTrim && !keywords.includes(keywordTrim)) {
        setKeywords([...keywords, keywordTrim]);
      }

      // 添加到SQL列表
      if (sqlTrim && !sqls.includes(sqlTrim)) {
        setSqls([...sqls, sqlTrim]);
      }

      // 清空输入框
      clearInputs();

      const latestTime = getLatestTime(timeOption);
      setTimeOption((prev: ITimeOption) => ({ ...prev, range: [latestTime.startTime, latestTime.endTime] }));
    },
    [keywords, setKeywords, sqls, setSqls, timeOption, setTimeOption],
  );

  // 加载已保存的搜索条件
  const handleLoadSearch = useCallback(
    (savedSearchParams: any) => {
      try {
        // 恢复关键词
        if (savedSearchParams.keywords && Array.isArray(savedSearchParams.keywords)) {
          setKeywords(savedSearchParams.keywords);
        } else {
          setKeywords([]);
        }

        // 恢复SQL条件
        if (savedSearchParams.whereSqls && Array.isArray(savedSearchParams.whereSqls)) {
          setSqls(savedSearchParams.whereSqls);
        } else {
          setSqls([]);
        }
      } catch (error) {
        console.error('加载搜索条件失败:', error);
      }
    },
    [setKeywords, setSqls],
  );

  return {
    handleClickKeyword,
    handleClickSql,
    handleCloseKeyword,
    handleCloseSql,
    handleSubmit,
    handleLoadSearch,
  };
};
