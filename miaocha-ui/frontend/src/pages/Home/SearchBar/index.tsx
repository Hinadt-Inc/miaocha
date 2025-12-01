import { useState, useMemo, useRef, useCallback, memo } from 'react';

import { Button, Space } from 'antd';

import AutoRefresh from '../AutoRefresh';
import { useHomeContext } from '../context';
import { useDataInit } from '../hooks/useDataInit';
import SavedSearchesButton from '../SavedSearchesButton';
import SaveSearchButton from '../SaveSearchButton';
import ShareButton from '../ShareButton';
import { debounce } from '../utils';

import { FilterTags, StatisticsInfo, KeywordInput, SqlInput, TimePickerWrapper, TimeGroupSelector } from './components';
import type { TimePickerWrapperRef } from './components/TimePickerWrapper';
import styles from './index.module.less';
import type { ISearchState } from './types';

const SearchBar = () => {
  const searchBarRef = useRef<HTMLDivElement>(null); // 搜索栏的ref
  const timePickerRef = useRef<TimePickerWrapperRef>(null); // TimePickerWrapper的ref
  const { searchParams, distributions, logTableColumns, detailData, updateSearchParams } = useHomeContext(); // 获取context中的数据
  const { fetchData, refreshFieldDistributions } = useDataInit();

  const [searchState, setSearchState] = useState<ISearchState>({
    keywords: '',
    sql: '',
  }); // 关键词输入框的值

  // 处理搜索提交
  const handleSubmit = useCallback(() => {
    const params: Partial<ILogSearchParams> = {};
    if (searchState.keywords) params.keywords = [...(searchParams?.keywords || []), searchState.keywords];
    if (searchState.sql) params.whereSqls = [...(searchParams?.whereSqls || []), searchState.sql];
    const newSearchParams = updateSearchParams(params);
    setSearchState((prev) => ({ ...prev, keywords: '', sql: '' }));
    fetchData({ searchParams: newSearchParams });
    refreshFieldDistributions(newSearchParams);
  }, [searchState, searchParams, distributions, fetchData, updateSearchParams]);

  // 防抖处理搜索（300ms）
  const debouncedHandleSubmit = useMemo(() => debounce(handleSubmit, 300), [handleSubmit]);

  const handleClickTag = (type: string, value: string) => {
    let newSearchParams = { ...searchParams };
    switch (type) {
      case 'keywords':
        newSearchParams = updateSearchParams({
          keywords: (searchParams?.keywords || []).filter((item: string) => item !== value),
        });
        setSearchState((prev) => ({
          ...prev,
          keywords: value,
        }));
        break;
      case 'closeKeywords':
        newSearchParams = updateSearchParams({
          keywords: (searchParams?.keywords || []).filter((item: string) => item !== value),
        });
        break;
      case 'sql':
        newSearchParams = updateSearchParams({
          whereSqls: (searchParams?.whereSqls || []).filter((item: string) => item !== value),
        });
        setSearchState((prev) => ({
          ...prev,
          sql: value,
        }));
        break;
      case 'closeSql':
        newSearchParams = updateSearchParams({
          whereSqls: (searchParams?.whereSqls || []).filter((item: string) => item !== value),
        });
        break;
      case 'time':
        // 打开时间选择器弹窗
        timePickerRef.current?.setVisible(true);
        break;
    }
    if (type === 'time') return;
    fetchData({ searchParams: newSearchParams });
    refreshFieldDistributions(newSearchParams);
  };

  // 渲染左侧统计信息
  const leftRender = useMemo(
    () => <StatisticsInfo totalCount={(detailData?.totalCount as number) || 0} />,
    [detailData?.totalCount],
  );

  // 渲染关键词输入框
  const keywordRender = useMemo(
    () => (
      <KeywordInput
        value={searchState.keywords}
        onChange={(value: string) => {
          setSearchState((prevState: any) => ({ ...prevState, keywords: value }));
        }}
      />
    ),
    [searchState.keywords],
  );

  // 渲染SQL输入框
  const sqlRender = useMemo(
    () => (
      <SqlInput
        columns={logTableColumns}
        value={searchState.sql}
        onChange={(value: string) => {
          setSearchState((prevState: any) => ({ ...prevState, sql: value }));
        }}
      />
    ),
    [searchState.sql, logTableColumns],
  );

  // 渲染时间分组选择器
  const timeGroupRender = useMemo(
    () => <TimeGroupSelector />,
    [
      // 状态
      searchParams.timeGrouping,
    ],
  );

  // 渲染时间范围选择器
  const timeRender = useMemo(() => <TimePickerWrapper ref={timePickerRef} />, [searchParams.timeRange]);

  // 渲染过滤标签
  const filterRender = useMemo(
    () => (
      <div className={styles.filter}>
        <FilterTags handleClickTag={handleClickTag} />
      </div>
    ),
    [searchParams.keywords, searchParams.whereSqls, searchParams.startTime, searchParams.endTime],
  );

  return (
    <div ref={searchBarRef} className={styles.searchBar}>
      <div className={styles.top}>
        <div className={styles.left}>{leftRender}</div>
        <div className={styles.right}>
          <Space size={8}>
            <AutoRefresh />
            <SaveSearchButton size="small" />
            <SavedSearchesButton size="small" />
            <ShareButton size="small" />
            {timeGroupRender}
            {timeRender}
          </Space>
        </div>
      </div>
      <div className={styles.form}>
        <div className={styles.item}>{keywordRender}</div>
        <div className={styles.item}>{sqlRender}</div>
        <div className={styles.item}>
          <Button size="small" type="primary" onClick={debouncedHandleSubmit}>
            搜索
          </Button>
        </div>
      </div>
      <div className={styles.footer}>{filterRender}</div>
    </div>
  );
};

export default memo(SearchBar);
