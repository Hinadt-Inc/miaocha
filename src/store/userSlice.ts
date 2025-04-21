import { createSlice, PayloadAction } from '@reduxjs/toolkit'

interface AuthTokens {
  accessToken: string
  refreshToken: string
  expiresAt: number
  refreshExpiresAt: number
}

interface UserState {
  userId: number
  name: string
  role: string
  isLoggedIn: boolean
  tokens?: AuthTokens
}

const initialState: UserState = {
  userId: 0,
  name: '',
  role: '',
  isLoggedIn: false
}

export const userSlice = createSlice({
  name: 'user',
  initialState,
  reducers: {
    login: (state, action: PayloadAction<{
      userId: number
      name: string
      role: string
      tokens: AuthTokens
    }>) => {
      state.userId = action.payload.userId
      state.name = action.payload.name
      state.role = action.payload.role
      state.isLoggedIn = true
      state.tokens = action.payload.tokens
      // 存储token到localStorage
      localStorage.setItem('accessToken', action.payload.tokens.accessToken)
      localStorage.setItem('refreshToken', action.payload.tokens.refreshToken)
      localStorage.setItem('tokenExpiresAt', action.payload.tokens.expiresAt.toString())
      localStorage.setItem('refreshTokenExpiresAt', action.payload.tokens.refreshExpiresAt.toString())
    },
    logout: (state) => {
      state.userId = 0
      state.name = ''
      state.role = ''
      state.isLoggedIn = false
      state.tokens = undefined
      // 清除localStorage中的token
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('tokenExpiresAt')
      localStorage.removeItem('refreshTokenExpiresAt')
    },
    setTokens: (state, action: PayloadAction<AuthTokens>) => {
      state.tokens = action.payload
      // 更新localStorage中的token
      localStorage.setItem('accessToken', action.payload.accessToken)
      localStorage.setItem('refreshToken', action.payload.refreshToken)
      localStorage.setItem('tokenExpiresAt', action.payload.expiresAt.toString())
      localStorage.setItem('refreshTokenExpiresAt', action.payload.refreshExpiresAt.toString())
    }
  }
})

export const { login, logout, setTokens } = userSlice.actions
export default userSlice.reducer
