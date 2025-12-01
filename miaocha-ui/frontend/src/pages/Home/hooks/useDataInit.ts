import { useCallback, useRef, useEffect } from 'react';

import { useSearchParams } from 'react-router-dom';

import * as api from '@/api/logs';
import * as modulesApi from '@/api/modules';

import { useHomeContext } from '../context';
import { IModuleQueryConfig } from '../types';
import { formatTimeString, handleShareSearchParams } from '../utils';

export interface IDetailOptions {
  moduleQueryConfig?: IModuleQueryConfig;
  searchParams: ILogSearchParams;
}

/**
 * 数据初始化Hook - 处理链式接口调用
 */
export const useDataInit = () => {
  const [urlSearchParams, setUrlSearchParams] = useSearchParams();
  const {
    abortRef,
    searchParams,
    logTableColumns,
    moduleQueryConfig,
    distributions,
    distributionLoading,
    updateSearchParams,
    setModuleOptions,
    setModuleQueryConfig,
    setLogTableColumns,
    setCommonColumns,
    setDetailData,
    setHistogramData,
    setLoading,
    setDistributions,
    setDistributionLoading,
    moduleQueryConfig: homeModuleQueryConfig,
  } = useHomeContext();

  // 使用ref保存最新的searchParams，避免闭包陷阱
  const searchParamsRef = useRef(searchParams);
  useEffect(() => {
    searchParamsRef.current = searchParams;
  }, [searchParams]);

  const fetchFieldDistribution = useCallback(
    async (columnName: string) => {
      const params: ILogSearchParams = {
        ...searchParams,
        fields: [columnName],
      };
      // 使用函数式更新，确保基于最新状态
      setDistributionLoading((prev) => ({ ...prev, [columnName]: true }));
      try {
        const res = await api.fetchDistributions(params);
        const { fieldDistributions = [], sampleSize } = res;
        const target: Record<string, IFieldDistributions> = {};
        fieldDistributions.map((item) => {
          const { fieldName } = item;
          target[fieldName] = { ...item, sampleSize };
        });
        setDistributions((prev) => ({
          ...prev,
          ...target,
        }));
      } finally {
        // 使用 finally 确保 loading 一定会被重置
        setTimeout(() => {
          setDistributionLoading((prev) => ({ ...prev, [columnName]: false }));
        }, 200);
      }
    },
    [searchParams],
  );

  const refreshFieldDistributions = useCallback(
    async (newSearchParams?: ILogSearchParams) => {
      const currentSearchParams = newSearchParams || searchParams;
      const fields = Object.keys(distributions);
      if (fields.length === 0) return;
      setDistributionLoading(
        fields.reduce((acc: any, field) => {
          acc[field] = true;
          return acc;
        }, {}),
      );
      try {
        const res = await api.fetchDistributions({
          ...currentSearchParams,
          fields,
        });
        const target: Record<string, IFieldDistributions> = {};
        const { fieldDistributions = [], sampleSize } = res;
        fieldDistributions.map((item) => {
          const { fieldName } = item;
          target[fieldName] = { ...item, sampleSize };
        });
        setDistributions({
          ...target,
        });
      } finally {
        setTimeout(() => {
          setDistributionLoading(
            fields.reduce((acc: any, field) => {
              acc[field] = false;
              return acc;
            }, {}),
          );
        }, 200);
      }
    },
    [distributions, searchParams, distributionLoading],
  );

  const fetchModuleQueryConfig = useCallback(
    async (module: string) => {
      const currentModule = (module || searchParams.module) as string;
      const moduleQueryConfig = await modulesApi.getModuleQueryConfig(currentModule);
      const normalizedConfig = { ...moduleQueryConfig, module: currentModule };
      setModuleQueryConfig(normalizedConfig);
      return normalizedConfig;
    },
    [searchParams, setModuleQueryConfig],
  );

  const fetchTableColumns = useCallback(
    async (module: string) => {
      const currentModule = (module || searchParams.module) as string;
      const columns = await api.fetchColumns({ module: currentModule });
      return columns;
    },
    [searchParams, setCommonColumns, setLogTableColumns, updateSearchParams],
  );

  const fetchData = useCallback(
    async (options: IDetailOptions) => {
      const { moduleQueryConfig, searchParams } = options;

      const params: ILogSearchParams = { ...searchParams };
      if (abortRef.current) {
        abortRef.current.abort();
      }
      abortRef.current = new AbortController();
      api.fetchLogDetails(params, { signal: abortRef.current.signal }).then((res) => {
        const { rows } = res;
        const timeField = moduleQueryConfig?.timeField || homeModuleQueryConfig?.timeField || 'log_time';

        // 为每条记录添加唯一ID并格式化时间字段
        (rows || []).forEach((item, index) => {
          if (item[timeField]) {
            item[timeField] = formatTimeString(item[timeField] as string);
          }
          item._originalSource = { ...item };
          item._key = `${Date.now()}_${index}`;
        });
        setDetailData(res);
      });
      api.fetchLogHistogram(params, { signal: abortRef.current.signal }).then((historyRes) => {
        setHistogramData(historyRes);
      });
    },
    [setDetailData, setHistogramData],
  );

  // 切换数据源，重置状态
  const handleReloadData = useCallback(
    async (options: { module: string }) => {
      const { module } = options;
      // 重置searchParams
      // 4. 获取模块查询配置（使用最新的params）
      const moduleQueryConfig = await fetchModuleQueryConfig(module as string);

      // 5. 获取列配置
      const columns = await fetchTableColumns(module as string);

      // 6. 重置状态
      const timeField = moduleQueryConfig?.timeField || 'log_time';
      const normalizedColumns = columns.map((col) => ({
        ...col,
        selected: col.columnName === timeField ? true : (col.selected ?? false),
        _createTime: new Date().getTime(),
      }));
      setLogTableColumns(normalizedColumns);
      const fields = columns.map((item: any) => item.columnName)?.filter((item: any) => !item.includes('.'));
      setCommonColumns(fields);
      const paramsWidthFields = updateSearchParams({
        ...searchParams,
        fields,
        sortFields: [],
        keywords: [],
        whereSqls: [],
        offset: 50,
        timeGrouping: 'auto',
        timeRange: 'last_15m',
        module,
      });
      setDistributions({});
      setDistributionLoading({});

      // 7. 使用最终的params获取日志数据
      fetchData({
        moduleQueryConfig,
        searchParams: paramsWidthFields,
      });
    },
    [
      fetchModuleQueryConfig,
      fetchTableColumns,
      setLogTableColumns,
      setCommonColumns,
      updateSearchParams,
      searchParams,
      logTableColumns,
      moduleQueryConfig,
      distributions,
      distributionLoading,
      setDistributions,
      setDistributionLoading,
      fetchData,
    ],
  );

  // 空状态初始化
  const handleInitData = useCallback(
    async (module: string) => {
      updateSearchParams({ module });
      // 4. 获取模块查询配置（使用最新的params）
      const moduleQueryConfig = await fetchModuleQueryConfig(module as string);

      // 5. 获取列配置
      const columns = await fetchTableColumns(module as string);

      // 便于后续处理分享和本地化存储
      const timeField = moduleQueryConfig?.timeField || 'log_time';
      const normalizedColumns = columns.map((col) => ({
        ...col,
        selected: col.columnName === timeField ? true : (col.selected ?? false),
        _createTime: new Date().getTime(),
      }));
      setLogTableColumns(normalizedColumns);
      const fields = columns.map((item: any) => item.columnName)?.filter((item: any) => !item.includes('.'));
      setCommonColumns(fields);
      const paramsWidthFields = updateSearchParams({
        ...searchParams,
        fields,
        sortFields: [],
        keywords: [],
        whereSqls: [],
        offset: 50,
        module,
      });
      setDistributions({});
      setDistributionLoading({});

      // 7. 使用最终的params获取日志数据
      fetchData({
        moduleQueryConfig,
        searchParams: paramsWidthFields,
      });
    },
    [
      updateSearchParams,
      fetchModuleQueryConfig,
      fetchTableColumns,
      setLogTableColumns,
      setCommonColumns,
      searchParams,
      logTableColumns,
      moduleQueryConfig,
      distributions,
      distributionLoading,
      setDistributions,
      setDistributionLoading,
      fetchData,
    ],
  );

  // 本地化回显 | 分享回显 | 检索条件回显
  const handleLoadCacheData = useCallback(
    async (params: Partial<ILogSearchParams>) => {
      const { module, fields, ...rest } = params;
      updateSearchParams({ module });
      // 4. 获取模块查询配置（使用最新的params）
      const moduleQueryConfig = await fetchModuleQueryConfig(module as string);

      // 5. 获取列配置
      const columns = await fetchTableColumns(module as string);

      // 便于后续处理分享和本地化存储
      const timeField = moduleQueryConfig?.timeField || 'log_time';
      const fieldsDots = fields?.filter((field) => field.includes('.')) || [];
      const normalizedColumns = columns.map((col) => ({
        ...col,
        selected:
          col.columnName && [...fieldsDots, timeField].includes(col.columnName) ? true : (col.selected ?? false),
        _createTime: new Date().getTime(),
      }));
      setLogTableColumns(normalizedColumns);
      const commonFields = columns.map((item: any) => item.columnName)?.filter((item: any) => !item.includes('.'));
      setCommonColumns(commonFields);
      const newFields = normalizedColumns.filter((item: any) => item.selected).map((item: any) => item.columnName);
      const paramsWidthFields = updateSearchParams({
        ...searchParams,
        fields: Array.from(new Set([...commonFields, ...newFields])),
        offset: 50,
        module,
        ...rest,
      });
      setDistributions({});
      setDistributionLoading({});

      // 7. 使用最终的params获取日志数据
      fetchData({
        moduleQueryConfig,
        searchParams: paramsWidthFields,
      });
    },
    [
      searchParams,
      logTableColumns,
      moduleQueryConfig,
      distributions,
      distributionLoading,
      updateSearchParams,
      fetchModuleQueryConfig,
      fetchTableColumns,
      setLogTableColumns,
      setCommonColumns,
      setDistributions,
      setDistributionLoading,
      fetchData,
    ],
  );

  /**
   * 初始化完整流程 - 链式调用多个接口
   */
  const initializeData = useCallback(async () => {
    try {
      setLoading?.(true);

      const tabId = urlSearchParams.get('tabId');

      // 1. 获取模块列表
      const moduleData = await api.fetchMyModules();
      const moduleOptions = moduleData.map(({ datasourceId, datasourceName, module }) => ({
        label: module,
        value: module,
        datasourceId,
        datasourceName,
        module,
      }));
      setModuleOptions(moduleOptions);

      let cachedParams: Partial<ILogSearchParams> = {};

      const sharedParams = handleShareSearchParams(urlSearchParams);
      if (sharedParams && sharedParams.module) {
        cachedParams = sharedParams as Partial<ILogSearchParams>;
      } else {
        try {
          if (tabId) {
            const storageParams = JSON.parse(localStorage.getItem(`${tabId}_searchParams`) || '{}');
            if (storageParams && storageParams.module) {
              cachedParams = storageParams || {};
            }
          } else {
            const storageTabIds = JSON.parse(localStorage.getItem('tabIds') || '[]');
            const tempId = new Date().getTime().toString();
            const newParams = new URLSearchParams(urlSearchParams);
            newParams.set('tabId', tempId);
            setUrlSearchParams(newParams, { replace: true });

            if (storageTabIds.length >= 5) {
              const oldTabId = storageTabIds.pop();
              localStorage.removeItem(`${oldTabId}_searchParams`);
            }
            localStorage.setItem(`${tempId}_searchParams`, JSON.stringify({}));
            localStorage.setItem('tabIds', JSON.stringify([tempId, ...storageTabIds]));
          }
        } catch (error) {
          console.log('error', error);
        }
      }

      if (cachedParams.module) {
        handleLoadCacheData(cachedParams);
      } else {
        const favoriteModule = localStorage.getItem('favoriteModule') || '';
        const favoriteModuleData = moduleOptions.find((item) => item.module === favoriteModule);
        // 2. 确定初始模块（可以从缓存、URL参数或默认取第一个）
        const initModule = favoriteModuleData?.module || moduleData[0]?.module || '';
        if (!initModule) {
          throw new Error('没有可用的模块');
        }
        handleInitData(initModule);
      }
    } catch (error) {
      console.error('初始化数据失败:', error);
    } finally {
      setLoading?.(false);
    }
  }, [
    searchParams,
    distributions,
    distributionLoading,
    setDistributionLoading,
    setDistributions,
    updateSearchParams,
    setModuleOptions,
    setModuleQueryConfig,
    setLogTableColumns,
    setDetailData,
    setLoading,
  ]);

  return {
    initializeData,
    handleLoadCacheData,
    handleReloadData,
    fetchData,
    fetchFieldDistribution,
    refreshFieldDistributions,
  };
};
