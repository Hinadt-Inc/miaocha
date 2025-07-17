import { useState, useEffect, useMemo, useRef, useCallback } from 'react';
import { Splitter } from 'antd';
import { useRequest } from 'ahooks';
import * as api from '@/api/logs';
import * as modulesApi from '@/api/modules';
import SearchBar from './SearchBar';
import Log from './Log';
import Sider from './Sider';
import { QUICK_RANGES, DATE_FORMAT_THOUSOND } from './utils';
import styles from './index.module.less';
import dayjs from 'dayjs';

const HomePage = () => {
  const [moduleOptions, setModuleOptions] = useState<IStatus[]>([]); // 模块名称列表，用于字段选择等组件
  const [detailData, setDetailData] = useState<ILogDetailsResponse | null>(null); // 日志数据
  const [logTableColumns, setLogTableColumns] = useState<ILogColumnsResponse[]>([]); // 日志字段列表
  const [histogramData, setHistogramData] = useState<ILogHistogramData | null>(null); // 日志时间分布列表
  const [whereSqlsFromSider, setWhereSqlsFromSider] = useState<IStatus[]>([]); // 侧边栏的where条件
  const [keywords, setKeywords] = useState<string[]>([]); // 新增
  const [sqls, setSqls] = useState<string[]>([]); // 新增
  const [activeColumns, setActiveColumns] = useState<string[]>([]); // 激活的字段列表
  const [selectedModule, setSelectedModule] = useState<string>(''); // 当前选中的模块
  const [sortConfig, setSortConfig] = useState<any[]>([]); // 排序配置
  const [commonColumns, setCommonColumns] = useState<string[]>([]); // 普通字段列表（不含有.的字段）
  const searchBarRef = useRef<any>(null);
  const siderRef = useRef<any>(null);
  const abortRef = useRef<AbortController | null>(null);

  // 查询配置相关状态
  const [moduleQueryConfig, setModuleQueryConfig] = useState<any>(null); // 存储完整的模块查询配置
  const [isInitialized, setIsInitialized] = useState(false); // 标记是否已经初始化
  const lastCallParamsRef = useRef<string>('');
  const requestTimerRef = useRef<NodeJS.Timeout | null>(null); // 新增：用于延迟请求的定时器
  const isInitializingRef = useRef(false); // 新增：标记是否正在初始化过程中

  // 默认的搜索参数
  const defaultSearchParams: ILogSearchParams = {
    offset: 0,
    pageSize: 1000,
    datasourceId: null,
    module: null,
    startTime: QUICK_RANGES.last_5m.from().format(DATE_FORMAT_THOUSOND),
    endTime: QUICK_RANGES.last_5m.to().format(DATE_FORMAT_THOUSOND),
    timeRange: 'last_5m',
    timeGrouping: 'auto',
  };
  // 日志检索请求参数
  const [searchParams, setSearchParams] = useState<ILogSearchParams>(defaultSearchParams);

  // 获取模块选项
  const generateModuleOptions = (modulesData: IMyModulesResponse[]): IStatus[] => {
    return (
      modulesData?.map(({ datasourceId, datasourceName, module }) => ({
        label: module,
        value: module,
        datasourceId,
        datasourceName,
        module,
      })) || []
    );
  };

  // 获取模块列表
  const fetchModulesRequest = useRequest(api.fetchMyModules, {
    onBefore: () => {
      isInitializingRef.current = true;
    },
    onSuccess: (res) => {
      const moduleOptions = generateModuleOptions(res);
      setModuleOptions(moduleOptions);

      // 只在初始化时设置默认模块，避免重复设置
      if ((!searchParams.datasourceId || !searchParams.module) && moduleOptions[0]) {
        const favoriteModule = localStorage.getItem('favoriteModule');
        const defaultModule = favoriteModule || moduleOptions[0].module;
        const defaultOption = moduleOptions.find((opt) => opt.module === defaultModule) || moduleOptions[0];

        // 批量更新状态，避免多次渲染
        setSelectedModule(defaultOption.module);
        setSearchParams((prev) => ({
          ...prev,
          datasourceId: Number(defaultOption.datasourceId),
          module: defaultOption.module,
        }));
      }
    },
    onError: () => {
      isInitializingRef.current = false;
    },
  });

  // 执行日志明细查询
  const getDetailData = useRequest(
    async (params: ILogSearchParams & { signal?: AbortSignal }) => {
      const requestParams: any = {
        ...params,
      };
      delete requestParams?.datasourceId;
      // 传 signal 给 api
      return api.fetchLogDetails(requestParams, { signal: params.signal });
    },
    {
      manual: true,
      onSuccess: (res) => {
        const { rows } = res;
        // 为每条记录添加唯一ID
        const timeField = moduleQueryConfig?.timeField || 'log_time'; // 如果没有配置则回退到log_time
        (rows || []).map((item, index) => {
          item._key = `${Date.now()}_${index}`;
          if (item[timeField]) {
            try {
              const timeValue = dayjs(item[timeField] as string);
              if (timeValue.isValid()) {
                item[timeField] = timeValue.format(DATE_FORMAT_THOUSOND);
              } else {
                item[timeField] = item[timeField] || '';
              }
            } catch (error) {
              item[timeField] = item[timeField] || '';
            }
          }
        });
        setDetailData(res);
      },
      onError: () => {
        setDetailData(null);
      },
    },
  );

  // 执行日志时间分布查询
  const getHistogramData = useRequest(
    async (params: ILogSearchParams & { signal?: AbortSignal }) => {
      const requestParams: any = {
        ...params,
      };

      delete requestParams?.datasourceId;
      // 传 signal 给 api
      return api.fetchLogHistogram(requestParams, { signal: params.signal });
    },
    {
      manual: true,
      onSuccess: (res: any) => {
        setHistogramData(res);
      },
      onError: () => {
        setHistogramData(null);
      },
    },
  );

  // 执行数据请求的函数
  const executeDataRequest = useCallback((params: ILogSearchParams) => {
    // 取消之前的请求
    if (abortRef.current) {
      abortRef.current.abort();
    }

    abortRef.current = new AbortController();
    const requestParams = { ...params, signal: abortRef.current.signal };

    getDetailData.run(requestParams);
    getHistogramData.run(requestParams);
  }, []);

  // 主要的数据请求逻辑
  useEffect(() => {
    // 检查是否满足调用条件
    if (!searchParams.datasourceId || !searchParams.module || !moduleQueryConfig) {
      return;
    }

    // 如果正在初始化中，跳过请求
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
        return;
      }

      // 再次检查是否正在初始化
      if (isInitializingRef.current) {
        return;
      }

      executeDataRequest(searchParams);

      // 更新最后调用的参数标识
      lastCallParamsRef.current = currentCallParams;

      // 标记已初始化
      if (!isInitialized) {
        setIsInitialized(true);
      }
    }, 300); // 增加延迟时间到500ms，确保状态完全稳定

    return () => {
      if (requestTimerRef.current) {
        clearTimeout(requestTimerRef.current);
      }
    };
  }, [searchParams, moduleQueryConfig, executeDataRequest, isInitialized]);

  // 处理列变化
  const handleChangeColumns = (columns: ILogColumnsResponse[]) => {
    setLogTableColumns(columns);
  };

  const handleSetWhereSqlsFromSider = (flag: '=' | '!=', columnName: string, value: string) => {
    const sql = `${columnName} ${flag} '${value}'`;
    const newSearchParams = {
      ...searchParams,
      offset: 0,
    };
    // 添加where条件
    if (flag === '=') {
      const oldSql = `${columnName} != '${value}'`;
      newSearchParams.whereSqls = [...(searchParams?.whereSqls || []), sql];
      (searchBarRef?.current as any)?.removeSql?.(oldSql);
      setWhereSqlsFromSider((prev: any) => [
        ...prev,
        {
          label: sql,
          value: value,
          field: columnName,
        },
      ]);
    } else {
      // 删除where条件
      const oldSql = `${columnName} = '${value}'`;
      newSearchParams.whereSqls = searchParams?.whereSqls?.filter((item: any) => item !== oldSql);
      (searchBarRef?.current as any)?.removeSql?.(oldSql);
      setWhereSqlsFromSider((prev: any) => prev.filter((item: any) => item.value !== value));
    }
    // 如果where条件为空，则删除where条件
    if (newSearchParams?.whereSqls?.length === 0) {
      delete newSearchParams.whereSqls;
    }
    // 添加sql
    (searchBarRef?.current as any)?.addSql?.(sql);
    setSearchParams(newSearchParams);
  };

  // 处理选中模块变化
  const handleSelectedModuleChange = useCallback(
    (selectedModule: string, datasourceId?: number) => {
      setSelectedModule(selectedModule);
      setKeywords([]); // 新增：切换模块时重置
      setSqls([]); // 新增：切换模块时重置
      // 只有当提供了datasourceId且与当前不同时才更新搜索参数
      if (
        selectedModule &&
        datasourceId &&
        (searchParams.datasourceId !== datasourceId || searchParams.module !== selectedModule)
      ) {
        setSearchParams((prev) => ({
          ...prev,
          datasourceId: datasourceId,
          module: selectedModule,
          offset: 0,
        }));
      }
    },
    [searchParams.datasourceId, searchParams.module],
  );

  // 处理排序配置变化
  const handleSortChange = useCallback((newSortConfig: any[]) => {
    setSortConfig(newSortConfig);
  }, []);

  // 优化字段选择组件的props
  const siderProps = {
    searchParams,
    modules: moduleOptions,
    setWhereSqlsFromSider: handleSetWhereSqlsFromSider,
    onSearch: setSearchParams,
    onChangeColumns: handleChangeColumns,
    onActiveColumnsChange: setActiveColumns,
    onSelectedModuleChange: handleSelectedModuleChange,
    moduleQueryConfig,
    onCommonColumnsChange: setCommonColumns,
  };

  // 使用useCallback稳定getDistributionWithSearchBar函数引用
  const getDistributionWithSearchBar = useCallback(() => {
    siderRef.current?.getDistributionWithSearchBar?.();
  }, []);

  const onSearchFromLog = (params: ILogSearchParams) => {
    // 绝对时间
    //   {
    //     "label": "2025-05-06 00:07:00 ~ 2025-05-23 05:04:26",
    //     "value": "2025-05-06 00:07:00 ~ 2025-05-23 05:04:26",
    //     "range": [
    //         "2025-05-06 00:07:00",
    //         "2025-05-23 05:04:26"
    //     ]
    // }
    const { startTime, endTime } = params;
    const timeOption = {
      label: `${startTime} ~ ${endTime}`,
      value: `${startTime} ~ ${endTime}`,
      range: [startTime, endTime],
      type: 'absolute',
    };
    (searchBarRef?.current as any)?.setTimeOption(timeOption);
  };

  // 处理列变化
  const handleChangeColumnsByLog = (col: any) => {
    const index = logTableColumns.findIndex((item) => item.columnName === col.title);
    if (index === -1) return;
    logTableColumns[index].selected = false;
    delete logTableColumns[index]._createTime;
    setLogTableColumns([...logTableColumns]);
  };

  // 优化log组件的props
  const logProps: any = useMemo(
    () => ({
      histogramData,
      detailData,
      getDetailData,
      searchParams,
      dynamicColumns: logTableColumns,
      whereSqlsFromSider,
      sqls,
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
      logTableColumns,
      searchParams,
      whereSqlsFromSider,
      sqls,
      moduleQueryConfig,
      handleSortChange,
      sortConfig, // 添加sortConfig到依赖数组
    ],
  );

  // 获取模块查询配置
  const getModuleQueryConfig = useRequest((moduleName: string) => modulesApi.getModuleQueryConfig(moduleName), {
    manual: true,
    onSuccess: (res) => {
      setModuleQueryConfig(res);

      // 清除初始化标记，允许数据请求执行
      setTimeout(() => {
        isInitializingRef.current = false;
      }, 100); // 延迟100ms清除标记，确保状态更新完成
    },
    onError: () => {
      setModuleQueryConfig(null);

      // 即使失败也要清除初始化标记
      isInitializingRef.current = false;
    },
  });

  // 当selectedModule变化时，获取模块查询配置
  useEffect(() => {
    if (selectedModule) {
      // 只有当模块真正变化时才重置状态和获取配置
      if (selectedModule !== moduleQueryConfig?.module) {
        setIsInitialized(false);
        lastCallParamsRef.current = '';
        setModuleQueryConfig(null); // 先清空配置，避免使用旧配置

        // 重新设置初始化标记
        isInitializingRef.current = true;

        getModuleQueryConfig.run(selectedModule);
      }
    } else {
      setModuleQueryConfig(null);
      setIsInitialized(false);
      lastCallParamsRef.current = '';
      isInitializingRef.current = false;
    }
  }, [selectedModule, moduleQueryConfig?.module]);

  // 组件卸载时清理定时器
  useEffect(() => {
    return () => {
      if (requestTimerRef.current) {
        clearTimeout(requestTimerRef.current);
      }
      if (abortRef.current) {
        abortRef.current.abort();
      }
      // 清理初始化标记
      isInitializingRef.current = false;
    };
  }, []);

  // 搜索栏组件props
  const searchBarProps = useMemo(
    () => ({
      searchParams,
      totalCount: detailData?.totalCount,
      onSearch: setSearchParams,
      setWhereSqlsFromSider,
      columns: logTableColumns,
      onSqlsChange: setSqls,
      activeColumns,
      getDistributionWithSearchBar,
      sortConfig,
      commonColumns,
      loading: getDetailData.loading, // 新增
      keywords, // 新增
      setKeywords, // 新增
      sqls, // 新增
      setSqls, // 新增
      setWhereSqlsFromSiderArr: whereSqlsFromSider, // 新增
    }),
    [
      searchParams,
      detailData?.totalCount,
      setSearchParams,
      setWhereSqlsFromSider,
      logTableColumns,
      activeColumns,
      getDistributionWithSearchBar,
      sortConfig,
      commonColumns,
      getDetailData.loading, // 新增
      keywords, // 新增
      sqls, // 新增
      whereSqlsFromSider, // 新增
    ],
  );

  return (
    <div className={styles.layout}>
      <SearchBar ref={searchBarRef} {...searchBarProps} />

      <Splitter className={styles.container}>
        <Splitter.Panel collapsible defaultSize={200} min={0} max="40%">
          <Sider ref={siderRef} {...siderProps} />
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
