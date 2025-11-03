import { configureStore } from '@reduxjs/toolkit';

import logReducer from './logSlice';
import { userSlice } from './userSlice';

export const store = configureStore({
  reducer: {
    user: userSlice.reducer,
    log: logReducer,
  },
});

export type AppDispatch = typeof store.dispatch;
export type RootState = ReturnType<typeof store.getState>;
