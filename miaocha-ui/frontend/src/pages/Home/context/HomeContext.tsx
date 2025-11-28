import { createContext, useContext, useState, ReactNode, useRef, MutableRefObject } from 'react';

import { DEFAULT_SEARCH_PARAMS } from '../constants';
import type {
  ILogColumnsResponse,
  ILogDetailsResponse,
  IModuleQueryConfig,
  ISharedParams,
  IStatus,
  SortConfig,
} from '../types';
import { DATE_FORMAT_THOUSOND, deduplicateAndDeleteWhereSqls, QUICK_RANGES } from '../utils';

/**
 * Home页面Context值接口
 */
interface HomeContextValue {
  // 状态
  moduleOptions: IStatus[];
  moduleQueryConfig: IModuleQueryConfig | null;
  detailData: ILogDetailsResponse | null;
  logTableColumns: ILogColumnsResponse[];
  histogramData: ILogHistogramResponse | null;
  whereSqlsFromSider: IStatus[];
  keywords: string[];
  sqls: string[];
  activeColumns: string[];
  selectedModule: string;
  sortConfig: SortConfig;
  commonColumns: string[];
  loadingQueryConfig: boolean;
  columnsLoaded: boolean;
  sharedParams: ISharedParams | null;
  hasAppliedSharedParams: boolean;
  isInitialized: boolean;
  searchParams: ILogSearchParams;
  loading: boolean;
  abortRef: MutableRefObject<AbortController | null>;
  distributions: Record<string, IFieldDistributions>;
  distributionLoading: Record<string, boolean>;

  // 方法
  setModuleOptions: (data: IStatus[]) => void;
  setModuleQueryConfig: (config: IModuleQueryConfig | null) => void;
  setDetailData: (data: ILogDetailsResponse | null) => void;
  setLogTableColumns: (columns: ILogColumnsResponse[]) => void;
  setHistogramData: (data: ILogHistogramResponse | null) => void;
  setWhereSqlsFromSider: (sqls: IStatus[]) => void;
  setKeywords: (keywords: string[]) => void;
  setSqls: (sqls: string[]) => void;
  setActiveColumns: (columns: string[]) => void;
  setSelectedModule: (module: string) => void;
  setSortConfig: (config: SortConfig) => void;
  setCommonColumns: (columns: string[]) => void;
  setLoadingQueryConfig: (loading: boolean) => void;
  setColumnsLoaded: (loaded: boolean) => void;
  setSharedParams: (params: ISharedParams | null) => void;
  setHasAppliedSharedParams: (applied: boolean) => void;
  setIsInitialized: (initialized: boolean) => void;
  setSearchParams: (params: ILogSearchParams) => void;
  setLoading: (loading: boolean) => void;
  // 新增：更新searchParams并返回最新值（用于立即调用接口）
  updateSearchParams: (updates: Partial<ILogSearchParams>) => ILogSearchParams;
  resetSearchParams: () => void;
  resetAllState: () => void;
  setDistributions: (
    columns:
      | Record<string, IFieldDistributions>
      | ((prev: Record<string, IFieldDistributions>) => Record<string, IFieldDistributions>),
  ) => void;
  setDistributionLoading: (
    columns: Record<string, boolean> | ((prev: Record<string, boolean>) => Record<string, boolean>),
  ) => void;
}

const HomeContext = createContext<HomeContextValue | undefined>(undefined);

