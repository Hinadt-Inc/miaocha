import { createBrowserRouter } from 'react-router-dom'
import { lazy, Suspense } from 'react'
// import App from '../App'
import HomePage from '../pages/HomePage'
import DashboardPage from '../pages/DashboardPage'

const App = lazy(() => import('../App'))

export const router = createBrowserRouter([
  {
    path: '/',
    element: (
      <Suspense fallback={<div>Loading...</div>}>
        <App />
      </Suspense>
    ),
    children: [
      {
        index: true,
        element: <HomePage />,
      },
      {
        path: 'dashboard',
        element: <DashboardPage />,
      },
      {
        path: 'system/user',
        element: <UserManagementPage />,
      },
    ],
  },
])
