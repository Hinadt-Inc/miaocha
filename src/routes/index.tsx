import { createBrowserRouter } from 'react-router-dom'
import { lazy, Suspense } from 'react'
// import App from '../App'
import HomePage from '../pages/HomePage'
import DashboardPage from '../pages/DashboardPage'
import UserManagementPage from '../pages/system/UserManagementPage'
import DataSourceManagementPage from '../pages/system/DataSourceManagementPage'
import PermissionManagementPage from '../pages/system/PermissionManagementPage'
import LoginPage from '../pages/LoginPage'
import SQLEditorPage from '../pages/SQLEditorPage'

const App = lazy(() => import('../App'))

export const router = createBrowserRouter([
  {
    path: '/login',
    element: <LoginPage />,
  },
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
      {
        path: 'system/datasource',
        element: <DataSourceManagementPage />,
      },
      {
        path: 'system/permission',
        element: <PermissionManagementPage />,
      },
      {
        path: 'sql-editor',
        element: <SQLEditorPage />,
      },
    ],
  },
])
