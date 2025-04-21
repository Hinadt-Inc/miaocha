import { post } from './request'

interface LoginParams {
  email: string
  password: string
  remember?: boolean
}

interface LoginResponse {
  name: string
  tokens: {
    accessToken: string
    refreshToken: string
  }
}

interface RefreshTokenParams {
  refreshToken: string
}

interface RefreshTokenResponse {
  accessToken: string
  refreshToken: string
}

export const login = (data: LoginParams) => {
  return post<LoginResponse>('/api/auth/login', data)
}

export const refreshToken = (data: RefreshTokenParams) => {
  return post<RefreshTokenResponse>('/api/auth/refresh', data)
}