export const HomeProvider = ({ children }: { children: ReactNode }) => {
  // 使用独立的useState管理每个状态
  const [moduleOptions, setModuleOptions] = useState<IStatus[]>([]);
  const [moduleQueryConfig, setModuleQueryConfig] = useState<IModuleQueryConfig | null>(null);
  const [detailData, setDetailData] = useState<ILogDetailsResponse | null>(null);
  const [logTableColumns, setLogTableColumns] = useState<ILogColumnsResponse[]>([]);
  const [histogramData, setHistogramData] = useState<ILogHistogramResponse | null>(null);
  const [commonColumns, setCommonColumns] = useState<string[]>([]);
  const [searchParams, setSearchParams] = useState<ILogSearchParams>({ ...DEFAULT_SEARCH_PARAMS });
  const [distributions, setDistributions] = useState<Record<string, IFieldDistributions>>({});
  const [distributionLoading, setDistributionLoading] = useState<Record<string, boolean>>({});

  const [activeColumns, setActiveColumns] = useState<string[]>([]);
  const [whereSqlsFromSider, setWhereSqlsFromSider] = useState<IStatus[]>([]);
  const [keywords, setKeywords] = useState<string[]>([]);
  const [sqls, setSqls] = useState<string[]>([]);
  const [selectedModule, setSelectedModule] = useState<string>('');
  const [sortConfig, setSortConfig] = useState<SortConfig>([]);
  const [loadingQueryConfig, setLoadingQueryConfig] = useState<boolean>(false);
  const [columnsLoaded, setColumnsLoaded] = useState<boolean>(false);
  const [sharedParams, setSharedParams] = useState<ISharedParams | null>(null);
  const [hasAppliedSharedParams, setHasAppliedSharedParams] = useState<boolean>(false);
  const [isInitialized, setIsInitialized] = useState<boolean>(false);
  const [loading, setLoading] = useState<boolean>(false);

  const abortRef = useRef<AbortController | null>(null);

  // 重置搜索参数
  const resetSearchParams = () => {
    setSearchParams({ ...DEFAULT_SEARCH_PARAMS });
  };

  // 更新searchParams并返回最新值（用于立即调用接口）
  const updateSearchParams = (updates: Partial<ILogSearchParams>) => {
    const newSearchParams = handleSetSearchParams({ ...searchParams, ...updates });
    return newSearchParams; // 返回最新值，调用方可以立即使用
  };

  const handleSetSearchParams = (params: ILogSearchParams) => {
    const { whereSqls, fields, timeRange, timeType, keywords, ...rest } = params;
    // 去重whereSqls：去掉空格后完全一致的元素只保留一个
    const newWhereSqls = deduplicateAndDeleteWhereSqls(whereSqls || []);
    const newKeywords = Array.from(new Set(keywords || []));
    const newFields = Array.from(new Set(fields || []));

    const newSearchParams = {
      ...rest,
      whereSqls: newWhereSqls,
      fields: newFields,
      keywords: newKeywords,
      timeRange,
      timeType,
    };
    if (timeType === 'quick') {
      newSearchParams.startTime = QUICK_RANGES[timeRange || 'last_15m'].from().format(DATE_FORMAT_THOUSOND);
      newSearchParams.endTime = QUICK_RANGES[timeRange || 'last_15m'].to().format(DATE_FORMAT_THOUSOND);
    }

    setSearchParams(newSearchParams);
    return newSearchParams;
  };

  // 重置所有状态
  const resetAllState = () => {
    setModuleOptions([]);
    setModuleQueryConfig(null);
    setDetailData(null);
    setLogTableColumns([]);
    setHistogramData(null);
    setWhereSqlsFromSider([]);
    setKeywords([]);
    setSqls([]);
    setActiveColumns([]);
    setSelectedModule('');
    setSortConfig([]);
    setCommonColumns([]);
    setLoadingQueryConfig(false);
    setColumnsLoaded(false);
    setSharedParams(null);
    setHasAppliedSharedParams(false);
    setIsInitialized(false);
    setSearchParams({ ...DEFAULT_SEARCH_PARAMS });
    setLoading(false);
    setDistributions({});
    abortRef.current?.abort();
  };

  const value: HomeContextValue = {
    // 状态
    moduleOptions,
    moduleQueryConfig,
    detailData,
    logTableColumns,
    histogramData,
    whereSqlsFromSider,
    keywords,
    sqls,
    activeColumns,
    selectedModule,
    sortConfig,
    commonColumns,
    loadingQueryConfig,
    columnsLoaded,
    sharedParams,
    hasAppliedSharedParams,
    isInitialized,
    searchParams,
    loading,
    abortRef,
    distributions,
    distributionLoading,

    // 方法
    setModuleOptions,
    setModuleQueryConfig,
    setDetailData,
    setLogTableColumns,
    setHistogramData,
    setWhereSqlsFromSider,
    setKeywords,
    setSqls,
    setActiveColumns,
    setSelectedModule,
    setSortConfig,
    setCommonColumns,
    setLoadingQueryConfig,
    setColumnsLoaded,
    setSharedParams,
    setHasAppliedSharedParams,
    setIsInitialized,
    setSearchParams: handleSetSearchParams,
    setLoading,
    updateSearchParams,
    resetSearchParams,
    resetAllState,
    setDistributions,
    setDistributionLoading,
  };

  return <HomeContext.Provider value={value}>{children}</HomeContext.Provider>;
};

export const useHomeContext = () => {
  const context = useContext(HomeContext);
  if (!context) {
    throw new Error('useHomeContext must be used within a HomeProvider');
  }
  return context;
};
