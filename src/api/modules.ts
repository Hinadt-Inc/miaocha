import { request } from './request';

export interface Module {
  id: number;
  name: string;
  datasourceId: number;
  datasourceName: string;
  tableName: string;
  dorisSql: string;
  createTime: string;
  updateTime: string;
  createUser: string;
  createUserName: string;
  updateUser: string;
  updateUserName: string;
  users?: Array<{
    userId: string;
    nickname: string;
  }>;
}

export interface CreateModuleParams {
  name: string;
  datasourceId: number;
  tableName: string;
  dorisSql: string;
}

export interface UpdateModuleParams {
  id: number;
  name?: string;
  datasourceId?: number;
  tableName?: string;
}

export const getModules = async (config?: any) => {
  return request<Module[]>({
    url: '/api/modules',
    method: 'GET',
    ...config,
  });
};

export const createModule = async (params: CreateModuleParams) => {
  return request<Module>({
    url: '/api/modules',
    method: 'POST',
    data: params,
  });
};

export const updateModule = async (params: UpdateModuleParams) => {
  return request<Module>({
    url: '/api/modules',
    method: 'PUT',
    data: params,
  });
};

export const deleteModule = async (id: number) => {
  return request({
    url: `/api/modules/${id}`,
    method: 'DELETE',
  });
};

export const getModuleDetail = async (id: number) => {
  return request<Module>({
    url: `/api/modules/${id}`,
    method: 'GET',
  });
};

export const executeDorisSql = async (id: number, sql: string) => {
  return request({
    url: `/api/modules/${id}/execute-doris-sql`,
    method: 'POST',
    data: { sql },
  });
};

export const authorizeModule = async (userId: string, moduleName: string) => {
  return request({
    url: `/api/permissions/modules/user/${userId}/grant?module=${moduleName}`,
    method: 'POST',
  });
};

export const revokeModule = async (userId: string, moduleName: string) => {
  return request({
    url: `/api/permissions/modules/user/${userId}/revoke?module=${moduleName}`,
    method: 'DELETE',
  });
};

// 批量授权
export const batchAuthorizeModules = async (userId: string, moduleNames: string[]) => {
  return request({
    url: `/api/permissions/modules/user/${userId}/batch-grant`,
    method: 'POST',
    data: { userId, modules: moduleNames },
  });
};

// 批量撤销授权
export const batchRevokeModules = async (userId: string, moduleNames: string[]) => {
  return request({
    url: `/api/permissions/modules/user/${userId}/batch-revoke`,
    method: 'DELETE',
    data: { userId, modules: moduleNames },
  });
};

export const batchAuthorizeModulesWithExpiry = async (
  userId: string,
  modules: Array<{
    moduleId: string;
    expireTime?: string;
  }>,
) => {
  return request({
    url: `/api/permissions/modules/user/${userId}/batch-grant-with-expiry`,
    method: 'POST',
    data: { userId, modules },
  });
};
