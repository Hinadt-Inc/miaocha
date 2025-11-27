import { useCallback, useRef, useEffect } from 'react';

import * as api from '@/api/logs';
import * as modulesApi from '@/api/modules';

import { useHomeContext } from '../context';
import { IModuleQueryConfig } from '../types';
import { formatTimeString } from '../utils';

export interface IDetailOptions {
  moduleQueryConfig?: IModuleQueryConfig;
  searchParams: ILogSearchParams;
}

/**
 * 数据初始化Hook - 处理链式接口调用
 */
export const useDataInit = () => {
  const {
    searchParams,
    updateSearchParams,
    setModuleOptions,
    setModuleQueryConfig,
    setLogTableColumns,
    setCommonColumns,
    setDetailData,
    setHistogramData,
    setLoading,
    abortRef,
    distributions,
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
    [distributions, searchParams],
  );

  const fetchModuleQueryConfig = useCallback(
    async (module: string) => {
      const currentModule = (module || searchParams.module) as string;
      const moduleQueryConfig = await modulesApi.getModuleQueryConfig(currentModule);
      const normalizedConfig = { ...moduleQueryConfig, module: currentModule };
      setModuleQueryConfig(normalizedConfig);
      return normalizedConfig;
    },
    [setModuleQueryConfig],
  );

  const fetchTableColumns = useCallback(
    async (module: string, moduleQueryConfig: modulesApi.QueryConfig) => {
      const currentModule = (module || searchParams.module) as string;
      const columns = await api.fetchColumns({ module: currentModule });
      const timeField = moduleQueryConfig?.timeField || 'log_time';
      const normalizedColumns = columns.map((col) => ({
        ...col,
        selected: col.columnName === timeField ? true : (col.selected ?? false),
        _createTime: new Date().getTime(),
      }));
      setLogTableColumns(normalizedColumns);
      const fields = columns.map((item: any) => item.columnName)?.filter((item: any) => !item.includes('.'));
      setCommonColumns(fields);
      return updateSearchParams({
        ...searchParams,
        fields,
        module: currentModule,
      });
    },
    [setCommonColumns, setLogTableColumns, updateSearchParams],
  );

  const fetchData = useCallback(
    async (options: IDetailOptions) => {
      const { moduleQueryConfig, searchParams } = options;

      const params: ILogSearchParams = { ...searchParams };
      delete params.datasourceId;
      if (abortRef.current) {
        abortRef.current.abort();
      }
      abortRef.current = new AbortController();
      api.fetchLogDetails(params, { signal: abortRef.current.signal }).then((res) => {
        const { rows } = res;
        const timeField = moduleQueryConfig?.timeField || homeModuleQueryConfig?.timeField || 'log_time';

        // 为每条记录添加唯一ID并格式化时间字段
        (rows || []).forEach((item, index) => {
          item._key = `${Date.now()}_${index}`;

          if (item[timeField]) {
            item[timeField] = formatTimeString(item[timeField] as string);
          }
          item._originalSource = { ...item };
        });
        setDetailData(res);
      });
      api.fetchLogHistogram(params, { signal: abortRef.current.signal }).then((historyRes) => {
        setHistogramData(historyRes);
      });
    },
    [setDetailData, setHistogramData],
  );

  const handleReloadData = async (options: { module: string }) => {
    const { module } = options;
    // 重置searchParams
    // todo  sortField 等
    // 4. 获取模块查询配置（使用最新的params）
    const moduleQueryConfig = await fetchModuleQueryConfig(module as string);

    // 5. 获取列配置
    const paramsWidthFields = await fetchTableColumns(module as string, moduleQueryConfig);

    // 7. 使用最终的params获取日志数据
    fetchData({
      moduleQueryConfig,
      searchParams: paramsWidthFields,
    });
  };

  /**
   * 初始化完整流程 - 链式调用多个接口
   */
  const initializeData = useCallback(async () => {
    try {
      setLoading?.(true);

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

      const favoriteModule = localStorage.getItem('favoriteModule') || '';

      const favoriteModuleData = moduleOptions.find((item) => item.module === favoriteModule);

      // 2. 确定初始模块（可以从缓存、URL参数或默认取第一个）
      const initModule = favoriteModuleData || moduleData[0];
      if (!initModule) {
        throw new Error('没有可用的模块');
      }

      // 3. 更新searchParams并获取最新值
      const paramsWithModule = updateSearchParams({
        datasourceId: initModule.datasourceId,
        module: initModule.module,
      });

      // 4. 获取模块查询配置（使用最新的params）
      const moduleQueryConfig = await fetchModuleQueryConfig(paramsWithModule.module as string);

      // 5. 获取列配置
      const paramsWidthFields = await fetchTableColumns(paramsWithModule.module as string, moduleQueryConfig);

      // 7. 使用最终的params获取日志数据
      fetchData({
        moduleQueryConfig,
        searchParams: paramsWidthFields,
      });
    } catch (error) {
      console.error('初始化数据失败:', error);
    } finally {
      setLoading?.(false);
    }
  }, [updateSearchParams, setModuleOptions, setModuleQueryConfig, setLogTableColumns, setDetailData, setLoading]);

  return {
    initializeData,
    handleReloadData,
    fetchData,
    fetchFieldDistribution,
    refreshFieldDistributions,
  };
};
