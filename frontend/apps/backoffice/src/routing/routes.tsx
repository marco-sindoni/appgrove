import type { RouteObject } from 'react-router-dom'
import { ShellLayout } from '../shell/ShellLayout'
import { ProtectedRoute, AppModuleHost, requireAuth } from './guards'
import { Dashboard } from '../pages/Dashboard'
import { Account } from '../pages/Account'
import { Billing } from '../pages/Billing'
import { Settings } from '../pages/Settings'
import { Login } from '../pages/Login'
import { Forbidden } from '../pages/Forbidden'
import { NotFound } from '../pages/NotFound'

/**
 * Albero route: pubbliche (login/forbidden) + area shell protetta (`requireAuth`) con pagine
 * nested e host dei moduli app **lazy** (`/app/:appId/*`, montati dal registry — #03 dec.8).
 */
export const routes: RouteObject[] = [
  { path: '/login', element: <Login /> },
  { path: '/forbidden', element: <Forbidden /> },
  {
    element: <ProtectedRoute guard={requireAuth} />,
    children: [
      {
        element: <ShellLayout />,
        children: [
          { index: true, element: <Dashboard /> },
          { path: 'account', element: <Account /> },
          { path: 'billing', element: <Billing /> },
          { path: 'settings', element: <Settings /> },
          { path: 'app/:appId/*', element: <AppModuleHost /> },
        ],
      },
    ],
  },
  { path: '*', element: <NotFound /> },
]
