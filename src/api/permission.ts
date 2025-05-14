import { get, post, request } from './request';
import type { DatasourcePermission } from '../types/permissionTypes';

/**
 * 授予用户表权限
 * @param userId 用户ID
 * @param datasourceId 数据源ID
 * @param tableName 表名
 * @returns Promise
 */
export function grantTablePermission(
  userId: string,
  datasourceId: string,
  tableName: string,
): Promise<DatasourcePermission> {
  return post(`/api/permissions/user/${userId}/datasource/${datasourceId}/table/${tableName}`);
}

/**
 * 撤销用户表权限
 * @param userId 用户ID
 * @param datasourceId 数据源ID
 * @param tableName 表名
 * @returns Promise
 */
export function revokeTablePermission(
  userId: string,
  datasourceId: string,
  tableName: string,
): Promise<void> {
  return request({
    method: 'DELETE',
    url: `/api/permissions/user/${userId}/datasource/${datasourceId}/table/${tableName}`,
  });
}

/**
 * 获取用户数据源权限
 * @param userId 用户ID
 * @param datasourceId 数据源ID
 * @returns Promise<DatasourcePermission[]>
 */
export function getUserDatasourcePermissions(
  userId: string,
  datasourceId: string,
): Promise<DatasourcePermission[]> {
  return get(`/api/permissions/user/${userId}/datasource/${datasourceId}`);
}

/**
 * 获取当前用户可访问的表
 * @returns Promise<DatasourcePermission[]>
 */
export function getMyTablePermissions(): Promise<DatasourcePermission[]> {
  return get('/api/permissions/my/tables');
}

/**
 * 通过ID撤销权限
 * @param permissionId 权限ID
 * @returns Promise<void>
 */
export function revokePermissionById(permissionId: string): Promise<void> {
  return request({
    method: 'DELETE',
    url: `/api/permissions/${permissionId}`,
  });
}

// 获取我的模块权限
export const getMyModules = () => {
  return get('/api/permissions/modules/my') as Promise<IModulesResponse[]>;
};
