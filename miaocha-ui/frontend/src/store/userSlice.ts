import { createSlice, PayloadAction, createAsyncThunk } from '@reduxjs/toolkit';

import { getUserInfo } from '@/api/auth';

const initialState: IStoreUser = {
  userId: 0,
  name: '',
  email: '',
  role: '',
  createTime: '',
  isLoggedIn: false,
  loading: false,
  error: null,
  sessionChecked: false,
  loginType: undefined,
};

// 异步获取用户信息
export const fetchUserInfo: any = createAsyncThunk('user/fetchUserInfo', async (_, { rejectWithValue }) => {
  try {
    const response = await getUserInfo();
    return response;
  } catch (error: unknown) {
    console.error('获取用户信息失败:', error);
    const message = error instanceof Error ? error.message : '获取用户信息失败';
    return rejectWithValue(message);
  }
});

// 异步退出登录
export const logoutUser = createAsyncThunk('user/logoutUser', () => {
  try {
    // 不再调用后端logout接口，直接返回成功
    localStorage.clear();
    return true;
  } catch (error: unknown) {
    console.error('退出登录失败:', error);
    // 即使退出 API 失败，我们也清理本地状态
    return true;
  }
});

// 清除令牌的辅助函数
const clearTokens = () => {
  const tokenKeys = ['accessToken', 'refreshToken', 'tokenExpiresAt', 'refreshTokenExpiresAt', 'loginType'];
  tokenKeys.forEach((key) => localStorage.removeItem(key));
};

// 获取令牌的辅助函数
const getTokens = (): ITokens | null => {
  const accessToken = localStorage.getItem('accessToken');
  const refreshToken = localStorage.getItem('refreshToken');
  const tokenExpiresAt = localStorage.getItem('tokenExpiresAt');
  const refreshTokenExpiresAt = localStorage.getItem('refreshTokenExpiresAt');

  if (!accessToken || !refreshToken || !tokenExpiresAt || !refreshTokenExpiresAt) {
    return null;
  }

  return {
    accessToken,
    refreshToken,
    expiresAt: parseInt(tokenExpiresAt),
    refreshExpiresAt: parseInt(refreshTokenExpiresAt),
  };
};

// 检查令牌是否有效
const isTokenValid = (tokens: ITokens): boolean => {
  const now = Date.now();
  return tokens.refreshExpiresAt > now;
};

export const restoreSession = createAsyncThunk('user/restoreSession', async (_, { dispatch }) => {
  try {
    // 获取并验证令牌
    const tokens = getTokens();
    if (!tokens || !isTokenValid(tokens)) {
      clearTokens();
      return { restored: false };
    }

    // 获取loginType
    const loginType = localStorage.getItem('loginType');

    // 恢复登录状态
    dispatch({
      type: 'user/setTokensAndLogin',
      payload: tokens,
    });

    // 恢复loginType
    if (loginType) {
      dispatch({
        type: 'user/updateUserInfo',
        payload: { loginType },
      });
    }

    // 如果不是登录页，获取用户信息
    if (window.location.pathname !== '/login') {
      try {
        const userInfo = await dispatch(fetchUserInfo()).unwrap();
        return { restored: true, userInfo };
      } catch (error) {
        console.error('恢复会话获取用户信息失败:', error);
        clearTokens();
        window.location.href = '/login';
        return { restored: false, error };
      }
    }

    return { restored: false };
  } catch (error) {
    console.error('恢复会话异常:', error);
    return { restored: false, error };
  }
});

