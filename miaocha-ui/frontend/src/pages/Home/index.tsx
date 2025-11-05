import { useCallback, useEffect, useMemo } from 'react';

import { Splitter } from 'antd';

import AIAssistantPanel from './components/AIAssistantPanel';
import {
  // useOAuthCallback,
  useBusinessLogic,
  useHomePageState,
  useLogDetails,
  useLogHistogram,
  useModuleQueryConfig,
  useModulesList,
  useUrlParams,
} from './hooks';
import styles from './index.module.less';
import Log from './LogModule/index';
import SearchBar from './SearchBar';
import Sider from './SiderModule/index';
import type { ILogSearchParams } from './types';

/**
 * Home页面组件
 * 使用模块化的hooks来组织代码，提供日志查询和分析功能
 */
const HomePage = () => {
  // 1. 获取所有状态和refs（状态集合器）
  const state = useHomePageState();
  const {
    moduleOptions,
    setModuleOptions,
    detailData,
    setDetailData,
    originalDetailData,
    setOriginalDetailData,
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
  // todo 建议删除
  // useOAuthCallback();

  // 3. URL参数处理（指分享）
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

        setSearchParams((prev: ILogSearchParams) => {
          const payload = {
            ...prev,
            datasourceId: Number(defaultOption.datasourceId),
            module: defaultOption.module,
          };
          const savedSearchParams = localStorage.getItem('searchBarParams');
          if (savedSearchParams) {
            try {
              const params = JSON.parse(savedSearchParams);
              const updatedParams = {
                ...params,
                ...payload,
              };
              localStorage.setItem('searchBarParams', JSON.stringify(updatedParams));
            } catch (error) {
              console.error('更新localStorage中的searchBarParams失败:', error);
              // 清理损坏的数据
              localStorage.removeItem('searchBarParams');
            }
          }
          return payload;
        });
      }
    }
  }, [modulesList.data]);

  // 8. 处理数据请求结果
  useEffect(() => {
    if (getDetailData.data) {
      // 检查当前是否有选择的字段
      const hasSelectedFields = state.activeColumns && state.activeColumns.length > 0;

      if (!hasSelectedFields) {
        // 没有选择字段时，保存原始完整数据
        setOriginalDetailData(getDetailData.data);
      } else {
        // 有选择字段时，为每条记录添加原始数据引用
        if (originalDetailData && getDetailData.data.rows) {
          const enhancedRows = getDetailData.data.rows.map((row: any, index: number) => {
            const originalRow = originalDetailData.rows?.[index];
            if (originalRow) {
              return {
                ...row,
                _originalSource: originalRow,
              };
            }
            return row;
          });

          setDetailData({
            ...getDetailData.data,
            rows: enhancedRows,
          });
          return;
        }
      }

      setDetailData(getDetailData.data);
    }
  }, [getDetailData.data, state.activeColumns, originalDetailData]);

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
  const handleSetWhereSqlsFromSider = useCallback(
    (flag: '=' | '!=', columnName: string, value: string) => {
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
    },
    [searchParams, state.setWhereSqlsFromSider, setSearchParams],
  );

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
      // 不需要手动调用refreshFieldDistributions，避免重复调用
    },
    [searchParams, state.setWhereSqlsFromSider],
  );

  const handleSortChange = useCallback((newSortConfig: any[]) => {
    state.setSortConfig(newSortConfig);
  }, []);

  // 分布数据刷新
  const refreshFieldDistributions = useCallback(() => {
    siderRef.current?.refreshFieldDistributions?.();
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
      // 数据与配置
      searchParams,
      modules: moduleOptions,
      moduleQueryConfig,
      selectedModule,
      activeColumns: state.activeColumns, // 传递activeColumns用于同步左侧已选字段显示
      setActiveColumns: state.setActiveColumns,
      // 回调（动作）
      onSearch: setSearchParams,
      setWhereSqlsFromSider: handleSetWhereSqlsFromSider,
      onChangeColumns: handleChangeColumns,
      onSelectedModuleChange: handleSelectedModuleChange,
      onActiveColumnsChange: state.setActiveColumns,
      onCommonColumnsChange: state.setCommonColumns,
      onColumnsLoaded: setColumnsLoaded, // 传递columns加载完成回调
    }),
    [
      // 数据与配置
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
      // 数据与配置
      histogramData,
      detailData,
      searchParams,
      moduleQueryConfig,
      dynamicColumns: state.logTableColumns,
      whereSqlsFromSider: state.whereSqlsFromSider,
      sqls: state.sqls,

      // 加载和排序信息
      getDetailData,
      onSortChange: handleSortChange,

      // 回调（动作）
      onSearch: onSearchFromLog,
      onChangeColumns: handleChangeColumnsByLog,
      onSearchFromTable: setSearchParams,
    }),
    [
      // 数据与配置
      histogramData,
      detailData,
      searchParams,
      moduleQueryConfig,
      state.logTableColumns,
      state.whereSqlsFromSider,
      state.sqls,

      // 加载和排序信息
      getDetailData,
      state.sortConfig,
      // 移除了函数依赖，它们应该是稳定的
    ],
  );

  const searchBarProps = useMemo(
    () => ({
      // 数据与配置
      searchParams,
      totalCount: detailData?.totalCount,
      columns: state.logTableColumns,
      activeColumns: state.activeColumns,
      commonColumns: state.commonColumns,
      sortConfig: state.sortConfig,

      // 查询状态
      keywords: state.keywords,
      sqls: state.sqls,

      // 加载态
      loading: getDetailData.loading,

      // 回调（动作）
      onSearch: setSearchParams,
      onRefresh: handleRefresh,
      setWhereSqlsFromSider: handleSetWhereSqlsFromSider,
      onRemoveSql: handleRemoveSql,
      refreshFieldDistributions,
      onSqlsChange: state.setSqls,
      setKeywords: state.setKeywords,
      setSqls: state.setSqls,
    }),
    [
      // 数据与配置
      searchParams,
      detailData?.totalCount,
      state.logTableColumns,
      state.activeColumns,
      state.commonColumns,
      state.sortConfig,

      // 查询状态
      state.keywords,
      state.sqls,

      // 加载态
      getDetailData.loading,

      // 回调（动作）
      setSearchParams,
      handleRefresh,
      handleSetWhereSqlsFromSider,
      handleRemoveSql,
      refreshFieldDistributions,
      state.setSqls,
      state.setKeywords,
    ],
  );

  return (
    <div className={styles.layout}>
      <SearchBar ref={searchBarRef} {...searchBarProps} />

      <Splitter className={styles.container}>
        <Splitter.Panel collapsible defaultSize={200} max="50%" min={0}>
          <Sider ref={siderRef} {...(siderProps as any)} />
        </Splitter.Panel>
        <Splitter.Panel collapsible>
          <div className={styles.right}>
            <Log {...logProps} />
          </div>
        </Splitter.Panel>
      </Splitter>

      {/* AI助手悬浮窗 */}
      <AIAssistantPanel
        executeDataRequest={executeDataRequest}
        refreshFieldDistributions={refreshFieldDistributions}
        searchBarRef={searchBarRef}
        searchParams={searchParams as any}
        setActiveColumns={setActiveColumns}
        setDetailData={setDetailData}
        setHistogramData={setHistogramData}
        setKeywords={setKeywords}
        setLogTableColumns={setLogTableColumns}
        setSearchParams={setSearchParams}
        state={state as any}
      />
    </div>
  );
};

export default HomePage;
