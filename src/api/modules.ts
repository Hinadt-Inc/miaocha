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
