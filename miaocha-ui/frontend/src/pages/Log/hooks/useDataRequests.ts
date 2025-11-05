import { useRequest } from 'ahooks';
import * as api from '@/api/logs';
import * as modulesApi from '@/api/modules';
import { formatTimeString } from '../utils';
import type { ILogSearchParams, IModuleQueryConfig } from '../types';

/**
 * 数据请求相关的hooks
 */

// 获取模块列表的hook
export const useModulesList = () => {
  return useRequest(api.fetchMyModules);
};

// 获取日志详情的hook
export const useLogDetails = (moduleQueryConfig: IModuleQueryConfig | null) => {
  return useRequest(
    async (params: ILogSearchParams & { signal?: AbortSignal }) => {
      const requestParams: any = { ...params };
      delete requestParams?.datasourceId;
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
      },
      onError: (error) => {
        console.error('获取日志详情失败:', error);
      },
    },
  );
};

// 获取日志时间分布的hook
export const useLogHistogram = () => {
  return useRequest(
    async (params: ILogSearchParams & { signal?: AbortSignal }) => {
      const requestParams: any = { ...params };
      delete requestParams?.datasourceId;
      return api.fetchLogHistogram(requestParams, { signal: params.signal });
    },
    {
      manual: true,
      onError: (error) => {
        console.error('获取日志时间分布失败:', error);
      },
    },
  );
};

// 获取模块查询配置的hook
export const useModuleQueryConfig = () => {
  return useRequest((moduleName: string) => modulesApi.getModuleQueryConfig(moduleName), {
    manual: true,
    onError: (error) => {
      console.error('获取模块查询配置失败:', error);
    },
  });
};
