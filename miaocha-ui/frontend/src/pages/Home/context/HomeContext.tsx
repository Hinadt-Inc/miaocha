import { createContext, useContext, useState, ReactNode, useRef, MutableRefObject, useEffect } from 'react';

import { useSearchParams } from 'react-router-dom';

import { DEFAULT_SEARCH_PARAMS } from '../constants';
import type { ILogColumnsResponse, ILogDetailsResponse, IModuleQueryConfig, IStatus } from '../types';
import { deduplicateAndDeleteWhereSqls, parseTimeRange } from '../utils';

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
  commonColumns: string[];
  searchParams: ILogSearchParams;
  loading: boolean;
  abortRef: MutableRefObject<AbortController | null>;
  searchParamsRef: MutableRefObject<ILogSearchParams>; // 暴露 ref 用于获取最新的 searchParams
  distributions: Record<string, IFieldDistributions>;
  distributionLoading: Record<string, boolean>;

  // 方法
  setModuleOptions: (data: IStatus[]) => void;
  setModuleQueryConfig: (config: IModuleQueryConfig | null) => void;
  setDetailData: (data: ILogDetailsResponse | null) => void;
  setLogTableColumns: (columns: ILogColumnsResponse[]) => void;
  setHistogramData: (data: ILogHistogramResponse | null) => void;
  setCommonColumns: (columns: string[]) => void;
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
  const [urlSearchParams] = useSearchParams();
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
  const [loading, setLoading] = useState<boolean>(false);

  const abortRef = useRef<AbortController | null>(null);
  const searchParamsRef = useRef<ILogSearchParams>(searchParams); // 创建 searchParams 的 ref

  // 同步 searchParams 到 ref
  useEffect(() => {
    searchParamsRef.current = searchParams;
  }, [searchParams]);

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
    const { whereSqls, fields, timeRange, keywords, ...rest } = params;
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
    };

    const { range, type } = parseTimeRange(newSearchParams?.timeRange);
    Object.assign(newSearchParams, { startTime: range?.[0], endTime: range?.[1], range, type });
    setSearchParams(newSearchParams);
    const tabId = urlSearchParams.get('tabId');
    if (tabId) {
      localStorage.setItem(`${tabId}_searchParams`, JSON.stringify(newSearchParams));
    }
    return newSearchParams;
  };

  // 重置所有状态
  const resetAllState = () => {
    setModuleOptions([]);
    setModuleQueryConfig(null);
    setDetailData(null);
    setLogTableColumns([]);
    setHistogramData(null);
    setCommonColumns([]);
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
    commonColumns,
    searchParams,
    loading,
    abortRef,
    searchParamsRef, // 暴露 searchParamsRef
    distributions,
    distributionLoading,

    // 方法
    setModuleOptions,
    setModuleQueryConfig,
    setDetailData,
    setLogTableColumns,
    setHistogramData,
    setCommonColumns,
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
