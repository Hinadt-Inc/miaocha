export {};
declare global {
  // 登录参数
  interface ILoginParams {
    email: string;
    password: string;
  }

  // 登录，刷新token
  interface ILoginResponse {
    userId: number;
    nickname: string;
    token: string;
    refreshToken: string;
    expiresAt: number;
    refreshExpiresAt: number;
    role: string;
  }

  // 刷新token
  interface IRefreshTokenParams {
    refreshToken: string;
  }

  // 用户信息
  interface IUserInfoResponse {
    id: number;
    nickname: string;
    email: string;
    uid: string;
    role: string;
    status: number;
    createTime: string;
    updateTime: string;
  }

  interface ITokens {
    accessToken: string;
    refreshToken: string;
    expiresAt: number;
    refreshExpiresAt: number;
  }

  interface IStoreUser {
    userId: number;
    name: string;
    email: string;
    role: string;
    avatar?: string;
    createTime?: string;
    isLoggedIn: boolean;
    tokens?: ITokens;
    loading: boolean;
    error: string | null;
    sessionChecked: boolean;
  }
}
