import { StrictMode, useEffect } from 'react'
import { createRoot } from 'react-dom/client'
import { Provider, useDispatch } from 'react-redux'
import { QueryProvider } from './providers/QueryProvider'
import { store } from './store/store'
import { RouterProvider } from 'react-router-dom'
import { router } from './routes'
import 'antd/dist/reset.css'
import './index.less'
import { restoreSession } from './store/userSlice'
import type { AppDispatch } from './store/store'

// 会话恢复组件
const SessionInitializer = () => {
  const dispatch = useDispatch<AppDispatch>()
  
  useEffect(() => {
    // 在应用启动时恢复会话
    dispatch(restoreSession())
  }, [dispatch])
  
  return null
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <Provider store={store}>
      <SessionInitializer />
      <QueryProvider>
        <RouterProvider router={router} />
      </QueryProvider>
    </Provider>
  </StrictMode>,
)
