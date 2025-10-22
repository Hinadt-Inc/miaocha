import { useState, useMemo, useEffect, forwardRef, useImperativeHandle, useRef, useCallback, memo } from 'react';

import { Button, Space } from 'antd';
import dayjs from 'dayjs';

import AutoRefresh from '../AutoRefresh/index';
import SavedSearchesButton from '../SavedSearchesButton';
import SaveSearchButton from '../SaveSearchButton';
import ShareButton from '../ShareButton';
import type { ITimeOption } from '../types';
import { getLatestTime, DATE_FORMAT_THOUSOND, QUICK_RANGES } from '../utils';

import { FilterTags, StatisticsInfo, KeywordInput, SqlInput, TimePickerWrapper, TimeGroupSelector } from './components';
import { STYLES } from './constants';
import { useSearchInput, useTimeState, useSearchActions } from './hooks';
import styles from './index.module.less';
import type { SearchBarProps, SearchBarRef } from './types';

const SearchBar = forwardRef<SearchBarRef, SearchBarProps>((props, ref) => {
  console.log('渲染：SearchBar组件');
  const {
    // 数据与配置
    searchParams,
    totalCount = 0,
    columns,
    activeColumns,
    commonColumns = [],
    sortConfig = [],

    // 查询状态
    keywords,
    sqls,

    // 加载态
    loading = false,

    // 回调（动作）
    onSearch,
    onRefresh,
    onSqlsChange,
    setKeywords,
    setSqls,
    setWhereSqlsFromSider,
    onRemoveSql,
    refreshFieldDistributions,
  } = props;

  const searchBarRef = useRef<HTMLDivElement>(null); // 搜索栏的ref
  const [initialized] = useState(true); // 标记是否初始化完成
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
      const newTimeString = `${dayjs(searchParams.startTime).format('YYYY-MM-DD HH:mm:ss')} ~ ${dayjs(searchParams.endTime).format('YYYY-MM-DD HH:mm:ss')}`;
      const currentTimeString = `${dayjs(timeState.timeOption?.range?.[0]).format('YYYY-MM-DD HH:mm:ss')} ~ ${dayjs(timeState.timeOption?.range?.[1]).format('YYYY-MM-DD HH:mm:ss')}`;

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

    // 页面第一次加载时不调用refreshFieldDistributions接口
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

    // 调用Sider组件的refreshFieldDistributions方法，但第一次加载时跳过
    if (shouldCallDistribution && refreshFieldDistributions) {
      refreshFieldDistributions();
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
    // 移除了可能导致循环的依赖：onSearch, refreshFieldDistributions, searchParams, onSqlsChange
  ]);

  // 单独处理时间变化的搜索
  useEffect(() => {
    // 如果是来自搜索按钮的强制触发，跳过初始化和columns检查
    const isFromSearchButton = timeState.timeOption?._fromSearch;

    if (!isFromSearchButton) {
      // 非搜索按钮触发的情况，需要检查初始化状态和columns
      if (!initialized || commonColumns.length === 0) {
        return;
      }

      // 如果时间更新来自外部params，不要重复搜索
      if (timeUpdateFromParamsRef.current) {
        timeUpdateFromParamsRef.current = false;
        return;
      }
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

    // 先构建基础参数，确保模块信息不丢失
    // 如果searchParams中没有模块信息，尝试从localStorage获取
    let effectiveDatasourceId = searchParams.datasourceId;
    let effectiveModule = searchParams.module;

    if (!effectiveDatasourceId || !effectiveModule) {
      try {
        const savedParams = localStorage.getItem('searchBarParams');
        if (savedParams) {
          const parsed = JSON.parse(savedParams);
          effectiveDatasourceId = effectiveDatasourceId || parsed.datasourceId;
          effectiveModule = effectiveModule || parsed.module;
        }
      } catch (error) {
        console.error('从localStorage获取模块信息失败:', error);
      }
    }

    const baseParams = {
      datasourceId: effectiveDatasourceId,
      module: effectiveModule,
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
    if (refreshFieldDistributions) {
      // 在调用字段分布查询之前，确保localStorage中有最新的参数
      try {
        const currentSearchParams = {
          ...params,
          // 确保包含有效的模块信息
          datasourceId: effectiveDatasourceId,
          module: effectiveModule,
        };
        localStorage.setItem('searchBarParams', JSON.stringify(currentSearchParams));
      } catch (error) {
        console.error('SearchBar更新localStorage失败:', error);
      }

      refreshFieldDistributions();
    }

    // 如果是来自搜索按钮的触发，执行完成后清除标识
    if (isFromSearchButton) {
      setTimeOption((prev: ITimeOption) => ({
        ...prev,
        _fromSearch: false,
      }));
    }
  }, [timeState.timeOption]); // 只依赖timeOption

  // 处理搜索提交
  const handleSubmit = useCallback(() => {
    searchActions.handleSubmit(searchState.keyword, searchState.sql, clearInputs);
  }, [searchActions, searchState.keyword, searchState.sql, clearInputs]);

  // 处理自动刷新
  const handleAutoRefresh = useCallback(() => {
    if (onRefresh) {
      onRefresh();
    }
  }, [onRefresh]);

  // 处理时间分组变化
  const handleTimeGroupChange = useCallback(
    (text: string) => {
      const latestTime = getLatestTime(timeState.timeOption);
      setTimeGroup(text);
      setTimeOption((prev: any) => ({ ...prev, range: [latestTime.startTime, latestTime.endTime] }));
      setOpenTimeGroup(false);
    },
    [timeState.timeOption, setTimeGroup, setTimeOption, setOpenTimeGroup],
  );

  // 处理时间标签点击
  const handleTimeFromTag = useCallback(() => {
    setOpenTimeRange(true);
  }, [setOpenTimeRange]);

  // 渲染过滤标签
  const filterRender = useMemo(
    () => (
      <div className={styles.filter}>
        <FilterTags
          // 数据
          keywords={keywords}
          sqls={sqls}
          timeOption={timeState.timeOption}
          // 交互回调
          onClickKeyword={searchActions.handleClickKeyword}
          onClickSql={searchActions.handleClickSql}
          onClickTime={handleTimeFromTag}
          onCloseKeyword={searchActions.handleCloseKeyword}
          onCloseSql={searchActions.handleCloseSql}
        />
      </div>
    ),
    [
      // 数据
      keywords,
      sqls,
      timeState.timeOption,
      // 交互回调
      searchActions,
      handleTimeFromTag,
    ],
  );

  // 渲染左侧统计信息
  const leftRender = useMemo(() => <StatisticsInfo totalCount={totalCount} />, [totalCount]);

  // 渲染时间范围选择器
  const timeRender = useMemo(
    () => (
      <TimePickerWrapper
        // 状态
        activeTab={timeState.activeTab}
        open={timeState.openTimeRange}
        onOpenChange={setOpenTimeRange}
        onSubmit={submitTime}
        timeOption={timeState.timeOption}
        // 回调
        setActiveTab={setActiveTab}
      />
    ),
    [
      // 状态
      timeState.openTimeRange,
      timeState.timeOption,
      timeState.activeTab,
      // 回调
      setOpenTimeRange,
      submitTime,
      setActiveTab,
    ],
  );

  // 渲染关键词输入框
  const keywordRender = useMemo(
    () => <KeywordInput value={searchState.keyword} onChange={changeKeyword} />,
    [searchState.keyword, changeKeyword],
  );

  // 渲染SQL输入框
  const sqlRender = useMemo(
    () => <SqlInput columns={columns} value={searchState.sql} onChange={changeSql} />,
    [searchState.sql, changeSql, columns],
  );

  // 渲染时间分组选择器
  const timeGroupRender = useMemo(
    () => (
      <TimeGroupSelector
        // 状态
        open={timeState.openTimeGroup}
        timeGrouping={searchParams.timeGrouping}
        // 回调
        onChange={handleTimeGroupChange}
        onOpenChange={setOpenTimeGroup}
      />
    ),
    [
      // 状态
      searchParams.timeGrouping,
      timeState.openTimeGroup,
      // 回调
      setOpenTimeGroup,
      handleTimeGroupChange,
    ],
  );

  return (
    <div ref={searchBarRef} className={styles.searchBar}>
      <div className={styles.top}>
        <div className={styles.left}>{leftRender}</div>
        <div className={styles.right}>
          <Space size={STYLES.SPACE_SIZE}>
            <AutoRefresh disabled={false} loading={loading} onRefresh={handleAutoRefresh} />
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
            <SavedSearchesButton size="small" onLoadSearch={searchActions.handleLoadSearch} />
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
          <Button loading={loading} size="small" type="primary" onClick={handleSubmit}>
            搜索
          </Button>
        </div>
      </div>
      <div className={styles.footer}>{filterRender}</div>
    </div>
  );
});

export default memo(SearchBar);
