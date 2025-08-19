/**
 * SearchBar 主组件 - 重构后的模块化版本
 */

// @ts-ignore
import React, { useState, useMemo, useEffect, forwardRef, useImperativeHandle, useRef } from 'react';
import { Button, Space } from 'antd';
import dayjs from 'dayjs';

// 组件导入
import { FilterTags, StatisticsInfo, KeywordInput, SqlInput, TimePickerWrapper, TimeGroupSelector } from './components';

// 外部组件导入
import AutoRefresh from '../AutoRefresh/index';
import SaveSearchButton from '../SaveSearchButton';
import SavedSearchesButton from '../SavedSearchesButton';
import ShareButton from '../ShareButton';

// 样式导入
import { searchBarStyles as styles } from './styles';

// 钩子导入
import { useSearchInput, useTimeState, useSearchActions } from './hooks';

// 类型和常量导入
import { ISearchBarProps, ISearchBarRef } from './types';
import type { ITimeOption } from '../types';
import { SEARCH_BUTTON, STYLES } from './constants';
import { getLatestTime, DATE_FORMAT_THOUSOND, QUICK_RANGES } from '../utils';

const SearchBar = forwardRef<ISearchBarRef, ISearchBarProps>((props, ref) => {
  const {
    loading = false,
    keywords,
    setKeywords,
    sqls,
    setSqls,
    onRefresh,
    searchParams,
    totalCount = 0,
    onSearch,
    setWhereSqlsFromSider,
    onRemoveSql,
    columns,
    onSqlsChange,
    activeColumns,
    getDistributionWithSearchBar,
    sortConfig = [],
    commonColumns = [],
  } = props;

  const searchBarRef = useRef<HTMLDivElement>(null);
  const [initialized] = useState(true);
  const timeUpdateFromParamsRef = useRef(false); // 标记时间更新是否来自外部params
  const [isFirstLoad, setIsFirstLoad] = useState<boolean>(true); // 标记是否第一次加载

  // 使用自定义钩子
  const { searchState, changeKeyword, changeSql, clearInputs } = useSearchInput();
  const { timeState, setTimeOption, setTimeGroup, setOpenTimeRange, setOpenTimeGroup, setActiveTab, submitTime } =
    useTimeState(searchParams);
  const searchActions = useSearchActions({
    keywords,
    setKeywords,
    sqls,
    setSqls,
    setWhereSqlsFromSider,
    setTimeOption,
    timeOption: timeState.timeOption,
    onRemoveSql,
    changeKeyword,
    changeSql,
  });

  // 监听 searchParams 中的时间变化，同步更新 timeOption
  useEffect(() => {
    if (searchParams.startTime && searchParams.endTime) {
      const newTimeString = `${searchParams.startTime} ~ ${searchParams.endTime}`;
      const currentTimeString = `${timeState.timeOption?.range?.[0]} ~ ${timeState.timeOption?.range?.[1]}`;

      // 如果 searchParams 中的时间与当前 timeOption 不一致，更新 timeOption
      if (currentTimeString !== newTimeString) {
        timeUpdateFromParamsRef.current = true; // 标记这次更新来自外部

        const newTimeOption = {
          value: searchParams.timeRange || newTimeString,
          range: [searchParams.startTime, searchParams.endTime],
          label:
            searchParams.timeRange && QUICK_RANGES[searchParams.timeRange]
              ? QUICK_RANGES[searchParams.timeRange].label
              : searchParams.timeRange || newTimeString,
          type:
            searchParams.timeType ||
            (searchParams.timeRange && QUICK_RANGES[searchParams.timeRange] ? 'quick' : 'absolute'),
          ...(searchParams.timeType === 'relative' &&
            searchParams.relativeStartOption &&
            searchParams.relativeEndOption && {
              startOption: searchParams.relativeStartOption,
              endOption: searchParams.relativeEndOption,
            }),
        };

        setTimeOption(newTimeOption as any);
      }
    }
  }, [searchParams.startTime, searchParams.endTime, searchParams.timeRange, setTimeOption]);

  // 暴露给父组件的方法
  useImperativeHandle(
    ref,
    () => ({
      // 渲染sql
      addSql: (sql: string) => {
        setSqls([...sqls, sql]);
      },
      removeSql: (sql: string) => {
        setSqls(sqls.filter((item: string) => item !== sql));
      },
      // 渲染时间
      setTimeOption,
      // 设置时间分组
      setTimeGroup,
      // 自动刷新方法（供父组件调用）
      autoRefresh: () => {
        // 更新时间到最新
        const latestTime = getLatestTime(timeState.timeOption);
        setTimeOption((prev: any) => ({ ...prev, range: [latestTime.startTime, latestTime.endTime] }));
        // 这会触发useEffect，自动调用onSearch
      },
    }),
    [sqls, timeState.timeOption, setSqls, setTimeOption, setTimeGroup],
  );

  // 当关键词、sqls或时间变化时触发搜索
  useEffect(() => {
    // 只有在组件初始化完成后才执行搜索逻辑
    if (!initialized) return;
    // 等待 commonColumns 准备好后再执行（页面初始化时需要等待，后端说模块下一定会有普通字段）
    if (commonColumns.length === 0) return;

    // 页面第一次加载时不调用getDistributionWithSearchBar接口
    const shouldCallDistribution = !isFirstLoad;
    if (isFirstLoad) {
      setIsFirstLoad(false);
    }

    const fieldsHasDot = activeColumns?.some((item: any) => item.includes('.'));
    const resSortConfig = sortConfig?.filter((item) => !item.fieldName.includes('.'));

    // 如果activeColumns为空或未定义，使用commonColumns作为默认值
    let effectiveFields = commonColumns;
    if (activeColumns && activeColumns.length > 0) {
      effectiveFields = fieldsHasDot ? [...commonColumns, ...activeColumns] : activeColumns;
    }

    // 先构建基础参数，避免searchParams中的空fields覆盖我们的effectiveFields
    const baseParams = {
      datasourceId: searchParams.datasourceId,
      module: searchParams.module,
      startTime: searchParams.startTime,
      endTime: searchParams.endTime,
      timeRange: searchParams.timeRange,
      timeType: searchParams.timeType,
      relativeStartOption: searchParams.relativeStartOption,
      relativeEndOption: searchParams.relativeEndOption,
      timeGrouping: timeState.timeGroup,
      offset: 0,
      fields: effectiveFields, // 确保使用我们计算的effectiveFields
      sortFields: resSortConfig || [],
      ...(keywords.length > 0 && { keywords }),
      ...(sqls.length > 0 && { whereSqls: sqls }),
    };

    const params = baseParams;

    if (keywords.length === 0) {
      delete params.keywords;
    }
    if (sqls.length === 0) {
      delete params.whereSqls;
    }

    onSearch(params as any);

    // 通知父组件sqls数据变化
    if (onSqlsChange) {
      onSqlsChange(sqls);
    }

    // 调用Sider组件的getDistributionWithSearchBar方法，但第一次加载时跳过
    if (shouldCallDistribution && getDistributionWithSearchBar) {
      getDistributionWithSearchBar();
    }
  }, [
    timeState.timeGroup,
    activeColumns,
    sortConfig,
    initialized,
    commonColumns,
    keywords,
    sqls,
    isFirstLoad,
    // 添加searchParams相关依赖，确保当外部searchParams变化时能够触发搜索
    searchParams.startTime,
    searchParams.endTime,
    searchParams.datasourceId,
    searchParams.module,
    // 移除了timeState.timeOption依赖，避免时间变化引起的循环
    // 移除了可能导致循环的依赖：onSearch, getDistributionWithSearchBar, searchParams, onSqlsChange
  ]);

  // 单独处理时间变化的搜索
  useEffect(() => {
    // 如果是来自搜索按钮的强制触发，跳过初始化和columns检查
    const isFromSearchButton = timeState.timeOption?._fromSearch;

    if (!isFromSearchButton) {
      // 非搜索按钮触发的情况，需要检查初始化状态和columns
      if (!initialized || commonColumns.length === 0) {
        console.log('⏸️ 等待初始化完成或columns加载完成...', {
          initialized,
          commonColumnsLength: commonColumns.length,
        });
        return;
      }

      // 如果时间更新来自外部params，不要重复搜索
      if (timeUpdateFromParamsRef.current) {
        timeUpdateFromParamsRef.current = false;
        return;
      }
    } else {
      console.log('🔍 来自搜索按钮的强制触发，跳过初始化检查');
    }

    // 时间发生变化时，需要更新搜索参数并触发搜索
    const fieldsHasDot = activeColumns?.some((item: any) => item.includes('.'));
    const resSortConfig = sortConfig?.filter((item) => !item.fieldName.includes('.'));

    // 处理字段逻辑
    let effectiveFields;
    if (isFromSearchButton) {
      // 搜索按钮触发时，优先使用activeColumns，如果没有则使用commonColumns，都没有就用空数组
      effectiveFields =
        activeColumns && activeColumns.length > 0
          ? fieldsHasDot
            ? [...(commonColumns || []), ...activeColumns]
            : activeColumns
          : commonColumns || [];
    } else {
      // 非搜索按钮触发时，使用原来的逻辑
      effectiveFields = commonColumns;
      if (activeColumns && activeColumns.length > 0) {
        effectiveFields = fieldsHasDot ? [...commonColumns, ...activeColumns] : activeColumns;
      }
    }

    // 先构建基础参数，避免searchParams中的空fields覆盖我们的effectiveFields
    const baseParams = {
      datasourceId: searchParams.datasourceId,
      module: searchParams.module,
      startTime: dayjs(timeState.timeOption?.range?.[0]).format(DATE_FORMAT_THOUSOND),
      endTime: dayjs(timeState.timeOption?.range?.[1]).format(DATE_FORMAT_THOUSOND),
      timeRange: timeState.timeOption?.value,
      timeType: timeState.timeOption?.type,
      ...(timeState.timeOption?.type === 'relative' &&
        timeState.timeOption?.startOption &&
        timeState.timeOption?.endOption && {
          relativeStartOption: timeState.timeOption.startOption,
          relativeEndOption: timeState.timeOption.endOption,
        }),
      timeGrouping: timeState.timeGroup,
      offset: 0,
      fields: effectiveFields, // 确保使用我们计算的effectiveFields
      sortFields: resSortConfig || [],
      ...(keywords.length > 0 && { keywords }),
      ...(sqls.length > 0 && { whereSqls: sqls }),
    };

    const params = baseParams;

    if (keywords.length === 0) {
      delete params.keywords;
    }
    if (sqls.length === 0) {
      delete params.whereSqls;
    }

    onSearch(params as any);

    // 同时触发字段分布数据更新
    if (getDistributionWithSearchBar) {
      // 在调用字段分布查询之前，确保localStorage中有最新的参数
      try {
        const currentSearchParams = {
          ...params,
          // 确保包含模块信息
          datasourceId: searchParams.datasourceId,
          module: searchParams.module,
        };
        localStorage.setItem('searchBarParams', JSON.stringify(currentSearchParams));
        console.log('🔄 SearchBar更新localStorage:', currentSearchParams);
      } catch (error) {
        console.error('SearchBar更新localStorage失败:', error);
      }

      getDistributionWithSearchBar();
    }

    console.log('🔍 SearchBar useEffect 已触发所有API请求', {
      fromSearch: timeState.timeOption?._fromSearch,
      forceUpdate: timeState.timeOption?._forceUpdate,
      params: params,
      searchParams: searchParams,
    });

    // 如果是来自搜索按钮的触发，执行完成后清除标识
    if (isFromSearchButton) {
      setTimeOption((prev: ITimeOption) => ({
        ...prev,
        _fromSearch: false,
      }));
    }
  }, [timeState.timeOption]); // 只依赖timeOption

  // 处理搜索提交
  const handleSubmit = () => {
    searchActions.handleSubmit(searchState.keyword, searchState.sql, clearInputs);
  };

  // 处理自动刷新
  const handleAutoRefresh = () => {
    if (onRefresh) {
      // 自动刷新时，直接调用父组件的onRefresh方法
      // 父组件会通过ref调用SearchBar的autoRefresh方法来更新时间
      onRefresh();
    }
  };

  // 处理时间分组变化
  const handleTimeGroupChange = (text: string) => {
    const latestTime = getLatestTime(timeState.timeOption);
    setTimeGroup(text);
    setTimeOption((prev: any) => ({ ...prev, range: [latestTime.startTime, latestTime.endTime] }));
    setOpenTimeGroup(false);
  };

  // 处理时间标签点击
  const handleTimeFromTag = () => {
    setOpenTimeRange(true);
  };

  // 渲染过滤标签
  const filterRender = useMemo(
    () => (
      <div className={styles.filter}>
        <FilterTags
          keywords={keywords}
          sqls={sqls}
          timeOption={timeState.timeOption}
          onClickKeyword={searchActions.handleClickKeyword}
          onCloseKeyword={searchActions.handleCloseKeyword}
          onClickSql={searchActions.handleClickSql}
          onCloseSql={searchActions.handleCloseSql}
          onClickTime={handleTimeFromTag}
        />
      </div>
    ),
    [sqls, keywords, timeState.timeOption, searchActions, handleTimeFromTag, styles.filter],
  );

  // 渲染左侧统计信息
  const leftRender = useMemo(() => <StatisticsInfo totalCount={totalCount} />, [totalCount]);

  // 渲染时间范围选择器
  const timeRender = useMemo(
    () => (
      <TimePickerWrapper
        timeOption={timeState.timeOption}
        open={timeState.openTimeRange}
        onOpenChange={setOpenTimeRange}
        onSubmit={submitTime}
        activeTab={timeState.activeTab}
        setActiveTab={setActiveTab}
      />
    ),
    [timeState.openTimeRange, timeState.timeOption, timeState.activeTab, setOpenTimeRange, submitTime, setActiveTab],
  );

  // 渲染关键词输入框
  const keywordRender = useMemo(
    () => <KeywordInput value={searchState.keyword} onChange={changeKeyword} />,
    [searchState.keyword, changeKeyword],
  );

  // 渲染SQL输入框
  const sqlRender = useMemo(
    () => <SqlInput value={searchState.sql} onChange={changeSql} columns={columns} />,
    [searchState.sql, changeSql, columns],
  );

  // 渲染时间分组选择器
  const timeGroupRender = useMemo(
    () => (
      <TimeGroupSelector
        timeGrouping={searchParams.timeGrouping}
        open={timeState.openTimeGroup}
        onOpenChange={setOpenTimeGroup}
        onChange={handleTimeGroupChange}
      />
    ),
    [searchParams.timeGrouping, timeState.openTimeGroup, setOpenTimeGroup, handleTimeGroupChange],
  );

  return (
    <div className={styles.searchBar} ref={searchBarRef}>
      <div className={styles.top}>
        <div className={styles.left}>{leftRender}</div>
        <div className={styles.right}>
          <Space size={STYLES.SPACE_SIZE}>
            <AutoRefresh onRefresh={handleAutoRefresh} loading={loading} disabled={false} />
            <SaveSearchButton
              searchParams={{
                keywords,
                whereSqls: sqls,
                timeRange: timeState.timeOption.value,
                startTime: timeState.timeOption.range?.[0],
                endTime: timeState.timeOption.range?.[1],
                timeGrouping: timeState.timeGroup,
                module: searchParams.module,
                sortConfig,
                timeType: timeState.timeOption?.type, // 添加时间类型信息
                ...(timeState.timeOption?.type === 'relative' &&
                  timeState.timeOption?.startOption &&
                  timeState.timeOption?.endOption && {
                    relativeStartOption: timeState.timeOption.startOption,
                    relativeEndOption: timeState.timeOption.endOption,
                  }),
              }}
              size="small"
            />
            <SavedSearchesButton onLoadSearch={searchActions.handleLoadSearch} size="small" />
            <ShareButton
              searchParams={{
                keywords,
                whereSqls: sqls,
                timeRange: timeState.timeOption.value,
                startTime: timeState.timeOption.range?.[0],
                endTime: timeState.timeOption.range?.[1],
                timeGrouping: timeState.timeGroup,
                module: searchParams.module,
                fields: activeColumns, // 使用用户实际选择的字段列表
                sortConfig,
                timeType: timeState.timeOption?.type, // 添加时间类型信息
                ...(timeState.timeOption?.type === 'relative' &&
                  timeState.timeOption?.startOption &&
                  timeState.timeOption?.endOption && {
                    relativeStartOption: timeState.timeOption.startOption,
                    relativeEndOption: timeState.timeOption.endOption,
                  }),
              }}
              size="small"
            />
            {timeGroupRender}
            {timeRender}
          </Space>
        </div>
      </div>
      <div className={styles.form}>
        <div className={styles.item}>{keywordRender}</div>
        <div className={styles.item}>{sqlRender}</div>
        <div className={styles.item}>
          <Button size={SEARCH_BUTTON.SIZE} type={SEARCH_BUTTON.TYPE} onClick={handleSubmit} loading={loading}>
            {SEARCH_BUTTON.TEXT}
          </Button>
        </div>
      </div>
      <div className={styles.footer}>{filterRender}</div>
    </div>
  );
});

export default SearchBar;
