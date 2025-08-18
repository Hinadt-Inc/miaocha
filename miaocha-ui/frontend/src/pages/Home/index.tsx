import { useState, useEffect, useMemo, useRef, useCallback } from 'react';
import { Splitter } from 'antd';
import { useRequest } from 'ahooks';
import { useSearchParams } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { login } from '@/store/userSlice';
import { oAuthCallback } from '@/api/auth';
import * as api from '@/api/logs';
import * as modulesApi from '@/api/modules';
import SearchBar from './SearchBar';
import Log from './Log';
import Sider from './Sider';
import AIAssistant from '@/components/AIAssistant';
import { QUICK_RANGES, DATE_FORMAT_THOUSOND, formatTimeString } from './utils';
import styles from './index.module.less';

const HomePage = () => {
  const dispatch = useDispatch();
  const [urlSearchParams] = useSearchParams();

  const [moduleOptions, setModuleOptions] = useState<IStatus[]>([]); // 模块名称列表，用于字段选择等组件
  const [detailData, setDetailData] = useState<ILogDetailsResponse | null>(null); // 日志数据
  const [logTableColumns, setLogTableColumns] = useState<ILogColumnsResponse[]>([]); // 日志字段列表
  const [histogramData, setHistogramData] = useState<ILogHistogramResponse | null>(null); // 日志时间分布列表
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
  const [loadingQueryConfig, setLoadingQueryConfig] = useState(false); // 防止重复请求查询配置
  const loadedConfigModulesRef = useRef<Set<string>>(new Set()); // 跟踪已加载配置的模块

  // 处理CAS回调
  useEffect(() => {
    const handleCASCallback = async () => {
      const ticket = urlSearchParams.get('ticket');

      if (ticket) {
        try {
          // 构造回调URL
          const redirectUri = `${window.location.origin}`;

          // 从sessionStorage获取provider信息
          const providerId = sessionStorage.getItem('oauthProvider') || 'mandao';

          // 调用后端回调接口
          const response = await oAuthCallback({
            provider: providerId,
            code: ticket,
            redirect_uri: redirectUri,
          });

          if (response) {
            // 登录成功，更新用户状态
            dispatch(
              login({
                userId: response.userId,
                name: response.nickname,
                role: response.role,
                tokens: {
                  accessToken: response.token,
                  refreshToken: response.refreshToken,
                  expiresAt: response.expiresAt,
                  refreshExpiresAt: response.refreshExpiresAt,
                },
              }),
            );

            // 清理sessionStorage中的provider信息
            sessionStorage.removeItem('oauthProvider');

            // 移除URL中的ticket参数，但保留分享参数
            const newUrl = new URL(window.location.href);
            newUrl.searchParams.delete('ticket');
            window.history.replaceState({}, '', newUrl.toString());

            // 可以显示成功提示
          }
        } catch (error) {
          console.error('CAS回调处理失败:', error);
          // 可以显示错误提示
        }
      }
    };

    handleCASCallback();
  }, [urlSearchParams, dispatch]);

  // 分享参数状态
  const [sharedParams, setSharedParams] = useState<any>(null);
  const [hasAppliedSharedParams, setHasAppliedSharedParams] = useState(false);
  const processedUrlRef = useRef<string>(''); // 用于跟踪已处理的URL参数，避免重复处理

  // 页面初始化时检查是否有保存的分享参数
  useEffect(() => {
    const savedSharedParams = sessionStorage.getItem('miaocha_shared_params');
    if (savedSharedParams && !sharedParams) {
      try {
        const parsedSavedParams = JSON.parse(savedSharedParams);

        // 对于保存的相对时间范围，重新计算时间
        if (parsedSavedParams.timeRange && QUICK_RANGES[parsedSavedParams.timeRange]) {
          const quickRange = QUICK_RANGES[parsedSavedParams.timeRange];
          parsedSavedParams.startTime = quickRange.from().format(DATE_FORMAT_THOUSOND);
          parsedSavedParams.endTime = quickRange.to().format(DATE_FORMAT_THOUSOND);
        }

        setSharedParams(parsedSavedParams);

        if (parsedSavedParams.module) {
          setSelectedModule(parsedSavedParams.module);
        }
      } catch (e) {
        console.error('解析保存的分享参数失败:', e);
        sessionStorage.removeItem('miaocha_shared_params');
      }
    }
  }, []); // 只在组件挂载时执行一次

  // 处理分享的URL参数
  useEffect(() => {
    const handleSharedParams = () => {
      try {
        const keywords = urlSearchParams.get('keywords');
        const whereSqls = urlSearchParams.get('whereSqls');
        const timeRange = urlSearchParams.get('timeRange');
        const startTime = urlSearchParams.get('startTime');
        const endTime = urlSearchParams.get('endTime');
        const module = urlSearchParams.get('module');
        const timeGrouping = urlSearchParams.get('timeGrouping');

        // 生成当前URL参数的唯一标识
        const currentUrlParams = `${keywords || ''}-${whereSqls || ''}-${timeRange || ''}-${startTime || ''}-${endTime || ''}-${module || ''}-${timeGrouping || ''}`;

        // 如果已经处理过相同的URL参数，则跳过
        if (processedUrlRef.current === currentUrlParams) {
          return;
        }

        // 如果有分享参数，解析并保存
        if (keywords || whereSqls || timeRange || module) {
          const parsedParams: any = {};

          if (keywords) {
            try {
              parsedParams.keywords = JSON.parse(keywords);
            } catch (e) {
              console.error('解析keywords参数失败:', e);
            }
          }

          if (whereSqls) {
            try {
              parsedParams.whereSqls = JSON.parse(whereSqls);
            } catch (e) {
              console.error('解析whereSqls参数失败:', e);
            }
          }

          if (timeRange) parsedParams.timeRange = timeRange;
          if (startTime) parsedParams.startTime = startTime;
          if (endTime) parsedParams.endTime = endTime;
          if (timeGrouping) parsedParams.timeGrouping = timeGrouping;
          if (module) parsedParams.module = module;

          // 保存分享参数到状态和sessionStorage，确保登录后不丢失
          if (Object.keys(parsedParams).length > 0) {
            // 对于相对时间范围，重新计算当前时间
            if (parsedParams.timeRange && QUICK_RANGES[parsedParams.timeRange]) {
              const quickRange = QUICK_RANGES[parsedParams.timeRange];
              parsedParams.startTime = quickRange.from().format(DATE_FORMAT_THOUSOND);
              parsedParams.endTime = quickRange.to().format(DATE_FORMAT_THOUSOND);
            }

            // 标记已处理这组URL参数
            processedUrlRef.current = currentUrlParams;

            setSharedParams(parsedParams);

            // 保存到sessionStorage，防止登录跳转后丢失
            sessionStorage.setItem('miaocha_shared_params', JSON.stringify(parsedParams));

            if (parsedParams.module) {
              setSelectedModule(parsedParams.module);
            }
          }
        } else {
          // 如果URL中没有分享参数，检查sessionStorage中是否有保存的分享参数
          const savedSharedParams = sessionStorage.getItem('miaocha_shared_params');
          if (savedSharedParams && !sharedParams) {
            try {
              const parsedSavedParams = JSON.parse(savedSharedParams);
              setSharedParams(parsedSavedParams);

              if (parsedSavedParams.module) {
                setSelectedModule(parsedSavedParams.module);
              }
            } catch (e) {
              console.error('解析保存的分享参数失败:', e);
              sessionStorage.removeItem('miaocha_shared_params');
            }
          }
        }
      } catch (error) {
        console.error('处理分享参数失败:', error);
      }
    };

    handleSharedParams();
  }, [urlSearchParams]); // 移除sharedParams依赖，避免循环

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
            const moduleOption = moduleOptions.find((option) => option.module === sharedParams.module);
            if (moduleOption) {
              setSearchParams((prev) => ({
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
          const newUrl = new URL(window.location.href);
          ['keywords', 'whereSqls', 'timeRange', 'startTime', 'endTime', 'module', 'timeGrouping'].forEach((param) => {
            newUrl.searchParams.delete(param);
          });
          window.history.replaceState({}, '', newUrl.toString());

          // 清理sessionStorage中的分享参数
          sessionStorage.removeItem('miaocha_shared_params');
        } catch (error) {
          console.error('应用分享参数失败:', error);
        }
      }, 200);

      return () => clearTimeout(timer);
    }
  }, [sharedParams, hasAppliedSharedParams, moduleOptions, searchBarRef, moduleQueryConfig, loadingQueryConfig]);

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
    startTime: QUICK_RANGES.last_15m.from().format(DATE_FORMAT_THOUSOND),
    endTime: QUICK_RANGES.last_15m.to().format(DATE_FORMAT_THOUSOND),
    timeRange: 'last_15m',
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
  useRequest(api.fetchMyModules, {
    onBefore: () => {
      isInitializingRef.current = true;
    },
    onSuccess: (res) => {
      const moduleOptions = generateModuleOptions(res);
      setModuleOptions(moduleOptions);

      // 如果有分享参数，优先应用分享的模块
      if (sharedParams && sharedParams.module && !hasAppliedSharedParams) {
        const sharedModuleOption = moduleOptions.find((option) => option.module === sharedParams.module);
        if (sharedModuleOption) {
          setSelectedModule(sharedParams.module);
          setSearchParams((prev) => ({
            ...prev,
            datasourceId: Number(sharedModuleOption.datasourceId),
            module: sharedParams.module,
          }));
          return; // 分享参数会在后续的 useEffect 中完整应用
        }
      }

      // 只在初始化时设置默认模块，避免重复设置
      if ((!searchParams.datasourceId || !searchParams.module) && moduleOptions[0]) {
        const defaultOption = moduleOptions[0];

        // 批量更新状态，避免多次渲染
        setSelectedModule(defaultOption.module);
        setSearchParams((prev) => ({
          ...prev,
          datasourceId: Number(defaultOption.datasourceId),
          module: defaultOption.module,
        }));
      }
    },
    onError: (error) => {
      console.error('获取模块列表失败:', error);
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
        const timeField = moduleQueryConfig?.timeField || 'log_time';

        // 为每条记录添加唯一ID并格式化时间字段
        (rows || []).forEach((item, index) => {
          item._key = `${Date.now()}_${index}`;

          if (item[timeField]) {
            item[timeField] = formatTimeString(item[timeField] as string);
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
    }, 300);

    return () => {
      if (requestTimerRef.current) {
        clearTimeout(requestTimerRef.current);
      }
    };
  }, [searchParams, moduleQueryConfig, executeDataRequest, isInitialized]);

  // 处理列变化
  const handleChangeColumns = useCallback((columns: ILogColumnsResponse[]) => {
    setLogTableColumns(columns);

    // 更新搜索参数中的fields字段，触发detail接口调用
    const selectedColumns = columns
      .filter((item) => item.selected && item.columnName)
      .map((item) => item.columnName!)
      .filter((name): name is string => Boolean(name));

    setSearchParams((prev) => ({
      ...prev,
      fields: selectedColumns.length > 0 ? selectedColumns : undefined,
      offset: 0, // 重置分页
    }));
  }, []);

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
      // 如果模块发生了变化，清理已加载配置记录
      if (selectedModule !== searchParams.module) {
        loadedConfigModulesRef.current.clear();
      }

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
    selectedModule, // 传递当前选中的模块，确保Sider组件与Home组件状态同步
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
  const handleChangeColumnsByLog = useCallback(
    (col: any) => {
      const index = logTableColumns.findIndex((item) => item.columnName === col.title);

      if (index === -1) {
        return;
      }

      // 更新列状态
      logTableColumns[index].selected = false;
      delete logTableColumns[index]._createTime;

      // 计算移除该列后的选中列列表
      const selectedColumns = logTableColumns
        .filter((item) => item.selected && item.columnName)
        .map((item) => item.columnName!)
        .filter((name): name is string => Boolean(name));

      // 更新本地搜索参数
      const _savedSearchParams = localStorage.getItem('searchBarParams');
      if (_savedSearchParams) {
        const savedSearchParams = JSON.parse(_savedSearchParams);
        localStorage.setItem(
          'searchBarParams',
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
      setSearchParams((prev) => {
        const newParams = {
          ...prev,
          fields: selectedColumns.length > 0 ? selectedColumns : undefined,
          offset: 0, // 重置分页
        };
        return newParams;
      });
    },
    [logTableColumns],
  );

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
      handleChangeColumnsByLog, // 添加这个依赖
      sortConfig, // 添加sortConfig到依赖数组
    ],
  );

  // 获取模块查询配置
  const getModuleQueryConfig = useRequest((moduleName: string) => modulesApi.getModuleQueryConfig(moduleName), {
    manual: true,
    onBefore: () => {
      setLoadingQueryConfig(true);
    },
    onSuccess: (res) => {
      setModuleQueryConfig(res);
      setLoadingQueryConfig(false);

      // 记录已加载配置的模块
      loadedConfigModulesRef.current.add(selectedModule);

      // 清除初始化标记，允许数据请求执行
      setTimeout(() => {
        isInitializingRef.current = false;
      }, 100);
    },
    onError: () => {
      setModuleQueryConfig(null);
      setLoadingQueryConfig(false);

      // 即使失败也要清除初始化标记
      isInitializingRef.current = false;
    },
  });

  // 当selectedModule变化时，获取模块查询配置
  useEffect(() => {
    if (selectedModule) {
      if (selectedModule !== moduleQueryConfig?.module) {
        setIsInitialized(false);
        lastCallParamsRef.current = '';
        // setModuleQueryConfig(null); // 先清空配置，避免使用旧配置
        // 重新设置初始化标记
        isInitializingRef.current = true;

        getModuleQueryConfig.run(selectedModule);
      }
    } else {
      setModuleQueryConfig(null);
      setIsInitialized(false);
      lastCallParamsRef.current = '';
      isInitializingRef.current = false;
      // 清空已加载模块的记录
      loadedConfigModulesRef.current.clear();
    }
  }, [selectedModule]); // 只依赖 selectedModule，避免循环触发

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
      // 清理已加载模块记录
      loadedConfigModulesRef.current.clear();
    };
  }, []);

  // 处理刷新操作
  const handleRefresh = useCallback(() => {
    // 通过SearchBar的ref调用autoRefresh方法
    // 这样可以确保时间更新并只触发一次接口调用
    if (searchBarRef.current?.autoRefresh) {
      searchBarRef.current.autoRefresh();
    } else {
      // 备用方案：直接执行数据请求
      if (searchParams.datasourceId && searchParams.module && moduleQueryConfig) {
        executeDataRequest(searchParams);
      }
    }
  }, [searchParams, moduleQueryConfig, executeDataRequest]);

  // 搜索栏组件props
  const searchBarProps = useMemo(
    () => ({
      searchParams,
      totalCount: detailData?.totalCount,
      onSearch: setSearchParams,
      onRefresh: handleRefresh,
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
      handleRefresh,
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

      {/* AI助手悬浮窗 */}
      <AIAssistant
        onLogSearch={(data) => {
          console.log('🏠 Home页面收到onLogSearch回调:', data);

          // 处理AI助手的搜索请求
          const searchParams = data.searchParams || data; // 向后兼容

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
            }
          }

          // 只有在没有skipRequest标记时才触发新的搜索请求
          if (!data.skipRequest) {
            console.log('🔄 触发executeDataRequest');
            executeDataRequest(searchParams);
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
            timeRange: timeRangeData.timeRange as any, // 类型断言
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
        currentSearchParams={{
          module: selectedModule,
          keywords,
          sqls,
          activeColumns,
          sortConfig,
          whereSqls: whereSqlsFromSider,
        }}
        logData={detailData}
        moduleOptions={moduleOptions}
      />
    </div>
  );
};

export default HomePage;
