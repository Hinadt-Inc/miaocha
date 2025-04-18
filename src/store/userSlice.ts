import { createSlice, PayloadAction } from '@reduxjs/toolkit'

interface UserState {
  name: string
  isLoggedIn: boolean
}

const initialState: UserState = {
  name: '',
  isLoggedIn: false
}

export const userSlice = createSlice({
  name: 'user',
  initialState,
  reducers: {
    login: (state, action: PayloadAction<string>) => {
      state.name = action.payload
      state.isLoggedIn = true
    },
    logout: (state) => {
      state.name = ''
      state.isLoggedIn = false
    }
  }
})

export const { login, logout } = userSlice.actions
export default userSlice.reducer
