import { useState, useRef } from 'react';
import type {
  IStatus,
  ILogDetailsResponse,
  ILogColumnsResponse,
  ILogHistogramData,
  ILogSearchParams,
  IModuleQueryConfig,
  ISharedParams,
  SortConfig,
} from '../types';
import { DEFAULT_SEARCH_PARAMS } from '../constants';

/**
 * Home页面状态管理的hook
 */
export const useHomePageState = () => {
  // 基础数据状态
  const [moduleOptions, setModuleOptions] = useState<IStatus[]>([]);
  const [detailData, setDetailData] = useState<ILogDetailsResponse | null>(null);
  const [originalDetailData, setOriginalDetailData] = useState<ILogDetailsResponse | null>(null); // 缓存原始完整数据
  const [logTableColumns, setLogTableColumns] = useState<ILogColumnsResponse[]>([]);
  const [histogramData, setHistogramData] = useState<ILogHistogramData | null>(null);
  const [whereSqlsFromSider, setWhereSqlsFromSider] = useState<IStatus[]>([]);
  const [keywords, setKeywords] = useState<string[]>([]);
  const [sqls, setSqls] = useState<string[]>([]);
  const [activeColumns, setActiveColumns] = useState<string[]>([]);
  const [selectedModule, setSelectedModule] = useState<string>('');
  const [sortConfig, setSortConfig] = useState<SortConfig>([]);
  const [commonColumns, setCommonColumns] = useState<string[]>([]);

  // 查询配置相关状态
  const [moduleQueryConfig, setModuleQueryConfig] = useState<IModuleQueryConfig | null>(null);
  const [loadingQueryConfig, setLoadingQueryConfig] = useState(false);
  const [columnsLoaded, setColumnsLoaded] = useState(false); // 追踪columns是否已加载

  // 分享参数状态
  const [sharedParams, setSharedParams] = useState<ISharedParams | null>(null);
  const [hasAppliedSharedParams, setHasAppliedSharedParams] = useState(false);

  // 初始化状态
  const [isInitialized, setIsInitialized] = useState(false);

  // 日志检索请求参数
  const [searchParams, setSearchParams] = useState<ILogSearchParams>(DEFAULT_SEARCH_PARAMS);

  // refs
  const searchBarRef = useRef<any>(null);
  const siderRef = useRef<any>(null);
  const abortRef = useRef<AbortController | null>(null);
  const loadedConfigModulesRef = useRef<Set<string>>(new Set());
  const lastCallParamsRef = useRef<string>('');
  const requestTimerRef = useRef<NodeJS.Timeout | null>(null);
  const isInitializingRef = useRef(false);
  const processedUrlRef = useRef<string>('');

  return {
    // 状态
    moduleOptions,
    setModuleOptions,
    detailData,
    setDetailData,
    originalDetailData,
    setOriginalDetailData,
    logTableColumns,
    setLogTableColumns,
    histogramData,
    setHistogramData,
    whereSqlsFromSider,
    setWhereSqlsFromSider,
    keywords,
    setKeywords,
    sqls,
    setSqls,
    activeColumns,
    setActiveColumns,
    selectedModule,
    setSelectedModule,
    sortConfig,
    setSortConfig,
    commonColumns,
    setCommonColumns,
    moduleQueryConfig,
    setModuleQueryConfig,
    loadingQueryConfig,
    setLoadingQueryConfig,
    columnsLoaded,
    setColumnsLoaded,
    sharedParams,
    setSharedParams,
    hasAppliedSharedParams,
    setHasAppliedSharedParams,
    isInitialized,
    setIsInitialized,
    searchParams,
    setSearchParams,

    // refs
    searchBarRef,
    siderRef,
    abortRef,
    loadedConfigModulesRef,
    lastCallParamsRef,
    requestTimerRef,
    isInitializingRef,
    processedUrlRef,
  };
};
