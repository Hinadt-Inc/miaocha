import { get, post } from './request'

interface LoginParams {
  email: string
  password: string
  remember?: boolean
}

interface LoginResponse {
    expiresAt: number
    nickname: string
    refreshExpiresAt: number
    refreshToken: string
    role: string
    token: string
    userId: number
}

interface RefreshTokenParams {
  refreshToken: string
}

interface RefreshTokenResponse {
  accessToken: string
  refreshToken: string
}

// 添加用户信息接口
interface UserInfoResponse {
  userId: number
  nickname: string
  email: string
  role: string
  avatar?: string
  createdAt: string
  lastLoginAt: string
}

export const login = (data: LoginParams) => {
  return post<LoginResponse>('/api/auth/login', data)
}

export const refreshToken = (data: RefreshTokenParams) => {
  return post<RefreshTokenResponse>('/api/auth/refresh', data)
}

// 添加获取用户信息API
export const getUserInfo = () => {
  return get<UserInfoResponse>('/api/users/me')
}

// 添加登出API
export const logout = () => {
  return post('/api/auth/logout')
}
