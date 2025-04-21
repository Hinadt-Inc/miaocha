import { createSlice, PayloadAction } from '@reduxjs/toolkit'

interface AuthTokens {
  accessToken: string
  refreshToken: string
}

interface UserState {
  name: string
  isLoggedIn: boolean
  tokens?: AuthTokens
}

const initialState: UserState = {
  name: '',
  isLoggedIn: false
}

export const userSlice = createSlice({
  name: 'user',
  initialState,
  reducers: {
    login: (state, action: PayloadAction<{name: string; tokens: AuthTokens}>) => {
      state.name = action.payload.name
      state.isLoggedIn = true
      state.tokens = action.payload.tokens
      // 存储token到localStorage
      localStorage.setItem('accessToken', action.payload.tokens.accessToken)
      localStorage.setItem('refreshToken', action.payload.tokens.refreshToken)
    },
    logout: (state) => {
      state.name = ''
      state.isLoggedIn = false
      state.tokens = undefined
      // 清除localStorage中的token
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
    },
    setTokens: (state, action: PayloadAction<AuthTokens>) => {
      state.tokens = action.payload
      // 更新localStorage中的token
      localStorage.setItem('accessToken', action.payload.accessToken)
      localStorage.setItem('refreshToken', action.payload.refreshToken)
    }
  }
})

export const { login, logout, setTokens } = userSlice.actions
export default userSlice.reducer
