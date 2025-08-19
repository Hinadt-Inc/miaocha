import { useMemo, useCallback, useEffect } from 'react';
import { Splitter } from 'antd';
import SearchBar from './SearchBar/index';
import Log from './LogModule/index';
import Sider from './SiderModule/index';
import styles from './index.module.less';
import AIAssistant from '@/components/AIAssistant/index';

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
    // 添加必要的状态解构
    setKeywords,
    setActiveColumns,
    setLogTableColumns,
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
      // 检查当前选中模块是否已经加载了配置
      if (!loadedConfigModulesRef.current.has(selectedModule)) {
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
  }, [selectedModule]); // 只依赖selectedModule，避免循环

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
  }, [getModuleQueryConfig.data]); // 移除selectedModule依赖，避免循环更新

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

    // 同步更新localStorage中的searchBarParams，确保分布数据查询能获取到最新的whereSqls
    const savedSearchParams = localStorage.getItem('searchBarParams');
    if (savedSearchParams) {
      try {
        const params = JSON.parse(savedSearchParams);
        const updatedParams = {
          ...params,
          whereSqls: newSearchParams.whereSqls || [],
        };
        localStorage.setItem('searchBarParams', JSON.stringify(updatedParams));
      } catch (error) {
        console.error('更新localStorage中的searchBarParams失败:', error);
      }
    }

    searchBarRef?.current?.addSql?.(sql);
    setSearchParams(newSearchParams);
  };

  // 删除SQL条件的处理函数
  const handleRemoveSql = useCallback(
    (sql: string) => {
      // 直接从searchParams中删除该SQL并重新搜索
      const newSearchParams = {
        ...searchParams,
        offset: 0,
        whereSqls: searchParams?.whereSqls?.filter((item: any) => item !== sql),
      };

      if (newSearchParams?.whereSqls?.length === 0) {
        delete newSearchParams.whereSqls;
      }

      // 从whereSqlsFromSider中删除对应项
      state.setWhereSqlsFromSider((prev: any) => prev.filter((item: any) => item.label !== sql));

      // 更新localStorage中的searchBarParams
      const savedSearchParams = localStorage.getItem('searchBarParams');
      if (savedSearchParams) {
        const params = JSON.parse(savedSearchParams);
        const updatedParams = {
          ...params,
          whereSqls: params.whereSqls?.filter((item: string) => item !== sql) || [],
        };
        localStorage.setItem('searchBarParams', JSON.stringify(updatedParams));
      }

      // 重新搜索
      setSearchParams(newSearchParams);

      // 注意：删除SQL标签后的分布数据更新由Sider组件的useEffect自动处理
      // 不需要手动调用getDistributionWithSearchBar，避免重复调用
    },
    [searchParams, state.setWhereSqlsFromSider],
  );

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
      activeColumns: state.activeColumns, // 传递activeColumns用于同步左侧已选字段显示
      onColumnsLoaded: setColumnsLoaded, // 传递columns加载完成回调
    }),
    [
      searchParams,
      moduleOptions,
      moduleQueryConfig,
      selectedModule,
      state.activeColumns, // 添加activeColumns依赖
      // 移除了函数依赖，它们应该是稳定的
    ],
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
      state.sortConfig,
      // 移除了函数依赖，它们应该是稳定的
    ],
  );

  const searchBarProps = useMemo(
    () => ({
      searchParams,
      totalCount: detailData?.totalCount,
      onSearch: setSearchParams,
      onRefresh: handleRefresh,
      setWhereSqlsFromSider: handleSetWhereSqlsFromSider,
      onRemoveSql: handleRemoveSql,
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
      sharedParams: state.sharedParams,
      hasAppliedSharedParams: state.hasAppliedSharedParams,
    }),
    [
      searchParams,
      detailData?.totalCount,
      state.logTableColumns,
      state.activeColumns,
      state.sortConfig,
      state.commonColumns,
      getDetailData.loading,
      state.keywords,
      state.sqls,
      state.whereSqlsFromSider,
      state.sharedParams,
      state.hasAppliedSharedParams,
      // 移除了setter函数和回调函数的依赖，它们应该是稳定的
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

      {/* AI助手悬浮窗 */}
      <AIAssistant
        onLogSearch={(data) => {
          console.log('🏠 Home页面收到onLogSearch回调:', data);

          // 处理AI助手的搜索请求
          let searchParams = data.searchParams || data; // 向后兼容

          // 确保AI提供的searchParams包含必要的模块信息
          // 如果AI没有提供模块信息，使用当前的模块信息
          if (!searchParams.datasourceId || !searchParams.module) {
            console.log('🔧 AI搜索参数缺少模块信息，使用当前模块信息补充');
            searchParams = {
              ...searchParams,
              datasourceId: searchParams.datasourceId || state.searchParams.datasourceId,
              module: searchParams.module || state.searchParams.module,
            };
            console.log('🔧 补充后的searchParams:', searchParams);
          }

          // 如果有搜索结果，直接更新状态
          if (data.searchResult) {
            console.log('📊 直接更新detailData状态');
            setDetailData(data.searchResult);
          }

          // 更新搜索参数
          setSearchParams(searchParams);

          // 主动更新本地状态以同步到SearchBar
          if (searchParams.keywords && searchParams.keywords.length > 0) {
            setKeywords(searchParams.keywords);
          }

          // 同步更新SQL条件到SearchBar
          if (searchParams.whereSqls && searchParams.whereSqls.length > 0) {
            // 更新sqls状态，这会触发SearchBar的useEffect重新搜索
            state.setSqls(searchParams.whereSqls);
          } else {
            // 如果没有SQL条件，清空现有的SQL条件
            state.setSqls([]);
          }

          // 主动更新SearchBar组件的显示状态
          if (searchBarRef.current && searchParams) {
            // 更新时间范围
            if (
              searchParams.startTime &&
              searchParams.endTime &&
              typeof searchBarRef.current.setTimeOption === 'function'
            ) {
              const timeOption = {
                label: `${searchParams.startTime} ~ ${searchParams.endTime}`,
                value: `${searchParams.startTime} ~ ${searchParams.endTime}`,
                range: [searchParams.startTime, searchParams.endTime],
                type: 'absolute',
              };
              searchBarRef.current.setTimeOption(timeOption);
            }

            // 更新字段选择
            if (searchParams.fields && searchParams.fields.length > 0) {
              setActiveColumns(searchParams.fields);

              // 同步更新logTableColumns的selected状态
              setLogTableColumns((prevColumns: any) => {
                return prevColumns.map((column: any) => ({
                  ...column,
                  selected: searchParams.fields!.includes(column.columnName || ''),
                  _createTime: searchParams.fields!.includes(column.columnName || '') ? Date.now() : undefined,
                }));
              });
            }
          }

          // 只有在没有skipRequest标记时才触发新的搜索请求
          if (!data.skipRequest) {
            console.log('🔄 触发executeDataRequest');
            executeDataRequest(searchParams);

            // 同步更新localStorage中的searchBarParams，确保字段分布查询能获取到最新参数
            try {
              const savedSearchParams = localStorage.getItem('searchBarParams');
              const currentParams = savedSearchParams ? JSON.parse(savedSearchParams) : {};
              const updatedParams = {
                ...currentParams,
                ...searchParams,
                // 确保关键信息不丢失
                datasourceId: searchParams.datasourceId,
                module: searchParams.module,
              };
              localStorage.setItem('searchBarParams', JSON.stringify(updatedParams));
              console.log('✅ 已更新localStorage中的searchBarParams:', updatedParams);
            } catch (error) {
              console.error('更新localStorage中的searchBarParams失败:', error);
            }

            // 同时触发字段分布数据更新
            console.log('🔄 触发getDistributionWithSearchBar');
            // 需要延迟执行，确保localStorage和字段状态已经更新
            setTimeout(() => {
              getDistributionWithSearchBar();
            }, 100);
          } else {
            console.log('⏭️ 跳过重复请求 (skipRequest=true)');
          }
        }}
        onFieldSelect={(fields) => {
          // 更新显示字段
          setActiveColumns(fields);
        }}
        onTimeRangeChange={(data) => {
          console.log('🏠 Home页面收到onTimeRangeChange回调:', data);

          // 处理时间范围变更
          let timeRangeData = data;

          // 向后兼容处理
          if (typeof data === 'string') {
            timeRangeData = { timeRange: data };
          }

          // 如果有直方图数据，直接更新状态
          if (timeRangeData.histogramData) {
            console.log('📊 直接更新histogramData状态:', timeRangeData.histogramData);
            console.log('📊 检查distributionData:', {
              hasDistributionData: !!timeRangeData.histogramData.distributionData,
              length: timeRangeData.histogramData.distributionData?.length,
              firstItem: timeRangeData.histogramData.distributionData?.[0],
            });
            // 修正：直接设置整个histogramData，而不是取第一个元素
            setHistogramData(timeRangeData.histogramData);
          }

          // 更新搜索参数中的时间范围
          const newSearchParams = {
            ...searchParams,
            timeRange: timeRangeData.timeRange,
            startTime: timeRangeData.startTime,
            endTime: timeRangeData.endTime,
          };
          setSearchParams(newSearchParams);

          // 只有在没有skipRequest标记时才触发新的搜索请求
          if (!timeRangeData.skipRequest) {
            console.log('🔄 触发executeDataRequest');
            executeDataRequest(newSearchParams);
          } else {
            console.log('⏭️ 跳过重复请求 (skipRequest=true)');
          }
        }}
      />
    </div>
  );
};

export default HomePage;
