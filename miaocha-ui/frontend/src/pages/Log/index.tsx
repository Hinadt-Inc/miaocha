import { useCallback, useEffect, useMemo, useRef } from 'react';

import { useDispatch, useSelector } from 'react-redux';

import {
  setModuleOptions,
  setDetailData,
  setOriginalDetailData,
  setHistogramData,
  setSearchParams,
  setSelectedModule,
  setModuleQueryConfig,
  setLoadingQueryConfig,
  setColumnsLoaded,
  setIsInitialized,
  setSharedParams,
  setWhereSqlsFromSider,
  setKeywords,
  setSqls,
  setActiveColumns,
  setSortConfig,
  setCommonColumns,
} from '@/store/logSlice';
import type { RootState, AppDispatch } from '@/store/store';

import { useBusinessLogic } from './hooks/useBusinessLogic';
import { useLogDetails, useLogHistogram, useModuleQueryConfig, useModulesList } from './hooks/useDataRequests';
import { useUrlParams } from './hooks/useUrlParams';
import styles from './index.module.less';
import SearchBar from './SearchBar';
import type { ILogSearchParams, ISharedParams, SortConfig } from './types';

/**
 * Log页面组件
 * 使用Redux store管理状态，提供日志查询和分析功能
 */
const LogPage = () => {
  const dispatch: AppDispatch = useDispatch();

  // 从 Redux store 中获取状态
  const logState = useSelector((state: RootState) => state.log);
  const {
    moduleOptions,
    detailData,
    originalDetailData,
    histogramData,
    searchParams,
    selectedModule,
    moduleQueryConfig,
    columnsLoaded,
    sharedParams,
    hasAppliedSharedParams,
    whereSqlsFromSider,
    keywords,
    sqls,
    activeColumns,
    logTableColumns,
    sortConfig,
    commonColumns,
  } = logState;

  // refs（这些仍然需要保留在组件中，因为它们不是状态）
  const searchBarRef = useRef<any>(null);
  const siderRef = useRef<any>(null);
  const abortRef = useRef<AbortController | null>(null);
  const loadedConfigModulesRef = useRef<Set<string>>(new Set());
  const lastCallParamsRef = useRef<string>('');
  const requestTimerRef = useRef<NodeJS.Timeout | null>(null);
  const isInitializingRef = useRef(false);
  const processedUrlRef = useRef<string>('');

  // 3. URL参数处理（指分享）
  const { cleanupUrlParams } = useUrlParams(
    sharedParams,
    (params: ISharedParams | null) => dispatch(setSharedParams(params)),
    (module: string) => dispatch(setSelectedModule(module)),
    processedUrlRef,
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
    useBusinessLogic(
      {
        // 传递必要的状态和方法给 useBusinessLogic
        sharedParams,
        hasAppliedSharedParams,
        activeColumns,
        setWhereSqlsFromSider: (value: any[]) => dispatch(setWhereSqlsFromSider(value)),
        setKeywords: (value: string[]) => dispatch(setKeywords(value)),
        setSqls: (value: string[]) => dispatch(setSqls(value)),
        setActiveColumns: (value: string[]) => dispatch(setActiveColumns(value)),
        setSortConfig: (value: SortConfig) => dispatch(setSortConfig(value)),
        setCommonColumns: (value: string[]) => dispatch(setCommonColumns(value)),
        logTableColumns,
        whereSqlsFromSider,
        keywords,
        sqls,
        sortConfig,
        commonColumns,
      },
      executeDataRequest,
      cleanupUrlParams,
      columnsLoaded,
    );

  // 7. 处理模块列表请求成功
  useEffect(() => {
    if (modulesList.data) {
      isInitializingRef.current = true;

      const moduleOptions = generateModuleOptions(modulesList.data);
      // 类型适配器，确保与Redux store中的IStatus类型匹配
      const adaptedModuleOptions = moduleOptions.map((option) => ({
        label: option.label,
        value: option.value,
        datasourceId: option.datasourceId,
        datasourceName: option.datasourceName,
        module: option.module,
      }));
      dispatch(setModuleOptions(adaptedModuleOptions));

      // 如果有分享参数，优先应用分享的模块
      if (sharedParams?.module && !hasAppliedSharedParams) {
        const sharedModuleOption = adaptedModuleOptions.find(
          (option: IStatus) => option.module === sharedParams?.module,
        );
        if (sharedModuleOption) {
          dispatch(setSelectedModule(sharedParams.module));
          dispatch(
            setSearchParams({
              ...searchParams,
              datasourceId: Number(sharedModuleOption.datasourceId),
              module: sharedParams?.module || searchParams.module,
            }),
          );
          return; // 分享参数会在后续的 useEffect 中完整应用
        }
      }

      // 只在初始化时设置默认模块，避免重复设置
      if ((!searchParams.datasourceId || !searchParams.module) && adaptedModuleOptions[0]) {
        const defaultOption = adaptedModuleOptions[0];

        // 批量更新状态，避免多次渲染
        dispatch(setSelectedModule(defaultOption.module));

        dispatch(
          setSearchParams({
            ...searchParams,
            datasourceId: Number(defaultOption.datasourceId),
            module: defaultOption.module,
          }),
        );
      }
    }
  }, [modulesList.data, sharedParams, hasAppliedSharedParams, searchParams, dispatch]);

  // 8. 处理数据请求结果
  useEffect(() => {
    if (getDetailData.data) {
      // 检查当前是否有选择的字段
      const hasSelectedFields = activeColumns && activeColumns.length > 0;

      if (!hasSelectedFields) {
        // 没有选择字段时，保存原始完整数据
        dispatch(setOriginalDetailData(getDetailData.data));
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

          dispatch(
            setDetailData({
              ...getDetailData.data,
              rows: enhancedRows,
            }),
          );
          return;
        }
      }

      dispatch(setDetailData(getDetailData.data));
    }
  }, [getDetailData.data, activeColumns, originalDetailData, dispatch]);

  useEffect(() => {
    if (getHistogramData.data) {
      dispatch(setHistogramData(getHistogramData.data as any));
    }
  }, [getHistogramData.data, dispatch]);

  // 9. 当selectedModule变化时，获取模块查询配置
  useEffect(() => {
    if (selectedModule) {
      // 检查当前选中模块是否已经加载了配置
      if (!loadedConfigModulesRef.current.has(selectedModule)) {
        dispatch(setIsInitialized(false));
        lastCallParamsRef.current = '';
        isInitializingRef.current = true;
        dispatch(setColumnsLoaded(false)); // 重置columns加载状态

        getModuleQueryConfig.run(selectedModule);
      }
    } else {
      dispatch(setModuleQueryConfig(null));
      dispatch(setIsInitialized(false));
      lastCallParamsRef.current = '';
      isInitializingRef.current = false;
      dispatch(setColumnsLoaded(false)); // 重置columns加载状态
      loadedConfigModulesRef.current.clear();
    }
  }, [selectedModule, dispatch]); // 只依赖selectedModule，避免循环

  // 10. 处理模块查询配置请求结果
  useEffect(() => {
    if (getModuleQueryConfig.data) {
      dispatch(setLoadingQueryConfig(false));
      // 为QueryConfig添加module属性以匹配IModuleQueryConfig
      const configWithModule = {
        ...getModuleQueryConfig.data,
        module: selectedModule,
      };
      dispatch(setModuleQueryConfig(configWithModule));
      loadedConfigModulesRef.current.add(selectedModule);

      // 清除初始化标记，允许数据请求执行
      setTimeout(() => {
        isInitializingRef.current = false;
      }, 100);
    }
  }, [getModuleQueryConfig.data, selectedModule, dispatch]); // 移除selectedModule依赖，避免循环更新

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
        dispatch(
          setWhereSqlsFromSider([
            ...whereSqlsFromSider,
            {
              label: sql,
              value: value,
              field: columnName,
            },
          ]),
        );
      } else {
        const oldSql = `${columnName} = '${value}'`;
        newSearchParams.whereSqls = searchParams?.whereSqls?.filter((item: any) => item !== oldSql);
        searchBarRef?.current?.removeSql?.(oldSql);
        dispatch(setWhereSqlsFromSider(whereSqlsFromSider.filter((item: any) => item.value !== value)));
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
      dispatch(setSearchParams(newSearchParams));
    },
    [searchParams, whereSqlsFromSider, dispatch],
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
      dispatch(setWhereSqlsFromSider(whereSqlsFromSider.filter((item: any) => item.label !== sql)));

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
      dispatch(setSearchParams(newSearchParams));

      // 注意：删除SQL标签后的分布数据更新由Sider组件的useEffect自动处理
      // 不需要手动调用refreshFieldDistributions，避免重复调用
    },
    [searchParams, whereSqlsFromSider, dispatch],
  );

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

  const searchBarProps = useMemo(
    () => ({
      // 数据与配置
      searchParams,
      totalCount: detailData?.totalCount,
      columns: logTableColumns,
      activeColumns,
      commonColumns,
      sortConfig,

      // 查询状态
      keywords,
      sqls,

      // 加载态
      loading: getDetailData.loading,

      // 回调（动作）
      onSearch: (params: any) => dispatch(setSearchParams(params)),
      onRefresh: handleRefresh,
      setWhereSqlsFromSider: handleSetWhereSqlsFromSider,
      onRemoveSql: handleRemoveSql,
      refreshFieldDistributions,
      onSqlsChange: (value: string[]) => dispatch(setSqls(value)),
      setKeywords: (value: string[]) => dispatch(setKeywords(value)),
      setSqls: (value: string[]) => dispatch(setSqls(value)),
    }),
    [
      // 数据与配置
      searchParams,
      detailData?.totalCount,
      logTableColumns,
      activeColumns,
      commonColumns,
      sortConfig,

      // 查询状态
      keywords,
      sqls,

      // 加载态
      getDetailData.loading,
    ],
  );

  return (
    <div className={styles.layout}>
      <SearchBar ref={searchBarRef} {...searchBarProps} />
    </div>
  );
};

export default LogPage;
