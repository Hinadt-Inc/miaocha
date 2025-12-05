import type { AxiosRequestConfig } from 'axios';

import type {
  UserListItem,
  CreateUserPayload,
  UpdateUserPayload,
  ChangePasswordPayloadBySuperAdmin,
} from '@/pages/System/User/types';

import { get, post, request } from './request';

// 获取所有用户
export function getUsers(config?: AxiosRequestConfig): Promise<UserListItem[]> {
  return get('/api/users', config);
}

// 创建用户
export function createUser(data: CreateUserPayload, config?: AxiosRequestConfig): Promise<UserListItem> {
  return post('/api/users', data, config);
}

// 更新用户
export function updateUser(data: UpdateUserPayload, config?: AxiosRequestConfig): Promise<UserListItem> {
  return request({
    ...config,
    method: 'PUT',
    url: '/api/users',
    data,
  });
}

// 修改用户密码(管理员)
export function changePasswordByAdmin(id: number, newPassword: string, config?: AxiosRequestConfig): Promise<void> {
  return request({
    ...config,
    method: 'PUT',
    url: `/api/users/${id}/password`,
    data: {
      password: newPassword,
    },
  });
}

// 修改自己密码
export function changePasswordBySuperAdmin(
  data: ChangePasswordPayloadBySuperAdmin,
  config?: AxiosRequestConfig,
): Promise<void> {
  return request({
    ...config,
    method: 'PUT',
    url: '/api/users/password',
    data,
  });
}

// 获取用户详情
export function getUserDetail(id: number, config?: AxiosRequestConfig): Promise<UserListItem> {
  return get(`/api/users/${id}`, config);
}

// 删除用户
export function deleteUser(id: number, config?: AxiosRequestConfig): Promise<void> {
  return request({
    ...config,
    method: 'DELETE',
    url: `/api/users/${id}`,
  });
}

// 获取当前用户信息
export function getCurrentUser(config?: AxiosRequestConfig): Promise<UserListItem> {
  return get('/api/users/me', config);
}

// 撤销模块授权
export const revokeModule = async (userId: number, moduleName: string) => {
  return request({
    url: `/api/permissions/modules/user/${userId}/revoke?module=${moduleName}`,
    method: 'DELETE',
  });
};

// 授权模块
export const authorizeModule = async (userId: number, moduleName: string) => {
  return request({
    url: `/api/permissions/modules/user/${userId}/grant?module=${moduleName}`,
    method: 'POST',
  });
};

// 批量授权
export const batchAuthorizeModules = async (userId: number, moduleNames: string[]) => {
  return request({
    url: `/api/permissions/modules/user/${userId}/batch-grant`,
    method: 'POST',
    data: { userId, modules: moduleNames },
  });
};

// 批量撤销
export const batchRevokeModules = async (userId: number, moduleNames: string[]) => {
  return request({
    url: `/api/permissions/modules/user/${userId}/batch-revoke`,
    method: 'DELETE',
    data: { userId, modules: moduleNames },
  });
};
