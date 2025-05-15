export {};
declare global {
  // 登录参数
  interface ILoginParams {
    email: string;
    password: string;
  }

  // 登录，刷新token
  interface ILoginResponse {
    expiresAt: number;
    nickname: string;
    refreshExpiresAt: number;
    refreshToken: string;
    role: string;
    token: string;
    userId: number;
  }

  // 刷新token
  interface IRefreshTokenParams {
    refreshToken: string;
  }

  // 用户信息
  interface IUserInfoResponse {
    createTime: string;
    email: string;
    id: number;
    nickname: string;
    role: string;
    status: number;
    uid: string;
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
