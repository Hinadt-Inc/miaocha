import {del, post, get } from './request';

// 获取我的模块权限
export function fetchMyModules(): Promise<IMyModulesResponse[]> {
  return get('/api/permissions/modules/my');
}

// 执行日志检索
export function fetchLogDetails(
  params: ILogSearchParams,
  options?: { signal?: AbortSignal },
): Promise<ILogDetailsResponse> {
  return post('/api/logs/search/details', params, { signal: options?.signal });
}

// 执行日志时间分布查询
export function fetchLogHistogram(
  params: ILogSearchParams,
  options?: { signal?: AbortSignal },
): Promise<ILogHistogramResponse> {
  return post('/api/logs/search/histogram', params, { signal: options?.signal });
}

// 仅查询指定字段的TOP5分布数据
export function fetchDistributions(
  params: ILogSearchParams,
  options?: { signal?: AbortSignal },
): Promise<IDistributionsResponse> {
  return post('/api/logs/search/field-distributions', params, { signal: options?.signal });
}
// 获取日志表字段
export function fetchColumns(params: ILogColumnsParams): Promise<ILogColumnsResponse[]> {
  return get('/api/logs/columns', { params });
}

// 获取用户保存的搜索条件
export function fetchSavedSearchConditions(): Promise<ISavedSearchCondition[]> {
  return get('/api/logs/search/conditions');
}

// 获取缓存的搜索条件列表
export function fetchCachedSearchConditions(): Promise<ICachedSearchCondition[]> {
  return get('/api/logs/search/conditions');
}

// 保存搜索条件
export function saveSearchCondition(params: ISaveSearchConditionParams): Promise<ISavedSearchCondition> {
  return post('/api/logs/search/conditions', params);
}

// 保存搜索条件并返回缓存键
export function saveSearchConditionWithCache(params: ISaveSearchConditionWithCacheParams): Promise<ISaveSearchConditionCacheResponse> {
  return post('/api/logs/search/save-condition', params);
}

// 通过缓存键加载搜索条件
export function loadSearchConditionByCache(cacheKey: string): Promise<ISaveSearchConditionWithCacheParams> {
  return get(`/api/logs/search/load-condition/${cacheKey}`);
}

// 删除搜索条件
export function deleteSearchCondition(id: string): Promise<void> {
  return post(`/api/logs/search/conditions/${id}/delete`);
}

// 删除缓存的搜索条件
export function deleteCachedSearchConditions(params: {
  cacheGroup: string;
  cacheKeys: string[];
}): Promise<void> {
  return del('/api/logs/search/conditions', params);
}
