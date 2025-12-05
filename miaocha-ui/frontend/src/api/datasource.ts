import type {
  DataSource,
  CreateDataSourceParams,
  UpdateDataSourceParams,
  TestConnectionParams,
  DatasourceConnectionTestResult,
} from '../types/datasourceTypes';

import { request } from './request';

// 获取数据源列表
export function getDataSources(): Promise<DataSource[]> {
  return request({
    url: '/api/datasources',
    method: 'GET',
  });
}

// 获取数据源详情
export function getDataSource(id: string): Promise<DataSource> {
  return request({
    url: `/api/datasources/${id}`,
    method: 'GET',
  });
}

// 更新数据源
export function updateDataSource(id: string, data: UpdateDataSourceParams): Promise<DataSource> {
  return request({
    url: `/api/datasources/${id}`,
    method: 'PUT',
    data,
  });
}

// 删除数据源
export function deleteDataSource(id: string): Promise<void> {
  return request({
    url: `/api/datasources/${id}`,
    method: 'DELETE',
  });
}

// 获取所有数据源
export function getAllDataSources(): Promise<DataSource[]> {
  return request({
    url: '/api/datasources',
    method: 'GET',
  });
}

// 创建数据源
export function createDataSource(data: CreateDataSourceParams): Promise<DataSource> {
  return request({
    url: '/api/datasources',
    method: 'POST',
    data,
  });
}

// 测试数据源连接
export function testDataSourceConnection(data: TestConnectionParams): Promise<DatasourceConnectionTestResult> {
  return request({
    url: '/api/datasources/test-connection',
    method: 'POST',
    data,
  });
}

// 测试现有数据源连接
export function testExistingDataSourceConnection(id: string): Promise<DatasourceConnectionTestResult> {
  return request({
    url: `/api/datasources/${id}/test-connection`,
    method: 'POST',
  });
}
