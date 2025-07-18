import { get, post } from './request';

// 用户登录接口
export const login = (data: ILoginParams) => {
  return post<ILoginResponse>('/api/auth/login', data);
};

// 刷新token接口
export const refreshToken = (data: IRefreshTokenParams) => {
  return post<ILoginResponse>('/api/auth/refresh', data);
};

// 获取当前用户信息接口
export const getUserInfo = () => {
  return get<IUserInfoResponse>('/api/users/me');
};

// 用户登出接口
export const logout = () => {
  return post('/api/auth/logout');
};
