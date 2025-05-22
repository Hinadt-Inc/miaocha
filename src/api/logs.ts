import { post, get } from './request';

// 获取我的模块权限
export function fetchMyModules(): Promise<IMyModulesResponse[]> {
  return get('/api/permissions/modules/my');
}

// 执行日志检索
export function fetchLogDetails(params: ILogSearchParams): Promise<ILogDetailsResponse> {
  return post('/api/logs/search/details', params);
}

// 执行日志时间分布查询
export function fetchLogHistogram(params: ILogSearchParams): Promise<ILogHistogramResponse> {
  return post('/api/logs/search/histogram', params);
}

// 仅查询指定字段的TOP5分布数据
export function fetchDistributions(params: ILogSearchParams): Promise<IDistributionsResponse> {
  return post('/api/logs/search/field-distributions', params);
}
// 获取日志表字段
export function fetchColumns(params: ILogColumnsParams): Promise<ILogColumnsResponse[]> {
  return get('/api/logs/columns', { params });
}