export const userSlice: any = createSlice({
  name: 'user',
  initialState,
  reducers: {
    login: (
      state,
      action: PayloadAction<{
        userId: number;
        name: string;
        role: string;
        tokens: ITokens;
        loginType?: string;
      }>,
    ) => {
      state.userId = action.payload.userId;
      state.name = action.payload.name;
      state.role = action.payload.role;
      state.isLoggedIn = true;
      state.tokens = action.payload.tokens;
      state.sessionChecked = true;
      state.loginType = action.payload.loginType;
      // 存储token到localStorage
      localStorage.setItem('accessToken', action.payload.tokens.accessToken);
      localStorage.setItem('refreshToken', action.payload.tokens.refreshToken);
      localStorage.setItem('tokenExpiresAt', action.payload.tokens.expiresAt.toString());
      localStorage.setItem('refreshTokenExpiresAt', action.payload.tokens.refreshExpiresAt.toString());
      // 存储loginType到localStorage
      if (action.payload.loginType) {
        localStorage.setItem('loginType', action.payload.loginType);
      }
    },
    logout: (state) => {
      state.userId = 0;
      state.name = '';
      state.email = '';
      state.role = '';
      state.avatar = undefined;
      state.createTime = undefined;
      state.isLoggedIn = false;
      state.tokens = undefined;
      state.sessionChecked = true;
      state.loginType = undefined;
      // 清除localStorage中的token和loginType
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('tokenExpiresAt');
      localStorage.removeItem('refreshTokenExpiresAt');
      localStorage.removeItem('loginType');
      localStorage.removeItem('siderCollapsed');
    },
    setTokens: (state, action: PayloadAction<ITokens>) => {
      state.tokens = action.payload;
      // 更新localStorage中的token
      localStorage.setItem('accessToken', action.payload.accessToken);
      localStorage.setItem('refreshToken', action.payload.refreshToken);
      localStorage.setItem('tokenExpiresAt', action.payload.expiresAt.toString());
      localStorage.setItem('refreshTokenExpiresAt', action.payload.refreshExpiresAt.toString());
    },
    setTokensAndLogin: (state, action: PayloadAction<ITokens>) => {
      state.tokens = action.payload;
      state.isLoggedIn = true;
    },
    updateUserInfo: (state, action: PayloadAction<Partial<IStoreUser>>) => {
      return { ...state, ...action.payload };
    },
  },
  extraReducers: (builder) => {
    builder
      // 处理获取用户信息
      .addCase(fetchUserInfo.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchUserInfo.fulfilled, (state, action) => {
        state.loading = false;
        state.userId = action.payload.id;
        state.name = action.payload.nickname;
        state.email = action.payload.email;
        state.role = action.payload.role;
        state.avatar = action.payload.avatar;
        state.createTime = action.payload.createTime;
        state.isLoggedIn = true;
      })
      .addCase(fetchUserInfo.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload as string;
        // 不再自动设置为未登录状态
        // 用户可能仍处于登录状态，只是获取信息失败
      })
      // 处理退出登录
      .addCase(logoutUser.fulfilled, (state) => {
        state.userId = 0;
        state.name = '';
        state.email = '';
        state.role = '';
        state.avatar = undefined;
        state.createTime = undefined;
        state.isLoggedIn = false;
        state.tokens = undefined;
        state.sessionChecked = true;
        state.loginType = undefined;
        // 清除localStorage中的token和loginType
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('tokenExpiresAt');
        localStorage.removeItem('refreshTokenExpiresAt');
        localStorage.removeItem('loginType');
      })
      // 处理恢复会话
      .addCase(restoreSession.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(restoreSession.fulfilled, (state, action) => {
        state.loading = false;
        state.sessionChecked = true;

        // 只有在明确恢复失败时才设置为未登录
        if (!action.payload.restored) {
          state.isLoggedIn = false;
        }

        // 恢复成功但获取信息失败时，保持登录状态
        if (action.payload.restored && action.payload.error) {
          state.error = '获取用户信息失败，请刷新页面重试';
        }
      })
      .addCase(restoreSession.rejected, (state) => {
        state.loading = false;
        // 不再自动登出用户
        state.sessionChecked = true;
        state.error = '会话恢复失败，但不影响您的登录状态';
      });
  },
});

export const { login, logout, setTokens, setTokensAndLogin, updateUserInfo } = userSlice.actions;
export default userSlice.reducer;
