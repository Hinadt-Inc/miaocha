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
import { QUICK_RANGES, DATE_FORMAT_THOUSOND } from './utils';
import styles from './index.module.less';
import dayjs from 'dayjs';

const HomePage = () => {
  const dispatch = useDispatch();
  const [urlSearchParams] = useSearchParams();
  
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
            console.log('CAS登录成功');
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

  // 页面初始化时检查是否有保存的分享参数
  useEffect(() => {
    const savedSharedParams = sessionStorage.getItem('miaocha_shared_params');
    if (savedSharedParams && !sharedParams) {
      try {
        const parsedSavedParams = JSON.parse(savedSharedParams);
        setSharedParams(parsedSavedParams);
        
        if (parsedSavedParams.module) {
          setSelectedModule(parsedSavedParams.module);
        }
        
        console.log('页面初始化时恢复分享参数:', parsedSavedParams);
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
            setSharedParams(parsedParams);
            
            // 保存到sessionStorage，防止登录跳转后丢失
            sessionStorage.setItem('miaocha_shared_params', JSON.stringify(parsedParams));
            
            if (parsedParams.module) {
              setSelectedModule(parsedParams.module);
            }
            
            // 先不清理URL参数，等到参数成功应用后再清理
            console.log('检测到分享参数:', parsedParams);
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
              
              console.log('从sessionStorage恢复分享参数:', parsedSavedParams);
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
  }, [urlSearchParams, sharedParams]);

  // 应用分享参数到搜索栏
  useEffect(() => {
    if (sharedParams && !hasAppliedSharedParams && searchBarRef.current && moduleOptions.length > 0) {
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
          
          // 应用时间范围到SearchBar
          if (sharedParams.timeRange || (sharedParams.startTime && sharedParams.endTime)) {
            const timeOption = {
              value: sharedParams.timeRange || `${sharedParams.startTime} ~ ${sharedParams.endTime}`,
              range: [sharedParams.startTime, sharedParams.endTime],
              label: sharedParams.timeRange ? (QUICK_RANGES[sharedParams.timeRange]?.label || sharedParams.timeRange) : `${sharedParams.startTime} ~ ${sharedParams.endTime}`,
              type: sharedParams.timeRange && QUICK_RANGES[sharedParams.timeRange] ? 'quick' : 'absolute',
            };
            searchBarRef.current?.setTimeOption?.(timeOption);
          }
          
          // 更新搜索参数
          if (sharedParams.module) {
            const moduleOption = moduleOptions.find(option => option.module === sharedParams.module);
            if (moduleOption) {
              setSearchParams(prev => ({
                ...prev,
                datasourceId: Number(moduleOption.datasourceId),
                module: sharedParams.module,
                startTime: sharedParams.startTime || prev.startTime,
                endTime: sharedParams.endTime || prev.endTime,
                timeRange: sharedParams.timeRange || prev.timeRange,
                timeGrouping: sharedParams.timeGrouping || prev.timeGrouping,
                keywords: sharedParams.keywords || [],
                whereSqls: sharedParams.whereSqls || [],
                offset: 0,
              }));
            }
          }
          
          setHasAppliedSharedParams(true);
          
          // 参数应用成功后，清理URL和sessionStorage
          const newUrl = new URL(window.location.href);
          ['keywords', 'whereSqls', 'timeRange', 'startTime', 'endTime', 'module', 'timeGrouping'].forEach(param => {
            newUrl.searchParams.delete(param);
          });
          window.history.replaceState({}, '', newUrl.toString());
          
          // 清理sessionStorage中的分享参数
          sessionStorage.removeItem('miaocha_shared_params');
          
          console.log('分享参数应用成功并已清理:', sharedParams);
        } catch (error) {
          console.error('应用分享参数失败:', error);
        }
      }, 200);

      return () => clearTimeout(timer);
    }
  }, [sharedParams, hasAppliedSharedParams, moduleOptions, searchBarRef]);
  
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
  const fetchModulesRequest = useRequest(api.fetchMyModules, {
    onBefore: () => {
      isInitializingRef.current = true;
    },
    onSuccess: (res) => {
      const moduleOptions = generateModuleOptions(res);
      setModuleOptions(moduleOptions);

      // 如果有分享参数，优先应用分享的模块
      if (sharedParams && sharedParams.module && !hasAppliedSharedParams) {
        const sharedModuleOption = moduleOptions.find(option => option.module === sharedParams.module);
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
  }, [getDetailData, getHistogramData]);

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
  const handleChangeColumnsByLog = useCallback((col: any) => {
    
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
  }, [logTableColumns]);

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
    onSuccess: (res) => {
      setModuleQueryConfig(res);

      // 清除初始化标记，允许数据请求执行
      // 移除手动触发请求的逻辑，避免重复请求
      // 让主要的 useEffect 负责监听 searchParams 变化并触发请求
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
