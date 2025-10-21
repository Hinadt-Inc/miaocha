import { request } from './request';

export interface Module {
  id: number;
  name: string;
  datasourceId: number;
  datasourceName: string;
  tableName: string;
  dorisSql: string;
  status?: number;
  createTime: string;
  updateTime: string;
  createUser: string;
  createUserName: string;
  updateUser: string;
  updateUserName: string;
  users?: {
    userId: string;
    nickname: string;
  }[];
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

export interface QueryConfigKeywordField {
  fieldName: string;
  searchMethod: 'LIKE' | 'MATCH_ALL' | 'MATCH_ANY' | 'MATCH_PHRASE';
}

export interface QueryConfig {
  timeField: string;
  excludeFields?: string[];
  keywordFields: QueryConfigKeywordField[];
}

export interface ModuleQueryConfigParams {
  moduleId: number;
  queryConfig: QueryConfig;
}

export interface ModuleFieldName {
  name: string;
  type: string;
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

export const deleteModule = async (id: number, deleteDorisTable?: boolean) => {
  return request({
    url: `/api/modules/${id}`,
    method: 'DELETE',
    params: {
      deleteDorisTable: deleteDorisTable === true ? 'true' : 'false',
    },
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

// 配置模块查询设置
export const updateModuleQueryConfig = async (params: ModuleQueryConfigParams) => {
  return request({
    url: '/api/modules/query-config',
    method: 'PUT',
    data: params,
  });
};

// 获取模块查询配置
export const getModuleQueryConfig = async (name: string) => {
  return request<QueryConfig>({
    url: '/api/modules/query-config',
    method: 'GET',
    params: { name },
  });
};

// 获取模块字段名列表
export const getModuleFieldNames = async (moduleId: number) => {
  return request<string[]>({
    url: `/api/modules/${moduleId}/field-names`,
    method: 'GET',
  });
};

// 启用/禁用模块状态
export const updateModuleStatus = async (id: number, status: number) => {
  return request({
    url: '/api/modules/status',
    method: 'PUT',
    data: { id, status },
  });
};
