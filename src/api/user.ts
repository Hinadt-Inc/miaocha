import { get, post, request } from './request';
import type { AxiosRequestConfig } from 'axios';

// 用户基础信息类型
export interface User {
  id: string;
  username: string;
  nickname?: string;
  uid?: string;
  email?: string;
  phone?: string;
  role: string;
  status: number;
  createdAt: string;
  updatedAt: string;
  createTime?: string;
  updateTime?: string;
}

// 创建用户参数
export interface CreateUserParams {
  username: string;
  nickname?: string;
  password: string;
  email?: string;
  phone?: string;
  role: string;
  status?: number;
}

// 更新用户参数
export interface UpdateUserParams {
  id?: string;
  username?: string;
  nickname?: string;
  email?: string;
  phone?: string;
  password?: string;
  role?: string;
  status?: number;
}

// 修改密码参数
export interface ChangePasswordParams {
  oldPassword: string;
  newPassword: string;
}

// 获取所有用户
export function getUsers(config?: AxiosRequestConfig): Promise<User[]> {
  return get('/api/users', config);
}

// 创建用户
export function createUser(data: CreateUserParams, config?: AxiosRequestConfig): Promise<User> {
  return post('/api/users', data, config);
}

// 更新用户
export function updateUser(data: UpdateUserParams, config?: AxiosRequestConfig): Promise<User> {
  return request({
    ...config,
    method: 'PUT',
    url: '/api/users',
    data,
  });
}

// 修改用户密码(管理员)
export function changeUserPassword(
  id: string,
  data: { newPassword: string },
  config?: AxiosRequestConfig,
): Promise<void> {
  return request({
    ...config,
    method: 'PUT',
    url: `/api/users/${id}/password`,
    data,
  });
}

// 修改自己密码
export function changeMyPassword(
  data: ChangePasswordParams,
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
export function getUserDetail(id: string, config?: AxiosRequestConfig): Promise<User> {
  return get(`/api/users/${id}`, config);
}

// 删除用户
export function deleteUser(id: string, config?: AxiosRequestConfig): Promise<void> {
  return request({
    ...config,
    method: 'DELETE',
    url: `/api/users/${id}`,
  });
}

// 获取当前用户信息
export function getCurrentUser(config?: AxiosRequestConfig): Promise<User> {
  return get('/api/users/me', config);
}
