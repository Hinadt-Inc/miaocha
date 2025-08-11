import { useMemo, useCallback, useEffect } from 'react';
import { Splitter } from 'antd';
import SearchBar from './SearchBar';
import Log from './Log';
import Sider from './Sider';
import styles from './index.module.less';

// 导入模块化的hooks
import {
  useHomePageState,
  useModulesList,
  useLogDetails,
  useLogHistogram,
  useModuleQueryConfig,
  useUrlParams,
  useOAuthCallback,
  useBusinessLogic,
} from './hooks';

// 导入类型和常量
import type { ILogSearchParams, IStatus } from './types';

/**
 * Home页面组件
 * 使用模块化的hooks来组织代码，提供日志查询和分析功能
 */
const HomePage = () => {
  // 1. 获取所有状态和refs
  const state = useHomePageState();
  const {
    moduleOptions,
    setModuleOptions,
    detailData,
    setDetailData,
    histogramData,
    setHistogramData,
    searchParams,
    setSearchParams,
    selectedModule,
    setSelectedModule,
    moduleQueryConfig,
    setModuleQueryConfig,
    setLoadingQueryConfig,
    columnsLoaded,
    setColumnsLoaded,
    isInitializingRef,
    loadedConfigModulesRef,
    setIsInitialized,
    lastCallParamsRef,
    abortRef,
    searchBarRef,
    siderRef,
    requestTimerRef,
  } = state;

  // 2. OAuth回调处理
  useOAuthCallback();

  // 3. URL参数处理
  const { cleanupUrlParams } = useUrlParams(
    state.sharedParams,
    state.setSharedParams,
    setSelectedModule,
    state.processedUrlRef,
  );

  // 4. 数据请求hooks
  const modulesList = useModulesList();
  const getDetailData = useLogDetails(moduleQueryConfig);
  const getHistogramData = useLogHistogram();
  const getModuleQueryConfig = useModuleQueryConfig();

  // 5. 执行数据请求的函数
  const executeDataRequest = useCallback(
    (params: ILogSearchParams) => {
      // 取消之前的请求
      if (abortRef.current) {
        abortRef.current.abort();
      }

      abortRef.current = new AbortController();
      const requestParams = { ...params, signal: abortRef.current.signal };

      getDetailData.run(requestParams);
      getHistogramData.run(requestParams);
    },
    [getDetailData, getHistogramData],
  );

  // 6. 业务逻辑处理
  const { generateModuleOptions, handleSelectedModuleChange, handleChangeColumns, handleChangeColumnsByLog } =
    useBusinessLogic(state, executeDataRequest, cleanupUrlParams, columnsLoaded);

  // 7. 处理模块列表请求成功
  useEffect(() => {
    if (modulesList.data) {
      isInitializingRef.current = true;

      const moduleOptions = generateModuleOptions(modulesList.data);
      setModuleOptions(moduleOptions);

      // 如果有分享参数，优先应用分享的模块
      if (state.sharedParams?.module && !state.hasAppliedSharedParams) {
        const sharedModuleOption = moduleOptions.find(
          (option: IStatus) => option.module === state.sharedParams?.module,
        );
        if (sharedModuleOption) {
          setSelectedModule(state.sharedParams.module);
          setSearchParams((prev: ILogSearchParams) => ({
            ...prev,
            datasourceId: Number(sharedModuleOption.datasourceId),
            module: state.sharedParams?.module || prev.module,
          }));
          return; // 分享参数会在后续的 useEffect 中完整应用
        }
      }

      // 只在初始化时设置默认模块，避免重复设置
      if ((!searchParams.datasourceId || !searchParams.module) && moduleOptions[0]) {
        const defaultOption = moduleOptions[0];

        // 批量更新状态，避免多次渲染
        setSelectedModule(defaultOption.module);
        setSearchParams((prev: ILogSearchParams) => ({
          ...prev,
          datasourceId: Number(defaultOption.datasourceId),
          module: defaultOption.module,
        }));
      }
    }
  }, [modulesList.data]);

  // 8. 处理数据请求结果
  useEffect(() => {
    if (getDetailData.data) {
      setDetailData(getDetailData.data);
    }
  }, [getDetailData.data]);

  useEffect(() => {
    if (getHistogramData.data) {
      setHistogramData(getHistogramData.data as any);
    }
  }, [getHistogramData.data]);

  // 9. 当selectedModule变化时，获取模块查询配置
  useEffect(() => {
    if (selectedModule) {
      if (selectedModule !== moduleQueryConfig?.module) {
        setIsInitialized(false);
        lastCallParamsRef.current = '';
        isInitializingRef.current = true;
        setColumnsLoaded(false); // 重置columns加载状态

        getModuleQueryConfig.run(selectedModule);
      }
    } else {
      setModuleQueryConfig(null);
      setIsInitialized(false);
      lastCallParamsRef.current = '';
      isInitializingRef.current = false;
      setColumnsLoaded(false); // 重置columns加载状态
      loadedConfigModulesRef.current.clear();
    }
  }, [selectedModule]);

  // 10. 处理模块查询配置请求结果
  useEffect(() => {
    if (getModuleQueryConfig.data) {
      setLoadingQueryConfig(false);
      // 为QueryConfig添加module属性以匹配IModuleQueryConfig
      const configWithModule = {
        ...getModuleQueryConfig.data,
        module: selectedModule,
      };
      setModuleQueryConfig(configWithModule);
      loadedConfigModulesRef.current.add(selectedModule);

      // 清除初始化标记，允许数据请求执行
      setTimeout(() => {
        isInitializingRef.current = false;
      }, 100);
    }
  }, [getModuleQueryConfig.data, selectedModule]);

  // 11. 其他业务处理函数
  const handleSetWhereSqlsFromSider = (flag: '=' | '!=', columnName: string, value: string) => {
    const sql = `${columnName} ${flag} '${value}'`;
    const newSearchParams = {
      ...searchParams,
      offset: 0,
    };

    if (flag === '=') {
      const oldSql = `${columnName} != '${value}'`;
      newSearchParams.whereSqls = [...(searchParams?.whereSqls || []), sql];
      searchBarRef?.current?.removeSql?.(oldSql);
      state.setWhereSqlsFromSider((prev: any) => [
        ...prev,
        {
          label: sql,
          value: value,
          field: columnName,
        },
      ]);
    } else {
      const oldSql = `${columnName} = '${value}'`;
      newSearchParams.whereSqls = searchParams?.whereSqls?.filter((item: any) => item !== oldSql);
      searchBarRef?.current?.removeSql?.(oldSql);
      state.setWhereSqlsFromSider((prev: any) => prev.filter((item: any) => item.value !== value));
    }

    if (newSearchParams?.whereSqls?.length === 0) {
      delete newSearchParams.whereSqls;
    }

    searchBarRef?.current?.addSql?.(sql);
    setSearchParams(newSearchParams);
  };

  const handleSortChange = useCallback((newSortConfig: any[]) => {
    state.setSortConfig(newSortConfig);
  }, []);

  const getDistributionWithSearchBar = useCallback(() => {
    siderRef.current?.getDistributionWithSearchBar?.();
  }, []);

  const onSearchFromLog = (params: ILogSearchParams) => {
    const { startTime, endTime } = params;
    const timeOption = {
      label: `${startTime} ~ ${endTime}`,
      value: `${startTime} ~ ${endTime}`,
      range: [startTime, endTime],
      type: 'absolute' as const,
    };
    searchBarRef?.current?.setTimeOption(timeOption);
  };

  const handleRefresh = useCallback(() => {
    if (searchBarRef.current?.autoRefresh) {
      searchBarRef.current.autoRefresh();
    } else if (searchParams.datasourceId && searchParams.module && moduleQueryConfig) {
      executeDataRequest(searchParams);
    }
  }, [searchParams, moduleQueryConfig, executeDataRequest]);

  // 12. 组件卸载时清理定时器
  useEffect(() => {
    return () => {
      if (requestTimerRef.current) {
        clearTimeout(requestTimerRef.current);
      }
      if (abortRef.current) {
        abortRef.current.abort();
      }
      isInitializingRef.current = false;
      loadedConfigModulesRef.current.clear();
    };
  }, []);

  // 13. 组件props优化
  const siderProps = useMemo(
    () => ({
      searchParams,
      modules: moduleOptions,
      setWhereSqlsFromSider: handleSetWhereSqlsFromSider,
      onSearch: setSearchParams,
      onChangeColumns: handleChangeColumns,
      onActiveColumnsChange: state.setActiveColumns,
      onSelectedModuleChange: handleSelectedModuleChange,
      moduleQueryConfig,
      onCommonColumnsChange: state.setCommonColumns,
      selectedModule,
      onColumnsLoaded: setColumnsLoaded, // 传递columns加载完成回调
    }),
    [searchParams, moduleOptions, moduleQueryConfig, selectedModule, handleSelectedModuleChange, handleChangeColumns],
  );

  const logProps: any = useMemo(
    () => ({
      histogramData,
      detailData,
      getDetailData,
      searchParams,
      dynamicColumns: state.logTableColumns,
      whereSqlsFromSider: state.whereSqlsFromSider,
      sqls: state.sqls,
      onSearch: onSearchFromLog,
      onChangeColumns: handleChangeColumnsByLog,
      onSearchFromTable: setSearchParams,
      moduleQueryConfig,
      onSortChange: handleSortChange,
    }),
    [
      histogramData,
      detailData,
      getDetailData,
      state.logTableColumns,
      searchParams,
      state.whereSqlsFromSider,
      state.sqls,
      moduleQueryConfig,
      handleSortChange,
      handleChangeColumnsByLog,
      state.sortConfig,
    ],
  );

  const searchBarProps = useMemo(
    () => ({
      searchParams,
      totalCount: detailData?.totalCount,
      onSearch: setSearchParams,
      onRefresh: handleRefresh,
      setWhereSqlsFromSider: handleSetWhereSqlsFromSider,
      columns: state.logTableColumns,
      onSqlsChange: state.setSqls,
      activeColumns: state.activeColumns,
      getDistributionWithSearchBar,
      sortConfig: state.sortConfig,
      commonColumns: state.commonColumns,
      loading: getDetailData.loading,
      keywords: state.keywords,
      setKeywords: state.setKeywords,
      sqls: state.sqls,
      setSqls: state.setSqls,
      setWhereSqlsFromSiderArr: state.whereSqlsFromSider,
      sharedParams: state.sharedParams,
      hasAppliedSharedParams: state.hasAppliedSharedParams,
    }),
    [
      searchParams,
      detailData?.totalCount,
      setSearchParams,
      handleRefresh,
      state.logTableColumns,
      state.activeColumns,
      getDistributionWithSearchBar,
      state.sortConfig,
      state.commonColumns,
      getDetailData.loading,
      state.keywords,
      state.sqls,
      state.whereSqlsFromSider,
      state.sharedParams,
      state.hasAppliedSharedParams,
    ],
  );

  return (
    <div className={styles.layout}>
      <SearchBar ref={searchBarRef} {...(searchBarProps as any)} />

      <Splitter className={styles.container}>
        <Splitter.Panel collapsible defaultSize={200} min={0} max="40%">
          <Sider ref={siderRef} {...(siderProps as any)} />
        </Splitter.Panel>
        <Splitter.Panel collapsible>
          <div className={styles.right}>
            <Log {...logProps} />
          </div>
        </Splitter.Panel>
      </Splitter>
    </div>
  );
};

export default HomePage;
