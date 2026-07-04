import type { RouteObject } from 'react-router-dom'
import { ShellLayout } from '../shell/ShellLayout'
import { ProtectedRoute, requireRole } from './guards'
import { LoginPage } from '../pages/LoginPage'
import { Forbidden } from '../pages/Forbidden'
import { NotFound } from '../pages/NotFound'
import { Overview } from '../pages/Overview'
import { Accounts } from '../pages/Accounts'
import { AccountDetail } from '../pages/AccountDetail'
import { Users } from '../pages/Users'
import { Entitlements } from '../pages/Entitlements'
import { Billing } from '../pages/Billing'
import { Apps } from '../pages/Apps'
import { GdprRights } from '../pages/GdprRights'
import { GdprTicketDetail } from '../pages/GdprTicketDetail'
import { GdprExportDetail } from '../pages/GdprExportDetail'

/**
 * Albero route della console admin: pubbliche `/login` e `/forbidden`; area shell protetta da
 * `requireRole('platform-admin')` con le sei sezioni (UC 0021). Tutto il resto → NotFound.
 */
export const routes: RouteObject[] = [
  { path: '/login', element: <LoginPage /> },
  { path: '/forbidden', element: <Forbidden /> },
  {
    element: <ProtectedRoute guard={requireRole('platform-admin')} />,
    children: [
      {
        element: <ShellLayout />,
        children: [
          { index: true, element: <Overview /> },
          { path: 'accounts', element: <Accounts /> },
          { path: 'accounts/:id', element: <AccountDetail /> },
          { path: 'users', element: <Users /> },
          { path: 'entitlements', element: <Entitlements /> },
          { path: 'billing', element: <Billing /> },
          { path: 'apps', element: <Apps /> },
          { path: 'gdpr', element: <GdprRights /> },
          { path: 'gdpr/tickets/:id', element: <GdprTicketDetail /> },
          { path: 'gdpr/exports/:id', element: <GdprExportDetail /> },
        ],
      },
    ],
  },
  { path: '*', element: <NotFound /> },
]
