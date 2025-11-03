import { createSlice, PayloadAction } from '@reduxjs/toolkit';

import { DEFAULT_SEARCH_PARAMS } from '@/pages/Log/constants';
import type {
  ILogDetailsResponse,
  ILogColumnsResponse,
  ILogHistogramData,
  ILogSearchParams,
  IModuleQueryConfig,
  ISharedParams,
  SortConfig,
  IStatus,
  IWhereSqlStatus,
} from '@/pages/Log/types';

// 用于 whereSqlsFromSider 的专门接口

// 定义日志状态的接口
export interface LogState {
  // 模块选项
  moduleOptions: IStatus[];

  // 日志数据
  detailData: ILogDetailsResponse | null;
  originalDetailData: ILogDetailsResponse | null;
  logTableColumns: ILogColumnsResponse[];
  histogramData: ILogHistogramData | null;

  // 查询条件
  whereSqlsFromSider: IWhereSqlStatus[];
  keywords: string[];
  sqls: string[];
  activeColumns: string[];
  selectedModule: string;
  sortConfig: SortConfig;
  commonColumns: string[];

  // 模块配置
  moduleQueryConfig: IModuleQueryConfig | null;
  loadingQueryConfig: boolean;
  columnsLoaded: boolean;

  // 分享参数
  sharedParams: ISharedParams | null;
  hasAppliedSharedParams: boolean;

  // 初始化状态
  isInitialized: boolean;

  // 搜索参数
  searchParams: ILogSearchParams;
}

const initialState: LogState = {
  // 模块选项
  moduleOptions: [],

  // 日志数据
  detailData: null,
  originalDetailData: null,
  logTableColumns: [],
  histogramData: null,

  // 查询条件
  whereSqlsFromSider: [],
  keywords: [],
  sqls: [],
  activeColumns: [],
  selectedModule: '',
  sortConfig: [],
  commonColumns: [],

  // 模块配置
  moduleQueryConfig: null,
  loadingQueryConfig: false,
  columnsLoaded: false,

  // 分享参数
  sharedParams: null,
  hasAppliedSharedParams: false,

  // 初始化状态
  isInitialized: false,

  // 搜索参数
  searchParams: DEFAULT_SEARCH_PARAMS,
};

export const logSlice = createSlice({
  name: 'log',
  initialState,
  reducers: {
    // 模块选项相关
    setModuleOptions: (state, action: PayloadAction<IStatus[]>) => {
      state.moduleOptions = action.payload;
    },

    // 日志数据相关
    setDetailData: (state, action: PayloadAction<ILogDetailsResponse | null>) => {
      state.detailData = action.payload;
    },
    setOriginalDetailData: (state, action: PayloadAction<ILogDetailsResponse | null>) => {
      state.originalDetailData = action.payload;
    },
    setLogTableColumns: (state, action: PayloadAction<ILogColumnsResponse[]>) => {
      state.logTableColumns = action.payload;
    },
    setHistogramData: (state, action: PayloadAction<ILogHistogramData | null>) => {
      state.histogramData = action.payload;
    },

    // 查询条件相关
    setWhereSqlsFromSider: (state, action: PayloadAction<IWhereSqlStatus[]>) => {
      state.whereSqlsFromSider = action.payload;
    },
    setKeywords: (state, action: PayloadAction<string[]>) => {
      state.keywords = action.payload;
    },
    setSqls: (state, action: PayloadAction<string[]>) => {
      state.sqls = action.payload;
    },
    setActiveColumns: (state, action: PayloadAction<string[]>) => {
      state.activeColumns = action.payload;
    },
    setSelectedModule: (state, action: PayloadAction<string>) => {
      state.selectedModule = action.payload;
    },
    setSortConfig: (state, action: PayloadAction<SortConfig>) => {
      state.sortConfig = action.payload;
    },
    setCommonColumns: (state, action: PayloadAction<string[]>) => {
      state.commonColumns = action.payload;
    },

    // 模块配置相关
    setModuleQueryConfig: (state, action: PayloadAction<IModuleQueryConfig | null>) => {
      state.moduleQueryConfig = action.payload;
    },
    setLoadingQueryConfig: (state, action: PayloadAction<boolean>) => {
      state.loadingQueryConfig = action.payload;
    },
    setColumnsLoaded: (state, action: PayloadAction<boolean>) => {
      state.columnsLoaded = action.payload;
    },

    // 分享参数相关
    setSharedParams: (state, action: PayloadAction<ISharedParams | null>) => {
      state.sharedParams = action.payload;
    },
    setHasAppliedSharedParams: (state, action: PayloadAction<boolean>) => {
      state.hasAppliedSharedParams = action.payload;
    },

    // 初始化状态相关
    setIsInitialized: (state, action: PayloadAction<boolean>) => {
      state.isInitialized = action.payload;
    },

    // 搜索参数相关
    setSearchParams: (state, action: PayloadAction<ILogSearchParams>) => {
      state.searchParams = action.payload;
    },
    updateSearchParams: (state, action: PayloadAction<Partial<ILogSearchParams>>) => {
      state.searchParams = { ...state.searchParams, ...action.payload };
    },

    // 重置状态
    resetLogState: () => initialState,
  },
});

export const {
  setModuleOptions,
  setDetailData,
  setOriginalDetailData,
  setLogTableColumns,
  setHistogramData,
  setWhereSqlsFromSider,
  setKeywords,
  setSqls,
  setActiveColumns,
  setSelectedModule,
  setSortConfig,
  setCommonColumns,
  setModuleQueryConfig,
  setLoadingQueryConfig,
  setColumnsLoaded,
  setSharedParams,
  setHasAppliedSharedParams,
  setIsInitialized,
  setSearchParams,
  updateSearchParams,
  resetLogState,
} = logSlice.actions;

export default logSlice.reducer;
