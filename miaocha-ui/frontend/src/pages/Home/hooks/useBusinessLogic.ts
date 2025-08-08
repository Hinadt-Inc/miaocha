import { useCallback, useEffect } from 'react';
import { QUICK_RANGES, DATE_FORMAT_THOUSOND } from '../utils';
import { SHARED_PARAMS_APPLY_DELAY, REQUEST_DEBOUNCE_DELAY, STORAGE_KEYS } from '../constants';
import type { 
  ILogSearchParams, 
  IStatus, 
  IMyModulesResponse, 
  ILogColumnsResponse
} from '../types';

/**
 * 业务逻辑处理相关的hook
 */
export const useBusinessLogic = (
  state: any, // 从useHomePageState返回的状态
  executeDataRequest: (params: ILogSearchParams) => void,
  cleanupUrlParams: () => void
) => {
  const {
    sharedParams,
    hasAppliedSharedParams,
    setHasAppliedSharedParams,
    moduleOptions,
    searchParams,
    setSearchParams,
    setKeywords,
    setSqls,
    setSelectedModule,
    setActiveColumns,
    setLogTableColumns,
    searchBarRef,
    moduleQueryConfig,
    loadingQueryConfig,
    lastCallParamsRef,
    requestTimerRef,
    isInitialized,
    setIsInitialized,
    isInitializingRef,
  } = state;

  // 生成模块选项
  const generateModuleOptions = useCallback((modulesData: IMyModulesResponse[]): IStatus[] => {
    return (
      modulesData?.map(({ datasourceId, datasourceName, module }) => ({
        label: module,
        value: module,
        datasourceId,
        datasourceName,
        module,
      })) || []
    );
  }, []);

  // 应用分享参数到搜索栏
  useEffect(() => {
    if (sharedParams && !hasAppliedSharedParams && searchBarRef.current && moduleOptions.length > 0) {
      // 如果有分享的模块参数，需要等待该模块的配置加载完成
      if (sharedParams.module && (!moduleQueryConfig || loadingQueryConfig)) {
        return; // 等待模块配置加载完成
      }
      
      // 短暂延迟确保 SearchBar 组件完全初始化
      const timer = setTimeout(() => {
        try {
          // 应用关键词
          if (sharedParams.keywords && Array.isArray(sharedParams.keywords)) {
            setKeywords(sharedParams.keywords);
          }
          
          // 应用SQL条件
          if (sharedParams.whereSqls && Array.isArray(sharedParams.whereSqls)) {
            setSqls(sharedParams.whereSqls);
          }
          
          // 应用时间分组
          if (sharedParams.timeGrouping) {
            searchBarRef.current?.setTimeGroup?.(sharedParams.timeGrouping);
          }
          
          // 应用时间范围到SearchBar - 优先处理相对时间范围
          let timeOption: any = null;
          let calculatedStartTime: string | undefined;
          let calculatedEndTime: string | undefined;
          
          if (sharedParams.timeRange && QUICK_RANGES[sharedParams.timeRange]) {
            // 有相对时间范围，重新计算当前时间
            const quickRange = QUICK_RANGES[sharedParams.timeRange];
            calculatedStartTime = quickRange.from().format(DATE_FORMAT_THOUSOND);
            calculatedEndTime = quickRange.to().format(DATE_FORMAT_THOUSOND);
            
            timeOption = {
              value: sharedParams.timeRange,
              range: [calculatedStartTime, calculatedEndTime],
              label: quickRange.label,
              type: 'quick',
            };
          } else if (sharedParams.startTime && sharedParams.endTime) {
            // 没有相对时间范围，使用绝对时间
            calculatedStartTime = sharedParams.startTime;
            calculatedEndTime = sharedParams.endTime;
            
            timeOption = {
              value: `${sharedParams.startTime} ~ ${sharedParams.endTime}`,
              range: [sharedParams.startTime, sharedParams.endTime],
              label: `${sharedParams.startTime} ~ ${sharedParams.endTime}`,
              type: 'absolute',
            };
          }
          
          if (timeOption) {
            searchBarRef.current?.setTimeOption?.(timeOption);
          }
          
          // 更新搜索参数
          if (sharedParams.module) {
            const moduleOption = moduleOptions.find((option: IStatus) => option.module === sharedParams.module);
            if (moduleOption) {
              setSearchParams((prev: ILogSearchParams) => ({
                ...prev,
                datasourceId: Number(moduleOption.datasourceId),
                module: sharedParams.module,
                startTime: calculatedStartTime || prev.startTime,
                endTime: calculatedEndTime || prev.endTime,
                timeRange: sharedParams.timeRange || prev.timeRange,
                timeGrouping: sharedParams.timeGrouping || prev.timeGrouping,
                keywords: sharedParams.keywords || [],
                whereSqls: sharedParams.whereSqls || [],
                offset: 0,
              }));
            } else {
              console.warn('未找到对应的模块选项:', sharedParams.module, moduleOptions);
            }
          }
          
          setHasAppliedSharedParams(true);
          
          // 参数应用成功后，清理URL和sessionStorage
          cleanupUrlParams();
          
        } catch (error) {
          console.error('应用分享参数失败:', error);
        }
      }, SHARED_PARAMS_APPLY_DELAY);

      return () => clearTimeout(timer);
    }
  }, [sharedParams, hasAppliedSharedParams, moduleOptions, searchBarRef, moduleQueryConfig, loadingQueryConfig]);

  // 主要的数据请求逻辑
  useEffect(() => {
    // 检查是否满足调用条件
    if (!searchParams.datasourceId || !searchParams.module || !moduleQueryConfig) {
      return;
    }

    // 如果正在初始化中（仅针对模块配置加载），跳过请求
    if (isInitializingRef.current) {
      return;
    }

    // 生成当前调用的参数标识，用于避免重复调用
    const currentCallParams = JSON.stringify({
      datasourceId: searchParams.datasourceId,
      module: searchParams.module,
      startTime: searchParams.startTime,
      endTime: searchParams.endTime,
      timeRange: searchParams.timeRange,
      whereSqls: searchParams.whereSqls,
      keywords: searchParams.keywords,
      offset: searchParams.offset,
      fields: searchParams.fields,
      sortFields: searchParams.sortFields,
      moduleQueryConfigTimeField: moduleQueryConfig?.timeField,
    });

    // 如果参数没有变化，则不执行请求
    if (lastCallParamsRef.current === currentCallParams) {
      return;
    }

    // 清除之前的定时器
    if (requestTimerRef.current) {
      clearTimeout(requestTimerRef.current);
    }

    // 延迟执行，避免快速连续调用
    requestTimerRef.current = setTimeout(() => {
      // 再次检查条件，确保在延迟期间状态没有变化导致条件不满足
      if (!searchParams.datasourceId || !searchParams.module || !moduleQueryConfig) {
        console.log('延迟执行时条件不满足，跳过请求');
        return;
      }

      // 再次检查是否正在初始化
      if (isInitializingRef.current) {
        console.log('延迟执行时仍在初始化中，跳过请求');
        return;
      }

      executeDataRequest(searchParams);

      // 更新最后调用的参数标识
      lastCallParamsRef.current = currentCallParams;

      // 标记已初始化
      if (!isInitialized) {
        setIsInitialized(true);
      }
    }, REQUEST_DEBOUNCE_DELAY);

    return () => {
      if (requestTimerRef.current) {
        clearTimeout(requestTimerRef.current);
      }
    };
  }, [searchParams, moduleQueryConfig, executeDataRequest, isInitialized]);

  // 处理选中模块变化
  const handleSelectedModuleChange = useCallback(
    (selectedModule: string, datasourceId?: number) => {
      // 如果模块发生了变化，清理已加载配置记录
      if (selectedModule !== searchParams.module) {
        state.loadedConfigModulesRef.current.clear();
      }
      
      setSelectedModule(selectedModule);
      setKeywords([]); // 切换模块时重置
      setSqls([]); // 切换模块时重置
      // 只有当提供了datasourceId且与当前不同时才更新搜索参数
      if (
        selectedModule &&
        datasourceId &&
        (searchParams.datasourceId !== datasourceId || searchParams.module !== selectedModule)
      ) {
        setSearchParams((prev: ILogSearchParams) => ({
          ...prev,
          datasourceId: datasourceId,
          module: selectedModule,
          offset: 0,
        }));
      }
    },
    [searchParams.datasourceId, searchParams.module],
  );

  // 处理列变化
  const handleChangeColumns = useCallback((columns: ILogColumnsResponse[]) => {
    setLogTableColumns(columns);
    
    // 更新搜索参数中的fields字段，触发detail接口调用
    const selectedColumns = columns
      .filter((item) => item.selected && item.columnName)
      .map((item) => item.columnName!)
      .filter((name): name is string => Boolean(name));
    
    setSearchParams((prev: ILogSearchParams) => ({
      ...prev,
      fields: selectedColumns.length > 0 ? selectedColumns : undefined,
      offset: 0, // 重置分页
    }));
  }, []);

  // 处理列变化（从Log组件）
  const handleChangeColumnsByLog = useCallback((col: any) => {
    const logTableColumns = state.logTableColumns;
    
    const index = logTableColumns.findIndex((item: ILogColumnsResponse) => item.columnName === col.title);
    
    if (index === -1) {
      return;
    }
    
    // 更新列状态
    logTableColumns[index].selected = false;
    delete logTableColumns[index]._createTime;
    
    // 计算移除该列后的选中列列表
    const selectedColumns = logTableColumns
      .filter((item: ILogColumnsResponse) => item.selected && item.columnName)
      .map((item: ILogColumnsResponse) => item.columnName!)
      .filter((name: string | undefined): name is string => Boolean(name));
    
    // 更新本地搜索参数
    const _savedSearchParams = localStorage.getItem(STORAGE_KEYS.SEARCH_BAR_PARAMS);
    if (_savedSearchParams) {
      const savedSearchParams = JSON.parse(_savedSearchParams);
      localStorage.setItem(
        STORAGE_KEYS.SEARCH_BAR_PARAMS,
        JSON.stringify({
          ...savedSearchParams,
          fields: selectedColumns,
        }),
      );
    }
    
    // 通知父组件激活字段变化
    setActiveColumns(selectedColumns);
    
    // 排序并更新列状态
    const sortedColumns = logTableColumns.sort(
      (a: ILogColumnsResponse, b: ILogColumnsResponse) => (a._createTime || 0) - (b._createTime || 0),
    );
    const updatedColumns = [...sortedColumns];
    setLogTableColumns(updatedColumns);
    
    // 更新搜索参数中的fields字段，触发detail接口调用
    setSearchParams((prev: ILogSearchParams) => {
      const newParams = {
        ...prev,
        fields: selectedColumns.length > 0 ? selectedColumns : undefined,
        offset: 0, // 重置分页
      };
      return newParams;
    });
  }, [state.logTableColumns]);

  return {
    generateModuleOptions,
    handleSelectedModuleChange,
    handleChangeColumns,
    handleChangeColumnsByLog,
  };
};
